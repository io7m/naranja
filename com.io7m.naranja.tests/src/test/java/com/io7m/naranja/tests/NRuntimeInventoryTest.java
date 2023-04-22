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
import com.io7m.naranja.core.NException;
import com.io7m.naranja.core.NOperatingSystem;
import com.io7m.naranja.core.NRuntimeDistribution;
import com.io7m.naranja.runtimes.NRuntimeInventories;
import com.io7m.naranja.core.NRuntimeInventoryType;
import com.io7m.quixote.core.QWebServerType;
import com.io7m.quixote.core.QWebServers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.io7m.naranja.tests.NTestDirectories.resourceBytesOf;
import static com.io7m.naranja.tests.NTestDirectories.resourceTextOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class NRuntimeInventoryTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(NRuntimeInventoryTest.class);

  private NRuntimeInventoryType inventory;
  private Path directory;
  private QWebServerType server;

  @BeforeEach
  public void setup()
    throws IOException
  {
    this.server =
      QWebServers.createServer(10000);
    this.directory =
      NTestDirectories.createBaseDirectory();
    this.inventory =
      new NRuntimeInventories()
        .openWithURI(this.directory, this.server.uri());
  }

  @AfterEach
  public void tearDown()
    throws IOException
  {
    for (final var request : this.server.requestsReceived()) {
      LOG.debug("request: {}", request);
    }

    NTestDirectories.deleteDirectory(this.directory);
    this.server.close();
  }

  @Test
  public void testInventoryEmpty()
    throws NException
  {
    assertEquals(
      List.of(),
      this.inventory.runtimesAvailableLocally()
    );
  }

  @Test
  public void testInventoryLocallyCorrupt()
    throws NException, IOException
  {
    final var path =
      this.directory.resolve("runtimes")
        .resolve("3880ef683b04f9e23cb95311e2588dad.rti");

    Files.createDirectories(path.getParent());
    Files.write(path, "\0".getBytes(StandardCharsets.UTF_8));

    final var locally =
      this.inventory.runtimesAvailableLocally();

    assertEquals(List.of(), locally);
  }

  @Test
  public void testInventoryDownloadNothing()
    throws NException
  {
    this.server.addResponse()
      .forPath("//disco/v3.0/distributions")
      .withFixedText("[]");
    this.server.addResponse()
      .forPath("//disco/v3.0/packages")
      .withFixedText("[]");
    this.server.addResponse()
      .forPath("//disco/v3.0/packages")
      .withFixedText("[]");

    final var remotely =
      this.inventory.runtimesAvailableRemotely(
        List.of(NOperatingSystem.linux()),
        List.of(NArchitecture.x86_64()),
        List.of(new NRuntimeDistribution("temurin")),
        17
      );

    assertEquals(0, remotely.size());
  }

  @Test
  public void testInventoryDownloadTemurin()
    throws Exception
  {
    this.server.addResponse()
      .forPath("//disco/v3.0/distributions")
      .withFixedText(resourceTextOf("inventory_request0.json"));
    this.server.addResponse()
      .forPath("//disco/v3.0/packages")
      .withFixedText(resourceTextOf("inventory_request1.json"));
    this.server.addResponse()
      .forPath("//disco/v3.0/packages")
      .withFixedText(resourceTextOf("inventory_request2.json"));
    this.server.addResponse()
      .forPath("//disco/v3.0/packages/3880ef683b04f9e23cb95311e2588dad")
      .withFixedText(resourceTextOf("inventory_request3.json"));
    this.server.addResponse()
      .forPath("//disco/v3.0/ids/3880ef683b04f9e23cb95311e2588dad")
      .withFixedText(resourceTextOf("inventory_request4.json"));
    this.server.addResponse()
      .forPath("//disco/v3.0/packages/3880ef683b04f9e23cb95311e2588dad")
      .withFixedText(resourceTextOf("inventory_request3.json"));
    this.server.addResponse()
      .forPath("//disco/v3.0/ids/3880ef683b04f9e23cb95311e2588dad")
      .withFixedText(resourceTextOf("inventory_request4.json"));
    this.server.addResponse()
      .forPath("/blob.zip")
      .withFixedData(resourceBytesOf("data.bin"));

    final var remotely =
      this.inventory.runtimesAvailableRemotely(
        List.of(NOperatingSystem.linux()),
        List.of(NArchitecture.x86_64()),
        List.of(new NRuntimeDistribution("temurin")),
        17
      );

    assertEquals(2, remotely.size());

    this.inventory.runtimeDownload(remotely.get(0))
      .future()
      .get();

    final var outputFile =
      this.directory.resolve("runtimes")
        .resolve("3880ef683b04f9e23cb95311e2588dad.rt");

    assertTrue(Files.isRegularFile(outputFile));
    assertEquals(65536L, Files.size(outputFile));

    this.inventory.runtimeDownload(remotely.get(0))
      .future()
      .get();

    assertEquals(
      List.of(remotely.get(0)),
      this.inventory.runtimesAvailableLocally()
    );

    this.inventory.runtimeDelete(remotely.get(0));
    assertFalse(Files.isRegularFile(outputFile));
  }

  @Test
  public void testInventoryDownloadFails()
    throws Exception
  {
    this.server.addResponse()
      .forPath("//disco/v3.0/distributions")
      .withFixedText(resourceTextOf("inventory_request0.json"));
    this.server.addResponse()
      .forPath("//disco/v3.0/packages")
      .withFixedText(resourceTextOf("inventory_request1.json"));
    this.server.addResponse()
      .forPath("//disco/v3.0/packages")
      .withFixedText(resourceTextOf("inventory_request2.json"));
    this.server.addResponse()
      .forPath("//disco/v3.0/packages/3880ef683b04f9e23cb95311e2588dad")
      .withFixedText(resourceTextOf("inventory_request3.json"));
    this.server.addResponse()
      .forPath("//disco/v3.0/ids/3880ef683b04f9e23cb95311e2588dad")
      .withFixedText(resourceTextOf("inventory_request4.json"));
    this.server.addResponse()
      .forPath("//disco/v3.0/packages/3880ef683b04f9e23cb95311e2588dad")
      .withFixedText(resourceTextOf("inventory_request3.json"));
    this.server.addResponse()
      .forPath("//disco/v3.0/ids/3880ef683b04f9e23cb95311e2588dad")
      .withFixedText(resourceTextOf("inventory_request4.json"));
    this.server.addResponse()
      .forPath("/blob.zip")
      .withStatus(500);

    final var remotely =
      this.inventory.runtimesAvailableRemotely(
        List.of(NOperatingSystem.linux()),
        List.of(NArchitecture.x86_64()),
        List.of(new NRuntimeDistribution("temurin")),
        17
      );

    assertEquals(2, remotely.size());

    assertThrows(ExecutionException.class, () -> {
      this.inventory.runtimeDownload(remotely.get(0))
        .future()
        .get();
    });
  }

  @Test
  public void testInventoryDownloadTruncated()
    throws Exception
  {
    this.server.addResponse()
      .forPath("//disco/v3.0/distributions")
      .withFixedText(resourceTextOf("inventory_request0.json"));
    this.server.addResponse()
      .forPath("//disco/v3.0/packages")
      .withFixedText(resourceTextOf("inventory_request1.json"));
    this.server.addResponse()
      .forPath("//disco/v3.0/packages")
      .withFixedText(resourceTextOf("inventory_request2.json"));
    this.server.addResponse()
      .forPath("//disco/v3.0/packages/3880ef683b04f9e23cb95311e2588dad")
      .withFixedText(resourceTextOf("inventory_request3.json"));
    this.server.addResponse()
      .forPath("//disco/v3.0/ids/3880ef683b04f9e23cb95311e2588dad")
      .withFixedText(resourceTextOf("inventory_request4.json"));
    this.server.addResponse()
      .forPath("//disco/v3.0/packages/3880ef683b04f9e23cb95311e2588dad")
      .withFixedText(resourceTextOf("inventory_request3.json"));
    this.server.addResponse()
      .forPath("//disco/v3.0/ids/3880ef683b04f9e23cb95311e2588dad")
      .withFixedText(resourceTextOf("inventory_request4.json"));
    this.server.addResponse()
      .forPath("/blob.zip")
      .withFixedText("short!");

    final var remotely =
      this.inventory.runtimesAvailableRemotely(
        List.of(NOperatingSystem.linux()),
        List.of(NArchitecture.x86_64()),
        List.of(new NRuntimeDistribution("temurin")),
        17
      );

    assertEquals(2, remotely.size());

    assertThrows(ExecutionException.class, () -> {
      this.inventory.runtimeDownload(remotely.get(0))
        .future()
        .get();
    });
  }
}
