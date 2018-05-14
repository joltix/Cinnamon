package cinnamon.engine.event;

import cinnamon.engine.event.Gamepad.Axis;
import cinnamon.engine.event.Gamepad.AxisWrapper;
import cinnamon.engine.event.Gamepad.ButtonWrapper;

import java.util.EnumMap;
import java.util.Map;

/**
 * <p>Configuration information for PlayStation 4 controllers.</p>
 *
 * <p><b>note</b> This configuration has only been tested with a first-party PS4 controller.</p>
 */
public final class PS4
{
    private PS4() { }

    /**
     * <p>Playstation 4 button constants. Each {@code Button} acts as an alias wrapping a {@link Gamepad.Button}.</p>
     */
    public enum Button implements ButtonWrapper
    {
        /**
         * <p>□.</p>
         */
        SQUARE(Gamepad.Button.BUTTON_0, "SQUARE"),

        /**
         * <p>X.</p>
         */
        X(Gamepad.Button.BUTTON_1, "X"),

        /**
         * <p>O.</p>
         */
        CIRCLE(Gamepad.Button.BUTTON_2, "CIRCLE"),

        /**
         * <p>△.</p>
         */
        TRIANGLE(Gamepad.Button.BUTTON_3, "TRIANGLE"),

        /**
         * <p>L1.</p>
         */
        L1(Gamepad.Button.BUTTON_4, "L1"),

        /**
         * <p>R1.</p>
         */
        R1(Gamepad.Button.BUTTON_5, "R1"),

        /**
         * <p>L2.</p>
         */
        L2(Gamepad.Button.BUTTON_6, "L2"),

        /**
         * <p>R2.</p>
         */
        R2(Gamepad.Button.BUTTON_7, "R2"),

        /**
         * <p>Share.</p>
         */
        SHARE(Gamepad.Button.BUTTON_8, "SHARE"),

        /**
         * <p>Options.</p>
         */
        OPTIONS(Gamepad.Button.BUTTON_9, "OPTIONS"),

        /**
         * <p>Left analog stick.</p>
         */
        L3(Gamepad.Button.BUTTON_10, "L3"),

        /**
         * <p>Right analog stick.</p>
         */
        R3(Gamepad.Button.BUTTON_11, "R3"),

        /**
         * <p>PlayStation logo.</p>
         */
        PS(Gamepad.Button.BUTTON_12, "PS"),

        /**
         * <p>Touchpad.</p>
         */
        TOUCH_PAD(Gamepad.Button.BUTTON_13, "TOUCH PAD"),

        /**
         * <p>Up on the directional pad.</p>
         */
        UP(Gamepad.Button.BUTTON_14, "UP"),

        /**
         * <p>Right on the directional pad.</p>
         */
        RIGHT(Gamepad.Button.BUTTON_15, "RIGHT"),

        /**
         * <p>Down on the directional pad.</p>
         */
        DOWN(Gamepad.Button.BUTTON_16, "DOWN"),

        /**
         * <p>Left on the directional pad.</p>
         */
        LEFT(Gamepad.Button.BUTTON_17, "LEFT");

        /**
         * <p>Number of buttons.</p>
         */
        public static final int COUNT = Button.values().length;

        // Underlying identity
        private final Gamepad.Button mButton;

        // Text representation
        private final String mText;

        Button(Gamepad.Button button, String text)
        {
            mButton = button;
            mText = text;
        }

        @Override
        public Gamepad.Button button()
        {
            return mButton;
        }

        @Override
        public String toString()
        {
            return mText;
        }
    }

    /**
     * <p>PlayStation 4 axis-based sensor constants. {@code Stick}s wrap the {@link Gamepad.Axis} constant(s) and
     * refer to those sensors which report a value between -1 and +1, inclusive. The gamepad's triggers are also
     * contained within these constants, albeit with only the vertical axis assigned.</p>
     */
    public enum Stick implements AxisWrapper
    {
        /**
         * <p>Left analog stick.</p>
         */
        LEFT_STICK(Axis.AXIS_0, Axis.AXIS_1, "L-STICK"),

        /**
         * <p>Right analog stick.</p>
         */
        RIGHT_STICK(Axis.AXIS_2, Axis.AXIS_5, "R-STICK"),

        /**
         * <p>Left trigger.</p>
         */
        LEFT_TRIGGER(null, Axis.AXIS_3, "L2"),

        /**
         * <p>Right trigger.</p>
         */
        RIGHT_TRIGGER(null, Axis.AXIS_4, "R2");

        /**
         * <p>Number of axis-based sensors.</p>
         */
        public static final int COUNT = Stick.values().length;

        // X axis
        private final Axis mHorizontal;

        // Y axis
        private final Axis mVertical;

        // Text representation
        private final String mText;

        Stick(Axis horizontal, Axis vertical, String text)
        {
            mVertical = vertical;
            mHorizontal = horizontal;
            mText = text;
        }

        @Override
        public Axis vertical()
        {
            return mVertical;
        }

        @Override
        public Axis horizontal()
        {
            return mHorizontal;
        }

        @Override
        public String toString()
        {
            return mText;
        }
    }

    /**
     * <p>Driver reported name for the PlayStation 4 gamepad.</p>
     */
    public static final String GAMEPAD_NAME = "Wireless Controller";

    /**
     * <p>PlayStation gamepad configuration.</p>
     */
    public static final PadProfile GAMEPAD_PROFILE = new PS4PadProfile();

    private static class PS4PadProfile extends PadProfile
    {
        private PS4PadProfile()
        {
            super(Button.class, Stick.class, PS4PadProfile.createRestingMap());
        }

        private static Map<Axis, Float> createRestingMap()
        {
            final Map<Axis, Float> resting = new EnumMap<>(Axis.class);

            // Left analog stick
            resting.put(Axis.AXIS_0, 0f);
            resting.put(Axis.AXIS_1, 0f);

            // Right analog stick
            resting.put(Axis.AXIS_2, 0f);
            resting.put(Axis.AXIS_5, 0f);

            // Left trigger
            resting.put(Axis.AXIS_3, -1f);

            // Right trigger
            resting.put(Axis.AXIS_4, -1f);

            return resting;
        }
    }
}
