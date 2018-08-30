package cinnamon.engine.utils;

import org.junit.*;

import java.util.*;

/**
 * Each supported type is tested with two properties: modifiable and unmodifiable. Because of this, the total number
 * of properties is 2x the number of supported types.
 */
public class PropertyMapTest
{
    private static final String UNRECOGNIZED_PROPERTY_NAME = "unknown";

    private static final String PROPERTY_STRING = "prop_string";

    private static final String PROPERTY_STRING_UNMODIFIABLE = "prop_string_unmodifiable";

    private static final String PROPERTY_DOUBLE = "prop_double";

    private static final String PROPERTY_DOUBLE_UNMODIFIABLE = "prop_double_unmodifiable";

    private static final String PROPERTY_INTEGER = "prop_integer";

    private static final String PROPERTY_INTEGER_UNMODIFIABLE = "prop_integer_unmodifiable";

    private static final String PROPERTY_BOOLEAN = "prop_boolean";

    private static final String PROPERTY_BOOLEAN_UNMODIFIABLE = "prop_boolean_unmodifiable";

    private static final String STRING = "chicken";

    private static final String STRING_UNMODIFIABLE = "pork";

    private static final double DOUBLE = 343.343d;

    private static final double DOUBLE_UNMODIFIABLE = 42.42d;

    private static final int INTEGER = 50;

    private static final int INTEGER_UNMODIFIABLE = 256;

    private static final boolean BOOLEAN = true;

    private static final boolean BOOLEAN_UNMODIFIABLE = false;

    // Properties and values
    private Map<String, Object> mProperties;

    // Expected property types
    private Map<String, Class> mExpectations;

    // Unmodifiable properties
    private Set<String> mUnmodifiables;

    private PropertyMap mMap;

    @Before
    public void setUp()
    {
        setUpPropertyMapArguments();

        mMap = new PropertyMap(mProperties, mExpectations, mUnmodifiables);
    }

    @After
    public void tearDown()
    {
        mProperties.clear();
        mExpectations.clear();
        mUnmodifiables.clear();

        mMap = null;
    }

    @Test (expected = NullPointerException.class)
    public void testConstructorNPEProperties()
    {
        new PropertyMap(null, new HashMap<>(mExpectations), new HashSet<>(mUnmodifiables));
    }

    @Test (expected = NullPointerException.class)
    public void testConstructorNPEUnmodifiables()
    {
        new PropertyMap(new HashMap<>(mProperties), new HashMap<>(mExpectations), null);
    }

    @Test (expected = NullPointerException.class)
    public void testConstructorNPEExpectations()
    {
        new PropertyMap(new HashMap<>(mProperties), null, new HashSet<>(mUnmodifiables));
    }

    @Test (expected = ClassCastException.class)
    public void testConstructorClassCastExceptionUnexpectedValueType()
    {
        final Map<String, Class> expectations = new HashMap<>(mExpectations);
        expectations.put(PROPERTY_STRING, Double.class);

        new PropertyMap(new HashMap<>(mProperties), expectations, new HashSet<>(mUnmodifiables));
    }

    @Test (expected = NoSuchElementException.class)
    public void testConstructorNSEEPropertyHasNullValue()
    {
        final Map<String, Object> properties = new HashMap<>(mProperties);
        properties.put(UNRECOGNIZED_PROPERTY_NAME, null);

        new PropertyMap(properties, new HashMap<>(mExpectations), new HashSet<>(mUnmodifiables));
    }

    @Test (expected = NoSuchElementException.class)
    public void testConstructorNSEEExpectationHasNullValue()
    {
        final Map<String, Class> expectations = new HashMap<>(mExpectations);
        expectations.put(PROPERTY_STRING, null);

        new PropertyMap(new HashMap<>(mProperties), expectations, new HashSet<>(mUnmodifiables));
    }

    @Test (expected = NullPointerException.class)
    public void testConstructorNPELocked()
    {
        new PropertyMap(new HashMap<>(mProperties), new HashMap<>(mExpectations), null);
    }

    @Test
    public void testGetStringProperty()
    {
        Assert.assertEquals(STRING, mMap.getStringProperty(PROPERTY_STRING));
    }

    @Test (expected = NullPointerException.class)
    public void testGetStringPropertyNPEName()
    {
        mMap.getStringProperty(null);
    }

    @Test (expected = NoSuchElementException.class)
    public void testGetStringPropertyNSEEUnrecognizedPropertyName()
    {
        mMap.getStringProperty(UNRECOGNIZED_PROPERTY_NAME);
    }

    @Test
    public void testSetStringProperty()
    {
        mMap.setStringProperty(PROPERTY_STRING, STRING);
    }

    @Test (expected = NullPointerException.class)
    public void testSetStringPropertyNPEName()
    {
        mMap.setStringProperty(null, STRING);
    }

    @Test (expected = NullPointerException.class)
    public void testSetStringPropertyNPEValue()
    {
        mMap.setStringProperty(PROPERTY_STRING, null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetStringPropertyIAELockedProperty()
    {
        mMap.setStringProperty(PROPERTY_STRING_UNMODIFIABLE, STRING);
    }

    @Test (expected = ClassCastException.class)
    public void testSetStringPropertyClassCastExceptionUnexpectedValueType()
    {
        mMap.setStringProperty(PROPERTY_DOUBLE, STRING);
    }

    @Test
    public void testGetDoubleProperty()
    {
        Assert.assertEquals(DOUBLE, mMap.getDoubleProperty(PROPERTY_DOUBLE), 0d);
    }

    @Test (expected = NullPointerException.class)
    public void testGetDoublePropertyNPEName()
    {
        mMap.getDoubleProperty(null);
    }

    @Test (expected = NoSuchElementException.class)
    public void testGetDoublePropertyNSEEUnrecognizedPropertyName()
    {
        mMap.getDoubleProperty(UNRECOGNIZED_PROPERTY_NAME);
    }

    @Test
    public void testSetDoubleProperty()
    {
        mMap.setDoubleProperty(PROPERTY_DOUBLE, DOUBLE_UNMODIFIABLE);
    }

    @Test (expected = NullPointerException.class)
    public void testSetDoublePropertyNPEName()
    {
        mMap.setDoubleProperty(null, DOUBLE_UNMODIFIABLE);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetDoublePropertyIAELockedProperty()
    {
        mMap.setDoubleProperty(PROPERTY_DOUBLE_UNMODIFIABLE, DOUBLE);
    }

    @Test (expected = ClassCastException.class)
    public void testSetDoublePropertyClassCastExceptionUnexpectedValueType()
    {
        mMap.setDoubleProperty(PROPERTY_STRING, DOUBLE);
    }

    @Test
    public void testGetIntegerProperty()
    {
        Assert.assertEquals(INTEGER, mMap.getIntegerProperty(PROPERTY_INTEGER));
    }

    @Test (expected = NullPointerException.class)
    public void testGetIntegerPropertyNPEName()
    {
        mMap.getIntegerProperty(null);
    }

    @Test (expected = NoSuchElementException.class)
    public void testGetIntegerPropertyNSEEUnrecognizedPropertyName()
    {
        mMap.getIntegerProperty(UNRECOGNIZED_PROPERTY_NAME);
    }

    @Test
    public void testSetIntegerProperty()
    {
        mMap.setIntegerProperty(PROPERTY_INTEGER, INTEGER_UNMODIFIABLE);
    }

    @Test (expected = NullPointerException.class)
    public void testSetIntegerPropertyNPEName()
    {
        mMap.setIntegerProperty(null, INTEGER_UNMODIFIABLE);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetIntegerPropertyIAELockedProperty()
    {
        mMap.setIntegerProperty(PROPERTY_INTEGER_UNMODIFIABLE, INTEGER);
    }

    @Test (expected = ClassCastException.class)
    public void testSetIntegerPropertyClassCastExceptionUnexpectedValueType()
    {
        mMap.setIntegerProperty(PROPERTY_DOUBLE, INTEGER);
    }

    @Test
    public void testGetBooleanProperty()
    {
        Assert.assertEquals(BOOLEAN, mMap.getBooleanProperty(PROPERTY_BOOLEAN));
    }

    @Test (expected = NullPointerException.class)
    public void testGetBooleanPropertyNPEName()
    {
        mMap.getBooleanProperty(null);
    }

    @Test (expected = NoSuchElementException.class)
    public void testGetBooleanPropertyNSEEUnrecognizedPropertyName()
    {
        mMap.getBooleanProperty(UNRECOGNIZED_PROPERTY_NAME);
    }

    @Test
    public void testSetBooleanProperty()
    {
        mMap.setBooleanProperty(PROPERTY_BOOLEAN, BOOLEAN_UNMODIFIABLE);
    }

    @Test (expected = NullPointerException.class)
    public void testSetBooleanPropertyNPEName()
    {
        mMap.setBooleanProperty(null, BOOLEAN_UNMODIFIABLE);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetBooleanPropertyIAELockedProperty()
    {
        mMap.setBooleanProperty(PROPERTY_BOOLEAN_UNMODIFIABLE, BOOLEAN);
    }

    @Test (expected = ClassCastException.class)
    public void testSetBooleanPropertyClassCastExceptionUnexpectedValueType()
    {
        mMap.setBooleanProperty(PROPERTY_INTEGER, BOOLEAN);
    }

    @Test
    public void testSetUnmodifiableStringProperty()
    {
        mMap.setUnmodifiableStringProperty(PROPERTY_STRING_UNMODIFIABLE, STRING);
    }

    @Test (expected = NullPointerException.class)
    public void testSetUnmodifiableStringPropertyNPEName()
    {
        mMap.setUnmodifiableStringProperty(null, STRING);
    }

    @Test (expected = NullPointerException.class)
    public void testSetUnmodifiableStringPropertyNPEValue()
    {
        mMap.setUnmodifiableStringProperty(PROPERTY_STRING_UNMODIFIABLE, null);
    }

    @Test (expected = ClassCastException.class)
    public void testSetUnmodifiableStringPropertyClassCastExceptionUnexpectedValueType()
    {
        mMap.setUnmodifiableStringProperty(PROPERTY_DOUBLE_UNMODIFIABLE, STRING);
    }

    @Test
    public void testSetUnmodifiableDoubleProperty()
    {
        mMap.setUnmodifiableDoubleProperty(PROPERTY_DOUBLE_UNMODIFIABLE, DOUBLE);
    }

    @Test (expected = NullPointerException.class)
    public void testSetUnmodifiableDoublePropertyNPEName()
    {
        mMap.setUnmodifiableDoubleProperty(null, DOUBLE);
    }

    @Test (expected = ClassCastException.class)
    public void testSetUnmodifiableDoublePropertyClassCastExceptionUnexpectedValueType()
    {
        mMap.setUnmodifiableDoubleProperty(PROPERTY_STRING_UNMODIFIABLE, DOUBLE);
    }

    @Test
    public void testSetUnmodifiableIntegerProperty()
    {
        mMap.setUnmodifiableIntegerProperty(PROPERTY_INTEGER_UNMODIFIABLE, INTEGER);
    }

    @Test (expected = NullPointerException.class)
    public void testSetUnmodifiableIntegerPropertyNPEName()
    {
        mMap.setUnmodifiableIntegerProperty(null, INTEGER);
    }

    @Test (expected = ClassCastException.class)
    public void testSetUnmodifiableIntegerPropertyClassCastExceptionUnexpectedValueType()
    {
        mMap.setUnmodifiableIntegerProperty(PROPERTY_STRING_UNMODIFIABLE, INTEGER);
    }

    @Test
    public void testSetUnmodifiableBooleanProperty()
    {
        mMap.setUnmodifiableBooleanProperty(PROPERTY_BOOLEAN_UNMODIFIABLE, BOOLEAN);
    }

    @Test (expected = NullPointerException.class)
    public void testSetUnmodifiableBooleanPropertyNPEName()
    {
        mMap.setUnmodifiableBooleanProperty(null, BOOLEAN);
    }

    @Test (expected = ClassCastException.class)
    public void testSetUnmodifiableBooleanPropertyClassCastExceptionUnexpectedValueType()
    {
        mMap.setUnmodifiableBooleanProperty(PROPERTY_STRING_UNMODIFIABLE, BOOLEAN);
    }

    @Test
    public void testContainsProperty()
    {
        Assert.assertTrue(mMap.containsProperty(PROPERTY_STRING));
    }

    @Test
    public void testContainsPropertyReturnsFalse()
    {
        Assert.assertFalse(mMap.containsProperty(UNRECOGNIZED_PROPERTY_NAME));
    }

    @Test (expected = NullPointerException.class)
    public void testContainsPropertyNPEName()
    {
        mMap.containsProperty(null);
    }

    @Test
    public void testAsMapReturnsExpectedKeysAndValues()
    {
        final Map<String, Object> map = mMap.asMap();

        Assert.assertEquals(mProperties, map);
    }

    @Test
    public void testAsMapReturnsNewInstances()
    {
        Assert.assertNotSame(mMap.asMap(), mMap.asMap());
    }

    @Test
    public void testEqualsReturnsFalse()
    {
        Assert.assertNotEquals(mMap, new Object());
    }

    @Test
    public void testEqualsReflexive()
    {
        Assert.assertEquals(mMap, mMap);
    }

    @Test
    public void testEqualsSymmetric()
    {
        final PropertyMap other = new PropertyMap(mProperties, mExpectations, mUnmodifiables);

        Assert.assertTrue(mMap.equals(other) && other.equals(mMap));
    }

    @Test
    public void testEqualsTransitive()
    {
        final PropertyMap mapB = new PropertyMap(mProperties, mExpectations, mUnmodifiables);
        final PropertyMap mapC = new PropertyMap(mProperties, mExpectations, mUnmodifiables);

        Assert.assertTrue(mMap.equals(mapB) && mapB.equals(mapC) && mMap.equals(mapC));
    }

    @Test
    public void testEqualsNullReturnFalse()
    {
        Assert.assertNotEquals(mMap, null);
    }

    @Test
    public void testHashCodesAreEqual()
    {
        final PropertyMap other = new PropertyMap(mProperties, mExpectations, mUnmodifiables);

        Assert.assertEquals(mMap.hashCode(), other.hashCode());
    }

    @Test
    public void testNonexistentUnmodifiablePropertyFailsToStopChanges()
    {
        mUnmodifiables.remove(PROPERTY_STRING_UNMODIFIABLE);
        final PropertyMap map = new PropertyMap(mProperties, mExpectations, mUnmodifiables);

        map.setStringProperty(PROPERTY_STRING_UNMODIFIABLE, STRING);
    }

    private void setUpPropertyMapArguments()
    {
        mProperties = new HashMap<>();
        mExpectations = new HashMap<>();
        mUnmodifiables = new HashSet<>();

        mProperties.put(PROPERTY_STRING, STRING);
        mProperties.put(PROPERTY_STRING_UNMODIFIABLE, STRING_UNMODIFIABLE);
        mProperties.put(PROPERTY_DOUBLE, DOUBLE);
        mProperties.put(PROPERTY_DOUBLE_UNMODIFIABLE, DOUBLE_UNMODIFIABLE);
        mProperties.put(PROPERTY_INTEGER, INTEGER);
        mProperties.put(PROPERTY_INTEGER_UNMODIFIABLE, INTEGER_UNMODIFIABLE);
        mProperties.put(PROPERTY_BOOLEAN, BOOLEAN);
        mProperties.put(PROPERTY_BOOLEAN_UNMODIFIABLE, BOOLEAN_UNMODIFIABLE);

        mExpectations.put(PROPERTY_STRING, String.class);
        mExpectations.put(PROPERTY_STRING_UNMODIFIABLE, String.class);
        mExpectations.put(PROPERTY_DOUBLE, Double.class);
        mExpectations.put(PROPERTY_DOUBLE_UNMODIFIABLE, Double.class);
        mExpectations.put(PROPERTY_INTEGER, Integer.class);
        mExpectations.put(PROPERTY_INTEGER_UNMODIFIABLE, Integer.class);
        mExpectations.put(PROPERTY_BOOLEAN, Boolean.class);
        mExpectations.put(PROPERTY_BOOLEAN_UNMODIFIABLE, Boolean.class);

        mUnmodifiables.add(PROPERTY_STRING_UNMODIFIABLE);
        mUnmodifiables.add(PROPERTY_DOUBLE_UNMODIFIABLE);
        mUnmodifiables.add(PROPERTY_INTEGER_UNMODIFIABLE);
        mUnmodifiables.add(PROPERTY_BOOLEAN_UNMODIFIABLE);
    }
}
