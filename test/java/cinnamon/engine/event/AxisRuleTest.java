package cinnamon.engine.event;

import cinnamon.engine.event.XB1.Stick;
import org.junit.Assert;
import org.junit.Test;

public class AxisRuleTest
{
    private static final EventListener<PadEvent> LISTENER = (event) -> {};

    private static final MotionPreferences PREFS = MotionPreferences.forTranslation();

    private static final Stick AXIS_A = XB1.Stick.RIGHT_TRIGGER;
    private static final Stick AXIS_B = XB1.Stick.LEFT_TRIGGER;

    @Test (expected = NullPointerException.class)
    public void testConstructorNPEListener()
    {
        new AxisRule<>(AXIS_A, null, PREFS, 0);
    }

    @Test (expected = NullPointerException.class)
    public void testConstructorNPEPreferences()
    {
        new AxisRule<>(AXIS_A, LISTENER, null, 0);
    }

    @Test (expected = NullPointerException.class)
    public void testConstructorNPEConstant()
    {
        new AxisRule<>(null, LISTENER, PREFS, 0);
    }

    @Test
    public void testEqualsReflexive()
    {
        final AxisRule<Stick, PadEvent> rule = new AxisRule<>(AXIS_A, LISTENER, PREFS, 0);

        Assert.assertTrue(rule.equals(rule));
    }

    @Test
    public void testEqualsSymmetric()
    {
        final AxisRule<Stick, PadEvent> a = new AxisRule<>(AXIS_A, LISTENER, PREFS, 0);
        final AxisRule<Stick, PadEvent> b = new AxisRule<>(AXIS_A, LISTENER, PREFS, 0);

        Assert.assertTrue(a.equals(b) && b.equals(a));
    }

    @Test
    public void testEqualsTransitive()
    {
        final AxisRule<Stick, PadEvent> a = new AxisRule<>(AXIS_A, LISTENER, PREFS, 0);
        final AxisRule<Stick, PadEvent> b = new AxisRule<>(AXIS_A, LISTENER, PREFS, 0);
        final AxisRule<Stick, PadEvent> c = new AxisRule<>(AXIS_A, LISTENER, PREFS, 0);

        Assert.assertTrue(a.equals(b) && b.equals(c) && a.equals(c));
    }

    @Test
    public void testEqualsReturnsFalse()
    {
        final AxisRule<Stick, PadEvent> a = new AxisRule<>(AXIS_A, LISTENER, PREFS, 0);
        final AxisRule<Stick, PadEvent> b = new AxisRule<>(AXIS_B, LISTENER, PREFS, 0);

        Assert.assertFalse(a.equals(b));
    }

    @Test
    public void testEqualsNullReturnsFalse()
    {
        Assert.assertFalse(new AxisRule<>(AXIS_A, LISTENER, PREFS, 0).equals(null));
    }

    @Test
    public void testHashCodesAreEquivalent()
    {
        final AxisRule<Stick, PadEvent> a = new AxisRule<>(AXIS_A, LISTENER, PREFS, 0);
        final AxisRule<Stick, PadEvent> b = new AxisRule<>(AXIS_A, LISTENER, PREFS, 0);

        Assert.assertEquals(a.hashCode(), b.hashCode());
    }
}
