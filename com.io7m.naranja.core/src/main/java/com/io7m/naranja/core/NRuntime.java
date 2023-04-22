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

import java.net.URI;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A description of a runtime.
 *
 * @param id              A unique identifier for the runtime
 * @param distribution    The distribution name (such as "temurin")
 * @param operatingSystem The operating system (such as "linux")
 * @param architecture    The architecture (such as "x86_64")
 * @param source          The source URI
 * @param archiveType     The archive type
 * @param version         The JDK version (such as 17)
 * @param versionText     The raw version text (such as "17.0.7_7")
 * @param size            The size of the distribution in octets
 */

public record NRuntime(
  String id,
  NRuntimeDistribution distribution,
  NOperatingSystem operatingSystem,
  NArchitecture architecture,
  NRuntimeArchiveType archiveType,
  URI source,
  int version,
  String versionText,
  long size)
{
  private static final Pattern VALID_ID =
    Pattern.compile("[a-f0-9]{1,256}");

  /**
   * A description of a runtime.
   *
   * @param id              A unique identifier for the runtime
   * @param distribution    The distribution name (such as "temurin")
   * @param operatingSystem The operating system (such as "linux")
   * @param architecture    The architecture (such as "x86_64")
   * @param source          The source URI
   * @param archiveType     The archive type
   * @param version         The JDK version (such as 17)
   * @param versionText     The raw version text (such as "17.0.7_7")
   * @param size            The size of the distribution in octets
   */

  public NRuntime
  {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(distribution, "distribution");
    Objects.requireNonNull(operatingSystem, "operatingSystem");
    Objects.requireNonNull(architecture, "architecture");
    Objects.requireNonNull(source, "source");
    Objects.requireNonNull(archiveType, "archiveType");
    Objects.requireNonNull(versionText, "versionText");

    if (!VALID_ID.matcher(id).matches()) {
      throw new IllegalArgumentException(
        "Runtime identifiers must match %s".formatted(VALID_ID)
      );
    }
  }
}
