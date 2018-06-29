package cinnamon.engine.event;

import cinnamon.engine.event.Input.InputHistories;
import cinnamon.engine.event.Keyboard.Key;
import cinnamon.engine.utils.FixedQueueArray;
import cinnamon.engine.utils.Point;
import cinnamon.engine.utils.Table;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Suite.class)
@Suite.SuiteClasses({InputControlsKeyboardAndMouseTest.class, InputControlsGamepadTest.class})
public class InputControlsTestSuite
{
    // Number of events to track per hardware sensor
    private static final int EVENT_HISTORY_LENGTH = 4;

    /**
     * <p>Creates a mock of {@code Input}.</p>
     *
     * <p>The following methods are supported.</p>
     * <ul>
     *     <li>getKeyboard()</li>
     *     <li>getMouse()</li>
     *     <li>getGamepadProfiles()</li>
     * </ul>
     *
     * @param profiles supported gamepad profiles.
     * @return input.
     */
    static Input mockInput(Map<String, PadProfile> profiles)
    {
        assert (profiles != null);

        final int len = EVENT_HISTORY_LENGTH;

        final Table<KeyEvent> keyPressHistory = new FixedQueueArray<>(Key.COUNT, len);
        final FixedQueueArray<KeyEvent> keyReleaseHistory = new FixedQueueArray<>(Key.COUNT, len);
        final FixedQueueArray<MouseEvent> buttonPressHistory = new FixedQueueArray<>(Mouse.Button.COUNT, len);
        final FixedQueueArray<MouseEvent> buttonReleaseHistory = new FixedQueueArray<>(Mouse.Button.COUNT, len);
        final FixedQueueArray<MouseEvent> mouseScrollHistory = new FixedQueueArray<>(1, len);

        final Input input = mock(Input.class);
        when(input.getGamepadProfiles()).thenReturn(profiles);
        when(input.getKeyboard()).thenReturn(new Keyboard(keyPressHistory, keyReleaseHistory));
        when(input.getMouse()).thenReturn(new Mouse(createMouseState(buttonPressHistory, buttonReleaseHistory,
                mouseScrollHistory)));

        return input;
    }

    /**
     * <p>Mocks {@code InputHistories}.</p>
     *
     * <p>The following methods are supported. </p>
     * <ul>
     *     <li>getKeyboardHistory()</li>
     *     <li>getMouseButtonHistory()</li>
     *     <li>getMouseScrollHistory()</li>
     * </ul>
     *
     * @return input histories
     */
    @SuppressWarnings("unchecked")
    static InputHistories mockInputHistories()
    {
        final Table<KeyEvent>[] keyboardHistory = new Table[] {mock(Table.class), mock(Table.class)};
        final Table<MouseEvent>[] mouseHistory = new Table[] {mock(Table.class), mock(Table.class)};
        final InputHistories histories = mock(InputHistories.class);

        when(histories.getKeyboardHistory()).thenReturn(keyboardHistory);
        when(histories.getMouseButtonHistory()).thenReturn(mouseHistory);
        when(histories.getMouseScrollHistory()).thenReturn(mock(Table.class));

        return histories;
    }

    private static Mouse.State createMouseState(FixedQueueArray<MouseEvent> pressHistory,
                                                FixedQueueArray<MouseEvent> releaseHistory,
                                                FixedQueueArray<MouseEvent> scrollHistory)
    {
        return Mouse.State.builder()
                .pressHistory(pressHistory)
                .releaseHistory(releaseHistory)
                .scrollHistory(scrollHistory)
                .position(new Point())
                .build();
    }
}
