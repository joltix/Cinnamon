package cinnamon.engine.utils;

import java.util.*;

/**
 * Acts as a standard implementation of the {@code Properties} interface and is intended to be wrapped by classes
 * wishing to implement the interface without filling itself with the details. Properties cannot be removed once
 * added and unmodifiable properties' values can be changed through setter variants such as
 * {@link #setUnmodifiableStringProperty(String, String)}.
 *
 * <h3>Map</h3>
 * <p>Although this class is effectively a restricted {@link Map}, the {@code Map} interface is not implemented due to
 * many of its methods allowing modification of the keys and values. While some methods such as
 * {@code Map.remove(Object)} allow throwing {@code UnsupportedOperationException}s, methods such as
 * {@code Map.keySet()} do not and document the returned collection to permit modification. If needed, a {@code Map}
 * version of this container can be retrieved with {@link #asMap()}.</p>
 */
public final class PropertyMap implements Properties
{
    private final Map<String, Object> mProperties;

    // Property names whose values cannot be changed by interface setters
    private final List<String> mUnmodifiables;

    /**
     * Constructs a {@code PropertyMap} and checks if each property is associated with a value of the expected type.
     *
     * <p>If a property does not have an entry in {@code expectations}, the property's current value type is
     * presumed correct.</p>
     *
     * @param properties properties.
     * @param unmodifiables unmodifiable properties.
     * @param expectations expected value types.
     * @throws NullPointerException if either properties or unmodifiables is null.
     * @throws IllegalArgumentException if a property' value type does not match an expectation.
     * @throws NoSuchElementException if a key in either properties or expectations has null as its value.
     */
    public PropertyMap(Map<String, Object> properties, List<String> unmodifiables, Map<String, Class> expectations)
    {
        checkNotNull(properties);
        checkNotNull(unmodifiables);
        checkNotNull(expectations);

        checkMapDoesNotHaveNullValues(properties);
        checkMapDoesNotHaveNullValues(expectations);
        checkPropertiesHaveCorrectValueTypes(properties, expectations);

        mProperties = new HashMap<>(properties);
        mUnmodifiables = new ArrayList<>(unmodifiables);
    }

    @Override
    public String getStringProperty(String name)
    {
        checkNotNull(name);
        checkPropertyExists(name);
        checkPropertyValueTypeMatches(name, String.class);

        return (String) mProperties.get(name);
    }

    @Override
    public double getDoubleProperty(String name)
    {
        checkNotNull(name);
        checkPropertyExists(name);
        checkPropertyValueTypeMatches(name, Double.class);

        return (Double) mProperties.get(name);
    }

    @Override
    public int getIntegerProperty(String name)
    {
        checkNotNull(name);
        checkPropertyExists(name);
        checkPropertyValueTypeMatches(name, Integer.class);

        return (Integer) mProperties.get(name);
    }

    @Override
    public boolean getBooleanProperty(String name)
    {
        checkNotNull(name);
        checkPropertyExists(name);
        checkPropertyValueTypeMatches(name, Boolean.class);

        return (Boolean) mProperties.get(name);
    }

    @Override
    public void setStringProperty(String name, String value)
    {
        checkNotNull(name);
        checkNotNull(value);
        checkPropertySettable(name);
        checkPropertyValueTypeMatches(name, String.class);

        mProperties.put(name, value);
    }

    @Override
    public void setDoubleProperty(String name, double value)
    {
        checkNotNull(name);
        checkPropertySettable(name);
        checkPropertyValueTypeMatches(name, Double.class);

        mProperties.put(name, value);
    }

    @Override
    public void setIntegerProperty(String name, int value)
    {
        checkNotNull(name);
        checkPropertySettable(name);
        checkPropertyValueTypeMatches(name, Integer.class);

        mProperties.put(name, value);
    }

    @Override
    public void setBooleanProperty(String name, boolean value)
    {
        checkNotNull(name);
        checkPropertySettable(name);
        checkPropertyValueTypeMatches(name, Boolean.class);

        mProperties.put(name, value);
    }

    @Override
    public boolean containsProperty(String name)
    {
        checkNotNull(name);

        return mProperties.containsKey(name);
    }

    /**
     * Sets a double property. Unlike {@link #setDoubleProperty(String, double)}, this method allows value changes
     * even if the property is unmodifiable.
     *
     * @param name property name.
     * @param value value.
     * @throws NullPointerException if name is null.
     * @throws IllegalArgumentException if the property uses a different value type.
     */
    public void setUnmodifiableDoubleProperty(String name, double value)
    {
        checkNotNull(name);
        checkPropertyValueTypeMatches(name, Double.class);

        mProperties.put(name, value);
    }

    /**
     * Sets a String property. Unlike {@link #setStringProperty(String, String)}, this method allows value changes
     * even if the property is unmodifiable.
     *
     * @param name property name.
     * @param value value.
     * @throws NullPointerException if either name or value is null.
     * @throws IllegalArgumentException if the property uses a different value type.
     */
    public void setUnmodifiableStringProperty(String name, String value)
    {
        checkNotNull(name);
        checkNotNull(value);
        checkPropertyValueTypeMatches(name, String.class);

        mProperties.put(name, value);
    }

    /**
     * Sets an integer property. Unlike {@link #setIntegerProperty(String, int)}, this method allows value changes
     * even if the property is unmodifiable.
     *
     * @param name property name.
     * @param value value.
     * @throws NullPointerException if name is null.
     * @throws IllegalArgumentException if the property uses a different value type.
     */
    public void setUnmodifiableIntegerProperty(String name, int value)
    {
        checkNotNull(name);
        checkPropertyValueTypeMatches(name, Integer.class);

        mProperties.put(name, value);
    }

    /**
     * Sets a boolean property. Unlike {@link #setBooleanProperty(String, boolean)}, this method allows value changes
     * even if the property is unmodifiable.
     *
     * @param name property name.
     * @param value value.
     * @throws NullPointerException if name is null.
     * @throws IllegalArgumentException if the property uses a different value type.
     */
    public void setUnmodifiableBooleanProperty(String name, boolean value)
    {
        checkNotNull(name);
        checkPropertyValueTypeMatches(name, Boolean.class);

        mProperties.put(name, value);
    }

    /**
     * Returns a snapshot of all properties and their values.
     *
     * @return {@code Map} of properties.
     */
    public Map<String, Object> asMap()
    {
        return new HashMap<>(mProperties);
    }

    /**
     * Gets the number of properties.
     *
     * @return property count.
     */
    public int size()
    {
        return mProperties.size();
    }

    /**
     * Checks if there are no properties.
     *
     * @return true if empty.
     */
    public boolean isEmpty()
    {
        return mProperties.isEmpty();
    }

    @Override
    public int hashCode()
    {
        final int hash = 17 * 31 + mProperties.hashCode();
        return 31 * hash + mUnmodifiables.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        } else if (obj == this) {
            return true;
        }

        final PropertyMap map = (PropertyMap) obj;

        return map.mProperties.equals(mProperties)
                && map.mUnmodifiables.equals(mUnmodifiables);
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }

    /**
     * Checks if a property's expected value type matches a given type.
     *
     * @param name property name.
     * @param type value type to test.
     * @throws IllegalArgumentException if a property's value type does not match its expected value type.
     */
    private void checkPropertyValueTypeMatches(String name, Class type)
    {
        final Object value = mProperties.get(name);

        // Check if property does not exist
        if (value == null) {
            return;
        }

        final Class actualType  = value.getClass();

        if (actualType != type) {
            final String format = "Property \"%s\" accepts %s values, given: %s";
            final String actualName = actualType.getSimpleName();
            final String typeName = type.getSimpleName();
            throw new IllegalArgumentException(String.format(format, name, actualName, typeName));
        }
    }

    private void checkMapDoesNotHaveNullValues(Map<String, ?> properties)
    {
        for (final String property : properties.keySet()) {
            final Object value = properties.get(property);

            if (value == null) {
                final String format = "Property \"%s\" must have a non-null value";
                throw new NoSuchElementException(String.format(format, properties));
            }
        }
    }

    /**
     * Checks a map of properties if each property's values are both non-null and of the expected value type, if the
     * property is recognized.
     *
     * @param properties properties.
     * @param expectations expected value types.
     * @throws NoSuchElementException if a property's value is null.
     * @throws IllegalArgumentException if a property's value type does not match its expected value type.
     */
    private void checkPropertiesHaveCorrectValueTypes(Map<String, Object> properties, Map<String, Class> expectations)
    {
        for (final String property : properties.keySet()) {

            final Object givenValue = properties.get(property);
            final Class expectedType = expectations.get(property);

            // Check given value type against expected
            if (expectedType != null && expectedType != givenValue.getClass()) {
                final String format = "Property \"%s\" expects %s, actual: %s";
                final String expectedName = expectedType.getSimpleName();
                final String actualName = givenValue.getClass().getSimpleName();
                throw new IllegalArgumentException(String.format(format, property, expectedName, actualName));
            }
        }
    }

    private void checkPropertyExists(String name)
    {
        if (!mProperties.containsKey(name)) {
            final String format = "No such property named \"%s\"";
            throw new NoSuchElementException(String.format(format,name));
        }
    }

    private void checkPropertySettable(String name)
    {
        if (mUnmodifiables.contains(name)) {
            final String format = "Property \"%s\" is not settable";
            throw new IllegalArgumentException(String.format(format, name));
        }
    }

    private void checkNotNull(Object object)
    {
        if (object == null) {
            throw new NullPointerException();
        }
    }
}
