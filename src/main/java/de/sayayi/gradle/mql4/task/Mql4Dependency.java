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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


/**
 * @author Jeroen Gremmen
 */
@ToString
public final class Mql4Dependency
{
  private static final Pattern INCLUDE_PATTERN =
      Pattern.compile("\\s*[<\"]([a-zA-Z0-9_/\\x5c\\x2e\\x2d]+)[>\"][\\x00-\\xff]*");

  private static final Logger LOGGER = Logging.getLogger(Mql4Dependency.class);


  @Getter @Setter
  private File parent;

  private Set<Mql4Dependency> dependencies = new HashSet<>();
  private boolean dirty;


  private Mql4Dependency(File parent) {
    this.parent = parent;
  }


  public static final Mql4Dependency from(File mql4Dir, File mql4File)
  {
    // sanity checks
    if (mql4File == null || !mql4File.exists() ||
        mql4Dir == null || !mql4Dir.isDirectory() ||
        !mql4File.getAbsolutePath().startsWith(mql4Dir.getAbsolutePath()))
      return null;

    Mql4Dependency dependency = new Mql4Dependency(mql4File);

    dependency.parseMql4File(mql4Dir);

    return dependency;
  }


  public boolean isSelf(File file) {
    return parent.equals(file);
  }


  public void markDirty(File file)
  {
    if (isSelf(file))
      dirty = true;
    else
      dependencies.forEach(dep -> dep.markDirty(file));
  }


  public boolean isDirty() {
    return dirty || dependencies.stream().filter(Mql4Dependency::isDirty).findAny().isPresent();
  }


  public Set<File> getDependencies()
  {
    final Set<File> deps = new HashSet<>();

    for(Mql4Dependency dep: dependencies)
    {
      deps.add(dep.parent);
      deps.addAll(dep.getDependencies());
    }

    return deps;
  }


  public Stream<File> streamDependenciesWithSelf()
  {
    final Set<File> deps = new HashSet<>();

    deps.add(parent);
    deps.addAll(getDependencies());

    return deps.stream();
  }


  private void parseMql4File(File mql4Dir)
  {
    final Set<File> collectedIncludes = new HashSet<>();

    try(BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(parent), "utf-8"))) {
      String line;

      while((line = reader.readLine()) != null)
      {
        int idx = line.indexOf("#include");
        if (idx >= 0)
        {
          Matcher matcher = INCLUDE_PATTERN.matcher(line.substring(idx + 8));

          if (matcher.matches())
          {
            File includeFile = new File(mql4Dir, "Include\\" + matcher.group(1));
            collectedIncludes.add(includeFile);
          }
        }
      }
    } catch(Exception ex) {
      LOGGER.error("failed to read file {}", parent.getAbsolutePath(), ex);
    }

    for(File includeFile: collectedIncludes)
      if (includeFile.exists())
        dependencies.add(from(mql4Dir, includeFile));
      else
        dependencies.add(new Mql4Dependency(includeFile));
  }
}
