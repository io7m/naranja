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

import static com.io7m.claypot.core.CLPCommandType.Status.SUCCESS;

/**
 * The "runtime-download" command.
 */

@Parameters(commandDescription = "Download a runtime.")
public final class NCmdRuntimeDownload extends CLPAbstractCommand
{
  @Parameter(
    names = "--id",
    description = "The runtime to download",
    required = true
  )
  private String runtimeId;

  /**
   * Construct a command.
   *
   * @param inContext The command context
   */

  public NCmdRuntimeDownload(
    final CLPCommandContextType inContext)
  {
    super(inContext);
  }

  @Override
  protected Status executeActual()
    throws NException, InterruptedException
  {
    final var inventories =
      new NRuntimeInventories();
    final var directories =
      NApplicationConfiguration.applicationDirectories();

    try (var inventory = inventories.open(directories.cacheDirectory())) {
      final var runtime =
        inventory.runtimeFind(this.runtimeId);
      final var download =
        inventory.runtimeDownload(runtime);

      this.logger().info("Downloading {}", runtime.source());

      while (!download.future().isDone()) {
        this.logger()
          .info(
            "{}/{} ({}%}",
            Long.toUnsignedString(download.sizeReceived()),
            Long.toUnsignedString(download.sizeExpected()),
            String.format("%.1f", Double.valueOf(download.progress() * 100.0))
          );
        Thread.sleep(1_000L);
      }
    }
    return SUCCESS;
  }

  @Override
  public String name()
  {
    return "runtime-download";
  }
}
