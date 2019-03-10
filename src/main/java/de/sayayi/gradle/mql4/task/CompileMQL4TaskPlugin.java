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

import static org.gradle.api.plugins.BasePlugin.ASSEMBLE_TASK_NAME;
import static org.gradle.api.plugins.BasePlugin.BUILD_GROUP;
import static org.gradle.api.plugins.BasePlugin.CLEAN_TASK_NAME;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.internal.os.OperatingSystem;


/**
 * @author Jeroen Gremmen
 */
public class CompileMQL4TaskPlugin implements Plugin<Project>
{
  static final String MQL4_EXTENSION_NAME = "mql4";
  static final String MQL4_CONFIGURATION_NAME = "mql4-metaeditor";
  static final String COMPILE_MQl4_TASK_NAME = "compileMql4";
  static final String EXTRACT_METAEDITOR_TASK_NAME = "extractMetaeditor";


  @Override
  public void apply(Project project)
  {
    project.getPlugins().apply("base");

    // create mql4 extension
    final Configuration mql4Configuration = createMql4Configuration(project);
    final CompileMQL4Extension mql4 = project.getExtensions()
        .create(MQL4_EXTENSION_NAME, CompileMQL4Extension.class, project, mql4Configuration);

    // auto configure wine on non-windows architectures
    if (!OperatingSystem.current().isWindows())
      autoConfigureWine(mql4, project);

    // create task
    final TaskContainer tasks = project.getTasks();
    final CompileMQL4Task compileMql4Task = createCompileMql4Task(tasks, mql4);

    // assemble.dependsOn('compileMql4')
    tasks.findByName(ASSEMBLE_TASK_NAME).dependsOn(compileMql4Task);

    // clean.doLast { ... }
    tasks.findByName(CLEAN_TASK_NAME).doLast(task -> {
        compileMql4Task.getEx4Files().forEach(File::delete);
    });
  }


  private CompileMQL4Task createCompileMql4Task(TaskContainer tasks, CompileMQL4Extension mql4)
  {
    return tasks.create(COMPILE_MQl4_TASK_NAME, CompileMQL4Task.class, task -> {
          task.setDescription("Compiles MQL4 indicator, expert advisor and script files.");
          task.setGroup(BUILD_GROUP);
          task.setExtension(mql4);
        });
  }


  private Configuration createMql4Configuration(Project project)
  {
    final RepositoryHandler repositories = project.getRepositories();
    repositories.add(repositories.maven(r -> r.setUrl("http://maven.sayayi.de/repository/maven-releases/")));

    final Configuration mql4Configuration = project.getConfigurations()
        .create(MQL4_CONFIGURATION_NAME)
        .setVisible(false)
        .setDescription("Metaeditor library to be used for this project.");

    return mql4Configuration;
  }


  protected void autoConfigureWine(CompileMQL4Extension mql4, Project project)
  {
    final Wine wine = mql4.getWine();
    final Logger logger = project.getLogger();

    wine.setEnabled(true);
    wine.setPrefix(new File(project.getBuildDir(), ".wine"));

    // try to find the most likely wine prefix
    File wineprefix = getEnvironmentVariableAsFile("WINEPREFIX");
    if (wineprefix == null)
    {
      final File _home = getEnvironmentVariableAsFile("HOME");
      if (_home != null && _home.isDirectory())
        wineprefix = new File(_home, ".wine");
    }

    if (wineprefix != null)
    {
      if (!wineprefix.isDirectory())
      {
        // environment does not exist yet; it will be created on 1st invocation of wine
        wine.setPrefix(wineprefix);
      }
      else
      {
        // environment already exists; check architecture
        final byte[] headSystemReg = new byte[100];
        boolean win32 = true;

        try(FileInputStream systemReg = new FileInputStream(new File(wineprefix, "system.reg"))) {
          logger.debug("detecting windows architecture for wine environment {}...", wineprefix);
          systemReg.read(headSystemReg);

          win32 &= !new String(headSystemReg, StandardCharsets.US_ASCII).contains("#arch=win32");
          logger.debug("{}-bit windows architecture found", win32 ? "32" : "64");
        } catch(final Exception ex) {
          // wine environment exists but system.reg is not accessible -> don't trust it and use fallback
          logger.warn("failed to read {}/system.reg", wineprefix, ex);
          win32 = false;
        }

        if (win32)
          wine.setPrefix(wineprefix);
        else
          logger.debug("wine environment {} has no 32-bit windows architecture", wineprefix);
      }
    }

    logger.debug("use wine environment {}", wine.getPrefix());

  }


  protected File getEnvironmentVariableAsFile(String property)
  {
    final String value = System.getenv(property);
    return (value != null && !value.isEmpty()) ? new File(value) : null;
  }
}
