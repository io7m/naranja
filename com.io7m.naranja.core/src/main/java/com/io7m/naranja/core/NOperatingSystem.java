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

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * The name of an operating system.
 *
 * @param name The operating system name
 */

public record NOperatingSystem(String name)
  implements Comparable<NOperatingSystem>
{
  private static final Pattern VALID_NAME =
    Pattern.compile("[a-z][0-9a-z_\\-]{0,127}");

  private static final NOperatingSystem OS_ANY =
    new NOperatingSystem("any");

  private static final NOperatingSystem OS_LINUX =
    new NOperatingSystem("linux");

  private static final NOperatingSystem OS_WINDOWS =
    new NOperatingSystem("windows");

  /**
   * @return Linux
   */

  public static NOperatingSystem linux()
  {
    return OS_LINUX;
  }

  /**
   * @return Windows
   */

  public static NOperatingSystem windows()
  {
    return OS_WINDOWS;
  }

  /**
   * @return The "any" OS for operating-system-independent artifacts
   */

  public static NOperatingSystem any()
  {
    return OS_ANY;
  }

  /**
   * The name of an operating system.
   *
   * @param name The operating system name
   */

  public NOperatingSystem
  {
    Objects.requireNonNull(name, "name");

    if (!VALID_NAME.matcher(name).matches()) {
      throw new IllegalArgumentException(
        "Names must match %s".formatted(VALID_NAME)
      );
    }
  }

  @Override
  public int compareTo(
    final NOperatingSystem o)
  {
    return this.name.compareTo(o.name);
  }
}
