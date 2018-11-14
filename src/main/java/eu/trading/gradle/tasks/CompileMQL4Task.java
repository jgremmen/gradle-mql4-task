package eu.trading.gradle.tasks;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.inject.Inject;

import org.codehaus.groovy.runtime.IOGroovyMethods;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecResult;
import org.gradle.process.internal.ExecAction;
import org.gradle.process.internal.ExecActionFactory;
import org.gradle.process.internal.ExecException;

import groovy.util.FileNameFinder;


/**
 * @author Jeroen Gremmen
 */
public class CompileMQL4Task extends DefaultTask
{
  private static final Logger LOGGER = Logging.getLogger(CompileMQL4Task.class);

  private String metaeditor;
  private boolean wine = false;

  private File mql4Dir = new File("MQL4");
  private String[] includes = new String[] { "Indicators/*.mq4", "Experts/*.mq4", "Scripts/*.mq4" };
  private String[] excludes = null;

  private boolean verbose = false;
  private boolean forceRecompile = false;


  @Inject
  protected ExecActionFactory getExecActionFactory() {
    throw new UnsupportedOperationException();
  }


  public void forceRecompile(boolean forceRecompile) {
    this.forceRecompile = forceRecompile;
  }


  public void forceRecompile(String forceRecompile) {
    this.forceRecompile = Boolean.parseBoolean(forceRecompile);
  }


  public boolean isForceRecompile() {
    return forceRecompile;
  }


  public void wine(boolean wine) {
    this.wine = wine;
  }


  public void wine(String wine) {
    this.wine = Boolean.parseBoolean(wine);
  }


  public boolean isWine() {
    return wine;
  }


  public boolean isVerbose() {
    return verbose;
  }


  public void verbose(boolean verbose) {
    this.verbose = verbose;
  }


  public void verbose(String verbose) {
    this.verbose = Boolean.parseBoolean(verbose);
  }


  public String[] getIncludes() {
    return includes;
  }


  public void includes(String[] includes) {
    this.includes = includes;
  }


  public void includes(Collection<String> includes) {
    this.includes = includes.toArray(new String[includes.size()]);
  }


  public String[] getExcludes() {
    return excludes;
  }


  public void excludes(String[] excludes) {
    this.excludes = excludes;
  }


  public void excludes(Collection<String> excludes) {
    this.excludes = excludes.toArray(new String[excludes.size()]);
  }


  public File getMql4Dir() {
    return mql4Dir;
  }


  public void mql4Dir(String mql4Dir) {
    this.mql4Dir = new File(mql4Dir);
  }


  public void mql4Dir(File mql4Dir) {
    this.mql4Dir = mql4Dir;
  }


  public void metaeditor(String metaeditor) {
    this.metaeditor = metaeditor;
  }


  @TaskAction
  public void compileMQL4() throws IOException
  {
    if (LOGGER.isDebugEnabled())
      verbose = true;

    if (!mql4Dir.isDirectory())
      throw new GradleException(mql4Dir.getAbsolutePath() + " is not a directory");

    File tmpBatch = null;

    if (wine)
    {
      tmpBatch = File.createTempFile("mql4c-", ".cmd", mql4Dir);
      tmpBatch.deleteOnExit();

      LOGGER.debug("created temporary batch file {}", tmpBatch);
    }

    try {
      for(final String file: getFiles())
        compileFile(file, tmpBatch);
    } finally {
      if (tmpBatch != null)
      {
        LOGGER.debug("remove temporary batch file {}", tmpBatch);

        tmpBatch.delete();
      }
    }
  }


  private Set<String> getFiles()
  {
    final FileNameFinder finder = new FileNameFinder();
    final Set<String> fileSet = new LinkedHashSet<>();
    final String mql4Path = mql4Dir.getAbsolutePath();

    if (includes != null && includes.length > 0)
    {
      Arrays.stream(includes).forEach(pattern -> {
        finder.getFileNames(mql4Path, pattern).forEach(f -> fileSet.add(makeRelative(f)));
      });

      if (excludes != null)
      {
        Arrays.stream(excludes).forEach(pattern -> {
          finder.getFileNames(mql4Path, pattern).forEach(f -> fileSet.remove(makeRelative(f)));
        });
      }
    }

    LOGGER.debug("select files: {}", fileSet);

    return fileSet;
  }


  protected String makeRelative(String f)
  {
    f = f.substring(mql4Dir.getAbsolutePath().length());
    if (f.startsWith("/") || f.startsWith("\\"))
      f = f.substring(1);

    return f;
  }


  protected void compileFile(String f, File tmpBatch) throws IOException
  {
    final File mq4File = new File(mql4Dir, f);
    final File ex4File = replaceExtension(mq4File, "ex4");
    final File logFile = replaceExtension(mq4File, "log");

    if (forceRecompile)
      ex4File.delete();
    else
    {
      // check if the generated ex4 file is up-to-date
      if (ex4File.exists() && ex4File.lastModified() >= mq4File.lastModified())
      {
        LOGGER.debug("{} is up-to-date; don't compile", ex4File.getAbsolutePath());
        return;
      }
    }

    final ExecAction execAction = getExecActionFactory().newExecAction();

    if (wine)
    {
      createBatchfile(f, tmpBatch);

      execAction.setExecutable("wine");
      execAction.setArgs(Arrays.asList("cmd", "/c", tmpBatch));
    }
    else
    {
      execAction.setExecutable(metaeditor);
      execAction.setArgs(Arrays.asList("/compile:\"" + f + "\"", "/inc:\"" + mql4Dir + "\"", "/log"));
    }

    execAction.setWorkingDir(mql4Dir);

    // metaeditor.exe returns the number of compiled files... we expect 1 (would be treated as an error by gradle)
    execAction.setIgnoreExitValue(true);

    if (verbose)
      System.out.println("compile file " + f + "...");

    final ExecResult result = execAction.execute();

    try {
      if ((!wine && result.getExitValue() != 1) ||
          !ex4File.exists() ||
          ex4File.lastModified() < mq4File.lastModified())
      {
        if (logFile.exists())
          System.err.println(readLogfile(logFile));

        throw new ExecException("failed to compile " + f);
      }

      if (verbose && logFile.exists())
        System.out.println(readLogfile(logFile));
    } finally {
      logFile.delete();
    }
  }


  protected void createBatchfile(String relativeMq4Path, File batchFile) throws IOException
  {
    try(Writer bos = new FileWriter(batchFile)) {
      bos.append("@ECHO OFF\r\n")
         .append('"').append(metaeditor).append("\" ")
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
      if (text.startsWith("?"))
        text = text.substring(1);
    } catch(final Exception ex) {
      LOGGER.error("failed to read file {}", logFile.getAbsolutePath(), ex);
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
