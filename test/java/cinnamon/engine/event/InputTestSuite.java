package cinnamon.engine.event;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({KeyboardTest.class, MouseTest.class, GamepadTest.class, PadProfileTest.class,
        IntegratableInputTestSuite.class})
public class InputTestSuite { }
