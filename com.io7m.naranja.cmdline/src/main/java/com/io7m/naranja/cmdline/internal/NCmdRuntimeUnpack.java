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
import com.io7m.naranja.core.NException;
import com.io7m.naranja.runtimes.NRuntimeInventories;

import java.nio.file.Path;

import static com.io7m.claypot.core.CLPCommandType.Status.SUCCESS;

/**
 * The "runtime-unpack" command.
 */

@Parameters(commandDescription = "Unpack a runtime.")
public final class NCmdRuntimeUnpack extends CLPAbstractCommand
{
  @Parameter(
    names = "--id",
    description = "The runtime to unpack",
    required = true
  )
  private String runtimeId;

  @Parameter(
    names = "--output-directory",
    description = "The output directory",
    required = true
  )
  private Path outputDirectory;

  /**
   * Construct a command.
   *
   * @param inContext The command context
   */

  public NCmdRuntimeUnpack(
    final CLPCommandContextType inContext)
  {
    super(inContext);
  }

  @Override
  protected Status executeActual()
    throws NException, InterruptedException
  {
    this.outputDirectory = this.outputDirectory.toAbsolutePath();

    final var inventories =
      new NRuntimeInventories();

    final var directories =
      NApplicationConfiguration.applicationDirectories();

    try (var inventory = inventories.open(directories.cacheDirectory())) {
      final var runtime = inventory.runtimeFind(this.runtimeId);
      inventory.runtimeUnpack(runtime, this.outputDirectory);
    }
    return SUCCESS;
  }

  @Override
  public String name()
  {
    return "runtime-unpack";
  }
}
