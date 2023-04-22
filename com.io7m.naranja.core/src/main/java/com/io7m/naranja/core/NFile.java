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

import com.io7m.naranja.core.NApplicationBuilderType.Executable;

import java.nio.file.Path;
import java.util.Objects;

/**
 * A file.
 *
 * @param name       The name within the application
 * @param source     The source file
 * @param executable The executable
 */

public record NFile(
  String name,
  Path source,
  Executable executable)
{
  /**
   * A file.
   *
   * @param name       The name within the application
   * @param source     The source file
   * @param executable The executable
   */

  public NFile
  {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(source, "source");
    Objects.requireNonNull(executable, "executable");
  }
}
