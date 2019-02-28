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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.gradle.api.Action;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


/**
 * Extension for configuring the compileMql4 task.
 *
 * @author Jeroen Gremmen
 */
@ToString
public class CompileMQL4Extension
{
  @Getter @Setter
  private String metaeditor;

  @Getter
  private File mql4Dir = new File("MQL4");

  @Getter
  private final List<String> includes =
      new ArrayList<>(Arrays.asList("Indicators/*.mq4", "Experts/*.mq4", "Scripts/*.mq4"));

  @Getter
  private final List<String> excludes = new ArrayList<>(Arrays.asList("**/*.mqh"));

  @Getter
  private final Wine wine = new Wine();

  @Getter
  private boolean verbose;


  public void setMql4Dir(String mql4Dir) {
    this.mql4Dir = new File(mql4Dir);
  }


  public void setMql4Dir(File mql4Dir) {
    this.mql4Dir = mql4Dir;
  }


  public void setInclude(String include) {
    includes.add(include);
  }


  public void setIncludes(String[] includes)
  {
    this.includes.clear();
    this.includes.addAll(Arrays.asList(includes));
  }


  public void setIncludes(Collection<String> includes)
  {
    this.includes.clear();
    this.includes.addAll(includes);
  }


  public void setExclude(String exclude) {
    excludes.add(exclude);
  }


  public void setExcludes(String[] excludes)
  {
    this.excludes.clear();
    this.excludes.addAll(Arrays.asList(excludes));
  }


  public void setExcludes(Collection<String> excludes)
  {
    this.excludes.clear();
    this.excludes.addAll(excludes);
  }


  public void wine(Action<? super Wine> action) {
    action.execute(wine);
  }


  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }


  public void setVerbose(String verbose) {
    this.verbose = Boolean.parseBoolean(verbose);
  }
}
