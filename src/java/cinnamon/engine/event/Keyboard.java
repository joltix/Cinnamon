package cinnamon.engine.event;

import cinnamon.engine.utils.PackedEnumIntMap;
import cinnamon.engine.utils.IntMap;
import cinnamon.engine.utils.IntMap.IntWrapper;
import cinnamon.engine.utils.Table;
import org.lwjgl.glfw.GLFW;

/**
 * <p>{@code Keyboard} represents the keyboard input device, providing a view of key states. Updates must be set
 * through the press and release event histories passed to the {@code Keyboard}'s constructor.</p>
 */
public final class Keyboard implements EventSilenceable
{
    /**
     * <p>Keyboard key constants.</p>
     */
    public enum Key implements IntWrapper
    {
        // Numerical constants (10)
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

        // Punctuation constants (11)
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

        // Auxiliary constants (20)
        KEY_ESCAPE (GLFW.GLFW_KEY_ESCAPE, "escape"),
        KEY_ENTER (GLFW.GLFW_KEY_ENTER, "enter"),
        KEY_SPACE (GLFW.GLFW_KEY_SPACE, "space"),
        KEY_BACKSPACE (GLFW.GLFW_KEY_BACKSPACE, "backspace"),
        KEY_TAB (GLFW.GLFW_KEY_TAB, "tab"),
        KEY_CAPS_LOCK (GLFW.GLFW_KEY_CAPS_LOCK, "caps lock"),
        KEY_LEFT_CTRL (GLFW.GLFW_KEY_LEFT_CONTROL, "left_control"),
        KEY_LEFT_SHIFT (GLFW.GLFW_KEY_LEFT_SHIFT, "left_shift"),
        KEY_LEFT_ALT (GLFW.GLFW_KEY_LEFT_ALT, "left_alt"),
        KEY_RIGHT_CTRL (GLFW.GLFW_KEY_RIGHT_CONTROL, "right_control"),
        KEY_RIGHT_SHIFT (GLFW.GLFW_KEY_RIGHT_SHIFT, "right_shift"),
        KEY_RIGHT_ALT (GLFW.GLFW_KEY_RIGHT_ALT, "right_alt"),
        KEY_RIGHT (GLFW.GLFW_KEY_RIGHT, "right"),
        KEY_DOWN (GLFW.GLFW_KEY_DOWN, "down"),
        KEY_LEFT (GLFW.GLFW_KEY_LEFT, "left"),
        KEY_UP (GLFW.GLFW_KEY_UP, "up"),
        KEY_LEFT_SUPER (GLFW.GLFW_KEY_LEFT_SUPER, "left_super"),
        KEY_RIGHT_SUPER (GLFW.GLFW_KEY_RIGHT_SUPER, "right_super"),
        KEY_PAGE_UP (GLFW.GLFW_KEY_PAGE_UP, "page up"),
        KEY_PAGE_DOWN (GLFW.GLFW_KEY_PAGE_DOWN, "page down"),

        // Function constants (12)
        KEY_F1 (GLFW.GLFW_KEY_F1, "f1"),
        KEY_F2 (GLFW.GLFW_KEY_F2, "f2"),
        KEY_F3 (GLFW.GLFW_KEY_F3, "f3"),
        KEY_F4 (GLFW.GLFW_KEY_F4, "f4"),
        KEY_F5 (GLFW.GLFW_KEY_F5, "f5"),
        KEY_F6 (GLFW.GLFW_KEY_F6, "f6"),
        KEY_F7 (GLFW.GLFW_KEY_F7, "f7"),
        KEY_F8 (GLFW.GLFW_KEY_F8, "f8"),
        KEY_F9 (GLFW.GLFW_KEY_F9, "f9"),
        KEY_F10 (GLFW.GLFW_KEY_F10, "f10"),
        KEY_F11 (GLFW.GLFW_KEY_F11, "f11"),
        KEY_F12 (GLFW.GLFW_KEY_F12, "f12"),

        // English alphabet constants (26)
        KEY_A (GLFW.GLFW_KEY_A, "a"),
        KEY_B (GLFW.GLFW_KEY_B, "b"),
        KEY_C (GLFW.GLFW_KEY_C, "c"),
        KEY_D (GLFW.GLFW_KEY_D, "d"),
        KEY_E (GLFW.GLFW_KEY_E, "e"),
        KEY_F (GLFW.GLFW_KEY_F, "f"),
        KEY_G (GLFW.GLFW_KEY_G, "g"),
        KEY_H (GLFW.GLFW_KEY_H, "h"),
        KEY_I (GLFW.GLFW_KEY_I, "i"),
        KEY_J (GLFW.GLFW_KEY_J, "j"),
        KEY_K (GLFW.GLFW_KEY_K, "k"),
        KEY_L (GLFW.GLFW_KEY_L, "l"),
        KEY_M (GLFW.GLFW_KEY_M, "m"),
        KEY_N (GLFW.GLFW_KEY_N, "n"),
        KEY_O (GLFW.GLFW_KEY_O, "o"),
        KEY_P (GLFW.GLFW_KEY_P, "p"),
        KEY_Q (GLFW.GLFW_KEY_Q, "q"),
        KEY_R (GLFW.GLFW_KEY_R, "r"),
        KEY_S (GLFW.GLFW_KEY_S, "s"),
        KEY_T (GLFW.GLFW_KEY_T, "t"),
        KEY_U (GLFW.GLFW_KEY_U, "u"),
        KEY_V (GLFW.GLFW_KEY_V, "v"),
        KEY_W (GLFW.GLFW_KEY_W, "w"),
        KEY_X (GLFW.GLFW_KEY_X, "x"),
        KEY_Y (GLFW.GLFW_KEY_Y, "y"),
        KEY_Z (GLFW.GLFW_KEY_Z, "z");

        /**
         * <p>Key count.</p>
         */
        public static final int COUNT = Key.values().length;

        private static final IntMap<Key> MAPPING = new PackedEnumIntMap<>(Key.class);

        private final int mGLFW;
        private final String mString;

        Key(int glfw, String string)
        {
            mGLFW = glfw;
            mString = string;
        }

        @Override
        public int toInt()
        {
            return mGLFW;
        }

        @Override
        public String toString()
        {
            return mString;
        }

        /**
         * <p>Gets the {@code Key} equivalent of a GLFW keyboard key constant.</p>
         *
         * @param glfw GLFW key.
         * @return key, or null if unrecognized.
         */
        public static Key from(int glfw)
        {
            return MAPPING.get(glfw);
        }
    }

    private static final PressCondition<KeyEvent> mPressCondition = new PressCondition<>();

    private final Table<KeyEvent> mPresses;
    private final Table<KeyEvent> mReleases;

    private boolean mMuted;

    /**
     * <p>Constructs a {@code Keyboard} with a {@code ButtonHistory} as its state.</p>
     *
     * @param pressHistory press events.
     * @param releaseHistory release events
     * @throws NullPointerException if history is null.
     */
    public Keyboard(Table<KeyEvent> pressHistory, Table<KeyEvent> releaseHistory)
    {
        if (pressHistory == null) {
            throw new NullPointerException();
        }
        if (releaseHistory == null) {
            throw new NullPointerException();
        }

        mPresses = pressHistory;
        mReleases = releaseHistory;
    }

    /**
     * <p>Checks if the last event for the given key is a press.</p>
     *
     * @param key key.
     * @return {@inheritDoc}
     * @throws NullPointerException if key is null.
     */
    public boolean isPressed(Key key)
    {
        if (key == null) {
            throw new NullPointerException();
        }

        return mPressCondition.isPressed(key, mPresses, mReleases);
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
}
