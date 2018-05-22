package cinnamon.engine.event;

import cinnamon.engine.event.Gamepad.AxisWrapper;
import cinnamon.engine.event.Gamepad.ButtonWrapper;
import cinnamon.engine.event.Gamepad.Connection;
import cinnamon.engine.event.Keyboard.Key;
import cinnamon.engine.event.Mouse.Button;

import java.util.*;

/**
 * <p>Sets bindings for the {@link Keyboard}, {@link Mouse}, and {@link Gamepad} input devices.</p>
 *
 * <h3>Bindings</h3>
 * <p>Bindings consists of a {@code Map} with a {@code String} key and either {@link ButtonRule}s or
 * {@link AxisRule}s for values as determined by the target input. For example, while keyboard events
 * describe press-release actions and so would be best served by {@code ButtonRule}, the analog sticks on a gamepad
 * produce a range of values more appropriate for {@code AxisRule}.</p>
 *
 * <p>The only exception to this is the scroll behavior with the mouse whose {@code Mouse.Button.MIDDLE} sensor uses
 * {@code AxisRule}.</p>
 *
 * <p><b>note</b> Changes made to {@code Map} instances do not affect the set control maps. If changes must be
 * made, the bindings must be set again.</p>
 */
public interface Controls
{
    /**
     * <p>Gets the current bindings for the keyboard's keys.</p>
     *
     * @return key bindings.
     */
    Map<String, ButtonRule<Key, KeyEvent>> getKeyboardKeys();

    /**
     * <p>Sets the bindings for the keyboard's keys.</p>
     *
     * <p>If the given {@code Map} is empty, any set bindings are cleared.</p>
     *
     * @param bindings key bindings.
     * @throws NullPointerException if bindings is null.
     */
    void setKeyboardKeys(Map<String, ButtonRule<Key, KeyEvent>> bindings);

    /**
     * <p>Gets the current bindings for the mouse's buttons.</p>
     *
     * @return mouse button bindings.
     */
    Map<String, ButtonRule<Button, MouseEvent>> getMouseButtons();

    /**
     * <p>Sets the bindings for the mouse's buttons.</p>
     *
     * <p>If the given {@code Map} is empty, any set bindings are cleared.</p>
     *
     * @param bindings mouse button bindings.
     * @throws NullPointerException if bindings is null.
     */
    void setMouseButtons(Map<String, ButtonRule<Button, MouseEvent>> bindings);

    /**
     * <p>Gets the current bindings for the mouse's scroll wheel.</p>
     *
     * @return mouse scroll wheel bindings.
     */
    Map<String, AxisRule<Button, MouseEvent>> getMouseScrolls();

    /**
     * <p>Sets the bindings for the mouse's scroll wheel. Each rule's constant must be solely
     * {@link Mouse.Button#MIDDLE}.</p>
     *
     * <p>If the given {@code Map} is empty, any set bindings are cleared.</p>
     *
     * @param bindings mouse scroll wheel bindings.
     * @throws NullPointerException if bindings is null.
     * @throws IllegalArgumentException if a rule's constant is not solely {@code Mouse.Button.MIDDLE}.
     */
    void setMouseScrolls(Map<String, AxisRule<Button, MouseEvent>> bindings);

    /**
     * <p>Gets the current bindings for a gamepad's buttons.</p>
     *
     * @param <T> type of button.
     * @param connection gamepad.
     * @param cls wrapper class.
     * @return bindings.
     * @throws NullPointerException if connection or cls is null.
     * @throws IllegalArgumentException if cls is not the class for the constant type used by the gamepad's profile.
     * @throws IllegalStateException if there is no available gamepad for the specified connection.
     */
    <T extends Enum<T> & ButtonWrapper> Map<String, ButtonRule<T, PadEvent>> getGamepadButtons(Connection connection,
                                                                                               Class<T> cls);
    /**
     * <p>Sets the bindings for a gamepad's buttons.</p>
     *
     * <p>If the given {@code Map} is empty, any set bindings are cleared.</p>
     *
     * @param connection gamepad.
     * @param bindings bindings.
     * @param <T> type of button.
     * @throws NullPointerException if connection or bindings is null.
     * @throws IllegalArgumentException if the bindings' constant type is not used by the gamepad's profile, assuming
     * bindings is not empty.
     * @throws IllegalStateException if there is no available gamepad for the specified connection.
     */
    <T extends Enum<T> & ButtonWrapper> void setGamepadButtons(Connection connection,
                                                               Map<String, ButtonRule<T, PadEvent>> bindings);

    /**
     * <p>Gets the bindings for a gamepad's axes.</p>
     *
     * @param connection gamepad.
     * @param cls wrapper class.
     * @param <T> type of axis.
     * @return bindings.
     * @throws NullPointerException if connection or cls is null.
     * @throws IllegalArgumentException if cls is not the class for the constant type used by the gamepad's profile.
     * @throws IllegalStateException if there is no available gamepad for the specified connection.
     */
    <T extends Enum<T> & AxisWrapper> Map<String, AxisRule<T, PadEvent>> getGamepadAxes(Connection connection,
                                                                                        Class<T> cls);

    /**
     * <p>Sets the bindings for a gamepad's axes.</p>
     *
     * <p>If the given {@code Map} is empty, any set bindings are cleared.</p>
     *
     * @param connection gamepad.
     * @param bindings bindings.
     * @param <T> type of axis.
     * @throws NullPointerException if connection or bindings is null.
     * @throws IllegalArgumentException if the bindings' constant type is not used by the gamepad's profile, assuming
     * bindings is not empty.
     * @throws IllegalStateException if there is no available gamepad for the specified connection.
     */
    <T extends Enum<T> & AxisWrapper> void setGamepadAxes(Connection connection,
                                                          Map<String, AxisRule<T, PadEvent>> bindings);

}
