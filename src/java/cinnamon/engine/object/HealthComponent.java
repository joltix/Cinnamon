package cinnamon.engine.object;

import static java.util.Objects.requireNonNull;

/**
 * Health for {@code Entity} objects.
 */
public final class HealthComponent extends Component
{
    /**
     * Largest possible health maximum.
     */
    public static final float MAX_HEALTH = 1_000.00f;

    // Local health ceiling
    private float mMax = 100.00f;

    // Current health
    private float mHealth = mMax;

    /**
     * Constructs a full {@code HealthComponent} with a maximum health of 100.
     */
    public HealthComponent() { }

    /**
     * Gets the health.
     * 
     * @return health.
     */
    public float getHealth()
    {
        return mHealth;
    }

    /**
     * Sets the health.
     * 
     * @param health health.
     * @throws IllegalArgumentException if {@code health} is either {@literal <} 0, {@code >= getMaximumHealth()}, is
     * {@code Float.NaN}, {@code Float.POSITIVE_INFINITY}, or {@code Float.NEGATIVE_INFINITY}.
     */
    public void setHealth(float health)
    {
        checkHealthNotNaN(health);
        checkHealthNotInfinite(health);
        checkHealthIsInRange(health);

        mHealth = health;
    }

    /**
     * Adds health.
     *
     * <p>Health cannot be added above the value returned by {@link #getMaximumHealth()}.</p>
     * 
     * @param health health.
     * @throws IllegalArgumentException if {@code health} is {@code Float.NaN}, {@code Float.POSITIVE_INFINITY}, or
     * {@code Float.NEGATIVE_INFINITY}.
     */
    public void addHealth(float health)
    {
        checkHealthNotNaN(health);
        checkHealthNotInfinite(health);

        mHealth = Math.max(0f, Math.min(mMax, mHealth + health));
    }

    /**
     * Gets the largest possible health this component allows by setting, adding, or removing.
     * 
     * @return maximum health.
     */
    public float getMaximumHealth()
    {
        return mMax;
    }

    /**
     * Sets the largest possible health value this {@code HealthComponent} can have.
     * 
     * <p>If the maximum health is changed to a smaller ceiling such that the current health would be higher than the
     * new maximum, the current health is clamped to the new maximum. {@code maximum} is clamped to
     * {@link #MAX_HEALTH}.</p>
     * 
     * @param maximum maximum health.
     * @throws IllegalArgumentException if {@code maximum} is either {@literal <=} 0, {@literal >}
     * {@code HealthComponent.MAX_HEALTH}, or is {@code Float.NaN}, {@code Float.POSITIVE_INFINITY}, or
     * {@code Float.NEGATIVE_INFINITY}.
     */
    public void setMaximumHealth(float maximum)
    {
        checkHealthNotNaN(maximum);
        checkHealthNotInfinite(maximum);
        checkHealthNotPositive(maximum);

        if (maximum <= 0f) {
            final String format = "Maximum health must be > 0, given: %f";
            throw new IllegalArgumentException(String.format(format, maximum));
        }
        if (maximum > MAX_HEALTH) {
            final String format = "Maximum health must be <= HealthComponent.MAX_HEALTH, given: %f";
            throw new IllegalArgumentException(String.format(format, maximum));
        }

        mMax = Math.min(maximum, MAX_HEALTH);
        mHealth = Math.min(mHealth, mMax);
    }

    /**
     * Gets the health as a percentage.
     *
     * <p>The returned value is always {@literal >=} 0% and {@literal <=} 100%.</p>
     * 
     * @return health percentage.
     */
    public float getPercentage()
    {
        return mHealth / mMax;
    }

    /**
     * Returns {@code true} if health is at 100%.
     * 
     * @return {@code true} if {@code getHealth() == getMaximumHealth()}.
     */
    public boolean isFull()
    {
        return mHealth == mMax;
    }

    /**
     * Returns {@code true} if health is at 0%.
     * 
     * @return {@code true} if {@code getHealth() == 0}.
     */
    public boolean isEmpty()
    {
        return mHealth == 0f;
    }

    /**
     * Copies the health and maximum health of another {@code HealthComponent}.
     *
     * @throws ClassCastException if {@code component} is not a {@code HealthComponent}.
     * @throws NullPointerException if {@code component} is {@code null}.
     */
    @Override
    public void copy(Component component)
    {
        requireNonNull(component);

        if (component.getClass() != HealthComponent.class) {
            throw new ClassCastException();
        }

        final HealthComponent other = (HealthComponent) component;

        mMax = other.getMaximumHealth();
        mHealth = other.getHealth();
    }

    @Override
    protected void onAttach() { }

    @Override
    protected void onDetach() { }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }

    private void checkHealthNotNaN(float health)
    {
        if (Float.isNaN(health)) {
            throw new IllegalArgumentException("Health cannot be NaN");
        }
    }

    private void checkHealthNotInfinite(float health)
    {
        if (Float.isInfinite(health)) {
            final String format = "Health cannot be %s_INFINITY";
            final String sign = (health == Float.POSITIVE_INFINITY) ? "POSITIVE" : "NEGATIVE";

            throw new IllegalArgumentException(String.format(format, sign));
        }
    }

    private void checkHealthIsInRange(float health)
    {
        if (health < 0f) {
            final String format = "Health cannot be negative, given: %f";
            throw new IllegalArgumentException(String.format(format, health));

        } else if (health > mMax) {
            final String format = "Health cannot be higher than maximum(%f), given: %f";
            throw new IllegalArgumentException(String.format(format, mMax, health));
        }
    }

    private void checkHealthNotPositive(float health)
    {
        if (health <= 0f) {
            final String format = "Health must be positive, given: %f";
            throw new IllegalArgumentException(String.format(format, health));
        }
    }
}