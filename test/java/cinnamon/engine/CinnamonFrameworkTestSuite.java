package cinnamon.engine;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({PackageCoreTestSuite.class, PackageEventTestSuite.class})
public class CinnamonFrameworkTestSuite { }
