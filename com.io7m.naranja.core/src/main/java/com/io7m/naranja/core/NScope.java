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
 * The scope of a dependency.
 *
 * @param name The scope name
 */

public record NScope(String name)
  implements Comparable<NScope>
{
  private static final Pattern VALID_NAME =
    Pattern.compile("[a-z][0-9a-z_\\-]{0,127}");

  private static final NScope JAVA_CLASS_PATH =
    new NScope("class-path");

  private static final NScope JAVA_MODULE_PATH =
    new NScope("module-path");

  /**
   * @return The scope that refers to the Java class path
   */

  public static NScope javaClassPath()
  {
    return JAVA_CLASS_PATH;
  }

  /**
   * @return The scope that refers to the Java module path
   */

  public static NScope javaModulePath()
  {
    return JAVA_MODULE_PATH;
  }

  /**
   * The scope of a dependency.
   *
   * @param name The scope name
   */

  public NScope
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
    final NScope o)
  {
    return this.name.compareTo(o.name);
  }
}
