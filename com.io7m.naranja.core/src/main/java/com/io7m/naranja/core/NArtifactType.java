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
 * The type of an artifact.
 *
 * @param name The type name
 */

public record NArtifactType(String name)
  implements Comparable<NArtifactType>
{
  private static final Pattern VALID_NAME =
    Pattern.compile("[a-z][a-z_\\-]{0,127}");

  private static final NArtifactType TYPE_JAR =
    new NArtifactType("jar");

  /**
   * @return The jar file type
   */

  public static NArtifactType jar()
  {
    return TYPE_JAR;
  }

  /**
   * The type of an artifact.
   *
   * @param name The type name
   */

  public NArtifactType
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
    final NArtifactType o)
  {
    return this.name.compareTo(o.name);
  }
}
