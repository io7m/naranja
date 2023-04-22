/*
 * Copyright © 2023 Mark Raynsford <code@io7m.com> https://www.io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.naranja.maven_plugin;

import com.io7m.naranja.core.NArchitecture;
import com.io7m.naranja.core.NOperatingSystem;
import com.io7m.naranja.core.NScope;
import org.eclipse.aether.artifact.Artifact;

import java.util.regex.Pattern;

/**
 * An expression to categorize an artifact.
 */

public final class Categorize
{
  private String scope =
    NScope.javaModulePath().name();

  private String platform =
    NOperatingSystem.any().name();

  private String arch =
    NArchitecture.any().name();

  private String groupPattern =
    ".*";

  private String artifactPattern =
    ".*";

  private String classifierPattern =
    ".*";

  /**
   * @return The scope in which this artifact will be placed
   */

  public NScope scope()
  {
    return new NScope(this.scope);
  }

  /**
   * @return The operating system in which this artifact will be placed
   */

  public NOperatingSystem operatingSystem()
  {
    return new NOperatingSystem(this.platform);
  }

  /**
   * @return The architecture in which this artifact will be placed
   */

  public NArchitecture architecture()
  {
    return new NArchitecture(this.arch);
  }

  /**
   * An expression to categorize an artifact.
   */

  public Categorize()
  {

  }

  /**
   * @param artifact The artifact
   *
   * @return {@code true} if the artifact matches
   */

  public boolean matches(
    final Artifact artifact)
  {
    if (!Pattern.matches(this.groupPattern, artifact.getGroupId())) {
      return false;
    }
    if (!Pattern.matches(this.artifactPattern, artifact.getArtifactId())) {
      return false;
    }

    var classifier = artifact.getClassifier();
    if (classifier == null) {
      classifier = "";
    }
    return Pattern.matches(this.classifierPattern, classifier);
  }
}
