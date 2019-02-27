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

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.BasePlugin;


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
    final CompileMQL4Extension extension =
        project.getExtensions().create(MQL4_EXTENSION, CompileMQL4Extension.class);

    project.getTasks().register(COMPILE_MQl4_TASK, CompileMQL4Task.class, compileMql4Task -> {
      compileMql4Task.setDescription("Compiles MQL4 indicator, expert advisor and script files.");
      compileMql4Task.setGroup(BasePlugin.BUILD_GROUP);
      compileMql4Task.setExtension(extension);
    });

    project.afterEvaluate(prj -> {
      // if base plugin is active then attach compileMql4 task to assemble task
      final Task assembleTask = prj.getTasks().findByName(BasePlugin.ASSEMBLE_TASK_NAME);
      if (assembleTask != null)
        assembleTask.dependsOn(COMPILE_MQl4_TASK);
    });
  }
}
