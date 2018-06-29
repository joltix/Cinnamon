package cinnamon.engine.event;

import cinnamon.engine.event.Gamepad.*;
import cinnamon.engine.event.Input.InputHistories;
import cinnamon.engine.event.Input.OnProfileAddListener;
import cinnamon.engine.event.XB1.Stick;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InputControlsGamepadTest
{
    // Subject gamepad
    private static final Connection CONNECTION = Connection.PAD_1;

    // Name for a profile that won't be known to Input
    private static final String UNKNOWN_PROFILE_NAME = "fish";

    private static final String RULE_NAME = "test_rule";

    // Used by Input to represent added profiles
    private Map<String, PadProfile> mGamepadProfiles = new HashMap<>();

    // Triggered when faking a new profile from Input
    private OnProfileAddListener mProfileListener;

    // Devices
    private Input mInput;

    // Devices' histories
    private InputHistories mHistories;

    private InputControls mControls;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp()
    {
        mInput = InputControlsTestSuite.mockInput(mGamepadProfiles);
        mHistories = InputControlsTestSuite.mockInputHistories();

        interceptProfileListener(mInput);
        mControls = new InputControls(mInput, mock(EventSource.class), mHistories);
    }

    @After
    public void tearDown()
    {
        mGamepadProfiles = null;
        mInput = null;
        mHistories = null;
        mProfileListener = null;
        mControls = null;
    }

    @Test
    public void testGetGamepadButtonsInUse()
    {
        Assert.assertNotNull(mControls.getGamepadButtons(CONNECTION, XB1.Button.class));
    }

    @Test (expected = NullPointerException.class)
    public void testGetGamepadButtonsInUseNPEConnection()
    {
        Assert.assertNotNull(mControls.getGamepadButtons(null, XB1.Button.class));
    }

    @Test (expected = NullPointerException.class)
    public void testGetGamepadButtonsInUseNPECls()
    {
        Assert.assertNotNull(mControls.getGamepadButtons(CONNECTION, null));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testGetGamepadButtonsInUseIAEWrongClass()
    {
        Assert.assertNotNull(mControls.getGamepadButtons(CONNECTION, WrongButton.class));
    }

    @Test
    public void testGetGamepadButtons()
    {
        mockGamepadProfileAdded(XB1.GAMEPAD_NAME, XB1.GAMEPAD_PROFILE);

        Assert.assertNotNull(mControls.getGamepadButtons(CONNECTION, XB1.GAMEPAD_NAME, XB1.Button.class));
    }

    @Test (expected = NullPointerException.class)
    public void testGetGamepadButtonsNPEConnection()
    {
        mockGamepadProfileAdded(XB1.GAMEPAD_NAME, XB1.GAMEPAD_PROFILE);

        mControls.getGamepadButtons(null, XB1.GAMEPAD_NAME, XB1.Button.class);
    }

    @Test (expected = NullPointerException.class)
    public void testGetGamepadButtonsNPEProfile()
    {
        mockGamepadProfileAdded(XB1.GAMEPAD_NAME, XB1.GAMEPAD_PROFILE);

        mControls.getGamepadButtons(CONNECTION, null, XB1.Button.class);
    }

    @Test (expected = NullPointerException.class)
    public void testGetGamepadButtonsNPECls()
    {
        mockGamepadProfileAdded(XB1.GAMEPAD_NAME, XB1.GAMEPAD_PROFILE);

        mControls.getGamepadButtons(CONNECTION, XB1.GAMEPAD_NAME, null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testGetGamepadButtonsIAEWrongClass()
    {
        mockGamepadProfileAdded(XB1.GAMEPAD_NAME, XB1.GAMEPAD_PROFILE);

        mControls.getGamepadButtons(CONNECTION, XB1.GAMEPAD_NAME, WrongButton.class);
    }

    @Test (expected = NoSuchElementException.class)
    public void testGetGamepadButtonsNSEEProfileNotKnown()
    {
        mControls.getGamepadButtons(CONNECTION, UNKNOWN_PROFILE_NAME, XB1.Button.class);
    }

    @Test
    public void testGetGamepadButtonsReturnsEmpty()
    {
        mockGamepadProfileAdded(XB1.GAMEPAD_NAME, XB1.GAMEPAD_PROFILE);

        Assert.assertTrue(mControls.getGamepadButtons(CONNECTION, XB1.GAMEPAD_NAME, XB1.Button.class).isEmpty());
    }

    @Test
    public void testSetGamepadButtons()
    {
        mockGamepadProfileAdded(XB1.GAMEPAD_NAME, XB1.GAMEPAD_PROFILE);

        mControls.setGamepadButtons(CONNECTION, XB1.GAMEPAD_NAME, createXboxButtonMap());
    }

    @Test (expected = NullPointerException.class)
    public void testSetGamepadButtonsNPEConnection()
    {
        mockGamepadProfileAdded(XB1.GAMEPAD_NAME, XB1.GAMEPAD_PROFILE);

        mControls.setGamepadButtons(null, XB1.GAMEPAD_NAME, createXboxButtonMap());
    }

    @Test (expected = NullPointerException.class)
    public void testSetGamepadButtonsNPEProfile()
    {
        mockGamepadProfileAdded(XB1.GAMEPAD_NAME, XB1.GAMEPAD_PROFILE);

        mControls.setGamepadButtons(CONNECTION, null, createXboxButtonMap());
    }

    @Test (expected = NullPointerException.class)
    public void testSetGamepadButtonsNPEBindings()
    {
        mockGamepadProfileAdded(XB1.GAMEPAD_NAME, XB1.GAMEPAD_PROFILE);

        mControls.setGamepadButtons(CONNECTION, XB1.GAMEPAD_NAME, null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetGamepadButtonsIAEWrongClass()
    {
        mockGamepadProfileAdded(XB1.GAMEPAD_NAME, XB1.GAMEPAD_PROFILE);

        mControls.setGamepadButtons(CONNECTION, XB1.GAMEPAD_NAME, createMockButtonMap());
    }

    @Test (expected = NoSuchElementException.class)
    public void testSetGamepadButtonsNSEEProfileNotKnown()
    {
        mControls.setGamepadButtons(CONNECTION, UNKNOWN_PROFILE_NAME, createXboxButtonMap());
    }

    @Test
    public void testGetGamepadAxesInUse()
    {
        Assert.assertNotNull(mControls.getGamepadAxes(CONNECTION, Stick.class));
    }

    @Test (expected = NullPointerException.class)
    public void testGetGamepadAxesInUseNPEConnection()
    {
        mControls.getGamepadAxes(null, Stick.class);
    }

    @Test (expected = NullPointerException.class)
    public void testGetGamepadAxesInUseNPEClass()
    {
        mControls.getGamepadAxes(CONNECTION, null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testGetGamepadAxesInUseIAEWrongClass()
    {
        mControls.getGamepadAxes(CONNECTION, WrongAxis.class);
    }

    @Test
    public void testGetGamepadAxesInUseReturnsEmpty()
    {
        Assert.assertTrue(mControls.getGamepadAxes(CONNECTION, Stick.class).isEmpty());
    }

    @Test
    public void testGetGamepadAxes()
    {
        mockGamepadProfileAdded(XB1.GAMEPAD_NAME, XB1.GAMEPAD_PROFILE);

        Assert.assertNotNull(mControls.getGamepadAxes(CONNECTION, XB1.GAMEPAD_NAME, Stick.class));
    }

    @Test (expected = NullPointerException.class)
    public void testGetGamepadAxesNPEConnection()
    {
        mockGamepadProfileAdded(XB1.GAMEPAD_NAME, XB1.GAMEPAD_PROFILE);

        mControls.getGamepadAxes(null, XB1.GAMEPAD_NAME, Stick.class);
    }

    @Test (expected = NullPointerException.class)
    public void testGetGamepadAxesNPEProfile()
    {
        mockGamepadProfileAdded(XB1.GAMEPAD_NAME, XB1.GAMEPAD_PROFILE);

        mControls.getGamepadAxes(CONNECTION, null, Stick.class);
    }

    @Test (expected = NullPointerException.class)
    public void testGetGamepadAxesNPEClass()
    {
        mockGamepadProfileAdded(XB1.GAMEPAD_NAME, XB1.GAMEPAD_PROFILE);

        mControls.getGamepadAxes(CONNECTION, XB1.GAMEPAD_NAME, null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testGetGamepadAxesIAEWrongClass()
    {
        mockGamepadProfileAdded(XB1.GAMEPAD_NAME, XB1.GAMEPAD_PROFILE);

        mControls.getGamepadAxes(CONNECTION, XB1.GAMEPAD_NAME, WrongAxis.class);
    }

    @Test (expected = NoSuchElementException.class)
    public void testGetGamepadAxesNSEEProfileNotKnown()
    {
        mControls.getGamepadAxes(CONNECTION, UNKNOWN_PROFILE_NAME, XB1.Stick.class);
    }

    @Test
    public void testGetGamepadAxesReturnsEmpty()
    {
        mockGamepadProfileAdded(XB1.GAMEPAD_NAME, XB1.GAMEPAD_PROFILE);

        Assert.assertTrue(mControls.getGamepadAxes(CONNECTION, XB1.GAMEPAD_NAME, Stick.class).isEmpty());
    }

    @Test (expected = NullPointerException.class)
    public void testSetGamepadAxesNPEConnection()
    {
        mockGamepadProfileAdded(XB1.GAMEPAD_NAME, XB1.GAMEPAD_PROFILE);

        mControls.setGamepadAxes(null, XB1.GAMEPAD_NAME, createXboxAxisMap());
    }

    @Test (expected = NullPointerException.class)
    public void testSetGamepadAxesNPEProfile()
    {
        mockGamepadProfileAdded(XB1.GAMEPAD_NAME, XB1.GAMEPAD_PROFILE);

        mControls.setGamepadAxes(CONNECTION, null, createXboxAxisMap());
    }

    @Test (expected = NullPointerException.class)
    public void testSetGamepadAxesNPEBindings()
    {
        mockGamepadProfileAdded(XB1.GAMEPAD_NAME, XB1.GAMEPAD_PROFILE);

        mControls.setGamepadAxes(CONNECTION, XB1.GAMEPAD_NAME, null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetGamepadAxesIAEWrongClass()
    {
        mockGamepadProfileAdded(XB1.GAMEPAD_NAME, XB1.GAMEPAD_PROFILE);

        mControls.setGamepadAxes(CONNECTION, XB1.GAMEPAD_NAME, createMockAxisMap());
    }

    @Test (expected = NoSuchElementException.class)
    public void testSetGamepadAxesNSEEProfileNotKnown()
    {
        mControls.setGamepadAxes(CONNECTION, UNKNOWN_PROFILE_NAME, createXboxAxisMap());
    }

    private void mockGamepadProfileAdded(String name, PadProfile profile)
    {
        when(mInput.containsGamepadProfile(name)).thenReturn(true);

        // Make available for Controls to read from Input
        mGamepadProfiles.put(name, profile);

        // Fake notification for a new profile
        mProfileListener.onAdd(name, profile);
    }

    /**
     * <p>Prepares to intercept the listener passed to
     * {@link Input#addOnGamepadProfileAddListener(OnProfileAddListener)}. This method is required to be
     * executed prior to calls to {@link #mockGamepadProfileAdded(String, PadProfile)}.</p>
     *
     * @param input input.
     */
    private void interceptProfileListener(Input input)
    {
        assert (input != null);

        // Intercept profile add listener
        doAnswer(invocation ->
        {
            mProfileListener = (OnProfileAddListener) invocation.getArguments()[0];
            return null;
        }).when(input).addOnGamepadProfileAddListener(any(OnProfileAddListener.class));
    }

    private Map<String, ButtonRule<WrongButton, PadEvent>> createMockButtonMap()
    {
        final Map<String, ButtonRule<WrongButton, PadEvent>> b = new HashMap<>();
        b.put(RULE_NAME, new ButtonRule<>(WrongButton.BUTTON_0, (event) -> {}, ButtonPreferences.forRelease(), 0));
        return b;
    }

    private Map<String, AxisRule<WrongAxis, PadEvent>> createMockAxisMap()
    {
        final Map<String, AxisRule<WrongAxis, PadEvent>> b = new HashMap<>();
        b.put(RULE_NAME, new AxisRule<>(WrongAxis.AXIS_0, (event) -> {}, MotionPreferences.forTranslation(), 0));
        return b;
    }

    private Map<String, ButtonRule<XB1.Button, PadEvent>> createXboxButtonMap()
    {
        final Map<String, ButtonRule<XB1.Button, PadEvent>> b = new HashMap<>();
        b.put(RULE_NAME, new ButtonRule<>(XB1.Button.A, (event) -> {}, ButtonPreferences.forRelease(), 0));
        return b;
    }

    private Map<String, AxisRule<Stick, PadEvent>> createXboxAxisMap()
    {
        final Map<String, AxisRule<Stick, PadEvent>> b = new HashMap<>();
        b.put(RULE_NAME, new AxisRule<>(Stick.RIGHT_TRIGGER, (event) -> {}, MotionPreferences.forTranslation(), 0));
        return b;
    }

    /**
     * <p>Used to force an {@code IllegalArgumentException} since gamepad won't be configured for this button class.</p>
     */
    private enum WrongButton implements ButtonWrapper
    {
        BUTTON_0;

        @Override
        public Gamepad.Button button()
        {
            return null;
        }
    }

    /**
     * <p>Used to force an {@code IllegalArgumentException} since gamepad won't be configured for this axis class.</p>
     */
    private enum WrongAxis implements AxisWrapper
    {
        AXIS_0;

        @Override
        public Axis vertical()
        {
            return null;
        }

        @Override
        public Axis horizontal()
        {
            return null;
        }
    }
}
