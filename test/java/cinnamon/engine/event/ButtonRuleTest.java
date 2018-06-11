package cinnamon.engine.event;

import cinnamon.engine.event.Keyboard.Key;
import org.junit.Assert;
import org.junit.Test;

public class ButtonRuleTest
{
    private static final EventListener<KeyEvent> LISTENER = (event) -> { };

    private static final ButtonPreferences PREFS_SINGLE = ButtonPreferences.forRelease();
    private static final ButtonPreferences PREFS_MULTI = ButtonPreferences.forMultiRelease(150L);

    private static final Key CONSTANT_SINGLE = Key.KEY_SPACE;
    private static final Key[] CONSTANT_MULTI = {Key.KEY_LEFT_SHIFT, Key.KEY_SPACE};
    private static final Key[] CONSTANT_MULTI_EMPTY = {null};

    @Test
    public void testConstructorSingleConstant()
    {
        new ButtonRule<>(CONSTANT_SINGLE, LISTENER, PREFS_SINGLE, 0);
    }

    @Test (expected = NullPointerException.class)
    public void testConstructorSingleConstantNPEListener()
    {
        new ButtonRule<>(CONSTANT_SINGLE, null, PREFS_SINGLE, 0);
    }

    @Test (expected = NullPointerException.class)
    public void testConstructorSingleConstantNPEPreferences()
    {
        new ButtonRule<>(CONSTANT_SINGLE, LISTENER, null, 0);
    }

    @Test (expected = NullPointerException.class)
    public void testConstructorSingleConstantNPEConstant()
    {
        final Key constant = null;
        new ButtonRule<>(constant, LISTENER, PREFS_SINGLE, 0);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorSingleConstantIAEMultiPreferencesWithSingleConstant()
    {
        new ButtonRule<>(CONSTANT_SINGLE, LISTENER, PREFS_MULTI, 0);
    }

    @Test
    public void testConstructorMultiConstant()
    {
        new ButtonRule<>(CONSTANT_MULTI, LISTENER, PREFS_MULTI, 0);
    }

    @Test (expected = NullPointerException.class)
    public void testConstructorMultiConstantNPEListener()
    {
        new ButtonRule<>(CONSTANT_MULTI, null, PREFS_MULTI, 0);
    }

    @Test (expected = NullPointerException.class)
    public void testConstructorMultiConstantNPEPreferences()
    {
        new ButtonRule<>(CONSTANT_MULTI, LISTENER, null, 0);
    }

    @Test (expected = NullPointerException.class)
    public void testConstructorMultiConstantNPEConstant()
    {
        final Key[] constant = null;
        new ButtonRule<>(constant, LISTENER, PREFS_MULTI, 0);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorMultiConstantIAEArrayHasAllNullElements()
    {
        new ButtonRule<>(CONSTANT_MULTI_EMPTY, LISTENER, PREFS_MULTI, 0);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorMultiConstantIAEMultiPreferencesWithSingleConstant()
    {
        new ButtonRule<>(new Key[] {CONSTANT_SINGLE}, LISTENER, PREFS_MULTI, 0);
    }

    @Test
    public void testEqualsReflexive()
    {
        final ButtonRule<Key, KeyEvent> rule = new ButtonRule<>(CONSTANT_MULTI, LISTENER, PREFS_MULTI, 0);

        Assert.assertEquals(rule, rule);
    }

    @Test
    public void testEqualsSymmetric()
    {
        final ButtonRule<Key, KeyEvent> a = new ButtonRule<>(CONSTANT_MULTI, LISTENER, PREFS_MULTI, 0);
        final ButtonRule<Key, KeyEvent> b = new ButtonRule<>(CONSTANT_MULTI, LISTENER, PREFS_MULTI, 0);

        Assert.assertTrue(a.equals(b) && b.equals(a));
    }

    @Test
    public void testEqualsTransitive()
    {
        final ButtonRule<Key, KeyEvent> a = new ButtonRule<>(CONSTANT_MULTI, LISTENER, PREFS_MULTI, 0);
        final ButtonRule<Key, KeyEvent> b = new ButtonRule<>(CONSTANT_MULTI, LISTENER, PREFS_MULTI, 0);
        final ButtonRule<Key, KeyEvent> c = new ButtonRule<>(CONSTANT_MULTI, LISTENER, PREFS_MULTI, 0);

        Assert.assertTrue(a.equals(b) && b.equals(c) && a.equals(c));
    }

    @Test
    public void testEqualsReturnsFalse()
    {
        final ButtonRule<Key, KeyEvent> a = new ButtonRule<>(CONSTANT_MULTI, LISTENER, PREFS_MULTI, 0);
        final ButtonRule<Key, KeyEvent> b = new ButtonRule<>(CONSTANT_SINGLE, LISTENER, PREFS_SINGLE, 0);

        Assert.assertNotEquals(a, b);
    }

    @Test
    public void testEqualsNullReturnsFalse()
    {
        Assert.assertNotEquals(new ButtonRule<>(CONSTANT_MULTI, LISTENER, PREFS_MULTI, 0), null);
    }

    @Test
    public void testHashCodesAreEquivalent()
    {
        final ButtonRule<Key, KeyEvent> a = new ButtonRule<>(CONSTANT_MULTI, LISTENER, PREFS_MULTI, 0);
        final ButtonRule<Key, KeyEvent> b = new ButtonRule<>(CONSTANT_MULTI, LISTENER, PREFS_MULTI, 0);

        Assert.assertEquals(a.hashCode(), b.hashCode());
    }
}
