package cinnamon.engine.event;

/**
 * <p>Specifies requirements for targeting different button-related actions. A {@code ButtonPreferences} instance
 * describes one of the following actions.</p>
 *
 * <ul>
 *     <li>Press</li>
 *     <li>Release</li>
 *     <li>Double click</li>
 * </ul>
 *
 * <p>{@code ButtonPreferences} cannot be changed and are created through static methods.</p>
 *
 * <pre>
 *     <code>
 *         // Target release events for a single button
 *         ButtonPreferences prefs = ButtonPreferences.forRelease(0);
 *     </code>
 * </pre>
 *
 * <h3>Unconstrained actions</h3>
 * <p>If a time constraint (e.g. duration) is too large to be stored as a {@code long} in nanoseconds, the amount is
 * clamped as if {@code Long.MAX_VALUE / 1000000}. This has the effect of having a time-constraint so large it
 * practically does not exist for general applications.</p>
 *
 * <h3>Multiple buttons</h3>
 * <p>The <i>press</i> and <i>release</i> actions can be performed with more than one
 * button. Static methods for these variants are prefixed by "Multi" and introduce a {@code tolerance} parameter.
 * This value is the largest millisecond duration allowed between the earliest and latest target events (e.g. Multi
 * press' tolerance checks as if {@code lastPressTime - firstPressTime <= tolerance}).</p>
 *
 * <p>Note that the only difference between preferences for a single button and those for multiple buttons is the
 * existence of a reasonable amount of tolerance. A multi-button instance with 0 tolerance is equivalent to that
 * created for a single-button action.</p>
 * <p><b>note</b> Multi-button double clicks are not expected thus {@code tolerance} for double clicks refer to the
 * time between both clicks' presses.</p>
 */
public final class ButtonPreferences implements Preferences
{
    /**
     * <p>Descriptors for how {@code ButtonPreferences} should be interpreted.</p>
     */
    enum Style
    {
        /**
         * <p>On press.</p>
         */
        PRESS,

        /**
         * <p>On release.</p>
         */
        RELEASE,

        /**
         * <p>On press of a second click.</p>
         */
        DOUBLE,
    }

    private static final long NANO_PER_MILLI = 1_000_000L;

    // Maximum time accepted for time constraints
    private static final long LARGEST_ALLOWED_TIME = Long.MAX_VALUE / NANO_PER_MILLI;

    // Default for time not used by the style
    private static final long UNUSED_TIME = 0L;

    private final Style mStyle;

    private final int mPriority;

    // Time allowed between each target event when multiple events are required
    private final long mTolerance;

    // Time allowed between press and subsequent release
    private final long mClickDur;

    private final int mHash;

    private ButtonPreferences(int priority, boolean onPress, long tolerance, long duration)
    {
        mStyle = (onPress) ? Style.PRESS : Style.RELEASE;
        mPriority = priority;
        mTolerance = clampToNano(tolerance);
        mClickDur = clampToNano(duration);

        mHash = computeHash();
    }

    private ButtonPreferences(int priority, long tolerance, long duration)
    {
        mStyle = Style.DOUBLE;
        mPriority = priority;
        mTolerance = clampToNano(tolerance);
        mClickDur = clampToNano(duration);

        mHash = computeHash();
    }

    @Override
    public int getPriority()
    {
        return mPriority;
    }

    /**
     * <p>Gets the preferred descriptor for handling the preferences.</p>
     *
     * @return style.
     */
    public Style getStyle()
    {
        return mStyle;
    }

    /**
     * <p>Gets the maximum time allowed between each button. If this value is {@literal >} 0, the preferences targets a
     * multi-button action.</p>
     *
     * @return time between buttons, in nanoseconds.
     */
    public long getTolerance()
    {
        return mTolerance;
    }

    /**
     * <p>Gets the maximum amount of time allowed between a click's {@code PRESS} and its {@code RELEASE} events. If
     * this value is not explicitly provided during creation, this method returns {@link Long#MAX_VALUE}.</p>
     *
     * @return allowed duration in nanoseconds.
     */
    public long getClickDuration()
    {
        return mClickDur;
    }

    @Override
    public int hashCode()
    {
        return mHash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        } else if (obj == this) {
            return true;
        }

        final ButtonPreferences prefs = (ButtonPreferences) obj;

        return prefs.getStyle() == getStyle() && prefs.getPriority() == getPriority() &&
                prefs.getTolerance() == getTolerance() &&
                prefs.getClickDuration() == getClickDuration();
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }

    private long clampToNano(long time)
    {
        return (time <= LARGEST_ALLOWED_TIME) ? time * NANO_PER_MILLI : LARGEST_ALLOWED_TIME;
    }

    private int computeHash()
    {
        int hash = 17 * 31 + mStyle.hashCode();
        hash = 31 * hash + Integer.hashCode(mPriority);
        hash = 31 * hash + Long.hashCode(mTolerance);
        return 31 * hash + Long.hashCode(mClickDur);
    }

    /**
     * <p>Creates preferences for execution on the {@code PRESS} event of a button.</p>
     *
     * @param priority priority.
     * @return preferences.
     */
    public static ButtonPreferences forPress(int priority)
    {
        return new ButtonPreferences(priority, true, UNUSED_TIME, UNUSED_TIME);
    }

    /**
     * <p>Creates preferences for execution on the first {@code PRESS} event belonging to a desired set of buttons.</p>
     *
     * <p>The maximum allowable time between the {@code PRESS} events of the earliest and latest target buttons can be
     * specified through {@code tolerance}. This time affects the difficulty of attempting to simultaneously press
     * the desired buttons where larger values ease the need for stringent time.</p>
     *
     * @param priority priority.
     * @param tolerance maximum time between the first and last press, in ms.
     * @return preferences.
     * @throws IllegalArgumentException if tolerance {@literal <} 0.
     */
    public static ButtonPreferences forMultiPress(int priority, long tolerance)
    {
        checkTolerance(tolerance);

        return new ButtonPreferences(priority, true, tolerance, UNUSED_TIME);
    }

    /**
     * <p>Creates preferences for execution on the first {@code RELEASE} event of a button.</p>
     *
     * @param priority priority.
     * @return preferences.
     */
    public static ButtonPreferences forRelease(int priority)
    {
        return new ButtonPreferences(priority, false, UNUSED_TIME, Long.MAX_VALUE);
    }

    /**
     * <p>Creates preferences for execution on the first {@code RELEASE} event of a button.</p>
     *
     * @param priority priority.
     * @param duration maximum time between the press and subsequent release, in ms.
     * @return preferences.
     * @throws IllegalArgumentException if duration {@literal <} 0.
     */
    public static ButtonPreferences forRelease(int priority, long duration)
    {
        checkClickDuration(duration);

        return new ButtonPreferences(priority, false, UNUSED_TIME, duration);
    }

    /**
     * <p>Creates preferences for execution on the first {@code RELEASE} event belonging to a desired set of buttons.</p>
     *
     * <p>The maximum allowable time between the {@code RELEASE} events of the earliest and latest target buttons can be
     * specified through {@code tolerance}. This time affects the difficulty of attempting to simultaneously release
     * the desired buttons where larger values ease the need for stringent time.</p>
     *
     * @param priority priority.
     * @param tolerance maximum time between the first and last release, in ms.
     * @return preferences.
     * @throws IllegalArgumentException if tolerance {@literal <} 0.
     */
    public static ButtonPreferences forMultiRelease(int priority, long tolerance)
    {
        checkTolerance(tolerance);

        return new ButtonPreferences(priority, false, tolerance, Long.MAX_VALUE);
    }

    /**
     * <p>Creates preferences for execution on the first {@code RELEASE} event belonging to a desired set of buttons.</p>
     *
     * <p>The maximum allowable time between the {@code RELEASE} events of the earliest and latest target buttons can be
     * specified through {@code tolerance}. This time affects the difficulty of attempting to simultaneously release
     * the desired buttons where larger values ease the need for stringent time.</p>
     *
     * @param priority priority.
     * @param duration maximum time between each press and its subsequent release, in ms.
     * @param tolerance maximum time between the first and last release, in ms.
     * @return preferences.
     * @throws IllegalArgumentException if tolerance or tolerance {@literal <} 0.
     */
    public static ButtonPreferences forMultiRelease(int priority, long duration, long tolerance)
    {
        checkTolerance(tolerance);
        checkClickDuration(duration);

        return new ButtonPreferences(priority, false, tolerance, duration);
    }

    /**
     * <p>Creates preferences for execution on the first press following a click.</p>
     *
     * <p>The time from the click's press to its release must be {@literal <}= {@code duration} and the time between
     * both presses must be {@literal <}= {@code tolerance}.</p>
     *
     * @param priority priority.
     * @param duration maximum time between a click's press and subsequent release, in ms.
     * @param tolerance maximum time between two presses, in ms.
     * @return preferences.
     * @throws IllegalArgumentException if duration or tolerance {@literal < 0}.
     */
    public static ButtonPreferences forDoubleClick(int priority, long duration, long tolerance)
    {
        checkTolerance(tolerance);

        return new ButtonPreferences(priority, tolerance, duration);
    }

    private static void checkTolerance(long tolerance)
    {
        if (tolerance < 0L) {
            final String format = "Tolerance must be >= 0 ms, actual: %d";
            throw new IllegalArgumentException(String.format(format, tolerance));
        }
    }

    private static void checkClickDuration(long duration)
    {
        if (duration < 0L) {
            final String format = "Click duration must be >= 0, actual: %d";
            throw new IllegalArgumentException(String.format(format, duration));
        }
    }
}
