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

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;


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
    final CompileMQL4Extension extension =
        project.getExtensions().create(MQL4_EXTENSION, CompileMQL4Extension.class);

    // create compileMql4 task
    final TaskProvider<CompileMQL4Task> compileMql4Task =
        project.getTasks().register(COMPILE_MQl4_TASK, CompileMQL4Task.class, task -> {
          task.setDescription("Compiles MQL4 indicator, expert advisor and script files.");
          task.setGroup(BUILD_GROUP);
          task.setExtension(extension);
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
}
