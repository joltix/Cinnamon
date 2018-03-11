package cinnamon.engine.utils;

import cinnamon.engine.utils.IntMap.IntWrapper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PackedEnumIntMapTest
{
    private static final int UNUSED_VALUE = -42;

    private PackedEnumIntMap<TestEnum> mMap;

    @Before
    public void setUp()
    {
        mMap = new PackedEnumIntMap<>(TestEnum.class);
    }

    @After
    public void tearDown()
    {
        mMap = null;
    }

    @Test (expected = NullPointerException.class)
    public void testConstructorNPE()
    {
        new PackedEnumIntMap<>(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAE()
    {
        new PackedEnumIntMap<>(TestIncorrectEnum.class);
    }

    @Test
    public void testGet()
    {
        Assert.assertEquals(PackedEnumIntMapTest.TestEnum.VALUE_1, mMap.get(PackedEnumIntMapTest.TestEnum
                .VALUE_1.toInt()));
    }

    @Test
    public void testGetReturnsNull()
    {
        Assert.assertNull(mMap.get(UNUSED_VALUE));
    }

    @Test
    public void testSize()
    {
        Assert.assertEquals(TestEnum.values().length, mMap.size());
    }

    private enum TestEnum implements IntWrapper
    {
        VALUE_0(-1),
        VALUE_1(0),
        VALUE_2(1);

        private int mValue;

        TestEnum(int value)
        {
            mValue = value;
        }

        @Override
        public int toInt()
        {
            return mValue;
        }
    }

    private enum TestIncorrectEnum implements IntWrapper
    {
        VALUE_0(),
        VALUE_1(),
        VALUE_2();

        @Override
        public int toInt()
        {
            return -1;
        }
    }
}
