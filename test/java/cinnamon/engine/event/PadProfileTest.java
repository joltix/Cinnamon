package cinnamon.engine.event;

import cinnamon.engine.event.Gamepad.AxisWrapper;
import cinnamon.engine.event.Gamepad.ButtonWrapper;
import cinnamon.engine.event.PadProfileTest.CustomPadProfile.*;
import org.junit.Test;

import java.util.EnumMap;
import java.util.Map;

public class PadProfileTest
{
    @Test
    public void testConstructor()
    {
        new CustomPadProfile(Button.class, Axis.class);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAEButtonNullRawButton()
    {
        new CustomPadProfile(NullButton.class, Axis.class);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAEAxisNullRawVertical()
    {
        new CustomPadProfile(Button.class, NullVerticalAxis.class);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAENoValuesButton()
    {
        new CustomPadProfile(NoValuesButton.class, Axis.class);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAERawButtonReused()
    {
        new CustomPadProfile(ReusedButton.class, Axis.class);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAEAxisHasNoValues()
    {
        new CustomPadProfile(Button.class, NoValuesAxis.class);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAEAxisReusedWithinAxis()
    {
        new CustomPadProfile(Button.class, ReusedWithinAxis.class);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAEHorizontalAxisReused()
    {
        new CustomPadProfile(Button.class, ReusedHorizontalAxis.class);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAEVerticalAxisReused()
    {
        new CustomPadProfile(Button.class, ReusedVerticalAxis.class);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAERestingValueTooSmall()
    {
        final Map<Gamepad.Axis, Float> resting = new EnumMap<>(Gamepad.Axis.class);
        resting.put(Gamepad.Axis.AXIS_0, -42f);

        new CustomPadProfile(Button.class, Axis.class, resting);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAERestingValueTooLarge()
    {
        final Map<Gamepad.Axis, Float> resting = new EnumMap<>(Gamepad.Axis.class);
        resting.put(Gamepad.Axis.AXIS_0, 42f);

        new CustomPadProfile(Button.class, Axis.class, resting);
    }

    @Test (expected = NullPointerException.class)
    public void testConstructorNPEButtonClass()
    {
        new CustomPadProfile(null, Axis.class);
    }

    @Test (expected = NullPointerException.class)
    public void testConstructorNPEAxisClass()
    {
        new CustomPadProfile(Button.class, null);
    }

    @Test (expected = NullPointerException.class)
    public void testConstructorNPERestingValues()
    {
        new CustomPadProfile(Button.class, Axis.class, null);
    }

    static class CustomPadProfile extends PadProfile
    {
        /**
         * <p>To test {@code IllegalArgumentException} when the underlying {@code Gamepad.Button} is null.</p>
         */
        public enum NullButton implements ButtonWrapper
        {
            LEFT;

            @Override
            public Gamepad.Button button()
            {
                return null;
            }
        }

        /**
         * <p>To test {@code IllegalArgumentException} when the button enum has no actual values.</p>
         */
        public enum NoValuesButton implements ButtonWrapper
        {
            ;

            @Override
            public Gamepad.Button button()
            {
                return Gamepad.Button.BUTTON_0;
            }
        }

        /**
         * <p>To test {@code IllegalArgumentException} when at least two buttons' underlying {@code Gamepad.Button}s
         * are the same.</p>
         */
        public enum ReusedButton implements ButtonWrapper
        {
            LEFT(Gamepad.Button.BUTTON_1),
            RIGHT(Gamepad.Button.BUTTON_1);

            private final Gamepad.Button mButton;

            ReusedButton(Gamepad.Button button)
            {
                mButton = button;
            }

            @Override
            public Gamepad.Button button()
            {
                return mButton;
            }
        }

        /**
         * <p>To test {@code IllegalArgumentException} when underlying vertical {@code Gamepad.Axis} is null.</p>
         */
        public enum NullVerticalAxis implements AxisWrapper
        {
            LEFT(Gamepad.Axis.AXIS_0);

            private final Gamepad.Axis mAxis;

            NullVerticalAxis(Gamepad.Axis axis)
            {
                mAxis = axis;
            }

            @Override
            public Gamepad.Axis vertical()
            {
                return null;
            }

            @Override
            public Gamepad.Axis horizontal()
            {
                return mAxis;
            }
        }

        /**
         * <p>To test {@code IllegalArgumentException} when the axis enum has no actual values.</p>
         */
        public enum NoValuesAxis implements AxisWrapper
        {
            ;

            @Override
            public Gamepad.Axis horizontal()
            {
                return Gamepad.Axis.AXIS_0;
            }

            @Override
            public Gamepad.Axis vertical()
            {
                return Gamepad.Axis.AXIS_1;
            }
        }

        /**
         * <p>To test {@code IllegalArgumentException} when an axis' horizontal and vertical {@code Gamepad.Axis} are
         * the same.</p>
         */
        public enum ReusedWithinAxis implements AxisWrapper
        {
            LEFT;

            @Override
            public Gamepad.Axis horizontal()
            {
                return Gamepad.Axis.AXIS_0;
            }

            @Override
            public Gamepad.Axis vertical()
            {
                return Gamepad.Axis.AXIS_0;
            }
        }

        /**
         * <p>To test {@code IllegalArgumentException} when at least two buttons' underlying horizontal
         * {@code Gamepad.Button}s are the same.</p>
         */
        public enum ReusedHorizontalAxis implements AxisWrapper
        {
            LEFT(Gamepad.Axis.AXIS_1, Gamepad.Axis.AXIS_2),
            RIGHT(Gamepad.Axis.AXIS_1, Gamepad.Axis.AXIS_3);

            private final Gamepad.Axis mHorizontal;
            private final Gamepad.Axis mVertical;

            ReusedHorizontalAxis(Gamepad.Axis horizontal, Gamepad.Axis vertical)
            {
                mHorizontal = horizontal;
                mVertical = vertical;
            }

            @Override
            public Gamepad.Axis horizontal()
            {
                return mHorizontal;
            }

            @Override
            public Gamepad.Axis vertical()
            {
                return mVertical;
            }
        }

        /**
         * <p>To test {@code IllegalArgumentException} when at least two buttons' underlying vertical
         * {@code Gamepad.Button}s are the same.</p>
         */
        public enum ReusedVerticalAxis implements AxisWrapper
        {
            LEFT(Gamepad.Axis.AXIS_2, Gamepad.Axis.AXIS_1),
            RIGHT(Gamepad.Axis.AXIS_3, Gamepad.Axis.AXIS_1);

            private final Gamepad.Axis mHorizontal;
            private final Gamepad.Axis mVertical;

            ReusedVerticalAxis(Gamepad.Axis horizontal, Gamepad.Axis vertical)
            {
                mHorizontal = horizontal;
                mVertical = vertical;
            }

            @Override
            public Gamepad.Axis horizontal()
            {
                return mHorizontal;
            }

            @Override
            public Gamepad.Axis vertical()
            {
                return mVertical;
            }
        }

        /**
         * <p>Normal {@code ButtonWrapper} use.</p>
         */
        public enum Button implements ButtonWrapper
        {
            LEFT(Gamepad.Button.BUTTON_0),
            RIGHT(Gamepad.Button.BUTTON_1);

            private final Gamepad.Button mButton;

            Button(Gamepad.Button button)
            {
                mButton = button;
            }

            @Override
            public Gamepad.Button button()
            {
                return mButton;
            }
        }

        /**
         * <p>Normal {@code AxisWrapper} use.</p>
         */
        public enum Axis implements AxisWrapper
        {
            STICK_0(Gamepad.Axis.AXIS_0, Gamepad.Axis.AXIS_1);

            private final Gamepad.Axis mPrimary;
            private final Gamepad.Axis mSecondary;

            Axis(Gamepad.Axis primary, Gamepad.Axis secondary)
            {
                mPrimary = primary;
                mSecondary = secondary;
            }

            @Override
            public Gamepad.Axis vertical()
            {
                return mPrimary;
            }

            @Override
            public Gamepad.Axis horizontal()
            {
                return mSecondary;
            }
        }

        protected <T extends Enum<T> & ButtonWrapper, V extends Enum<V> & AxisWrapper> CustomPadProfile
                (Class<T> button, Class<V> axis)
        {
            super(button, axis, new EnumMap<>(Gamepad.Axis.class));
        }

        protected <T extends Enum<T> & ButtonWrapper, V extends Enum<V> & AxisWrapper> CustomPadProfile
                (Class<T> button, Class<V> axis, Map<Gamepad.Axis, Float> resting)
        {
            super(button, axis, resting);
        }
    }
}
