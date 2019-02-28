package de.sayayi.gradle.mql4.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;


/**
 * @author Jeroen Gremmen
 */
public class Mql4DependencyTest
{
  private static final File MQL4_DIR = new File("src/test/resources/MQL4");

  private static final File TEST1_MQ4 = new File(MQL4_DIR, "Indicators/Test1.mq4");
  private static final File TEST2_MQ4 = new File(MQL4_DIR, "Indicators/Test2.mq4");
  private static final File TEST3_MQ4 = new File(MQL4_DIR, "Indicators/Test3.mq4");

  private static final File INC1_MQH = new File(MQL4_DIR, "Include/Inc1.mqh");
  private static final File INC2_MQH = new File(MQL4_DIR, "Include/Inc2.mqh");
  private static final File INC3_MQH = new File(MQL4_DIR, "Indicators/Inc3.mqh");


  @Test
  public void testFrom0()
  {
    assertNull(Mql4Dependency.from(MQL4_DIR, null));
    assertNull(Mql4Dependency.from(MQL4_DIR, new File(MQL4_DIR, "Experts/EA.mq4")));
    assertNull(Mql4Dependency.from(null, TEST1_MQ4));
    assertNull(Mql4Dependency.from(TEST1_MQ4, TEST2_MQ4));
    assertNull(Mql4Dependency.from(new File(MQL4_DIR, "Include"), TEST1_MQ4));
  }


  @Test
  public void testFrom1()
  {
    final Mql4Dependency dep1 = Mql4Dependency.from(MQL4_DIR, TEST1_MQ4);

    assertNotNull(dep1);
    assertFalse(dep1.isDirty());
    assertTrue(dep1.isSelf(TEST1_MQ4));

    final Set<File> deps = dep1.getDependencies();
    assertEquals(2, deps.size());
    assertTrue(deps.contains(INC1_MQH));
    assertTrue(deps.contains(INC2_MQH));
  }


  @Test
  public void testFrom2()
  {
    final Mql4Dependency dep2 = Mql4Dependency.from(MQL4_DIR, TEST2_MQ4);

    assertNotNull(dep2);
    assertFalse(dep2.isDirty());
    assertTrue(dep2.isSelf(TEST2_MQ4));

    final Set<File> deps = dep2.getDependencies();
    assertEquals(3, deps.size());
    assertTrue(deps.contains(INC1_MQH));
    assertTrue(deps.contains(INC2_MQH));
    assertTrue(deps.contains(INC3_MQH));
  }


  @Test
  public void testFrom3()
  {
    final Mql4Dependency dep3 = Mql4Dependency.from(MQL4_DIR, TEST3_MQ4);

    assertNotNull(dep3);
    assertFalse(dep3.isDirty());
    assertTrue(dep3.isSelf(TEST3_MQ4));

    final Set<File> deps = dep3.getDependencies();
    assertEquals(2, deps.size());
    assertTrue(deps.contains(INC2_MQH));
    assertTrue(deps.contains(new File(MQL4_DIR, "Indicators/IncUnknown.mqh")));
  }


  @Test
  public void testMarkDirtySelf()
  {
    final Mql4Dependency dep2 = Mql4Dependency.from(MQL4_DIR, TEST2_MQ4);
    assertFalse(dep2.isDirty());

    dep2.markDirty(TEST2_MQ4);
    assertTrue(dep2.isDirty());
  }


  @Test
  public void testMarkDirtyInc2()
  {
    final Mql4Dependency dep2 = Mql4Dependency.from(MQL4_DIR, TEST2_MQ4);
    assertFalse(dep2.isDirty());

    dep2.markDirty(INC2_MQH);
    assertTrue(dep2.isDirty());
  }


  @Test
  public void testMarkDirtyUnknown()
  {
    final Mql4Dependency dep3 = Mql4Dependency.from(MQL4_DIR, TEST3_MQ4);
    assertFalse(dep3.isDirty());

    dep3.markDirty(new File(MQL4_DIR, "Indicators/IncUnknown.mqh"));
    assertTrue(dep3.isDirty());
  }


  @Test
  public void testMarkDirtyNoDep()
  {
    final Mql4Dependency dep2 = Mql4Dependency.from(MQL4_DIR, TEST2_MQ4);
    assertFalse(dep2.isDirty());

    dep2.markDirty(new File(MQL4_DIR, "Include/IncUnknown.mqh"));
    assertFalse(dep2.isDirty());
  }


  @Test
  public void testStream()
  {
    final Mql4Dependency dep2 = Mql4Dependency.from(MQL4_DIR, TEST2_MQ4);
    final String filenames = dep2.streamDependenciesWithSelf()
        .map(File::getName)
        .sorted()
        .collect(Collectors.joining(","));

    assertEquals("Inc1.mqh,Inc2.mqh,Inc3.mqh,Test2.mq4", filenames);
  }
}
