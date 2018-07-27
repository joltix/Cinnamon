package cinnamon.engine;

import cinnamon.engine.core.BaseSystemTest;
import cinnamon.engine.core.DomainTest;
import cinnamon.engine.core.GameTestSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({GameTestSuite.class, DomainTest.class, BaseSystemTest.class})
public class PackageCoreTestSuite { }
