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

package com.io7m.naranja.cmdline.internal;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.io7m.claypot.core.CLPAbstractCommand;
import com.io7m.claypot.core.CLPCommandContextType;
import com.io7m.naranja.core.NArchitecture;
import com.io7m.naranja.core.NException;
import com.io7m.naranja.core.NOperatingSystem;
import com.io7m.naranja.core.NRuntime;
import com.io7m.naranja.core.NRuntimeDistribution;
import com.io7m.naranja.runtimes.NRuntimeInventories;
import com.io7m.naranja.core.NRuntimeInventoryType;

import java.util.List;
import java.util.Objects;

import static com.io7m.claypot.core.CLPCommandType.Status.SUCCESS;

/**
 * The "runtime-search" command.
 */

@Parameters(commandDescription = "Find runtimes.")
public final class NCmdRuntimeSearch extends CLPAbstractCommand
{
  @Parameter(
    names = "--operating-system",
    description = "The operating system(s) to include"
  )
  private List<String> operatingSystems = List.of();

  @Parameter(
    names = "--architecture",
    description = "The architecture(s) to include"
  )
  private List<String> architectures = List.of();

  @Parameter(
    names = "--distribution",
    description = "The distribution(s) to include"
  )
  private List<String> distributions = List.of();

  @Parameter(
    names = "--jdk-version",
    description = "The JDK version to include"
  )
  private int version = 17;

  private List<NRuntimeDistribution> distributionsT;
  private List<NArchitecture> architecturesT;
  private List<NOperatingSystem> operatingSystemsT;

  /**
   * Construct a command.
   *
   * @param inContext The command context
   */

  public NCmdRuntimeSearch(
    final CLPCommandContextType inContext)
  {
    super(inContext);
  }

  @Override
  protected Status executeActual()
    throws NException
  {
    this.operatingSystemsT =
      this.operatingSystems.stream()
        .map(NOperatingSystem::new)
        .toList();

    this.architecturesT =
      this.architectures.stream()
        .map(NArchitecture::new)
        .toList();

    this.distributionsT =
      this.distributions.stream()
        .map(NRuntimeDistribution::new)
        .toList();

    final var inventories =
      new NRuntimeInventories();

    final var directories =
      NApplicationConfiguration.applicationDirectories();

    try (var inventory = inventories.open(directories.cacheDirectory())) {
      this.showHeader();
      this.showLocalRuntimes(inventory);
      this.showRemoteRuntimes(inventory);
    }
    return SUCCESS;
  }

  private void showRemoteRuntimes(
    final NRuntimeInventoryType inventory)
    throws NException
  {
    final var runtimes =
      inventory.runtimesAvailableRemotely(
        this.operatingSystemsT,
        this.architecturesT,
        this.distributionsT,
        this.version
      );

    for (final var runtime : runtimes) {
      this.showRuntime(runtime);
    }
  }

  private void showLocalRuntimes(
    final NRuntimeInventoryType inventory)
    throws NException
  {
    final var locally =
      inventory.runtimesAvailableLocally();

    for (final var local : locally) {
      if (this.matchesArch(local)
          && this.matchesOS(local)
          && this.matchesDistribution(local)
          && this.matchesVersion(local)) {
        this.showRuntime(local);
      }
    }
  }

  private boolean matchesVersion(
    final NRuntime local)
  {
    return local.version() == this.version;
  }

  private boolean matchesDistribution(
    final NRuntime runtime)
  {
    if (this.distributionsT.isEmpty()) {
      return true;
    }

    for (final var distribution : this.distributionsT) {
      if (Objects.equals(runtime.distribution(), distribution)) {
        return true;
      }
    }
    return false;
  }

  private boolean matchesOS(
    final NRuntime runtime)
  {
    if (this.operatingSystemsT.isEmpty()) {
      return true;
    }

    for (final var system : this.operatingSystemsT) {
      if (Objects.equals(runtime.operatingSystem(), system)) {
        return true;
      }
    }
    return false;
  }

  private boolean matchesArch(
    final NRuntime runtime)
  {
    if (this.architecturesT.isEmpty()) {
      return true;
    }

    for (final var arch : this.architecturesT) {
      if (Objects.equals(runtime.architecture(), arch)) {
        return true;
      }
    }
    return false;
  }

  private void showHeader()
  {
    System.out.printf(
      "# %-46s | %-8s | %-8s | %-16s | %-16s | %s%n",
      "ID",
      "OS",
      "ARCH",
      "DISTRIBUTION",
      "VERSION",
      "SIZE"
    );
  }

  private void showRuntime(
    final NRuntime runtime)
  {
    System.out.printf(
      "%-48s | %-8s | %-8s | %-16s | %-16s | %s%n",
      runtime.id(),
      runtime.operatingSystem().name(),
      runtime.architecture().name(),
      runtime.distribution().name(),
      runtime.versionText(),
      Long.toUnsignedString(runtime.size())
    );
  }

  @Override
  public String name()
  {
    return "runtime-search";
  }
}
