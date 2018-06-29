package cinnamon.engine.event;

import cinnamon.engine.event.Input.InputHistories;
import cinnamon.engine.event.Keyboard.Key;
import cinnamon.engine.event.Mouse.Button;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

public class InputControlsKeyboardAndMouseTest
{
    private static final String RULE_NAME = "test_rule";

    // Devices
    private Input mInput;

    // Devices' histories
    private InputHistories mHistories;

    private InputControls mControls;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp()
    {
        mHistories = InputControlsTestSuite.mockInputHistories();
        mInput = InputControlsTestSuite.mockInput(new HashMap<>());
        mControls = new InputControls(mInput, mock(EventSource.class), mHistories);
    }

    @After
    public void tearDown()
    {
        mHistories = null;
        mInput = null;
        mControls = null;
    }

    @SuppressWarnings("unchecked")
    @Test (expected = NullPointerException.class)
    public void testConstructorNPEInput()
    {
        new InputControls(null, mock(EventSource.class), mHistories);
    }

    @Test (expected = NullPointerException.class)
    public void testConstructorNPESource()
    {
        new InputControls(mInput, null, mHistories);
    }

    @SuppressWarnings("unchecked")
    @Test (expected = NullPointerException.class)
    public void testConstructorNPEHistories()
    {
        new InputControls(mInput, mock(EventSource.class), null);
    }

    @Test
    public void testExecute()
    {
        mControls.execute();
    }

    @Test
    public void testGetKeyboardKeys()
    {
        Assert.assertNotNull(mControls.getKeyboardKeys());
    }

    @Test
    public void testGetKeyboardKeysReturnsEmpty()
    {
        Assert.assertTrue(mControls.getKeyboardKeys().isEmpty());
    }

    @Test
    public void testGetKeyboardKeysReturnsPreviouslySet()
    {
        final Map<String, ButtonRule<Key, KeyEvent>> b = new HashMap<>();
        b.put(RULE_NAME, new ButtonRule<>(Key.KEY_SPACE, (event) -> {}, ButtonPreferences.forRelease(), 0));

        mControls.setKeyboardKeys(b);

        Assert.assertEquals(b, mControls.getKeyboardKeys());
    }

    @Test
    public void testSetKeyboardKeys()
    {
        mControls.setKeyboardKeys(new HashMap<>());
    }

    @Test (expected = NullPointerException.class)
    public void testSetKeyboardKeysNPE()
    {
        mControls.setKeyboardKeys(null);
    }

    @Test
    public void testGetMouseButtons()
    {
        Assert.assertNotNull(mControls.getMouseButtons());
    }

    @Test
    public void testGetMouseButtonsReturnsEmpty()
    {
        Assert.assertTrue(mControls.getMouseButtons().isEmpty());
    }

    @Test
    public void testGetMouseButtonsReturnsPreviouslySet()
    {
        final Map<String, ButtonRule<Button, MouseEvent>> b = new HashMap<>();
        b.put(RULE_NAME, new ButtonRule<>(Button.LEFT, (event) -> {}, ButtonPreferences.forRelease(), 0));

        mControls.setMouseButtons(b);

        Assert.assertEquals(b, mControls.getMouseButtons());
    }

    @Test
    public void testSetMouseButtons()
    {
        mControls.setMouseButtons(new HashMap<>());
    }

    @Test (expected = NullPointerException.class)
    public void testSetMouseButtonsNPE()
    {
        mControls.setMouseButtons(null);
    }

    @Test
    public void testGetMouseScrolls()
    {
        Assert.assertNotNull(mControls.getMouseScrolls());
    }

    @Test
    public void testGetMouseScrollsReturnsEmpty()
    {
        Assert.assertTrue(mControls.getMouseScrolls().isEmpty());
    }

    @Test
    public void testGetMouseScrollsReturnsPreviouslySet()
    {
        final HashMap<String, AxisRule<Button, MouseEvent>> b = new HashMap<>();
        b.put(RULE_NAME, new AxisRule<>(Button.MIDDLE, (event) -> {}, MotionPreferences.forTranslation(), 0));

        mControls.setMouseScrolls(b);

        Assert.assertEquals(b, mControls.getMouseScrolls());
    }

    @Test
    public void testSetMouseScrolls()
    {
        mControls.setMouseScrolls(new HashMap<>());
    }

    @Test (expected = NullPointerException.class)
    public void testSetMouseScrollsNPE()
    {
        mControls.setMouseScrolls(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetMouseScrollsIAERuleDoesNotUseMiddleButton()
    {
        final HashMap<String, AxisRule<Button, MouseEvent>> b = new HashMap<>();
        b.put(RULE_NAME, new AxisRule<>(Button.LEFT, (event) -> {}, MotionPreferences.forTranslation(), 0));

        mControls.setMouseScrolls(b);
    }
}
