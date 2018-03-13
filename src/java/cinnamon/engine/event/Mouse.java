package cinnamon.engine.event;

import cinnamon.engine.utils.*;
import cinnamon.engine.utils.IntMap.IntWrapper;
import org.lwjgl.glfw.GLFW;

/**
 * <p>{@code Mouse} represents the mouse input device, providing a view of the cursor's position, scroll offsets, and
 * button states. Updates must be set through the {@code Mouse.State} passed to the {@code Mouse}'s constructor.</p>
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
         * <p>Button count.</p>
         */
        public static final int COUNT = Button.values().length;

        private static final IntMap<Button> MAPPING = new SparseEnumIntMap<>(Button.class);

        // GLFW button
        private final int mButton;

        Button(int glfw)
        {
            mButton = glfw;
        }

        @Override
        public int toInt()
        {
            return mButton;
        }

        /**
         * <p>Gets the {@code Button} equivalent of a GLFW mouse button constant.</p>
         *
         * @param glfw GLFW button.
         * @return button, or null if unrecognized.
         */
        public static Button from(int glfw)
        {
            return MAPPING.get(glfw);
        }
    }

    private static final PressCondition<MouseEvent> mPressCondition = new PressCondition<>();

    private final State mState;
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
     * <p>Checks if the most recent event for the given button is a press.</p>
     *
     * @param button button.
     * @return true if pressed.
     * @throws NullPointerException if button is null.
     */
    public boolean isPressed(Button button)
    {
        checkNull(button);

        return mPressCondition.isPressed(button, mState.mPresses, mState.mReleases);
    }

    /**
     * <p>Gets the mouse' position.</p>
     *
     * <p>Changes to the returned {@code Point} does not affect the current position.</p>
     *
     * @return position.
     */
    public Point getPosition()
    {
        return new Point(mState.mPt);
    }

    /**
     * <p>Gets the scroll offset along the x axis.</p>
     *
     * @return horizontal scroll.
     */
    public float getHorizontalScroll()
    {
        return mState.getHorizontalScrollOffset();
    }

    /**
     * <p>Gets the scroll offset along the y axis.</p>
     *
     * @return vertical scroll.
     */
    public float getVerticalScroll()
    {
        return mState.getVerticalScrollOffset();
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

    private static void checkNull(Object object)
    {
        if (object == null) {
            throw new NullPointerException();
        }
    }

    /**
     * <p>{@code Mouse.State} allows changing the {@code Mouse}'s position, scroll offsets, and event histories.</p>
     */
    public static class State
    {
        private final Table<MouseEvent> mPresses;
        private final Table<MouseEvent> mReleases;

        private final Point mPt = new Point();

        private float mScrollH = 0f;
        private float mScrollV = 0f;

        /**
         * <p>Constructs a {@code Mouse.State}.</p>
         *
         * @param pressHistory press event history.
         * @param releaseHistory release event history.
         * @throws NullPointerException if pressHistory or releaseHistory is null.
         */
        public State(Table<MouseEvent> pressHistory, Table<MouseEvent> releaseHistory)
        {
            checkNull(pressHistory);
            checkNull(releaseHistory);

            mPresses = pressHistory;
            mReleases = releaseHistory;
        }

        /**
         * <p>Gets the position.</p>
         *
         * @return position.
         */
        public Point getPosition()
        {
            return mPt;
        }

        /**
         * <p>Sets the position.</p>
         *
         * @param x x.
         * @param y y.
         */
        public void setPosition(float x, float y)
        {
            mPt.setX(x);
            mPt.setY(y);
        }

        /**
         * <p>Gets the horizontal scroll offset.</p>
         *
         * @return horizontal scroll offset.
         */
        public float getHorizontalScrollOffset()
        {
            return mScrollH;
        }

        /**
         * <p>Sets the horizontal scroll offset.</p>
         *
         * @param offset horizontal scroll offset.
         */
        public void setHorizontalScrollOffset(float offset)
        {
            mScrollH = offset;
        }

        /**
         * <p>Gets the vertical scroll offset.</p>
         *
         * @return vertical scroll offset.
         */
        public float getVerticalScrollOffset()
        {
            return mScrollV;
        }

        /**
         * <p>Sets the vertical scroll offset.</p>
         *
         * @param offset vertical scroll offset.
         */
        public void setVerticalScrollOffset(float offset)
        {
            mScrollV = offset;
        }

        @Override
        protected final Object clone() throws CloneNotSupportedException
        {
            throw new CloneNotSupportedException();
        }
    }
}
