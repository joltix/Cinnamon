package cinnamon.engine.event;

import cinnamon.engine.utils.PackedEnumIntMap;
import cinnamon.engine.utils.IntMap;
import cinnamon.engine.utils.IntMap.IntWrapper;
import cinnamon.engine.utils.Table;
import org.lwjgl.glfw.GLFW;

/**
 * <p>An event-based representation of the user's keyboard. Updating the keyboard's state requires updating the
 * table of events provided during instantiation.</p>
 */
public final class Keyboard implements EventSilenceable
{
    /**
     * <p>Keyboard key constants.</p>
     */
    public enum Key implements IntWrapper
    {
        // Numerical (10)
        KEY_0 (GLFW.GLFW_KEY_0, "0"),
        KEY_1 (GLFW.GLFW_KEY_1, "1"),
        KEY_2 (GLFW.GLFW_KEY_2, "2"),
        KEY_3 (GLFW.GLFW_KEY_3, "3"),
        KEY_4 (GLFW.GLFW_KEY_4, "4"),
        KEY_5 (GLFW.GLFW_KEY_5, "5"),
        KEY_6 (GLFW.GLFW_KEY_6, "6"),
        KEY_7 (GLFW.GLFW_KEY_7, "7"),
        KEY_8 (GLFW.GLFW_KEY_8, "8"),
        KEY_9 (GLFW.GLFW_KEY_9, "9"),

        // Punctuation (11)
        KEY_GRAVE (GLFW.GLFW_KEY_GRAVE_ACCENT, "`"),
        KEY_MINUS (GLFW.GLFW_KEY_MINUS, "-"),
        KEY_EQUAL (GLFW.GLFW_KEY_EQUAL, "="),
        KEY_OPEN_BRACKET (GLFW.GLFW_KEY_LEFT_BRACKET, "["),
        KEY_CLOSE_BRACKET (GLFW.GLFW_KEY_RIGHT_BRACKET, "]"),
        KEY_BACK_SLASH (GLFW.GLFW_KEY_BACKSLASH, "\\"),
        KEY_SEMICOLON (GLFW.GLFW_KEY_SEMICOLON, ";"),
        KEY_APOSTROPHE (GLFW.GLFW_KEY_APOSTROPHE, "'"),
        KEY_COMMA (GLFW.GLFW_KEY_COMMA, ","),
        KEY_PERIOD (GLFW.GLFW_KEY_PERIOD, "."),
        KEY_FORWARD_SLASH (GLFW.GLFW_KEY_SLASH, "/"),

        // Auxiliary (20)
        KEY_ESCAPE (GLFW.GLFW_KEY_ESCAPE, "ESCAPE"),
        KEY_ENTER (GLFW.GLFW_KEY_ENTER, "ENTER"),
        KEY_SPACE (GLFW.GLFW_KEY_SPACE, "SPACE"),
        KEY_BACKSPACE (GLFW.GLFW_KEY_BACKSPACE, "BACKSPACE"),
        KEY_TAB (GLFW.GLFW_KEY_TAB, "TAB"),
        KEY_CAPS_LOCK (GLFW.GLFW_KEY_CAPS_LOCK, "CAPS_LOCK"),
        KEY_LEFT_CTRL (GLFW.GLFW_KEY_LEFT_CONTROL, "LEFT_CONTROL"),
        KEY_LEFT_SHIFT (GLFW.GLFW_KEY_LEFT_SHIFT, "LEFT_SHIFT"),
        KEY_LEFT_ALT (GLFW.GLFW_KEY_LEFT_ALT, "LEFT_ALT"),
        KEY_RIGHT_CTRL (GLFW.GLFW_KEY_RIGHT_CONTROL, "RIGHT_CONTROL"),
        KEY_RIGHT_SHIFT (GLFW.GLFW_KEY_RIGHT_SHIFT, "RIGHT_SHIFT"),
        KEY_RIGHT_ALT (GLFW.GLFW_KEY_RIGHT_ALT, "RIGHT_ALT"),
        KEY_RIGHT (GLFW.GLFW_KEY_RIGHT, "RIGHT"),
        KEY_DOWN (GLFW.GLFW_KEY_DOWN, "DOWN"),
        KEY_LEFT (GLFW.GLFW_KEY_LEFT, "LEFT"),
        KEY_UP (GLFW.GLFW_KEY_UP, "UP"),
        KEY_LEFT_SUPER (GLFW.GLFW_KEY_LEFT_SUPER, "LEFT_SUPER"),
        KEY_RIGHT_SUPER (GLFW.GLFW_KEY_RIGHT_SUPER, "RIGHT_SUPER"),
        KEY_PAGE_UP (GLFW.GLFW_KEY_PAGE_UP, "PAGE_UP"),
        KEY_PAGE_DOWN (GLFW.GLFW_KEY_PAGE_DOWN, "PAGE_DOWN"),

        // Function (12)
        KEY_F1 (GLFW.GLFW_KEY_F1, "F1"),
        KEY_F2 (GLFW.GLFW_KEY_F2, "F2"),
        KEY_F3 (GLFW.GLFW_KEY_F3, "F3"),
        KEY_F4 (GLFW.GLFW_KEY_F4, "F4"),
        KEY_F5 (GLFW.GLFW_KEY_F5, "F5"),
        KEY_F6 (GLFW.GLFW_KEY_F6, "F6"),
        KEY_F7 (GLFW.GLFW_KEY_F7, "F7"),
        KEY_F8 (GLFW.GLFW_KEY_F8, "F8"),
        KEY_F9 (GLFW.GLFW_KEY_F9, "F9"),
        KEY_F10 (GLFW.GLFW_KEY_F10, "F10"),
        KEY_F11 (GLFW.GLFW_KEY_F11, "F11"),
        KEY_F12 (GLFW.GLFW_KEY_F12, "F12"),

        // English alphabet (26)
        KEY_A (GLFW.GLFW_KEY_A, "A"),
        KEY_B (GLFW.GLFW_KEY_B, "B"),
        KEY_C (GLFW.GLFW_KEY_C, "C"),
        KEY_D (GLFW.GLFW_KEY_D, "D"),
        KEY_E (GLFW.GLFW_KEY_E, "E"),
        KEY_F (GLFW.GLFW_KEY_F, "F"),
        KEY_G (GLFW.GLFW_KEY_G, "G"),
        KEY_H (GLFW.GLFW_KEY_H, "H"),
        KEY_I (GLFW.GLFW_KEY_I, "I"),
        KEY_J (GLFW.GLFW_KEY_J, "J"),
        KEY_K (GLFW.GLFW_KEY_K, "K"),
        KEY_L (GLFW.GLFW_KEY_L, "L"),
        KEY_M (GLFW.GLFW_KEY_M, "M"),
        KEY_N (GLFW.GLFW_KEY_N, "N"),
        KEY_O (GLFW.GLFW_KEY_O, "O"),
        KEY_P (GLFW.GLFW_KEY_P, "P"),
        KEY_Q (GLFW.GLFW_KEY_Q, "Q"),
        KEY_R (GLFW.GLFW_KEY_R, "R"),
        KEY_S (GLFW.GLFW_KEY_S, "S"),
        KEY_T (GLFW.GLFW_KEY_T, "T"),
        KEY_U (GLFW.GLFW_KEY_U, "U"),
        KEY_V (GLFW.GLFW_KEY_V, "V"),
        KEY_W (GLFW.GLFW_KEY_W, "W"),
        KEY_X (GLFW.GLFW_KEY_X, "X"),
        KEY_Y (GLFW.GLFW_KEY_Y, "Y"),
        KEY_Z (GLFW.GLFW_KEY_Z, "Z");

        /**
         * <p>Number of keys.</p>
         */
        public static final int COUNT = Key.values().length;

        // Mapping between keys and lower level constants
        private static final IntMap<Key> MAPPING = new PackedEnumIntMap<>(Key.class);

        // Lower level constant
        private final int mConstant;

        // Text representation
        private final String mString;

        Key(int constant, String string)
        {
            mConstant = constant;
            mString = string;
        }

        @Override
        public int toInt()
        {
            return mConstant;
        }

        @Override
        public String toString()
        {
            return mString;
        }

        /**
         * <p>Gets the {@code Key} equivalent of a lower level constant.</p>
         *
         * @param constant lower level constant.
         * @return key or null if unrecognized.
         */
        public static Key from(int constant)
        {
            return MAPPING.get(constant);
        }
    }

    // Press event history
    private final Table<KeyEvent> mPresses;

    // Release event history
    private final Table<KeyEvent> mReleases;

    // True if events should be ignored
    private boolean mMuted;

    /**
     * <p>Constructs a {@code Keyboard} around event histories for press and releases.</p>
     *
     * @param pressHistory press event history.
     * @param releaseHistory release event history.
     * @throws NullPointerException if pressHistory or releaseHistory is null.
     */
    public Keyboard(Table<KeyEvent> pressHistory, Table<KeyEvent> releaseHistory)
    {
        checkNull(pressHistory);
        checkNull(releaseHistory);

        mPresses = pressHistory;
        mReleases = releaseHistory;
    }

    /**
     * <p>Checks if the most recent event for a key is a press.</p>
     *
     * @param key key.
     * @return true if pressed.
     * @throws NullPointerException if key is null.
     */
    public boolean isPressed(Key key)
    {
        checkNull(key);

        return PressChecker.isPressed(key, mPresses, mReleases);
    }

    @Override
    public boolean isMuted()
    {
        return mMuted;
    }

    @Override
    public void mute()
    {
        mMuted = true;
    }

    @Override
    public void unmute()
    {
        mMuted = false;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }

    private void checkNull(Object object)
    {
        if (object == null) {
            throw new NullPointerException();
        }
    }
}
