package cinnamon.engine.event;

import java.util.Arrays;

/**
 * <p>Specifies requirements for targeting different axis-related actions. An {@code AxisPreferences}
 * instance describes one of the following kinds of motion.</p>
 *
 * <ul>
 *     <li>motion along certain (or all) axes</li>
 *     <li>motion whose offset has a specified sign along an axis</li>
 *     <li>motion in the direction away from/towards a starting position</li>
 * </ul>
 *
 * <p>{@code AxisPreferences} cannot be changed and are created through static methods.</p>
 *
 * <pre>
 *     <code>
 *         // Target motion along the positive half of the x axis
 *         AxisPreferences prefs = AxisPreferences.forSignedTranslation(0, Axis.X, true);
 *     </code>
 * </pre>
 */
public final class AxisPreferences implements Preferences
{
    public enum Axis
    {
        /**
         * <p>X axis.</p>
         */
        X,

        /**
         * <p>Y axis.</p>
         */
        Y;
    }

    public enum Translation
    {
        SPECIFIC_AXIS,
        POSITIVE_SIGN,
        NEGATIVE_SIGN,
        INCREASING_DISTANCE,
        DECREASING_DISTANCE;
    }

    private final Axis[] mAxes;

    // Descriptor
    private final Translation mTranslation;

    private final int mPriority;

    private final int mHash;

    private AxisPreferences(int priority, Axis[] axes, Translation translation)
    {
        mPriority = priority;
        mAxes = axes;
        mTranslation = translation;

        mHash = computeHash();
    }

    @Override
    public int getPriority()
    {
        return mPriority;
    }

    public Axis[] getAxes()
    {
        return Arrays.copyOf(mAxes, mAxes.length);
    }

    public Translation getTarget()
    {
        return mTranslation;
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

        return prefs.getPriority() == getPriority() &&
                Arrays.equals(prefs.getAxes(), getAxes()) &&
                prefs.getTarget() == prefs.getTarget();
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }

    private int computeHash()
    {
        int hash = 17 * 31 + Integer.hashCode(mPriority);
        hash = hash * 31 + Arrays.hashCode(mAxes);
        return hash * 31 + mTranslation.hashCode();
    }

    /**
     * <p>Creates preferences for execution on motion along both the x and y axes.</p>
     *
     * @param priority priority.
     * @return preferences.
     */
    public static AxisPreferences forTranslation(int priority)
    {
        return new AxisPreferences(priority, new Axis[] {Axis.X, Axis.Y}, Translation.SPECIFIC_AXIS);
    }

    /**
     * <p>Creates preferences for execution on motion along a single axis.</p>
     *
     * @param priority priority.
     * @param axis axis.
     * @return preferences.
     * @throws NullPointerException if axis is null.
     */
    public static AxisPreferences forTranslation(int priority, Axis axis)
    {
        checkNull(axis);

        return new AxisPreferences(priority, new Axis[] {axis}, Translation.SPECIFIC_AXIS);
    }

    /**
     * <p>Creates preferences for execution on motion along half of an axis.</p>
     *
     * @param priority priority.
     * @param axis axis.
     * @param positive true if the positive half of the axis is targeted.
     * @return preferences.
     * @throws NullPointerException if axis is null.
     */
    public static AxisPreferences forSignedTranslation(int priority, Axis axis, boolean positive)
    {
        checkNull(axis);

        final Translation translation = (positive) ? Translation.POSITIVE_SIGN : Translation.NEGATIVE_SIGN;

        return new AxisPreferences(priority, new Axis[] {axis}, translation);
    }

    /**
     * <p>Creates preferences for execution on motion along both the x and y axes directed away from a starting
     * point.</p>
     *
     * @param priority priority.
     * @param away true if the desired motion is directed away.
     * @return preferences.
     */
    public static AxisPreferences forDirectedTranslation(int priority, boolean away)
    {
        final Translation translation = (away) ? Translation.INCREASING_DISTANCE : Translation.DECREASING_DISTANCE;

        return new AxisPreferences(priority, new Axis[] {Axis.X, Axis.Y}, translation);
    }

    /**
     * <p>Creates preferences for execution on motion along an axis directed away from a starting point.</p>
     *
     * @param priority priority.
     * @param axis axis.
     * @param away true if the desired motion is directed away.
     * @return preferences.
     * @throws NullPointerException if axis is null.
     */
    public static AxisPreferences forDirectedTranslation(int priority, Axis axis, boolean away)
    {
        checkNull(axis);

        final Translation translation = (away) ? Translation.INCREASING_DISTANCE : Translation.DECREASING_DISTANCE;

        return new AxisPreferences(priority, new Axis[] {axis}, translation);
    }

    private static void checkNull(Object object)
    {
        if (object == null) {
            throw new NullPointerException();
        }
    }
}
