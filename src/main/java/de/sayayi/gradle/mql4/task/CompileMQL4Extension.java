package de.sayayi.gradle.mql4.task;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import lombok.Getter;
import lombok.Setter;


/**
 * Extension for configuring the compileMql4 task.
 *
 * @author Jeroen Gremmen
 */
public class CompileMQL4Extension
{
  @Getter @Setter
  private String metaeditor;

  @Getter
  private File mql4Dir = new File("MQL4");

  @Getter
  private List<String> includes = new ArrayList<>(Arrays.asList("Indicators/*.mq4", "Experts/*.mq4", "Scripts/*.mq4"));

  @Getter
  private List<String> excludes = new ArrayList<>(Arrays.asList("**/*.mqh"));

  @Getter
  private boolean forceRecompile;

  @Getter
  private boolean wine;

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


  public void setForceRecompile(boolean forceRecompile) {
    this.forceRecompile = forceRecompile;
  }


  public void setForceRecompile(String forceRecompile) {
    this.forceRecompile = Boolean.parseBoolean(forceRecompile);
  }


  public void setWine(boolean wine) {
    this.wine = wine;
  }


  public void setWine(String wine) {
    this.wine = Boolean.parseBoolean(wine);
  }


  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }


  public void setVerbose(String verbose) {
    this.verbose = Boolean.parseBoolean(verbose);
  }
}
