package de.sayayi.gradle.mql4.task;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.BasePlugin;


/**
 * @author Jeroen Gremmen
 */
public class CompileMQL4TaskPlugin implements Plugin<Project>
{
  private static final String COMPILE_MQl4_TASK = "compileMql4";


  @Override
  public void apply(Project project)
  {
    final CompileMQL4Extension extension =
        project.getExtensions().create("mql4", CompileMQL4Extension.class);

    project.getTasks().register(COMPILE_MQl4_TASK, CompileMQL4Task.class, compileMql4Task -> {
      compileMql4Task.setDescription("Compiles MQL4 indicator, expert advisor and script files.");
      compileMql4Task.setGroup(BasePlugin.BUILD_GROUP);
      compileMql4Task.setExtension(extension);
    });

    project.afterEvaluate(prj -> {
      // if base plugin is active then attach compileMql4 task to assemble task
      prj.getTasks().getByName(BasePlugin.ASSEMBLE_TASK_NAME, assemble -> assemble.dependsOn(COMPILE_MQl4_TASK));
    });
  }
}
