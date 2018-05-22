package cinnamon.engine;

import cinnamon.engine.event.*;
import cinnamon.engine.utils.FixedQueueArrayTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({InputTestSuite.class, ControlsTestSuite.class, FixedQueueArrayTest.class})
public class PackageEventTestSuite { }
