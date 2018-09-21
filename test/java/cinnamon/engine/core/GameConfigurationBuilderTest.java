package cinnamon.engine.core;

import cinnamon.engine.core.Game.Preference;
import cinnamon.engine.gfx.WindowTestSuite.DummyCanvas;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class GameConfigurationBuilderTest
{
    private static final String BLANK_STRING = "   ";

    private static final String TITLE = GameConfigurationBuilderTest.class.getSimpleName();

    private static final String DEVELOPER = "Developer Name";

    private static final String VERSION = "0.001";

    private static final int BUILD = 0;

    private static final int TICK_RATE = 30;

    private static final Preference KEY_STRING = Preference.ICON;

    private static final Preference KEY_INT = Preference.RESOLUTION_X;

    private static final Preference KEY_BOOL = Preference.VSYNC;

    private static final String VALUE_STRING = "cat";

    private static final int VALUE_INT = 42;

    private static final boolean VALUE_BOOL = true;

    private Game.Configuration.Builder mBuilder;

    @Before
    public void setUp()
    {
        mBuilder = new Game.Configuration.Builder();
    }

    @After
    public void tearDown()
    {
        mBuilder = null;
    }

    @Test
    public void testWithHeader()
    {
        mBuilder.withHeader(TITLE, DEVELOPER);
    }

    @Test (expected = NullPointerException.class)
    public void testWithHeaderNPETitle()
    {
        mBuilder.withHeader(null, DEVELOPER);
    }

    @Test (expected = NullPointerException.class)
    public void testWithHeaderNPEDeveloper()
    {
        mBuilder.withHeader(TITLE, null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testWithHeaderIAEBlankTitle()
    {
        mBuilder.withHeader(BLANK_STRING, DEVELOPER);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testWithHeaderIAEBlankDeveloper()
    {
        mBuilder.withHeader(TITLE, BLANK_STRING);
    }

    @Test
    public void testWithVersion()
    {
        mBuilder.withVersion(VERSION, BUILD);
    }

    @Test (expected = NullPointerException.class)
    public void testWithVersionNPEVersion()
    {
        mBuilder.withVersion(null, BUILD);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testWithVersionIAEVersionBlankVersion()
    {
        mBuilder.withVersion(" ", BUILD);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testWithversionIAENegativeBuild()
    {
        mBuilder.withVersion(VERSION, -1);
    }

    @Test
    public void testWithTickRate()
    {
        mBuilder.withTickRate(TICK_RATE);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testWithTickRateIAENonPositiveRate()
    {
        mBuilder.withTickRate(0);
    }

    @Test
    public void testWithCanvas()
    {
        mBuilder.withCanvas(new DummyCanvas());
    }

    @Test (expected = NullPointerException.class)
    public void testWithCanvasNPECanvas()
    {
        mBuilder.withCanvas(null);
    }

    @Test
    public void testBuild()
    {
        mBuilder.withHeader(TITLE, DEVELOPER)
                .withVersion(VERSION, BUILD)
                .withTickRate(TICK_RATE)
                .withCanvas(new DummyCanvas());

        mBuilder.withPreference(KEY_STRING, VALUE_STRING)
                .withPreference(KEY_INT, VALUE_INT)
                .withPreference(KEY_BOOL, VALUE_BOOL);

        Assert.assertNotNull(mBuilder.build());
    }

    @Test (expected = IllegalStateException.class)
    public void testBuildISEUnspecifiedHeader()
    {
        mBuilder.withTickRate(TICK_RATE)
                .withCanvas(new DummyCanvas());

        mBuilder.build();
    }

    @Test (expected = IllegalStateException.class)
    public void testBuildISEUnspecifiedVersion()
    {
        mBuilder.withHeader(TITLE, DEVELOPER)
                .withTickRate(TICK_RATE)
                .withCanvas(new DummyCanvas());

        mBuilder.build();
    }

    @Test (expected = IllegalStateException.class)
    public void testBuildISEUnspecifiedTickRate()
    {
        mBuilder.withHeader(TITLE, DEVELOPER);
        mBuilder.withVersion(VERSION, BUILD);
        mBuilder.withCanvas(new DummyCanvas());

        mBuilder.build();
    }

    @Test (expected = IllegalStateException.class)
    public void testBuildISEUnspecifiedCanvas()
    {
        mBuilder.withHeader(TITLE, DEVELOPER);
        mBuilder.withVersion(VERSION, BUILD);
        mBuilder.withTickRate(TICK_RATE);

        mBuilder.build();
    }

    @Test
    public void testWithPreferenceString()
    {
        mBuilder.withPreference(KEY_STRING, VALUE_STRING);
    }

    @Test (expected = NullPointerException.class)
    public void testWithPreferenceStringNPEKey()
    {
        mBuilder.withPreference(null, VALUE_STRING);
    }

    @Test (expected = NullPointerException.class)
    public void testWithPreferenceStringNPEValue()
    {
        mBuilder.withPreference(KEY_STRING, null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testWithPreferenceStringIAEUnexpectedValueType()
    {
        mBuilder.withPreference(KEY_INT, VALUE_STRING);
    }

    @Test
    public void testWithPreferenceInt()
    {
        mBuilder.withPreference(KEY_INT, VALUE_INT);
    }

    @Test (expected = NullPointerException.class)
    public void testWithPreferenceIntNPEKey()
    {
        mBuilder.withPreference(null, VALUE_INT);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testWithPreferenceIntIAEUnexpectedValueType()
    {
        mBuilder.withPreference(KEY_BOOL, VALUE_INT);
    }

    @Test
    public void testWithPreferenceBoolean()
    {
        mBuilder.withPreference(KEY_BOOL, VALUE_BOOL);
    }

    @Test (expected = NullPointerException.class)
    public void testWithPreferenceBooleanNPEKey()
    {
        mBuilder.withPreference(null, VALUE_BOOL);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testWithPreferenceBooleanIAEUnexpectedValueType()
    {
        mBuilder.withPreference(KEY_STRING, VALUE_BOOL);
    }
}
