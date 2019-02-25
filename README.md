# Gradle MQL4 Compile Task
[![Apache 2.0](https://img.shields.io/badge/license-Apache%202.0-red.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/de.sayayi.gradle/gradle-mql4-task.svg)](https://search.maven.org/search?q=g:de.sayayi.gradle%20a:gradle-mql4-task)
[![Gradle Plugin](https://img.shields.io/badge/gradle--plugin-1.0.1-brightgreen.svg)](https://plugins.gradle.org/plugin/de.sayayi.gradle.mql4-plugin)
![Platform](https://img.shields.io/badge/platform-windows%20%7C%20linux%20%7C%20macos-lightgrey.svg)

Grade Task for compiling MetaTrader MQL4 files.

## Supported Features

- Compilation of a single .mq4 or all .mq4 source files found in the MQL4 directory structure
- Gradle incremental build support
- Integrate compile logging in gradle build output
- Wine support which allows for compilation on non-windows platforms

## Usage

### Include Task

Edit your `build.gradle` file and add :

```groovy
  plugins {
    id "de.sayayi.gradle.mql4-plugin" version "1.0.1"
  }
```

Or as part of your `buildscript` section:

```groovy
  buildscript {
    repositories {
      maven {
        url "https://plugins.gradle.org/m2/"
      }
    }
    dependencies {
      classpath "de.sayayi.gradle:gradle-mql4-task:1.0.1"
    }
  }

  apply plugin: "de.sayayi.gradle.mql4-plugin"
```

This will create both a `compileMql4` task and a `mql4` extension. If the base plugin `assemble` task
is available this task is added as a dependency.

### Configure Task

```groovy
  mql4 {
    mql4Dir "${rootProject.projectDir}/MQL4"
    metaeditor "C:\\MT4\\metaeditor.exe"
    verbose = true
  }
```

#### Supported Properties
Name | Type | Description
--- | --- | ---
mql4Dir | File | *Required.* The MQL4 path. For windows this property must contain a windows path (eg. `C:\Project\MyMQL4`), for unix this property must contain a unix path.
metaeditor | String | *Required.* Full windows path to `metaeditor.exe`.
includes | String[] | *Optional.* A set of .mq4 files to include for compilation. The includes must be relative to the path specified in `mql4Dir`. Default: `[ "Indicators/*.mq4", "Experts/*.mq4", "Scripts/*.mq4" ]`
excludes | String[] | *Optional.* A set of .mq4 files to exclude from compilation. The excludes must be relative to the path specified in `mql4Dir`. Default: `[ "**/*.mqh" ]`
wine | boolean | *Optional.* `true` to run the compiler in a wine environment. `false` to run the compiler in a windows environment. Default: `false` (no auto detection)
forceRecompile | boolean | *Optional.* `true` forces the compiler to recompile each .mq4 file. `false` incrementally build .mq4 files. Default: `false`
verbose | boolean | *Optional.* `true` redirects the compile log output to the gradle build output. `false` only include compile log in case of a compilation error. Default: `false`

## License

The license is Apache 2.0, see LICENSE file.

Copyright (c) 2019, Jeroen Gremmen
