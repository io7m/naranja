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

import com.io7m.lanark.core.RDottedName;
import com.io7m.verona.core.Version;

import java.util.Comparator;
import java.util.Objects;

/**
 * An artifact.
 *
 * @param group The group
 * @param name    The name
 * @param version The version
 * @param type    The type
 */

public record NArtifact(
  RDottedName group,
  RDottedName name,
  Version version,
  NArtifactType type)
  implements Comparable<NArtifact>
{
  /**
   * An artifact.
   *
   * @param group The group
   * @param name    The name
   * @param version The version
   * @param type    The type
   */

  public NArtifact
  {
    Objects.requireNonNull(group, "group");
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(version, "version");
    Objects.requireNonNull(type, "type");
  }

  @Override
  public int compareTo(
    final NArtifact o)
  {
    return Comparator.comparing(NArtifact::group)
      .thenComparing(NArtifact::name)
      .thenComparing(NArtifact::version)
      .thenComparing(NArtifact::type)
      .compare(this, o);
  }
}
