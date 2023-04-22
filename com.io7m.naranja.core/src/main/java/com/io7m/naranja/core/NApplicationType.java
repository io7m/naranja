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

package com.io7m.naranja.core;

import com.io7m.lanark.core.RDottedName;
import com.io7m.verona.core.Version;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A compiled application.
 */

public interface NApplicationType
{
  /**
   * @return The application name
   */

  RDottedName name();

  /**
   * @return The application version
   */

  Version version();

  /**
   * @return The extra files
   */

  Map<String, NFile> files();

  /**
   * @return The application metadata
   */

  Map<String, String> applicationMetadata();

  /**
   * @return The map of artifacts
   */

  Map<NArtifact, NAttachedArtifact> artifacts();

  /**
   * @return The set of runtimes
   */

  Set<NRuntime> runtimes();

  /**
   * @param runtime The runtime
   *
   * @return The set of minimized modules for the runtime
   */

  Optional<Set<String>> runtimeMinimizedModules(
    NRuntime runtime);

  /**
   * @param artifact The artifact
   *
   * @return The artifact
   */

  default Optional<NAttachedArtifact> artifact(
    final NArtifact artifact)
  {
    return Optional.ofNullable(
      this.artifacts()
        .get(Objects.requireNonNull(artifact, "artifact"))
    );
  }

  /**
   * @param artifact The artifact
   *
   * @return The source file for the given artifact
   *
   * @throws NException On nonexistent artifacts
   */

  Path fileForArtifact(NArtifact artifact)
    throws NException;

  /**
   * @param artifact The artifact
   *
   * @return The metadata for the given artifact
   */

  Optional<Map<String, String>> artifactMetadataOptional(
    NArtifact artifact);

  /**
   * @param artifact The artifact
   *
   * @return The metadata for the given artifact
   *
   * @throws NException On nonexistent artifacts
   */

  Map<String, String> artifactMetadata(NArtifact artifact)
    throws NException;

  /**
   * @param scope           The scope
   * @param operatingSystem The operating system
   * @param architecture    The hardware architecture
   *
   * @return The set of artifacts for the given platform in the given scope
   */

  Set<NAttachedArtifact> artifactsFor(
    NScope scope,
    NOperatingSystem operatingSystem,
    NArchitecture architecture);

  /**
   * @param runtime The runtime
   *
   * @return The metadata for the given runtime
   */

  Optional<Map<String, String>> runtimeMetadataOptional(
    NRuntime runtime);

  /**
   * @param runtime The runtime
   *
   * @return The metadata for the given runtime
   *
   * @throws NException On nonexistent runtimes
   */

  Map<String, String> runtimeMetadata(NRuntime runtime)
    throws NException;
}
