package de.sayayi.gradle.mql4.task;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;


/**
 * @author Jeroen Gremmen
 */
public class Mql4DependencyTest
{
  private static final File MQL4_1_DIR = new File("src/test/resources/MQL4-1");

  private static final File TEST1_MQ4 = new File(MQL4_1_DIR, "Indicators/Test1.mq4");
  private static final File TEST2_MQ4 = new File(MQL4_1_DIR, "Indicators/Test2.mq4");
  private static final File TEST3_MQ4 = new File(MQL4_1_DIR, "Indicators/Test3.mq4");


  @Test
  public void testFrom0()
  {
    assertNull(Mql4Dependency.from(MQL4_1_DIR, null));
    assertNull(Mql4Dependency.from(MQL4_1_DIR, new File(MQL4_1_DIR, "Experts/EA.mq4")));
    assertNull(Mql4Dependency.from(null, TEST1_MQ4));
    assertNull(Mql4Dependency.from(TEST1_MQ4, TEST2_MQ4));
    assertNull(Mql4Dependency.from(new File(MQL4_1_DIR, "Include"), TEST1_MQ4));
  }


  @Test
  public void testFrom1()
  {
    Mql4Dependency dep1 = Mql4Dependency.from(MQL4_1_DIR, TEST1_MQ4);

    assertNotNull(dep1);
    assertFalse(dep1.isDirty());
    assertTrue(dep1.isSelf(TEST1_MQ4));

    Set<File> deps = dep1.getDependencies();
    assertEquals(2, deps.size());
    assertTrue(deps.contains(new File(MQL4_1_DIR, "Include/Inc1.mqh")));
    assertTrue(deps.contains(new File(MQL4_1_DIR, "Include/Inc2.mqh")));
  }


  @Test
  public void testFrom2()
  {
    Mql4Dependency dep2 = Mql4Dependency.from(MQL4_1_DIR, TEST2_MQ4);

    assertNotNull(dep2);
    assertFalse(dep2.isDirty());
    assertTrue(dep2.isSelf(TEST2_MQ4));

    Set<File> deps = dep2.getDependencies();
    assertEquals(3, deps.size());
    assertTrue(deps.contains(new File(MQL4_1_DIR, "Include/Inc1.mqh")));
    assertTrue(deps.contains(new File(MQL4_1_DIR, "Include/Inc2.mqh")));
    assertTrue(deps.contains(new File(MQL4_1_DIR, "Include/Inc3.mqh")));
  }


  @Test
  public void testFrom3()
  {
    Mql4Dependency dep3 = Mql4Dependency.from(MQL4_1_DIR, TEST3_MQ4);

    assertNotNull(dep3);
    assertFalse(dep3.isDirty());
    assertTrue(dep3.isSelf(TEST3_MQ4));

    Set<File> deps = dep3.getDependencies();
    assertEquals(2, deps.size());
    assertTrue(deps.contains(new File(MQL4_1_DIR, "Include/Inc2.mqh")));
    assertTrue(deps.contains(new File(MQL4_1_DIR, "Include/IncUnknown.mqh")));
  }


  @Test
  public void testMarkDirtySelf()
  {
    Mql4Dependency dep2 = Mql4Dependency.from(MQL4_1_DIR, TEST2_MQ4);
    assertFalse(dep2.isDirty());

    dep2.markDirty(TEST2_MQ4);
    assertTrue(dep2.isDirty());
  }


  @Test
  public void testMarkDirtyInc2()
  {
    Mql4Dependency dep2 = Mql4Dependency.from(MQL4_1_DIR, TEST2_MQ4);
    assertFalse(dep2.isDirty());

    dep2.markDirty(new File(MQL4_1_DIR, "Include/Inc2.mqh"));
    assertTrue(dep2.isDirty());
  }


  @Test
  public void testMarkDirtyUnknown()
  {
    Mql4Dependency dep3 = Mql4Dependency.from(MQL4_1_DIR, TEST3_MQ4);
    assertFalse(dep3.isDirty());

    dep3.markDirty(new File(MQL4_1_DIR, "Include/IncUnknown.mqh"));
    assertTrue(dep3.isDirty());
  }


  @Test
  public void testMarkDirtyNoDep()
  {
    Mql4Dependency dep2 = Mql4Dependency.from(MQL4_1_DIR, TEST2_MQ4);
    assertFalse(dep2.isDirty());

    dep2.markDirty(new File(MQL4_1_DIR, "Include/IncUnknown.mqh"));
    assertFalse(dep2.isDirty());
  }


  @Test
  public void testStream()
  {
    Mql4Dependency dep2 = Mql4Dependency.from(MQL4_1_DIR, TEST2_MQ4);
    String filenames = dep2.streamDependenciesWithSelf()
        .map(File::getName)
        .sorted()
        .collect(Collectors.joining(","));

    assertEquals("Inc1.mqh,Inc2.mqh,Inc3.mqh,Test2.mq4", filenames);
  }
}
