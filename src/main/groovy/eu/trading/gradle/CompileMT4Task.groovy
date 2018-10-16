package eu.trading.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

import org.gradle.process.internal.ExecAction;
import org.gradle.process.internal.ExecActionFactory;
import org.gradle.process.internal.ExecException;

import org.gradle.process.ExecResult;

import java.io.File;

import javax.inject.Inject;


/**
 * @author Jeroen Gremmen
 */
class CompileMT4Task extends DefaultTask
{
  String mql4Dir = "MQL4";
  String metaeditor;
  String[] includes = [ "Indicators/*.mq4", "Experts/*.mq4", "Scripts/*.mq4" ]
  String[] excludes = []

  boolean verbose = false;
  boolean wine = false;
  boolean forceRecompile = false;


  @Inject
  protected ExecActionFactory getExecActionFactory() {
    throw new UnsupportedOperationException();
  }


  public void setForceRecompile(boolean forceRecompile) {
    this.forceRecompile = forceRecompile;
  }


  public void setForceRecompile(String forceRecompile) {
    this.forceRecompile = Boolean.parseBoolean(forceRecompile);
  }


  public boolean isForceRecompile() {
    return forceRecompile;
  }


  public void setWine(boolean wine) {
    this.wine = wine;
  }


  public void setWine(String wine) {
    this.wine = Boolean.parseBoolean(wine);
  }


  public boolean isWine() {
    return wine;
  }


  @TaskAction
  def compileMT4()
  {
    if (!new File(mql4Dir).isDirectory())
      throw new GradleException(mql4Dir + " is not a directory");

    File tmpBatch = null;

    if (this.wine)
    {
      tmpBatch = File.createTempFile("mql4c", ".cmd", new File(mql4Dir));
      tmpBatch.deleteOnExit();

      if (verbose)
        println("created temporary batch file " + tmpBatch);
    }

    try {
      getFiles().each { f ->
        compileFile(f, tmpBatch)
      }
    } finally {
      if (tmpBatch != null)
        tmpBatch.delete();
    }
  }


  private Set<String> getFiles()
  {
    def finder = new FileNameFinder()
    Set<String> fileSet = new LinkedHashSet<String>();

    includes.each { pattern ->
      finder.getFileNames(mql4Dir, pattern).each { f -> fileSet.add(makeRelative(f)) }
    }

    excludes.each { pattern ->
      finder.getFileNames(mql4Dir, pattern).each { f -> fileSet.remove(makeRelative(f)) }
    }

    return fileSet;
  }


  protected String makeRelative(String f)
  {
    f = f.substring(mql4Dir.length());
    if (f.startsWith("/") || f.startsWith("\\"))
      f = f.substring(1);

    return f;
  }


  protected void compileFile(String f, File tmpBatch)
  {
    def mq4File = new File(mql4Dir, f)
    def ex4File = new File(mql4Dir, f.take(f.lastIndexOf('.')) + ".ex4")
    def logFile = new File(mql4Dir, f.take(f.lastIndexOf('.')) + ".log")

    if (this.forceRecompile)
      ex4File.delete();
    else
    {
      // check if the genrated ex4 file is up-to-date
      if (ex4File.exists() && ex4File.lastModified() >= mq4File.lastModified())
        return;
    }

    ExecAction execAction = getExecActionFactory().newExecAction();

    if (this.wine)
    {
      tmpBatch.text = createBatchfile(f);

      execAction.setExecutable("wine");
      execAction.setArgs([ "cmd", "/c", tmpBatch ]);
    }
    else
    {
      execAction.setExecutable(metaeditor);
      execAction.setArgs([ "/compile:\"" + f + "\"", "/inc:\"" + mql4Dir + "\"", "/log" ]);
    }

    execAction.setWorkingDir(new File(mql4Dir));

    // metaeditor.exe returns the number of compiled files... we expect 1 (would be treated as an error by gradle)
    execAction.setIgnoreExitValue(true);

    if (verbose)
      println "compile file " + f + "..."

    ExecResult result = execAction.execute();

    if ((!this.wine && result.getExitValue() != 1) ||
        !ex4File.exists() ||
        ex4File.lastModified() < mq4File.lastModified())
    {
      if (logFile.exists())
        System.err.println logFile.text.trim();

      throw new ExecException("failed to compile " + f);
    }

    if (verbose && logFile.exists())
      System.out.println logFile.text.trim();
  }


  protected String createBatchfile(String f)
  {
    return new StringBuilder()
       .append("@ECHO OFF\r\n")
       .append('"').append(metaeditor).append("\" ")
       .append("/compile:\"").append(f.replace("/", "\\")).append("\" ")
       .append("/log")
       .append("\r\n")
       .toString();
  }
}
