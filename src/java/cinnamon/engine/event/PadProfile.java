package cinnamon.engine.event;

import cinnamon.engine.event.Gamepad.Axis;
import cinnamon.engine.event.Gamepad.AxisWrapper;
import cinnamon.engine.event.Gamepad.Button;
import cinnamon.engine.event.Gamepad.ButtonWrapper;

import java.util.EnumMap;
import java.util.Map;

/**
 * <p>Holds configuration information for {@link Gamepad} instances. {@code PadProfile}s keep track of the buttons
 * and axes constants expected for an individual gamepad as well as the resting states of axis-based sensors such as
 * analog sticks.</p>
 *
 * <p>{@code PadProfile}s require two {@code enum} classes with one implementing {@link ButtonWrapper} and the other
 * implementing {@link AxisWrapper}. Both classes must adhere to the following conditions for {@code PadProfile} to
 * interact properly with other systems.
 * <ul>
 *     <li>no two values may share the same underlying {@code Gamepad.Button} or {@code Gamepad.Axis}</li>
 *     <li>an {@code AxisWrapper} {@code enum}'s vertical and horizontal {@code Gamepad.Axis} are not the same</li>
 *     <li>each {@code enum} class must have at least one wrapper value</li>
 *     <li>the {@code Gamepad.Button} and {@code Gamepad.Axis} values returned by each {@code enum} value must
 *     <b>never</b> change at runtime</li>
 * </ul>
 */
public abstract class PadProfile
{
    // Expected wrapper alias for buttons
    private final Class mButtonCls;

    // Expected wrapper alias for axes
    private final Class mAxisCls;

    // Number of gamepad button constants actually used
    private final int mUsedButtonsCount;

    // Number of gamepad axis constants actually used
    private final int mUsedAxesCount;

    // Resting value per axis
    private final Map<Axis, Float> mResting;

    /**
     * <p>Constructs a {@code PadProfile}.</p>
     *
     * @param button button constant class.
     * @param axis axis constant class.
     * @param resting values at rest.
     * @param <T> button class implementing {@link ButtonWrapper}.
     * @param <V> axis class implementing {@link AxisWrapper}.
     * @throws NullPointerException if either button, axis, or resting is null.
     * @throws IllegalArgumentException if either button or axis have no enum values, a {@code Gamepad.Button} is
     * returned by more than one button enum value, an {@code Gamepad.Axis} is returned by more than one axis enum
     * value or is returned as both a given {@code AxisWrapper}'s vertical and horizontal, a {@code ButtonWrapper}'s
     * button is null, or an {@code AxisWrapper}'s vertical is null.
     *
     */
    @SuppressWarnings("unchecked")
    protected <T extends Enum<T> & ButtonWrapper, V extends Enum<V> & AxisWrapper> PadProfile(Class<T> button,
                                                                                              Class<V> axis,
                                                                                              Map<Axis, Float> resting)
    {
        checkNull(button);
        checkNull(axis);
        checkNull(resting);

        checkButtonEnumIsValid(button);
        checkAxisEnumIsValid(axis);
        checkRestingBoundsAreValid(resting);

        mButtonCls = button;
        mAxisCls = axis;
        mResting = new EnumMap<>(resting);

        mUsedButtonsCount = ((ButtonWrapper[]) mButtonCls.getEnumConstants()).length;
        mUsedAxesCount = countUsedAxes((AxisWrapper[]) mAxisCls.getEnumConstants());
    }

    /**
     * <p>Gets a {@code Map} of the resting values for all {@code Gamepad.Axis} constants used by the profile.</p>
     *
     * @return axes' resting values.
     */
    public final Map<Axis, Float> getRestingAxisValues()
    {
        return mResting;
    }

    /**
     * <p>Gets the number of unique {@code Gamepad.Button} values.</p>
     *
     * @return button count.
     */
    public final int getButtonCount()
    {
        return mUsedButtonsCount;
    }

    /**
     * <p>Gets the number of unique {@code Gamepad.Axis} values.</p>
     *
     * @return axis count.
     */
    public final int getAxisCount()
    {
        return mUsedAxesCount;
    }

    /**
     * <p>Gets the {@code Class} to expect for button type constants. The returned class is that of an {@code enum}
     * implementing {@link ButtonWrapper}.</p>
     *
     * @return class.
     */
    @SuppressWarnings("unchecked")
    public final Class getButtonClass()
    {
        return mButtonCls;
    }

    /**
     * <p>Gets the {@code Class} to expect for axis type constants. The returned class is that of an {@code enum}
     * implementing {@link AxisWrapper}.</p>
     *
     * @return class.
     */
    public final Class getAxisClass()
    {
        return mAxisCls;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }

    /**
     * <p>Goes over all {@code Enum} values of the given {@code Class} and throws
     * {@code IllegalArgumentException}s if the {@code Class} cannot be used to represent button type constants.</p>
     *
     * @param cls constant's class.
     * @param <T> type of button constant.
     * @throws NullPointerException if cls is null.
     * @throws IllegalArgumentException if the given class has no enum constants, the
     * implementation of {@link ButtonWrapper#button()} for at least one constant returns null, or a given
     * {@link Gamepad.Button} is returned by more than one of the constants.
     */
    private <T extends Enum<T> & ButtonWrapper> void checkButtonEnumIsValid(Class<T> cls)
    {
        final ButtonWrapper[] constants = cls.getEnumConstants();
        final String wrapperName = ButtonWrapper.class.getSimpleName();
        final String rawName = Button.class.getSimpleName();

        // Check if enum has no values
        if (constants.length == 0) {
            final String format = "%s enum has no values";
            throw new IllegalArgumentException(String.format(format, wrapperName));
        }

        final boolean[] seen = new boolean[Gamepad.Button.values().length];

        for (final ButtonWrapper wrapper : constants) {
            final Gamepad.Button button = wrapper.button();

            // Check if there is no underlying button
            if (button == null) {
                final String format = "%s value \"%s\" has no underlying %s";
                final String valueName = wrapper.toString();
                throw new IllegalArgumentException(String.format(format, wrapperName, valueName, rawName));

            } else if (seen[button.toInt()]) {
                // Check if underlying button has already been used
                final String format = "%s values cannot share a %s";
                throw new IllegalArgumentException(String.format(format, wrapperName, rawName));
            } else {

                // Mark underlying button as used
                seen[button.toInt()] = true;
            }
        }
    }

    /**
     * <p>Goes over all {@code Enum} values of the given {@code Class} and throws
     * {@code IllegalArgumentException}s if the {@code Class} cannot be used to represent axis type constants.</p>
     *
     * @param cls constant's class.
     * @param <T> type of axis constant.
     * @throws NullPointerException if cls is null.
     * @throws IllegalArgumentException if the given class has no enum constants, the
     * implementation of {@link AxisWrapper#vertical()} for at least one constant returns null, or a given
     * {@link Gamepad.Axis} is returned by more than one of the constants.
     */
    private <T extends Enum<T> & AxisWrapper> void checkAxisEnumIsValid(Class<T> cls)
    {
        final String wrapperName = AxisWrapper.class.getSimpleName();
        final String rawName = Axis.class.getSimpleName();
        final AxisWrapper[] constants = cls.getEnumConstants();

        // Check if enum has no values
        if (constants.length == 0) {
            final String format = "%s enum has no values";
            throw new IllegalArgumentException(String.format(format, wrapperName));
        }

        final boolean[] seen = new boolean[Axis.values().length];

        for (final AxisWrapper wrapper : constants) {

            final Axis vertical = wrapper.vertical();
            final Axis horizontal = wrapper.horizontal();

            // Check if there's no vertical
            if (vertical == null) {
                final String format = "%s value \"%s\" has no underlying vertical %s";
                final String valueName = wrapper.toString();
                throw new IllegalArgumentException(String.format(format, wrapper, valueName, rawName));

            } else if (seen[vertical.toInt()]) {

                // Check if vertical has already been used
                final String format = "%s values cannot share a %s";
                throw new IllegalArgumentException(String.format(format, wrapperName, rawName));
            } else {

                // Mark vertical as used
                seen[vertical.toInt()] = true;
            }

            if (horizontal != null) {
                // Check if horizontal has already been used
                if (seen[horizontal.toInt()]) {
                    final String format = "%s values cannot share a %s";
                    throw new IllegalArgumentException(String.format(format, wrapperName, rawName));
                } else {

                    // Mark horizontal as used
                    seen[horizontal.toInt()] = true;
                }
            }
        }
    }

    /**
     * <p>Throws an {@code IllegalArgumentException} if a resting value is {@literal <} -1 or {@literal >} 1.</p>
     *
     * @param resting resting values.
     * @throws IllegalArgumentException if a value is out of range.
     */
    private void checkRestingBoundsAreValid(Map<Axis, Float> resting)
    {
        for (final Float value : resting.values()) {
            if (value < -1f || value > 1f) {
                throw new IllegalArgumentException("All resting values must be >= -1 and <= 1");
            }
        }
    }

    /**
     * <p>Counts the total number of {@code Gamepad.Axis} values used in the array of wrappers.</p>
     *
     * @param wrappers wrappers.
     * @return total number of axes.
     */
    private int countUsedAxes(AxisWrapper[] wrappers)
    {
        int count = 0;
        for (final AxisWrapper wrapper : wrappers) {
            count++;
            count = (wrapper.horizontal() == null) ? count : count + 1;
        }
        return count;
    }

    private void checkNull(Object object)
    {
        if (object == null) {
            throw new NullPointerException();
        }
    }
}
