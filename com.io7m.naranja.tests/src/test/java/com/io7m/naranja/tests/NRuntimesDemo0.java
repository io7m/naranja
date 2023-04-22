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


package com.io7m.naranja.tests;

import com.io7m.naranja.core.NArchitecture;
import com.io7m.naranja.core.NOperatingSystem;
import com.io7m.naranja.core.NRuntimeDistribution;
import com.io7m.naranja.runtimes.NRuntimeInventories;

import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class NRuntimesDemo0
{
  private NRuntimesDemo0()
  {

  }

  public static void main(
    final String[] args)
    throws Exception
  {
    final var inventories = new NRuntimeInventories();
    try (var inventory = inventories.open(Paths.get("/tmp"))) {
      final var runtimes =
        inventory.runtimesAvailableRemotely(
          List.of(NOperatingSystem.linux()),
          List.of(NArchitecture.x86_64()),
          List.of(new NRuntimeDistribution("temurin")),
          17
        );

      final var runtime =
        runtimes.get(0);

      {
        final var download =
          inventory.runtimeDownload(runtime);

        while (!download.future().isDone()) {
          System.out.printf(
            "%s: %s/%s %f%n",
            runtime.source(),
            Long.toUnsignedString(download.sizeReceived()),
            Long.toUnsignedString(download.sizeExpected()),
            Double.valueOf(download.progress())
          );
          Thread.sleep(1_000L);
        }

        download.future().get();

        assertEquals(
          List.of(runtime),
          inventory.runtimesAvailableLocally()
        );
      }

      inventory.runtimeDelete(runtime);

      {
        final var download =
          inventory.runtimeDownload(runtime);

        while (!download.future().isDone()) {
          System.out.printf(
            "%s: %s/%s %f%n",
            runtime.source(),
            Long.toUnsignedString(download.sizeReceived()),
            Long.toUnsignedString(download.sizeExpected()),
            Double.valueOf(download.progress())
          );
          Thread.sleep(1_000L);
        }

        download.future().get();

        assertEquals(
          List.of(runtime),
          inventory.runtimesAvailableLocally()
        );
      }
    }
  }
}
