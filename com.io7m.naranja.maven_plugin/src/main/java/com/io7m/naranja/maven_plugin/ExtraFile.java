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

package com.io7m.naranja.maven_plugin;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * An extra file.
 */

public final class ExtraFile
{
  /**
   * The path of the file as it will appear in the application.
   */

  private String fileOutput;

  /**
   * The source file.
   */

  private String fileSource;

  /**
   * {@code true} if the file should be executable
   */

  private boolean executable;

  /**
   * @return The path of the file as it will appear in the application.
   */

  public String fileOutput()
  {
    return this.fileOutput;
  }

  /**
   * @return The source file.
   */

  public Path fileSource()
  {
    return Paths.get(this.fileSource)
      .toAbsolutePath()
      .normalize();
  }

  /**
   * @return {@code true} if the file should be executable
   */

  public boolean isExecutable()
  {
    return this.executable;
  }

  /**
   * An extra file.
   */

  public ExtraFile()
  {

  }
}
