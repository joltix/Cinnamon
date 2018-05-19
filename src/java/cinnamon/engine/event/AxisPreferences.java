package cinnamon.engine.event;

/**
 * <p>Specifies requirements for targeting different axis-related actions. An {@code AxisPreferences}
 * instance describes one of the following kinds of motion.</p>
 *
 * <ul>
 *     <li>Both x and y axes</li>
 *     <li>Only x-axis</li>
 *     <li>Only y-axis</li>
 *     <li>Positive x-axis</li>
 *     <li>Negative x-axis</li>
 *     <li>Positive y-axis</li>
 *     <li>Negative y-axis</li>
 * </ul>
 *
 * <p>{@code AxisPreferences} cannot be changed and are created through static methods.</p>
 *
 * <pre>
 *     <code>
 *         // Target motion along half of the x axis
 *         AxisPreferences prefs = AxisPreferences.forHorizontal(0, true);
 *     </code>
 * </pre>
 */
public final class AxisPreferences implements Preferences
{
    /**
     * <p>Preferred way of understanding the preferences' parameters.</p>
     */
    public enum Style
    {
        /**
         * <p>On motion along both the x and y axes.</p>
         */
        FREE,

        /**
         * <p>On motion along the x axis.</p>
         */
        HORIZONTAL,

        /**
         * <p>On motion along the y axis.</p>
         */
        VERTICAL,
    }

    // Descriptor
    private final Style mStyle;

    private final int mPriority;

    // True if specific kinds of coordinates are desired
    private final boolean mDiscriminate;

    // True if targeting positive coordinates
    private final boolean mPositive;

    private final int mHash;

    private AxisPreferences(int priority)
    {
        mStyle = Style.FREE;
        mPriority = priority;
        mDiscriminate = false;
        mPositive = false;

        mHash = computeHash();
    }

    private AxisPreferences(int priority, boolean vertical)
    {
        mStyle = (vertical) ? Style.VERTICAL : Style.HORIZONTAL;
        mPriority = priority;
        mDiscriminate = false;
        mPositive = false;

        mHash = computeHash();
    }

    private AxisPreferences(int priority, boolean vertical, boolean rising)
    {
        mStyle = (vertical) ? Style.VERTICAL : Style.HORIZONTAL;
        mPriority = priority;
        mDiscriminate = true;
        mPositive = rising;

        mHash = computeHash();
    }

    @Override
    public int getPriority()
    {
        return mPriority;
    }

    /**
     * <p>Gets the preferred way of understanding the preferences.</p>
     *
     * @return style.
     */
    public Style getStyle()
    {
        return mStyle;
    }

    /**
     * <p>Checks if the preferences targets certain kinds of values within a single axis e.g. positive vs negative
     * values.</p>
     *
     * @return true if desired motion is specific.
     */
    public boolean isDiscriminating()
    {
        return mDiscriminate;
    }

    /**
     * <p>Checks if the preferences targets the positive range of values on an axis (including 0). This value only
     * matters for preferences with the {@link Style#HORIZONTAL} or {@link Style#VERTICAL} styles.</p>
     *
     * @return true if the preferences targets an axis' positive range.
     */
    public boolean isPositive()
    {
        return mPositive;
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

        final AxisPreferences prefs = (AxisPreferences) obj;
        return prefs.getStyle() == getStyle() &&
                prefs.getPriority() == getPriority() &&
                prefs.isPositive() == isPositive();
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }

    private int computeHash()
    {
        int hash = 17 * 31 + mStyle.hashCode();
        hash = hash * 31 + Integer.hashCode(mPriority);
        return hash * 31 + Boolean.hashCode(mPositive);
    }

    /**
     * <p>Creates preferences for execution on motion along both the x and y axes.</p>
     *
     * @param priority priority.
     * @return preferences.
     */
    public static AxisPreferences forMotion(int priority)
    {
        return new AxisPreferences(priority);
    }

    /**
     * <p>Creates preferences for execution on motion along the x axis.</p>
     *
     * @param priority priority.
     * @return preferences.
     */
    public static AxisPreferences forHorizontal(int priority)
    {
        return new AxisPreferences(priority, false);
    }

    /**
     * <p>Creates preferences for execution on motion along the positive half of the x axis.</p>
     *
     * @param priority priority.
     * @param positive true if only positive x values are desired.
     * @return preferences.
     */
    public static AxisPreferences forHorizontal(int priority, boolean positive)
    {
        return new AxisPreferences(priority, false, positive);
    }

    /**
     * <p>Creates preferences for execution on motion along the y axis.</p>
     *
     * @param priority priority.
     * @return preferences.
     */
    public static AxisPreferences forVertical(int priority)
    {
        return new AxisPreferences(priority, true);
    }

    /**
     * <p>Creates preferences for execution on motion along the positive half of the y axis.</p>
     *
     * @param priority priority.
     * @param positive true if only positive y values are desired.
     * @return preferences.
     */
    public static AxisPreferences forVertical(int priority, boolean positive)
    {
        return new AxisPreferences(priority, true, positive);
    }
}
