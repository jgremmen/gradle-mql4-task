package de.sayayi.gradle.mql4.task;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.codehaus.groovy.runtime.IOGroovyMethods;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
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


  @OutputDirectory
  public File getMql4Dir() {
    return extension.getMql4Dir();
  }


  @Input
  public String getMetaeditor() {
    return extension.getMetaeditor();
  }


  @SkipWhenEmpty
  @InputFiles
  public FileCollection getMql4Files()
  {
    return getProject().files(getFiles().values()
        .stream()
        .flatMap(Mql4Dependency::streamDependenciesWithSelf)
        .collect(Collectors.toSet()));
  }


  @TaskAction
  public void compileMQL4(IncrementalTaskInputs inputs) throws IOException
  {
    final Logger logger = getLogger();
    final LogLevel level = (logger.isDebugEnabled() || extension.isVerbose()) ? LogLevel.QUIET : LogLevel.DEBUG;

    if (!inputs.isIncremental())
      extension.setForceRecompile(true);

    Map<String,Mql4Dependency> mql4Files = getFiles();
    logger.debug("selected mql4 files: {}", mql4Files);

    inputs.outOfDate(change -> {
      mql4Files.values().forEach(dep -> dep.markDirty(change.getFile()));
      replaceExtension(change.getFile(), "ex4").delete();
    });

    inputs.removed(change -> {
      replaceExtension(change.getFile(), "ex4").delete();
    });


    if (!extension.getMql4Dir().isDirectory())
      throw new GradleException(extension.getMql4Dir().getAbsolutePath() + " is not a directory");

    File tmpBatch = null;

    if (extension.isWine())
    {
      if (extension.isVerbose())
        logger.log(level, "prepare for wine environment");

      tmpBatch = File.createTempFile("mql4c-", ".cmd", extension.getMql4Dir());
      tmpBatch.deleteOnExit();

      logger.debug("created temporary batch file {}", tmpBatch);
    }

    try {
      for(final Entry<String,Mql4Dependency> mql4FileEntry: mql4Files.entrySet())
      {
        if (extension.isForceRecompile() || mql4FileEntry.getValue().isDirty())
        {
          logger.log(level, "compile {} (dependencies {})", mql4FileEntry.getKey(),
              mql4FileEntry.getValue().getDependencies());
          compileFile(mql4FileEntry, tmpBatch);
        }
        else
        {
          logger.log(level, "{} is up-to-date; don't compile",
              replaceExtension(mql4FileEntry.getValue().getParent(), "ex4"));
        }
      }
    } finally {
      if (tmpBatch != null)
      {
        logger.debug("remove temporary batch file {}", tmpBatch);
        Files.delete(tmpBatch.toPath());
      }
    }
  }


  private Map<String,Mql4Dependency> getFiles()
  {
    final FileNameFinder finder = new FileNameFinder();
    final Map<String,Mql4Dependency> fileSet = new LinkedHashMap<>();
    final File mql4Dir = extension.getMql4Dir();
    final String mql4Path = mql4Dir.getAbsolutePath();

    if (!extension.getIncludes().isEmpty())
    {
      extension.getIncludes().forEach(pattern -> {
        finder.getFileNames(mql4Path, pattern).forEach(f ->
            fileSet.put(makeRelative(mql4Path, f), Mql4Dependency.from(mql4Dir, new File(f))));
      });

      extension.getExcludes().forEach(pattern -> {
        finder.getFileNames(mql4Path, pattern).forEach(f -> fileSet.remove(makeRelative(mql4Path, f)));
      });
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
    final File mq4File = mql4FileEntry.getValue().getParent();
    final File ex4File = replaceExtension(mq4File, "ex4");
    final File logFile = replaceExtension(mq4File, "log");
    final ExecAction execAction = getExecActionFactory().newExecAction();

    if (extension.isWine())
    {
      createBatchfile(mql4FileEntry.getKey(), tmpBatch);

      execAction.setExecutable("wine");
      execAction.setArgs(Arrays.asList("cmd", "/c", tmpBatch));
    }
    else
    {
      execAction.setExecutable(extension.getMetaeditor());
      execAction.setArgs(Arrays.asList("/compile:\"" + mql4FileEntry.getKey() + "\"", "/inc:\"" +
          extension.getMql4Dir() + "\"", "/log"));
    }

    execAction.setWorkingDir(extension.getMql4Dir());

    // metaeditor.exe returns the number of compiled files... we expect 1 (would be treated as an error by gradle)
    execAction.setIgnoreExitValue(true);

    final ExecResult result = execAction.execute();

    try {
      if ((!extension.isWine() && result.getExitValue() != 1) ||
          !ex4File.exists() ||
          ex4File.lastModified() < mq4File.lastModified())
      {
        if (logFile.exists())
          getLogger().error("{}\n", readLogfile(logFile));

        throw new ExecException("failed to compile " + mql4FileEntry.getKey());
      }

      if (logFile.exists())
        getLogger().log(extension.isVerbose() ? LogLevel.QUIET : LogLevel.DEBUG, "{}\n", readLogfile(logFile));
    } finally {
      logFile.delete();
    }
  }


  protected void createBatchfile(String relativeMq4Path, File batchFile) throws IOException
  {
    try(Writer bos = new FileWriter(batchFile)) {
      bos.append("@ECHO OFF\r\n")
         .append('"').append(extension.getMetaeditor()).append("\" ")
         .append("/compile:\"").append(relativeMq4Path.replace("/", "\\")).append("\" ")
         .append("/log")
         .append("\r\n");
    }
  }


  protected String readLogfile(File logFile)
  {
    String text = "";

    try {
      text = IOGroovyMethods.getText(Files.newBufferedReader(logFile.toPath(), StandardCharsets.UTF_16LE));
      while(text.startsWith("?"))
        text = text.substring(1);
    } catch(final Exception ex) {
      getLogger().error("failed to read file {}", logFile.getAbsolutePath(), ex);
    }

    return text.trim().replaceAll("(\\r)?\\n", System.lineSeparator());
  }


  protected String replaceExtension(String filename, String ext)
  {
    final int dotIdx = filename.lastIndexOf('.');
    return (dotIdx < 0) ? (filename + '.' + ext) : (filename.substring(0,  dotIdx + 1) + ext);
  }


  protected File replaceExtension(File f, String ext) {
    return new File(f.getParent(), replaceExtension(f.getName(), ext));
  }
}
