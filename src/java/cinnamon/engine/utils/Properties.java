package cinnamon.engine.utils;

import java.util.NoSuchElementException;

/**
 * Allows read and write of named values with basic types (i.e. {@code String}, {@code double}, {@code int}, or
 * {@code boolean}).
 *
 * <p>Properties are intended to always be readable; as a result, {@code String} values are not permitted to be {@code
 * null}.</p>
 *
 * <p>{@code Float} values are foregone for {@code double}s as the extra precision stored can be more useful since the
 * amount of floating point properties are not expected to be large.</p>
 */
public interface Properties
{
    /**
     * Gets a string property.
     *
     * @param name property name.
     * @return value.
     * @throws NullPointerException if name is null.
     * @throws NoSuchElementException if there is no string property with the given name.
     */
    String getStringProperty(String name);

    /**
     * Gets a double property.
     *
     * @param name property name.
     * @return value.
     * @throws NullPointerException if name is null.
     * @throws NoSuchElementException if there is no double property with the given name.
     */
    double getDoubleProperty(String name);

    /**
     * Gets an integer property.
     *
     * @param name property name.
     * @return value.
     * @throws NullPointerException if name is null.
     * @throws NoSuchElementException if there is no integer property with the given name.
     */
    int getIntegerProperty(String name);

    /**
     * Gets a boolean property.
     *
     * @param name property name.
     * @return value.
     * @throws NullPointerException if name is null.
     * @throws NoSuchElementException if there is no boolean property with the given name.
     */
    boolean getBooleanProperty(String name);

    /**
     * Sets a string property.
     *
     * @param name property name.
     * @param value value.
     * @throws NullPointerException if name or value is null.
     * @throws IllegalArgumentException if the property either cannot be changed or uses a different value type.
     */
    void setStringProperty(String name, String value);

    /**
     * Sets a double property.
     *
     * @param name property name.
     * @param value value.
     * @throws NullPointerException if name is null.
     * @throws IllegalArgumentException if the property either cannot be changed or uses a different value type.
     */
    void setDoubleProperty(String name, double value);

    /**
     * Sets an integer property.
     *
     * @param name property name.
     * @param value value.
     * @throws NullPointerException if name is null.
     * @throws IllegalArgumentException if the property either cannot be changed or uses a different value type.
     */
    void setIntegerProperty(String name, int value);

    /**
     * Sets a boolean property.
     *
     * @param name property name.
     * @param value value.
     * @throws NullPointerException if name is null.
     * @throws IllegalArgumentException if the property either cannot be changed or uses a different value type.
     */
    void setBooleanProperty(String name, boolean value);

    /**
     * Checks if a property exists with the given name.
     *
     * <p>This method will not differentiate between a property of matching name but incorrect value type. The value
     * type of a given property name should be expected and should not be tested for.</p>
     *
     * @param name property name.
     * @return true if property exists.
     * @throws NullPointerException if name is null.
     */
    boolean containsProperty(String name);
}
