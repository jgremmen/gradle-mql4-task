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
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.internal.os.OperatingSystem;


/**
 * @author Jeroen Gremmen
 */
public class CompileMQL4TaskPlugin implements Plugin<Project>
{
  static final String MQL4_EXTENSION = "mql4";
  static final String COMPILE_MQl4_TASK = "compileMql4";


  @Override
  public void apply(Project project)
  {
    project.getPlugins().apply("base");

    // create mql4 extension
    final CompileMQL4Extension mql4 = project.getExtensions().create(MQL4_EXTENSION, CompileMQL4Extension.class);
    mql4.getWine().setProject(project);

    // auto configure wine on non-windows architectures
    if (!OperatingSystem.current().isWindows())
      autoConfigureWine(mql4, project);

    // create compileMql4 task
    final TaskProvider<CompileMQL4Task> compileMql4Task =
        project.getTasks().register(COMPILE_MQl4_TASK, CompileMQL4Task.class, task -> {
          task.setDescription("Compiles MQL4 indicator, expert advisor and script files.");
          task.setGroup(BUILD_GROUP);
          task.setExtension(mql4);
        });

    project.afterEvaluate(prj -> {
      final TaskContainer tasks = prj.getTasks();

      // assemble.dependsOn('compileMql4')
      tasks.findByName(ASSEMBLE_TASK_NAME).dependsOn(compileMql4Task.get());

      // clean.doLast { }
      tasks.findByName(CLEAN_TASK_NAME).doLast(task -> {
          compileMql4Task.get().getEx4Files().forEach(File::delete);
      });
    });
  }


  protected void autoConfigureWine(CompileMQL4Extension mql4, Project project)
  {
    final Wine wine = mql4.getWine();
    final Logger logger = project.getLogger();

    wine.setEnabled(true);
    wine.setPrefix(new File(project.getBuildDir(), ".wine"));

    // try to find the most likely wine prefix
    File wineprefix = getSystemPropertyAsFile("WINEPREFIX");
    if (wineprefix == null)
    {
      final File _home = getSystemPropertyAsFile("HOME");
      if (_home != null && _home.isDirectory())
        wineprefix = new File(_home, ".wine");
    }

    if (wineprefix != null)
    {
      if (!wineprefix.isDirectory())
      {
        // environment does not exist yet; it will be created on 1st invocation of wine
        logger.debug("expect wine environment in {}", wineprefix);
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
          logger.debug("use default wine environment: {}", wine.getPrefix());
      }
    }
  }


  protected File getSystemPropertyAsFile(String property)
  {
    final String value = System.getProperty(property);
    return (value != null && !value.isEmpty()) ? new File(value) : null;
  }
}
