package cinnamon.engine.event;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;

@RunWith(Suite.class)
@Suite.SuiteClasses({IntegratableInputTest.class, IntegratableInputCallbackTest.class})
public class IntegratableInputTestSuite { }
