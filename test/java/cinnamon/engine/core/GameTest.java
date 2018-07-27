package cinnamon.engine.core;

import cinnamon.engine.core.GameTestSuite.TestCanvas;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.NoSuchElementException;

import static cinnamon.engine.core.GameTestSuite.*;

public class GameTest
{
    // How long game should run, in milliseconds
    private static final long GAME_DURATION = 4000L;

    private static final int TEST_SPECIFIC_TICK_RATE = 70;

    private Game mGame;

    @Before
    public void setUp()
    {
        final Map<String, Object> properties = GameTestSuite.createWorkingProperties();
        properties.put(Game.TICK_RATE, TEST_SPECIFIC_TICK_RATE);

        mGame = new AutoStopGame(new TestCanvas(), properties, GAME_DURATION);
    }

    @After
    public void tearDown()
    {
        mGame = null;
    }

    @Test (expected = NullPointerException.class)
    public void testConstructorNPECanvas()
    {

        final Map<String, Object> properties = GameTestSuite.createWorkingProperties();
        new AutoStopGame(null, properties);
    }

    @Test (expected = NullPointerException.class)
    public void testConstructorNPEProperties()
    {
        new AutoStopGame(new TestCanvas(), null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAEUnexpectedValueType()
    {
        final Map<String, Object> properties = GameTestSuite.createWorkingProperties();
        properties.put(Game.TITLE, 42f);

        new AutoStopGame(new TestCanvas(), properties);
    }

    @Test (expected = NoSuchElementException.class)
    public void testConstructorNSEEPropertyHasMissingValue()
    {
        final Map<String, Object> properties = GameTestSuite.createWorkingProperties();
        properties.put(Game.TITLE, null);

        new AutoStopGame(new TestCanvas(), properties);
    }

    @Test
    public void testGetMeasuredTickRate()
    {
        mGame.start();

        final int diff = Math.abs(mGame.getMeasuredTickRate() - TEST_SPECIFIC_TICK_RATE);

        Assert.assertTrue(diff >= 0 && diff < 2);
    }

    @Test
    public void testIsRunning()
    {
        final Map<String, Object> properties = GameTestSuite.createWorkingProperties();
        mGame = new AutoStopGame(new TestCanvas(), properties, GAME_DURATION / 4, () ->
        {
            Assert.assertTrue(mGame.isRunning());
        });

        mGame.start();
    }

    @Test
    public void testIsRunningReturnsFalseBeforeStart()
    {
        Assert.assertFalse(mGame.isRunning());
    }

    @Test
    public void testIsRunningReturnsFalseAfterStop()
    {
        final Map<String, Object> properties = GameTestSuite.createWorkingProperties();
        mGame = new AutoStopGame(new TestCanvas(), properties, 0L, () ->
        {
            mGame.stop();
            Assert.assertFalse(mGame.isRunning());
        });

        mGame.start();
    }

    @Test
    public void testGetStringProperty()
    {
        Assert.assertEquals(TITLE, mGame.getStringProperty(Game.TITLE));
    }

    @Test (expected = NullPointerException.class)
    public void testGetStringPropertyNPEName()
    {
        mGame.getStringProperty(null);
    }

    @Test (expected = NoSuchElementException.class)
    public void testGetStringPropertyNSEEUnrecognizedPropertyName()
    {
        mGame.getStringProperty(UNRECOGNIZED_PROPERTY_NAME);
    }

    @Test
    public void testSetStringProperty()
    {
        mGame.setStringProperty(CUSTOM_STRING_PROPERTY_NAME, "something");
    }

    @Test (expected = NullPointerException.class)
    public void testSetStringPropertyNPEName()
    {
        mGame.setStringProperty(null, TITLE);
    }

    @Test (expected = NullPointerException.class)
    public void testSetStringPropertyNPEValue()
    {
        mGame.setStringProperty(Game.WINDOW_TITLE, null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetStringPropertyIAELockedProperty()
    {
        mGame.setStringProperty(Game.TITLE, TITLE);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetStringPropertyIAEUnexpectedValueType()
    {
        mGame.setStringProperty(Game.BUILD, TITLE);
    }

    @Test
    public void testGetDoubleProperty()
    {
        Assert.assertEquals(BUILD, mGame.getDoubleProperty(Game.BUILD), 0d);
    }

    @Test (expected = NullPointerException.class)
    public void testGetDoublePropertyNPEName()
    {
        mGame.getDoubleProperty(null);
    }

    @Test (expected = NoSuchElementException.class)
    public void testGetDoublePropertyNSEEUnrecognizedPropertyName()
    {
        mGame.getDoubleProperty(UNRECOGNIZED_PROPERTY_NAME);
    }

    @Test
    public void testSetDoubleProperty()
    {
        mGame.setDoubleProperty(CUSTOM_DOUBLE_PROPERTY_NAME, BUILD);
    }

    @Test (expected = NullPointerException.class)
    public void testSetDoublePropertyNPEName()
    {
        mGame.setDoubleProperty(null, BUILD);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetDoublePropertyIAELockedProperty()
    {
        mGame.setDoubleProperty(Game.BUILD, BUILD);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetDoublePropertyIAEUnexpectedValueType()
    {
        mGame.setDoubleProperty(Game.TITLE, 42d);
    }

    @Test
    public void testGetIntegerProperty()
    {
        Assert.assertEquals(TEST_SPECIFIC_TICK_RATE, mGame.getIntegerProperty(Game.TICK_RATE));
    }

    @Test (expected = NullPointerException.class)
    public void testGetIntegerPropertyNPEName()
    {
        mGame.getIntegerProperty(null);
    }

    @Test (expected = NoSuchElementException.class)
    public void testGetIntegerPropertyNSEEUnrecognizedPropertyName()
    {
        mGame.getIntegerProperty(UNRECOGNIZED_PROPERTY_NAME);
    }

    @Test
    public void testSetIntegerProperty()
    {
        mGame.setIntegerProperty(CUSTOM_INTEGER_PROPERTY_NAME, 42);
    }

    @Test (expected = NullPointerException.class)
    public void testSetIntegerPropertyNPEName()
    {
        mGame.setIntegerProperty(null, 42);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetIntegerPropertyIAELockedProperty()
    {
        mGame.setIntegerProperty(Game.TICK_RATE, TEST_SPECIFIC_TICK_RATE);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetIntegerPropertyIAEUnexpectedValueType()
    {
        mGame.setIntegerProperty(Game.WINDOW_TITLE, 42);
    }

    @Test
    public void testGetBooleanProperty()
    {
        Assert.assertTrue(mGame.getBooleanProperty(Game.WINDOW_BORDERLESS));
    }

    @Test (expected = NullPointerException.class)
    public void testGetBooleanPropertyNPEName()
    {
        mGame.getBooleanProperty(null);
    }

    @Test (expected = NoSuchElementException.class)
    public void testGetBooleanPropertyNSEEUnrecognizedPropertyName()
    {
        mGame.getBooleanProperty(UNRECOGNIZED_PROPERTY_NAME);
    }

    @Test
    public void testSetBooleanProperty()
    {
        mGame.setBooleanProperty(CUSTOM_BOOLEAN_PROPERTY_NAME, true);
    }

    @Test (expected = NullPointerException.class)
    public void testSetBooleanPropertyNPEName()
    {
        mGame.setBooleanProperty(null, true);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetBooleanPropertyIAELockedProperty()
    {
        mGame.setBooleanProperty(Game.WINDOW_BORDERLESS, false);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetBooleanPropertyIAEUnexpectedValueType()
    {
        mGame.setBooleanProperty(Game.WINDOW_TITLE, true);
    }

    @Test
    public void testContainsProperty()
    {
        Assert.assertTrue(mGame.containsProperty(Game.TITLE));
    }

    @Test
    public void testContainsPropertyReturnsFalse()
    {
        Assert.assertFalse(mGame.containsProperty(CUSTOM_STRING_PROPERTY_NAME + "."));
    }

    @Test (expected = NullPointerException.class)
    public void testContainsPropertyNPEName()
    {
        mGame.containsProperty(null);
    }
}
