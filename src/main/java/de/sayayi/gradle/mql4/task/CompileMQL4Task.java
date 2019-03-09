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

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_16LE;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFiles;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;
import org.gradle.process.ExecResult;
import org.gradle.process.internal.ExecAction;
import org.gradle.process.internal.ExecActionFactory;
import org.gradle.process.internal.ExecException;

import groovy.util.FileNameFinder;
import lombok.Setter;


/**
 * @author Jeroen Gremmen
 */
public class CompileMQL4Task extends DefaultTask
{
  @Setter
  private CompileMQL4Extension extension;


  @Inject
  protected ExecActionFactory getExecActionFactory() {
    throw new UnsupportedOperationException();
  }


  @Internal
  public String getMetaeditor() {
    return extension.getMetaeditor();
  }


  @SkipWhenEmpty
  @InputFiles
  public FileCollection getMq4Files()
  {
    // return a set of all selected files (mq4) and their dependencies (mqh)
    return getProject().files(getInputFilesWithDependency().values()
        .stream()
        .flatMap(Mql4Dependency::streamDependenciesWithSelf)
        .collect(Collectors.toSet()));
  }


  @OutputFiles
  public FileCollection getEx4Files()
  {
    // return a set of all expected files (ex4)
    return getProject().files(getInputFilesWithDependency().values()
        .stream()
        .map(dep -> replaceExtension(dep.getFile(), "ex4"))
        .collect(Collectors.toSet()));
  }


  @TaskAction
  public void compileMQL4(IncrementalTaskInputs inputs) throws IOException
  {
    resolveMetaeditor();

    final File mql4dir = extension.getMql4Dir();
    if (!mql4dir.isDirectory())
      throw new GradleException(mql4dir.getAbsolutePath() + " is not a directory");

    final Logger logger = getLogger();
    final Map<String,Mql4Dependency> mql4Files = getInputFilesWithDependency();
    logger.debug("selected mql4 files: {}", mql4Files);

    inputs.outOfDate(change -> {
      mql4Files.values().forEach(dep -> dep.markDirty(change.getFile()));
      replaceExtension(change.getFile(), "ex4").delete();
    });

    inputs.removed(change -> {
      replaceExtension(change.getFile(), "ex4").delete();
    });

    final LogLevel level = extension.isVerbose() ? LogLevel.QUIET : LogLevel.DEBUG;
    compileMQL4(logger, level, mql4Files, mql4dir, inputs.isIncremental());
  }


  private void compileMQL4(Logger logger, LogLevel level, Map<String,Mql4Dependency> mql4Files, File mql4dir,
      boolean incremental) throws IOException
  {
    final String mql4DirPath = mql4dir.getAbsolutePath();
    File tmpBatch = null;

    if (extension.getWine().isEnabled())
    {
      logger.log(level, "prepare for wine environment");

      tmpBatch = File.createTempFile("mql4c-", ".cmd", mql4dir);
      tmpBatch.deleteOnExit();
      logger.debug("created temporary batch file {}", tmpBatch);
    }

    try {
      for(final Entry<String,Mql4Dependency> mql4FileEntry: mql4Files.entrySet())
      {
        if (!incremental || mql4FileEntry.getValue().isDirty())
        {
          logger.log(level, "compile {} (dependencies {})",
              mql4FileEntry.getKey(),
              mql4FileEntry.getValue().getDependencies()
                           .stream()
                           .map(f -> makeRelative(mql4DirPath, f.getAbsolutePath()))
                           .collect(Collectors.toList()));

          compileFile(mql4FileEntry, tmpBatch);
        }
        else
          logger.log(level, "{} is up-to-date", replaceExtension(mql4FileEntry.getKey(), "ex4"));
      }
    } finally {
      if (tmpBatch != null)
      {
        logger.debug("remove temporary batch file {}", tmpBatch);
        Files.delete(tmpBatch.toPath());
      }
    }
  }


  private Map<String,Mql4Dependency> getInputFilesWithDependency()
  {
    final Map<String,Mql4Dependency> fileSet = new LinkedHashMap<>();

    if (!extension.getIncludes().isEmpty())
    {
      final Set<String> inputFiles = new TreeSet<>();

      final FileNameFinder finder = new FileNameFinder();
      final File mql4Dir = extension.getMql4Dir();
      final String mql4Path = mql4Dir.getAbsolutePath();

      extension.getIncludes().forEach(pattern -> inputFiles.addAll(finder.getFileNames(mql4Path, pattern)));
      extension.getExcludes().forEach(pattern -> inputFiles.removeAll(finder.getFileNames(mql4Path, pattern)));

      inputFiles.forEach(f -> fileSet.put(makeRelative(mql4Path, f), Mql4Dependency.from(mql4Dir, new File(f))));
    }

    return fileSet;
  }


  protected String makeRelative(String base, String f)
  {
    final int baseLength = base.length();

    if (f.length() > baseLength && f.substring(0, baseLength).equals(base))
    {
      f = f.substring(baseLength);
      if (f.startsWith("/") || f.startsWith("\\"))
        f = f.substring(1);
    }

    return f;
  }


  protected void compileFile(Entry<String,Mql4Dependency> mql4FileEntry, File tmpBatch) throws IOException
  {
    final ExecAction execAction = getExecActionFactory().newExecAction();
    final Wine wine = extension.getWine();

    if (wine.isEnabled())
    {
      createBatchfile(mql4FileEntry.getKey(), tmpBatch);

      execAction.setExecutable(wine.getExecutable());
      execAction.setArgs(Arrays.asList("cmd", "/c", tmpBatch.getAbsolutePath()));

      configureWineEnvironment(execAction);
    }
    else
    {
      execAction.setExecutable(extension.getMetaeditor());
      execAction.setArgs(Arrays.asList("/compile:\"" + mql4FileEntry.getKey() + "\"", "/inc:\"" +
          extension.getMql4Dir() + "\"", "/log"));
    }

    execAction.setWorkingDir(extension.getMql4Dir());

    // windows: metaeditor.exe returns the number of compiled files... we expect 1
    // non-windows: wine will return a code which has no relation to whether compilation has succeeded/failed
    // -> ignore return code as it is useless
    execAction.setIgnoreExitValue(true);

    final ExecResult result = execAction.execute();
    final File mq4File = mql4FileEntry.getValue().getFile();
    final File logFile = replaceExtension(mq4File, "log");

    try {
      final File ex4File = replaceExtension(mq4File, "ex4");

      if ((!wine.isEnabled() && result.getExitValue() != 1) ||
          !ex4File.exists() ||
          ex4File.lastModified() < mq4File.lastModified())
      {
        if (logFile.exists())
          getLogger().error("{}", readLogfile(logFile));

        throw new ExecException("failed to compile " + mql4FileEntry.getKey());
      }

      if (logFile.exists())
        getLogger().log(extension.isVerbose() ? LogLevel.QUIET : LogLevel.DEBUG, "{}", readLogfile(logFile));
    } finally {
      logFile.delete();
    }
  }


  protected void configureWineEnvironment(ExecAction execAction)
  {
    final Map<String,Object> environment = execAction.getEnvironment();

    // disable debugging messages on the console
    environment.put("WINEDEBUG", "-all");

    // set custom wine prefix
    final File winePrefix = extension.getWine().getPrefix();
    if (winePrefix != null)
    {
      environment.put("WINEPREFIX", winePrefix.getAbsolutePath());

      // if the wine prefix is not a directory it will be created the first time wine is started.
      // make sure wine configures itself as a 32-bit windows architecture
      if (!winePrefix.isDirectory())
        environment.put("WINEARCH", "win32");
    }
  }


  protected void createBatchfile(String relativeMq4Path, File batchFile) throws IOException
  {
    try(Writer batchWriter = new OutputStreamWriter(new FileOutputStream(batchFile), ISO_8859_1)) {
      batchWriter.append("@ECHO OFF\r\n")
                 .append('"').append(extension.getMetaeditor()).append("\" ")
                 .append("/compile:\"").append(relativeMq4Path.replace("/", "\\")).append("\" ")
                 .append("/log")
                 .append("\r\n");
    }
  }


  protected String readLogfile(File logFile)
  {
    final StringBuilder text = new StringBuilder();

    try(BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(logFile), UTF_16LE))) {
      boolean start = true;
      String line;

      while((line = reader.readLine()) != null)
      {
        if (start)
        {
          // strip BOM
          if (line.startsWith("\ufeff"))
            line = line.substring(1);

          // skip empty lines at the beginning of the log file
          if (line.trim().length() == 0)
            continue;
        }

        text.append("|  ").append(line).append('\n');
        start = false;
      }
    } catch(final Exception ex) {
      getLogger().error("failed to read log file {}", logFile.getAbsolutePath(), ex);
    }

    return text.toString();
  }


  protected String replaceExtension(String filename, String ext)
  {
    final int dotIdx = filename.lastIndexOf('.');
    return (dotIdx < 0) ? (filename + '.' + ext) : (filename.substring(0, dotIdx + 1) + ext);
  }


  protected File replaceExtension(File f, String ext) {
    return new File(f.getParent(), replaceExtension(f.getName(), ext));
  }


  protected void resolveMetaeditor()
  {
    final Project project = getProject();
    final File buildDir = project.getBuildDir();
    final Configuration configuration = extension.getMql4Configuration();
    final DependencySet configurationDependencies = configuration.getDependencies();

    if (configurationDependencies.isEmpty())
    {
      // no dependencies but metaeditor is set -> custom metaeditor provided; nothing to do.
      if (extension.getMetaeditor() != null)
        return;

      configurationDependencies.add(project.getDependencies().create("de.sayayi:metaeditor:5.+@jar"));
    }

    final File metaeditorExe = new File(buildDir, "metaeditor.exe");
    extension.setMetaeditor(metaeditorExe.getAbsolutePath());

    if (!metaeditorExe.exists())
    {
      project.getLogger().debug("extracting metaeditor.exe to {}", metaeditorExe);
      project.copy(copy -> {
        copy.from(project.zipTree(configuration.getSingleFile()));
        copy.include("metaeditor.exe");
        copy.into(buildDir);
      });
    }
  }
}
