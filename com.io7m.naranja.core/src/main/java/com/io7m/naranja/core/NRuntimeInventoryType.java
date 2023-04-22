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
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * The runtime inventory.
 */

public interface NRuntimeInventoryType extends AutoCloseable
{
  /**
   * List the runtimes available remotely that match the given parameters.
   *
   * @param operatingSystems The operating systems to match against
   * @param architectures    The architectures to match against
   * @param distributions    The distributions to match against
   * @param jdkVersion       The JDK major version
   *
   * @return A list of matching runtimes
   *
   * @throws NException On errors
   */

  List<NRuntime> runtimesAvailableRemotely(
    Collection<NOperatingSystem> operatingSystems,
    Collection<NArchitecture> architectures,
    Collection<NRuntimeDistribution> distributions,
    int jdkVersion)
    throws NException;

  /**
   * List the available runtimes that have been downloaded locally.
   *
   * @return The runtimes
   *
   * @throws NException On errors
   */

  List<NRuntime> runtimesAvailableLocally()
    throws NException;

  /**
   * Start a download of the given runtime. If the runtime has already been
   * downloaded, the download completes instantly.
   *
   * @param runtime The runtime
   *
   * @return The download
   *
   * @throws NException On errors
   */

  NRuntimeDownloadType runtimeDownload(
    NRuntime runtime)
    throws NException;

  /**
   * Unpack a downloaded runtime into the given directory.
   *
   * @param runtime The runtime
   * @param output  The output directory
   *
   * @throws NException On errors
   */

  void runtimeUnpack(
    NRuntime runtime,
    Path output)
    throws NException;

  /**
   * Unpack a downloaded runtime into the given directory, re-linking it with
   * {@code jlink} to produce a smaller runtime.
   *
   * @param runtime The runtime
   * @param output  The output directory
   * @param modules The set of modules that must be present in the runtime
   *
   * @throws NException On errors
   */

  void runtimeUnpackAndRelink(
    NRuntime runtime,
    Path output,
    Set<String> modules)
    throws NException;

  /**
   * Delete the given runtime, if it exists locally.
   *
   * @param runtime The runtime
   *
   * @throws NException On errors
   */

  void runtimeDelete(NRuntime runtime)
    throws NException;

  @Override
  void close()
    throws NException;

  /**
   * Find a runtime with the given ID.
   *
   * @param id The ID
   *
   * @return The runtime
   *
   * @throws NException On errors
   */

  NRuntime runtimeFind(String id)
    throws NException;
}
