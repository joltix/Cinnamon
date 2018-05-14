package cinnamon.engine.event;

import cinnamon.engine.event.Gamepad.Axis;
import cinnamon.engine.event.Gamepad.AxisWrapper;
import cinnamon.engine.event.Gamepad.ButtonWrapper;

import java.util.EnumMap;
import java.util.Map;

/**
 * <p>Configuration information for Xbox One controllers.</p>
 *
 * <p><b>note</b> This configuration has only been tested with a first-party XB1 controller.</p>
 */
public final class XB1
{
    private XB1() { }

    /**
     * <p>Xbox One button constants. Each {@code Button} acts as an alias wrapping a {@link Gamepad.Button}.</p>
     */
    public enum Button implements ButtonWrapper
    {
        /**
         * <p>A.</p>
         */
        A(Gamepad.Button.BUTTON_0, "A"),

        /**
         * <p>B.</p>
         */
        B(Gamepad.Button.BUTTON_1, "B"),

        /**
         * <p>X.</p>
         */
        X(Gamepad.Button.BUTTON_2, "X"),

        /**
         * <p>Y.</p>
         */
        Y(Gamepad.Button.BUTTON_3, "Y"),

        /**
         * <p>Left bumper.</p>
         */
        LEFT_BUMPER(Gamepad.Button.BUTTON_4, "LB"),

        /**
         * <p>Right bumper.</p>
         */
        RIGHT_BUMPER(Gamepad.Button.BUTTON_5, "RB"),

        /**
         * <p>View.</p>
         */
        VIEW(Gamepad.Button.BUTTON_6, "VIEW"),

        /**
         * <p>Menu.</p>
         */
        MENU(Gamepad.Button.BUTTON_7, "MENU"),

        /**
         * <p>Left analog stick.</p>
         */
        LS(Gamepad.Button.BUTTON_8, "LS"),

        /**
         * <p>Right analog stick.</p>
         */
        RS(Gamepad.Button.BUTTON_9, "RS"),

        /**
         * <p>Up on the directional pad.</p>
         */
        UP(Gamepad.Button.BUTTON_10, "UP"),

        /**
         * <p>Right on the directional pad.</p>
         */
        RIGHT(Gamepad.Button.BUTTON_11, "RIGHT"),

        /**
         * <p>Down on the directional pad.</p>
         */
        DOWN(Gamepad.Button.BUTTON_12, "DOWN"),

        /**
         * <p>Left on the directional pad.</p>
         */
        LEFT(Gamepad.Button.BUTTON_13, "LEFT");

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
     * <p>Xbox One axis-based sensor constants. {@code Stick}s wrap the {@link Gamepad.Axis} constant(s) and refer to
     * those sensors which report a value between -1 and +1, inclusive. The gamepad's triggers are also contained
     * within these constants, albeit with only the vertical axis assigned.</p>
     */
    public enum Stick implements AxisWrapper
    {
        /**
         * <p>Left analog stick.</p>
         */
        LEFT_STICK(Axis.AXIS_0, Gamepad.Axis.AXIS_1, "L-STICK"),

        /**
         * <p>Right analog stick.</p>
         */
        RIGHT_STICK(Axis.AXIS_2, Gamepad.Axis.AXIS_3, "R-STICK"),

        /**
         * <p>Left trigger.</p>
         */
        LEFT_TRIGGER(null, Gamepad.Axis.AXIS_4, "LT"),

        /**
         * <p>Right trigger.</p>
         */
        RIGHT_TRIGGER(null, Gamepad.Axis.AXIS_5, "RT");

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
            mHorizontal = horizontal;
            mVertical = vertical;
            mText = text;
        }

        @Override
        public Axis horizontal()
        {
            return mHorizontal;
        }

        @Override
        public Axis vertical()
        {
            return mVertical;
        }

        @Override
        public String toString()
        {
            return mText;
        }
    }

    /**
     * <p>Driver reported name for the Xbox One gamepad.</p>
     */
    public static final String GAMEPAD_NAME = "Xbox 360 Controller";

    /**
     * <p>Xbox One gamepad configuration.</p>
     */
    public static final PadProfile GAMEPAD_PROFILE = new X1PadProfile();

    private static class X1PadProfile extends PadProfile
    {
        private X1PadProfile()
        {
            super(Button.class, Stick.class, X1PadProfile.createRestingMap());
        }
    
        private static Map<Axis, Float> createRestingMap()
        {
            final Map<Axis, Float> resting = new EnumMap<>(Axis.class);

            // Left analog stick
            resting.put(Axis.AXIS_0, 0f);
            resting.put(Axis.AXIS_1, 0f);

            // Right analog stick
            resting.put(Axis.AXIS_2, 0f);
            resting.put(Axis.AXIS_3, 0f);

            // Left trigger
            resting.put(Axis.AXIS_4, -1f);

            // Right trigger
            resting.put(Axis.AXIS_5, -1f);

            return resting;
        }
    }
}
