package cinnamon.engine.event;

import java.util.Arrays;

/**
 * <p>Describes different motion-based actions. A {@code MotionPreferences} illustrates one of the following.</p>
 * <ul>
 *     <li>translation along certain (or all) axes</li>
 *     <li>translation whose value has a specified sign along an axis</li>
 *     <li>translation in the direction away from/towards a position</li>
 * </ul>
 *
 * <p>{@code MotionPreferences} cannot be changed and are created through static methods.</p>
 * <pre>
 *     <code>
 *         // Translation along the positive half of the x axis
 *         MotionPreferences prefs = MotionPreferences.forSignedTranslation(0, Axis.X, true);
 *     </code>
 * </pre>
 */
public final class MotionPreferences implements Preferences
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
        Y
    }

    public enum Translation
    {
        /**
         * <p>Translation along targeted axes.</p>
         */
        SPECIFIC_AXIS,

        /**
         * <p>Translation with positive values.</p>
         */
        POSITIVE_SIGN,

        /**
         * <p>Translation with negative values.</p>
         */
        NEGATIVE_SIGN,

        /**
         * <p>Translation heading away from a position.</p>
         */
        INCREASING_DISTANCE,

        /**
         * <p>Translation heading toward a position.</p>
         */
        DECREASING_DISTANCE
    }

    private final Axis[] mAxes;

    // Descriptor
    private final Translation mTranslation;

    private final int mPriority;

    private final int mHash;

    private MotionPreferences(int priority, Axis[] axes, Translation translation)
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

    /**
     * <p>Gets the affected axes</p>
     *
     * <p>Changes to the returned array does not change the preferences.</p>
     *
     * @return axes.
     */
    public Axis[] getAxes()
    {
        return Arrays.copyOf(mAxes, mAxes.length);
    }

    /**
     * <p>Gets the desired translation style.</p>
     *
     * @return translation style.
     */
    public Translation getTranslation()
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

        final MotionPreferences prefs = (MotionPreferences) obj;

        return prefs.getPriority() == getPriority() &&
                Arrays.equals(prefs.getAxes(), getAxes()) &&
                prefs.getTranslation() == prefs.getTranslation();
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
    public static MotionPreferences forTranslation(int priority)
    {
        return new MotionPreferences(priority, new Axis[] {Axis.X, Axis.Y}, Translation.SPECIFIC_AXIS);
    }

    /**
     * <p>Creates preferences for execution on motion along a single axis.</p>
     *
     * @param priority priority.
     * @param axis axis.
     * @return preferences.
     * @throws NullPointerException if axis is null.
     */
    public static MotionPreferences forTranslation(int priority, Axis axis)
    {
        checkNull(axis);

        return new MotionPreferences(priority, new Axis[] {axis}, Translation.SPECIFIC_AXIS);
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
    public static MotionPreferences forSignedTranslation(int priority, Axis axis, boolean positive)
    {
        checkNull(axis);

        final Translation translation = (positive) ? Translation.POSITIVE_SIGN : Translation.NEGATIVE_SIGN;

        return new MotionPreferences(priority, new Axis[] {axis}, translation);
    }

    /**
     * <p>Creates preferences for execution on motion along both the x and y axes directed away from a point.</p>
     *
     * @param priority priority.
     * @param away true if the desired motion is directed away.
     * @return preferences.
     */
    public static MotionPreferences forDirectedTranslation(int priority, boolean away)
    {
        final Translation translation = (away) ? Translation.INCREASING_DISTANCE : Translation.DECREASING_DISTANCE;

        return new MotionPreferences(priority, new Axis[] {Axis.X, Axis.Y}, translation);
    }

    /**
     * <p>Creates preferences for execution on motion along an axis directed away from a point.</p>
     *
     * @param priority priority.
     * @param axis axis.
     * @param away true if the desired motion is directed away.
     * @return preferences.
     * @throws NullPointerException if axis is null.
     */
    public static MotionPreferences forDirectedTranslation(int priority, Axis axis, boolean away)
    {
        checkNull(axis);

        final Translation translation = (away) ? Translation.INCREASING_DISTANCE : Translation.DECREASING_DISTANCE;

        return new MotionPreferences(priority, new Axis[] {axis}, translation);
    }

    private static void checkNull(Object object)
    {
        if (object == null) {
            throw new NullPointerException();
        }
    }
}
