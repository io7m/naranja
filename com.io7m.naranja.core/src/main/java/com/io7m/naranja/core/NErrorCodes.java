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

/**
 * The standard error codes.
 */

public final class NErrorCodes
{
  private static final NErrorCode ERROR_ARTIFACT_DUPLICATE =
    new NErrorCode("error-artifact-duplicate");

  private static final NErrorCode ERROR_ARTIFACT_NONEXISTENT =
    new NErrorCode("error-artifact-nonexistent");

  private static final NErrorCode ERROR_IO =
    new NErrorCode("error-io");

  private NErrorCodes()
  {

  }

  /**
   * @return An attempt was made to register an artifact twice
   */

  public static NErrorCode errorArtifactDuplicate()
  {
    return ERROR_ARTIFACT_DUPLICATE;
  }

  /**
   * @return An attempt was made to reference a nonexistent artifact
   */

  public static NErrorCode errorArtifactNonexistent()
  {
    return ERROR_ARTIFACT_NONEXISTENT;
  }

  /**
   * @return An I/O error
   */

  public static NErrorCode errorIo()
  {
    return ERROR_IO;
  }
}
