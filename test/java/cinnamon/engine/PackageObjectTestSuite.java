package cinnamon.engine;

import cinnamon.engine.object.ComponentManagerTest;
import cinnamon.engine.object.EntityManagerTest;
import cinnamon.engine.object.EntityManagerTunerTest;
import cinnamon.engine.object.EntityTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({EntityTest.class, EntityManagerTest.class, EntityManagerTunerTest.class,
        ComponentManagerTest.class})
public class PackageObjectTestSuite { }
