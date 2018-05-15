package cinnamon.engine.event;

import cinnamon.engine.event.Gamepad.Connection;
import cinnamon.engine.utils.Table;

import java.util.Map;

/**
 * <p>This interface serves as an entry-point for {@link InputEvent}s into the input pipeline and provides
 * read-only access to the device states for the keyboard, mouse, and gamepad(s).</p>
 *
 * <p>It is up to implementations to decide how to harvest input. However, for user intervention, artificial input
 * can be explicitly provided through {@code submit(KeyEvent)} and its variations. Introducing events in this manner
 * ignore device mute status as this action is essentially an override.</p>
 *
 * <h3>Gamepad availability</h3>
 * <p>While the {@link Keyboard} and {@link Mouse} instances are always available through {@code getKeyboard()} and
 * {@code getMouse()}, gamepads can be unavailable. Aside from an unconnected controller, the following conditions
 * affect availability:</p>
 * <ul>
 *     <li>the number of available {@link Gamepad} instances cannot be more than the size of the set of values from
 *     {@link Connection}</li>
 *     <li>each connecting gamepad must have a matching {@link PadProfile} prior to connection</li>
 * </ul>
 *
 * <p>When a gamepad is disconnected, its corresponding {@code Gamepad} instance reflects this connection change and
 * is then discarded. The instance's state is no longer updated and a new instance must be retrieved through
 * {@code getGamepad(Connection)} (assuming a gamepad has connected to take its place).</p><br>
 *
 * <h3>Gamepad configuration</h3>
 * <p>A {@code PadProfile} must have already been added through {@code addGamepadProfile(PadProfile)} for a
 * {@code Gamepad} instance to be created and configured. If a gamepad connects to the machine with a device name
 * that does not match that of an added profile, the connection is ignored.</p><br>
 */
public interface Input
{
    /**
     * <p>Submits a {@code KeyEvent} to the input pipeline.</p>
     *
     * <p>This method is meant to allow the introduction of artificial input, i.e. input not actually coming from the
     * keyboard. The event will be processed even if {@link Keyboard#isMuted()} returns {@code true}.</p>
     *
     * @param event event.
     * @throws NullPointerException if event is null.
     */
    void submit(KeyEvent event);

    /**
     * <p>Submits a {@code MouseEvent} to the input pipeline.</p>
     *
     * <p>This method is meant to allow the introduction of artificial input, i.e. input not actually coming from the
     * mouse. The event will be processed even if {@link Mouse#isMuted()} returns {@code true}</p>
     *
     * @param event event.
     * @throws NullPointerException if event is null.
     */
    void submit(MouseEvent event);

    /**
     * <p>Submits a {@code PadEvent} to the input pipeline.</p>
     *
     * <p>This method is meant to allow the introduction of artificial input, i.e. input not actually coming from a
     * gamepad. The event will be processed even if {@link Gamepad#isMuted()} returns {@code true}. If the
     * corresponding source {@code Gamepad} is not available, this method does nothing.</p>
     *
     * @param event event.
     * @throws NullPointerException if event is null.
     */
    void submit(PadEvent event);

    /**
     * <p>Gets the keyboard.</p>
     *
     * @return keyboard.
     */
    Keyboard getKeyboard();

    /**
     * <p>Gets the mouse.</p>
     *
     * @return mouse.
     */
    Mouse getMouse();

    /**
     * <p>Gets a gamepad.</p>
     *
     * @param connection connection.
     * @return gamepad, or null if none is available through the specified connection.
     * @throws NullPointerException if connection is null.
     */
    Gamepad getGamepad(Connection connection);

    /**
     * <p>Gets a {@code Map} of all available {@code Gamepad}s.</p>
     *
     * @return available gamepads.
     */
    Map<Connection, Gamepad> getGamepads();

    /**
     * <p>Adds a profile for configuring connecting gamepads.</p>
     *
     * <p>Changes made to a profile after being added are not reflected in gamepad configurations. If a change must
     * be made, a new profile should be created.</p>
     *
     * @param name profile name.
     * @param profile profile.
     * @throws NullPointerException if name or profile is null.
     * @throws IllegalArgumentException if name is already in use by an existing profile.
     */
    void addGamepadProfile(String name, PadProfile profile);

    /**
     * <p>Checks if a {@code PadProfile} has been added with the given name.</p>
     *
     * @param name name.
     * @return true if name is already in use.
     * @throws NullPointerException if name is null.
     */
    boolean containsGamepadProfile(String name);

    /**
     * <p>Adds an {@code OnConnectionChangeListener} to be notified of gamepad connections and disconnections. The
     * listener will be called once the {@code Gamepad}'s connection state has been updated.</p>
     *
     * @param listener listener.
     * @throws NullPointerException if listener is null.
     */
    void addGamepadOnConnectionChangeListener(OnConnectionChangeListener listener);

    /**
     * <p>Removes an {@code OnConnectionChangeListener}.</p>
     *
     * @param listener listener.
     * @throws NullPointerException if listener is null.
     */
    void removeGamepadOnConnectionChangeListener(OnConnectionChangeListener listener);

    /**
     * <p>Listener for changes to a gamepad's connection state.</p>
     */
    interface OnConnectionChangeListener
    {
        /**
         * <p>Called when a gamepad's connection state has changed.</p>
         *
         * @param gamepad gamepad.
         */
        void onChange(Gamepad gamepad);
    }

    /**
     * <p>This interface allows read-only access to the event histories of input devices and is meant for systems
     * that need to read events older than the latest.</p>
     */
    interface InputHistories
    {
        /**
         * <p>Gets the keyboard's event history.</p>
         *
         * @return history.
         */
        Table<KeyEvent>[] getKeyboardHistory();

        /**
         * <p>Gets the mouse's button event history.</p>
         *
         * @return button history.
         */
        Table<MouseEvent>[] getMouseButtonHistory();

        /**
         * <p>Gets the mouse's scroll event history.</p>
         *
         * @return scroll history.
         */
        Table<MouseEvent> getMouseScrollHistory();

        /**
         * <p>Gets a gamepad's button-type event history.</p>
         *
         * @param connection connection.
         * @return button history, or null if there is no available gamepad for the connection.
         * @throws NullPointerException if connection is null.
         */
        Table<PadEvent>[] getGamepadButtonHistory(Connection connection);

        /**
         * <p>Gets a gamepad's axis-type event history.</p>
         *
         * @return motion history, or null if there is no available gamepad for the connection.
         * @param connection connection.
         * @throws NullPointerException if connection is null.
         */
        Table<PadEvent> getGamepadMotionHistory(Connection connection);
    }
}
