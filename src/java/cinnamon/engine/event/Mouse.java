package cinnamon.engine.event;

import cinnamon.engine.utils.*;
import cinnamon.engine.utils.IntMap.IntWrapper;
import org.lwjgl.glfw.GLFW;

/**
 * <p>An event-based representation of the user's mouse. Updating the mouse's state requires updating the table of
 * events provided by the {@code Mouse}'s {@link Mouse.State}. Each {@code Mouse} instance's state is controlled by
 * the {@code Mouse.State} provided during instantiation.</p>
 */
public final class Mouse implements EventSilenceable
{
    /**
     * <p>Mouse button constants.</p>
     */
    public enum Button implements IntWrapper
    {
        /**
         * <p>Left mouse button.</p>
         */
        LEFT(GLFW.GLFW_MOUSE_BUTTON_LEFT),
        /**
         * <p>Right mouse button.</p>
         */
        RIGHT(GLFW.GLFW_MOUSE_BUTTON_RIGHT),
        /**
         * <p>Middle mouse button.</p>
         */
        MIDDLE(GLFW.GLFW_MOUSE_BUTTON_MIDDLE);

        /**
         * <p>Number of buttons.</p>
         */
        public static final int COUNT = Button.values().length;

        // Mapping between buttons and lower level constants
        private static final IntMap<Button> MAPPING = new SparseEnumIntMap<>(Button.class);

        // Lower level constants
        private final int mConstant;

        Button(int constant)
        {
            mConstant = constant;
        }

        @Override
        public int toInt()
        {
            return mConstant;
        }

        /**
         * <p>Gets the {@code Button} equivalent of a lower level constant.</p>
         *
         * @param constant lower level constant.
         * @return button or null if unrecognized.
         */
        public static Button from(int constant)
        {
            return MAPPING.get(constant);
        }
    }

    // Event histories and position on screen
    private final State mState;

    // True if events should be ignored
    private boolean mMute;

    /**
     * <p>Constructs a {@code Mouse}.</p>
     *
     * @param state state.
     * @throws NullPointerException if state is null.
     */
    public Mouse(State state)
    {
        checkNull(state);

        mState = state;
    }

    /**
     * <p>Checks if the most recent event for a button is a press.</p>
     *
     * @param button button.
     * @return true if pressed.
     * @throws NullPointerException if button is null.
     */
    public boolean isPressed(Button button)
    {
        checkNull(button);

        return PressChecker.isPressed(button, mState.getPressHistory(), mState.getReleaseHistory());
    }

    /**
     * <p>Gets the mouse' position.</p>
     *
     * <p>Changes to the returned {@code Point} do not affect the actual position.</p>
     *
     * @return position.
     */
    public Point getPosition()
    {
        return new Point(mState.getPosition());
    }

    /**
     * <p>Gets the scroll offset along the x axis.</p>
     *
     * @return horizontal scroll.
     */
    public float getHorizontalScroll()
    {
        final MouseEvent event = mState.getScrollHistory().get(0, 0);
        return (event == null) ? 0f : event.getHorizontal();
    }

    /**
     * <p>Gets the scroll offset along the y axis.</p>
     *
     * @return vertical scroll.
     */
    public float getVerticalScroll()
    {
        final MouseEvent event = mState.getScrollHistory().get(0, 0);
        return (event == null) ? 0f : event.getVertical();
    }

    @Override
    public boolean isMuted()
    {
        return mMute;
    }

    @Override
    public void mute()
    {
        mMute = true;
    }

    @Override
    public void unmute()
    {
        mMute = false;
    }

    @Override
    public String toString()
    {
        final String format = "%s(@(%.2f,%.2f))";
        final String name = getClass().getSimpleName();
        final Point pos = getPosition();

        return String.format(format, name, pos.getX(), pos.getY());
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }

    private static void checkNull(Object object)
    {
        if (object == null) {
            throw new NullPointerException();
        }
    }

    /**
     * <p>{@code Mouse.State} allows changing the {@code Mouse}'s position and updating its press, release, and
     * scroll events.</p>
     *
     * <p>{@code Mouse.State}s are constructed with a guided builder.</p>
     * <pre>
     *     <code>
     *         Mouse.State state = Mouse.State.builder()
     *             .pressHistory(presses)
     *             .releaseHistory(releases)
     *             .scrollHistory(scrolls)
     *             .position(position)
     *             .build();
     *     </code>
     * </pre>
     */
    public static class State
    {
        // Press event history
        private final FixedQueueArray<MouseEvent> mPresses;

        // Release event history
        private final FixedQueueArray<MouseEvent> mReleases;

        // Scroll event history
        private final FixedQueueArray<MouseEvent> mScrolls;

        // Mouse position
        private final Point mPosition;

        private State(FixedQueueArray<MouseEvent> pressHistory, FixedQueueArray<MouseEvent> releaseHistory,
                      FixedQueueArray<MouseEvent> scrollHistory, Point position)
        {
            mPresses = pressHistory;
            mReleases = releaseHistory;
            mScrolls = scrollHistory;
            mPosition = position;
        }

        /**
         * <p>Gets the editable history for press events.</p>
         *
         * @return editable press history.
         */
        public FixedQueueArray<MouseEvent> getPressHistory()
        {
            return mPresses;
        }

        /**
         * <p>Gets the editable history for release events.</p>
         *
         * @return editable release history.
         */
        public FixedQueueArray<MouseEvent> getReleaseHistory()
        {
            return mReleases;
        }

        /**
         * <p>Gets the editable history for scroll events.</p>
         *
         * @return editable scroll history.
         */
        public FixedQueueArray<MouseEvent> getScrollHistory()
        {
            return mScrolls;
        }

        /**
         * <p>Gets the editable position.</p>
         *
         * @return editable position.
         */
        public Point getPosition()
        {
            return mPosition;
        }

        @Override
        protected final Object clone() throws CloneNotSupportedException
        {
            throw new CloneNotSupportedException();
        }

        /**
         * <p>Begins the process to build a {@code Mouse.State}.</p>
         *
         * @return first build step.
         */
        public static PressHistoryStep builder()
        {
            return new StepBuilder();
        }

        private static class StepBuilder implements PressHistoryStep, ReleaseHistoryStep, ScrollHistoryStep,
                PositionStep, BuildStep
        {
            private FixedQueueArray<MouseEvent> mPressHistory;
            private FixedQueueArray<MouseEvent> mReleaseHistory;
            private FixedQueueArray<MouseEvent> mScrollHistory;
            private Point mPosition;

            @Override
            public ReleaseHistoryStep pressHistory(FixedQueueArray<MouseEvent> history)
            {
                checkNull(history);

                mPressHistory = history;
                return this;
            }

            @Override
            public ScrollHistoryStep releaseHistory(FixedQueueArray<MouseEvent> history)
            {
                checkNull(history);

                mReleaseHistory = history;
                return this;
            }

            @Override
            public PositionStep scrollHistory(FixedQueueArray<MouseEvent> history)
            {
                checkNull(history);

                mScrollHistory = history;
                return this;
            }

            @Override
            public BuildStep position(Point position)
            {
                checkNull(position);

                mPosition = position;
                return this;
            }

            @Override
            public State build()
            {
                return new State(mPressHistory, mReleaseHistory, mScrollHistory, mPosition);
            }
        }

        public interface PressHistoryStep
        {
            /**
             * <p>Sets the mouse's button press event history.</p>
             *
             * @param history press history.
             * @return next step.
             * @throws NullPointerException if history is null.
             */
            ReleaseHistoryStep pressHistory(FixedQueueArray<MouseEvent> history);
        }

        public interface ReleaseHistoryStep
        {
            /**
             * <p>Sets the mouse's button release event history.</p>
             *
             * @param history release history.
             * @return next step.
             * @throws NullPointerException if history is null.
             */
            ScrollHistoryStep releaseHistory(FixedQueueArray<MouseEvent> history);
        }

        public interface ScrollHistoryStep
        {
            /**
             * <p>Sets the mouse's scroll event history.</p>
             *
             * @param history scroll history.
             * @return next step.
             * @throws NullPointerException if history is null.
             */
            PositionStep scrollHistory(FixedQueueArray<MouseEvent> history);
        }

        public interface PositionStep
        {
            /**
             * <p>Sets the mouse's position.</p>
             *
             * @param position position.
             * @return next step.
             * @throws NullPointerException if position is null.
             */
            BuildStep position(Point position);
        }

        public interface BuildStep
        {
            /**
             * <p>Creates the {@code Mouse.State}.</p>
             *
             * @return mouse state.
             */
            State build();
        }
    }
}
