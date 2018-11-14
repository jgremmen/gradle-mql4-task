package eu.trading.gradle.tasks;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtraPropertiesExtension;


/**
 * @author Jeroen Gremmen
 */
public class CompileMQL4TaskPlugin implements Plugin<Project>
{
  @Override
  public void apply(Project project)
  {
    final ExtraPropertiesExtension extraProperties = project.getExtensions().getExtraProperties();
    extraProperties.set(CompileMQL4Task.class.getSimpleName(), CompileMQL4Task.class);
  }
}
