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
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.gradle.api.GradleException;
import org.gradle.internal.os.OperatingSystem;

import lombok.Getter;
import lombok.ToString;


/**
 * @author Jeroen Gremmen
 */
@ToString
public class Wine
{
  /**
   * wine executable path. The default is {@code wine} without a path reference.
   */
  @Getter
  private String executable = "wine";

  /**
   * Wine prefix path. The environment variable {@code WINEPREFIX} will be set to the prefix path
   */
  @Getter
  private File prefix = null;

  /**
   * <p>
   *  {@code true}, enables mql4 compiling using a wine environment. {@code false}, assumes gradle is running on a
   *  windows environment and runs {@code metaeditor.exe} accordingly.
   * </p>
   *
   * <p>
   *   The default is calculated based on the operating system reported by Gradle.
   * </p>
   */
  @Getter
  private boolean enabled;

  @Getter
  private String systemWideDrive = "z:";


  public Wine()
  {
    // enable wine on non-windows architectures
    enabled = !OperatingSystem.current().isWindows();

    // if WINEPREFIX is set in environment, copy it into the configuration
    final String winePrefix = System.getProperty("WINEPREFIX");
    if (winePrefix != null && new File(winePrefix).isDirectory())
      setPrefix(winePrefix);
  }


  public void setExecutable(String executable) {
    this.executable = executable;
  }


  public void setExecutable(File executable) {
    this.executable = executable.getAbsolutePath();
  }


  public void setPrefix(File prefix) {
    this.prefix = prefix;
  }


  public void setPrefix(String prefix)
  {
    this.prefix = new File(prefix);

    final File dosdevices = new File(this.prefix, "dosdevices");

    if (dosdevices.isDirectory())
    {
      // find all drives
      final File[] drives = dosdevices.listFiles((FileFilter) file -> {
        final String name = file.getName();
        return name.length() == 2 && name.charAt(1) == ':' && Files.isSymbolicLink(file.toPath());
      });

      for(final File drive: drives)
      {
        try {
          final Path link = Files.readSymbolicLink(drive.toPath());
          if ("/".equals(link.toFile().getAbsolutePath()))
          {
            // root symlink
            systemWideDrive = drive.getName();
            break;
          }
        } catch(final IOException ex) {
          // ignore
        }
      }
    }
  }


  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }


  public void setEnabled(String enabled) {
    this.enabled = Boolean.parseBoolean(enabled);
  }


  public void setSystemWideDrive(String systemWideDrive)
  {
    if (!systemWideDrive.matches("[a-zA-Z]\\x3a"))
      throw new GradleException("system wide drive must be a windows drive specification (eg. d:)");

    this.systemWideDrive = systemWideDrive.toLowerCase();
  }
}
