# Gradle MQL4 Compile Task

Grade Task for compiling MetaTrader MQL4 files.

## Supported Features
- Compilation of a single .mq4 or all .mq4 source files found in the MQL4 directory structure
- Integrate compile logging in gradle build output
- Wine support

## Usage

### 1. Include Task

Edit your `build.gradle`file and add a `buildscript` section. Something like this:

```groovy
buildscript {
  repositories {
    maven { url "http://maven.sayayi.de/repository/maven-public/" }
  }

  dependencies {
    classpath "de.sayayi.gradle:gradle-mql4-task:1.0.1"
  }
}

apply plugin: "de.sayayi.gradle.mql4-plugi"
```

### 2. Use Task

```groovy
task compileMq4(type: CompileMQL4Task) {
  mql4Dir "${rootProject.projectDir}/MQL4"
  metaeditor "C:\\MT4\\metaeditor.exe"
}
```

### Supported Properties
Name | Type | Description
--- | --- | ---
mql4Dir | File | *Required.* The MQL4 path. For windows this property must contain a windows path (eg. `C:\Project\MyMQL4`), for unix this property must contain a unix path.
metaeditor | String | *Required.* Full windows path to `metaeditor.exe`.
includes | String[] | *Optional.* A set of .mq4 files to include for compilation. The includes must be relative to the path specified in `mql4Dir`. Default: `[ "Indicators/*.mq4", "Experts/*.mq4", "Scripts/*.mq4" ]`
excludes | String[] | *Optional.* A set of .mq4 files to exclude from compilation. The excludes must be relative to the path specified in `mql4Dir`.
wine | boolean | *Optional.* `true` to run the compiler in a wine environment. `false` to run the compiler in a windows environment. Default: `false` (no auto detection)
forceRecompile | boolean | *Optional.* `true` forces the compiler to recompile each .mq4 file. `false` incrementally build .mq4 files. Default: `false`
verbose | boolean | *Optional.* `true` redirects the compile log output to the gradle build output. `false` only include compile log in case of a compilation error. Default: `false`
