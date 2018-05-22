package cinnamon.engine.event;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({InputControlsTest.class, ButtonHandlerTest.class, AxisHandlerTest.class, ButtonRuleTest.class,
        AxisRuleTest.class})
public class ControlsTestSuite { }
