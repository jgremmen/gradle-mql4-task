/**
 * Copyright 2019 Jeroen Gremmen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.sayayi.gradle.mql4.task;

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;

import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.internal.impldep.org.junit.FixMethodOrder;
import org.gradle.internal.impldep.org.junit.runners.MethodSorters;
import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.internal.FeatureCheckBuildResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;

import io.github.glytching.junit.extension.folder.TemporaryFolder;
import io.github.glytching.junit.extension.folder.TemporaryFolderExtension;


/**
 * @author Jeroen Gremmen
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith(TemporaryFolderExtension.class)
public class CompileMQL4TaskTest
{
  private static File MQL4_BASE;
  private static String METAEDITOR;

  private TemporaryFolder folder;


  @BeforeAll
  public static void prepare()
  {
    MQL4_BASE = new File(System.getProperty("MQL4_BASE"));
    METAEDITOR = new File(System.getProperty("METAEDITOR")).getAbsolutePath().replace("\\", "\\\\");
  }


  @BeforeEach
  public void prepare(TemporaryFolder temporaryFolder) {
    folder = temporaryFolder;
  }


  @Test
  @Order(1)
  public void testTaskCreation() throws IOException
  {
    final Project project = ProjectBuilder.builder().withProjectDir(folder.getRoot()).build();

    project.apply(Collections.<String,Object>singletonMap("plugin", "de.sayayi.gradle.mql4-plugin"));
    project.getTasks().getByName(CompileMQL4TaskPlugin.COMPILE_MQl4_TASK);

    final ExtensionContainer extensions = project.getExtensions();
    final CompileMQL4Extension extension = extensions.getByType(CompileMQL4Extension.class);

    assertEquals(extension, extensions.getByName(CompileMQL4TaskPlugin.MQL4_EXTENSION));
  }


  protected GradleRunner createRunner(String[] tasks, String build) throws IOException
  {
    try(FileWriter buildFile = new FileWriter(new File(folder.getRoot(), "build.gradle"))) {
      buildFile.append(build);
    }

    return createRunner(tasks);
  }


  protected GradleRunner createRunner(String... tasks)
  {
    return GradleRunner.create()
        .withDebug(true)
        .withProjectDir(folder.getRoot())
        .withArguments(tasks)
        .withPluginClasspath();
  }


  @Test
  @Order(2)
  @EnabledOnOs(OS.WINDOWS)
  public void testCompileMql4() throws IOException
  {
    final File testDir = folder.createDirectory("MQL4");
    final String mql4 = new File(MQL4_BASE, "MQL4").getAbsolutePath().replace("\\",  "\\\\");

    createRunner(new String[] { "clean", "compileMql4" },
        "plugins { id 'de.sayayi.gradle.mql4-plugin' }\n" +
        "task copyMql4Dir(type: Copy) {\n" +
        "    from \"" + mql4 + "\"\n" +
        "  into \"${projectDir}/MQL4\"\n" +
        "}\n" +
        "compileMql4.dependsOn copyMql4Dir\n" +
        "mql4 {\n" +
        "  mql4Dir file('MQL4')\n" +
        "  exclude 'Indicators/Test3.mq4'\n" +
        "  metaeditor \"" + METAEDITOR + "\"\n" +
        "}\n").build();

    assertTrue(new File(testDir, "Indicators/Test1.ex4").exists());
    assertTrue(new File(testDir, "Indicators/Test2.ex4").exists());
  }


  @Test
  @Order(3)
  @EnabledOnOs(OS.WINDOWS)
  public void testAssembleAndClean() throws IOException
  {
    final File testDir = folder.createDirectory("MQL4");
    final String mql4 = new File(MQL4_BASE, "MQL4").getAbsolutePath().replace("\\",  "\\\\");

    createRunner(new String[] {"clean", "assemble" },
        "plugins { id 'de.sayayi.gradle.mql4-plugin' }\n" +
        "task copyMql4Dir(type: Copy) {\n" +
        "  from \"" + mql4 + "\"\n" +
        "  into \"${projectDir}/MQL4\"\n" +
        "}\n" +
        "compileMql4.dependsOn copyMql4Dir\n" +
        "mql4 {\n" +
        "  mql4Dir file('MQL4')\n" +
        "  exclude 'Indicators/Test3.mq4'\n" +
        "  metaeditor \"" + METAEDITOR + "\"\n" +
        "}\n").build();

    assertTrue(new File(testDir, "Indicators/Test1.ex4").exists());
    assertTrue(new File(testDir, "Indicators/Test2.ex4").exists());

    final FeatureCheckBuildResult result = (FeatureCheckBuildResult)createRunner("clean").build();
    assertEquals(SUCCESS, result.task(":clean").getOutcome());

    assertFalse(new File(testDir, "Indicators/Test1.ex4").exists());
    assertFalse(new File(testDir, "Indicators/Test2.ex4").exists());
  }
}
