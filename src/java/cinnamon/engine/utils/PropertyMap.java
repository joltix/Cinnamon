package cinnamon.engine.utils;

import java.util.*;

/**
 * Acts as a standard implementation of the {@code Properties} interface and is intended to be wrapped by classes
 * wishing to implement the interface without filling itself with the details. Properties cannot be removed once
 * added and unmodifiable properties' values can be changed through setter variants such as
 * {@link #setUnmodifiableStringProperty(String, String)}.
 *
 * <h3>Map</h3>
 * <p>Although this class is effectively a restricted {@link Map}, the {@code Map} interface is not implemented due
 * to a lack of present need. This may change in the future as this framework's concept of properties is likely to
 * change. If required, a {@code Map} version of this container can be retrieved with {@link #asMap()}.</p>
 */
public final class PropertyMap implements Properties
{
    private final Map<String, Object> mProperties;

    // Property names whose values cannot be changed by interface setters
    private final List<String> mUnmodifiables;

    /**
     * Constructs a {@code PropertyMap} and checks if each property is associated with a value of the expected type.
     *
     * <p>If a property does not exist in {@code expectations}, the property's value type is presumed correct.</p>
     *
     * @param properties properties.
     * @param expectations expected value types.
     * @throws NullPointerException if either properties or expectations is null.
     * @throws ClassCastException if a property's value type does not match an expectation.
     * @throws NoSuchElementException if a key in either properties or expectations has null as its value.
     */
    public PropertyMap(Map<String, Object> properties, Map<String, Class> expectations)
    {
        checkNotNull(properties);
        checkNotNull(expectations);

        checkMapDoesNotHaveNullValues(properties);
        checkMapDoesNotHaveNullValues(expectations);
        checkPropertiesHaveCorrectValueTypes(properties, expectations);

        mProperties = new HashMap<>(properties);
        mUnmodifiables = new ArrayList<>();
    }

    /**
     * Constructs a {@code PropertyMap} and checks if each property is associated with a value of the expected type.
     *
     * <p>If a property does not exist in {@code expectations}, the property's value type is presumed correct.</p>
     *
     * @param properties properties.
     * @param expectations expected value types.
     * @param unmodifiables unmodifiable properties.
     * @throws NullPointerException if either properties, expectations, or unmodifiables is null.
     * @throws ClassCastException if a property' value type does not match an expectation.
     * @throws NoSuchElementException if a key in either properties or expectations has null as its value.
     */
    public PropertyMap(Map<String, Object> properties, Map<String, Class> expectations, Set<String> unmodifiables)
    {
        checkNotNull(properties);
        checkNotNull(expectations);
        checkNotNull(unmodifiables);

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

        final Object value = mProperties.get(name);
        checkPropertyExists(name, value, String.class);

        return (String) value;
    }

    @Override
    public double getDoubleProperty(String name)
    {
        checkNotNull(name);

        final Object value = mProperties.get(name);
        checkPropertyExists(name, value, Double.class);

        return (Double) value;
    }

    @Override
    public int getIntegerProperty(String name)
    {
        checkNotNull(name);

        final Object value = mProperties.get(name);
        checkPropertyExists(name, value, Integer.class);

        return (Integer) value;
    }

    @Override
    public boolean getBooleanProperty(String name)
    {
        checkNotNull(name);

        final Object value = mProperties.get(name);
        checkPropertyExists(name, value, Boolean.class);

        return (Boolean) value;
    }

    @Override
    public void setStringProperty(String name, String value)
    {
        checkNotNull(name);
        checkNotNull(value);
        checkPropertySettable(name);

        final Object current = mProperties.get(name);

        if (current != null) {
            checkTypeMatches(String.class, current.getClass());
        }

        mProperties.put(name, value);
    }

    @Override
    public void setDoubleProperty(String name, double value)
    {
        checkNotNull(name);
        checkPropertySettable(name);

        final Object current = mProperties.get(name);

        if (current != null) {
            checkTypeMatches(Double.class, current.getClass());
        }

        mProperties.put(name, value);
    }

    @Override
    public void setIntegerProperty(String name, int value)
    {
        checkNotNull(name);
        checkPropertySettable(name);

        final Object current = mProperties.get(name);

        if (current != null) {
            checkTypeMatches(Integer.class, current.getClass());
        }

        mProperties.put(name, value);
    }

    @Override
    public void setBooleanProperty(String name, boolean value)
    {
        checkNotNull(name);
        checkPropertySettable(name);

        final Object current = mProperties.get(name);

        if (current != null) {
            checkTypeMatches(Boolean.class, current.getClass());
        }

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
     * @throws ClassCastException if the property uses a different value type.
     */
    public void setUnmodifiableDoubleProperty(String name, double value)
    {
        checkNotNull(name);

        final Object current = mProperties.get(name);

        if (current != null) {
            checkTypeMatches(Double.class, current.getClass());
        }

        mProperties.put(name, value);
    }

    /**
     * Sets a String property. Unlike {@link #setStringProperty(String, String)}, this method allows value changes
     * even if the property is unmodifiable.
     *
     * @param name property name.
     * @param value value.
     * @throws NullPointerException if either name or value is null.
     * @throws ClassCastException if the property uses a different value type.
     */
    public void setUnmodifiableStringProperty(String name, String value)
    {
        checkNotNull(name);
        checkNotNull(value);

        final Object current = mProperties.get(name);

        if (current != null) {
            checkTypeMatches(String.class, current.getClass());
        }

        mProperties.put(name, value);
    }

    /**
     * Sets an integer property. Unlike {@link #setIntegerProperty(String, int)}, this method allows value changes
     * even if the property is unmodifiable.
     *
     * @param name property name.
     * @param value value.
     * @throws NullPointerException if name is null.
     * @throws ClassCastException if the property uses a different value type.
     */
    public void setUnmodifiableIntegerProperty(String name, int value)
    {
        checkNotNull(name);

        final Object current = mProperties.get(name);

        if (current != null) {
            checkTypeMatches(Integer.class, current.getClass());
        }

        mProperties.put(name, value);
    }

    /**
     * Sets a boolean property. Unlike {@link #setBooleanProperty(String, boolean)}, this method allows value changes
     * even if the property is unmodifiable.
     *
     * @param name property name.
     * @param value value.
     * @throws NullPointerException if name is null.
     * @throws ClassCastException if the property uses a different value type.
     */
    public void setUnmodifiableBooleanProperty(String name, boolean value)
    {
        checkNotNull(name);

        final Object current = mProperties.get(name);

        if (current != null) {
            checkTypeMatches(Boolean.class, current.getClass());
        }

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
     * Checks a map if each key's values are both non-null and of the expected value type, if the key is expected.
     *
     * @param properties properties.
     * @param expectations mapping between keys and their values' expected types.
     * @throws ClassCastException if a value's type is not as expected.
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
                throw new ClassCastException(String.format(format, property, expectedName, actualName));
            }
        }
    }

    /**
     * Throws a {@code ClassCastException} if the actual class is not the expected.
     *
     * @param expected expected type.
     * @param actual actual type.
     * @throws ClassCastException if actual != expected.
     */
    private void checkTypeMatches(Class expected, Class actual)
    {
        if (actual != expected) {
            final String format = "Expected %s, given: %s";
            final String actualName = actual.getSimpleName();
            final String expectedName = expected.getSimpleName();
            throw new ClassCastException(String.format(format, actualName, expectedName));
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
     * Throws a {@code NoSuchElementException} if either a property's value is {@code null} or its type is unexpected.
     *
     * @param property property name.
     * @param value property's current value.
     * @param type expected value type.
     * @throws NoSuchElementException if {@code value} == null or its class != {@code type}.
     */
    private void checkPropertyExists(String property, Object value, Class type)
    {
        if (value == null || value.getClass() != type) {
            final String format = "No such %s property named \'%s\'";
            final String typeName = type.getSimpleName();
            throw new NoSuchElementException(String.format(format, typeName, property));
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
