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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Convenient hash functions.
 */

public final class NHashing
{
  private NHashing()
  {

  }

  /**
   * Get the SHA-256 hash of the given file.
   *
   * @param file The file
   *
   * @return The hash
   *
   * @throws NoSuchAlgorithmException On errors
   * @throws IOException              On errors
   */

  public static String sha256Of(
    final File file)
    throws NoSuchAlgorithmException, IOException
  {
    final var digest =
      MessageDigest.getInstance("SHA-256");

    try (var stream =
           new DigestInputStream(new FileInputStream(file), digest)) {
      try (var nullOut = OutputStream.nullOutputStream()) {
        stream.transferTo(nullOut);
      }
    }
    return HexFormat.of().formatHex(digest.digest());
  }

  /**
   * Get the SHA-256 hash of the given file.
   *
   * @param file The file
   *
   * @return The hash
   *
   * @throws NoSuchAlgorithmException On errors
   * @throws IOException              On errors
   */

  public static String sha256Of(
    final Path file)
    throws NoSuchAlgorithmException, IOException
  {
    return sha256Of(file.toFile());
  }
}
