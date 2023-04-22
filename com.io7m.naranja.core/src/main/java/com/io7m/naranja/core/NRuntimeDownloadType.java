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

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * A download in progress.
 */

public interface NRuntimeDownloadType
{
  /**
   * @return The expected size of the download
   */

  long sizeExpected();

  /**
   * @return The currently received size of the download
   */

  long sizeReceived();

  /**
   * @return The download progress in the range {@code [0, 1]}
   */

  default double progress()
  {
    return (double) this.sizeReceived() / (double) this.sizeExpected();
  }

  /**
   * @return The download future
   */

  CompletableFuture<Path> future();
}
