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

import java.nio.file.Path;
import java.util.Set;

/**
 * An application builder.
 */

public interface NApplicationBuilderType
{
  /**
   * Set application metadata.
   *
   * @param name  The name
   * @param value The value
   *
   * @return this
   */

  NApplicationBuilderType metadataSet(
    String name,
    String value);

  /**
   * A specification of whether or not a file should be executable.
   */

  enum Executable
  {

    /**
     * The file should be executable.
     */

    EXECUTABLE,

    /**
     * The file should not be executable.
     */

    NOT_EXECUTABLE
  }

  /**
   * Add an extra file to the application.
   *
   * @param name       The name within the application
   * @param input      The actual input file
   * @param executable The executable spec
   *
   * @return this
   */

  NApplicationBuilderType fileAdd(
    String name,
    Path input,
    Executable executable);

  /**
   * Add an artifact to the application.
   *
   * @param file            The source file
   * @param scope           The artifact scope
   * @param operatingSystem The artifact operating system
   * @param architecture    The artifact hardware architecture
   * @param artifact        The artifact
   *
   * @return this
   *
   * @throws NException On errors
   */

  NApplicationBuilderType artifactAdd(
    Path file,
    NScope scope,
    NOperatingSystem operatingSystem,
    NArchitecture architecture,
    NArtifact artifact)
    throws NException;

  /**
   * Add metadata to an artifact in the application.
   *
   * @param artifact The artifact
   * @param name     The metadata key
   * @param value    The metadata value
   *
   * @return this
   *
   * @throws NException On errors
   */

  NApplicationBuilderType artifactMetadataAdd(
    NArtifact artifact,
    String name,
    String value)
    throws NException;

  /**
   * Add a runtime to the application.
   *
   * @param runtime The runtime
   *
   * @return this
   *
   * @throws NException On errors
   */

  NApplicationBuilderType runtimeAdd(
    NRuntime runtime)
    throws NException;

  /**
   * Add metadata to a runtime in the application.
   *
   * @param runtime The runtime
   * @param name    The metadata key
   * @param value   The metadata value
   *
   * @return this
   *
   * @throws NException On errors
   */

  NApplicationBuilderType runtimeMetadataAdd(
    NRuntime runtime,
    String name,
    String value)
    throws NException;

  /**
   * Specify that the given runtime should be minimized to the set of given
   * modules.
   *
   * @param runtime The runtime
   * @param modules The modules
   *
   * @return this
   *
   * @throws NException On errors
   */

  NApplicationBuilderType runtimeSetMinimized(
    NRuntime runtime,
    Set<String> modules)
    throws NException;

  /**
   * @return An immutable application
   *
   * @throws NException On errors
   */

  NApplicationType build()
    throws NException;
}
