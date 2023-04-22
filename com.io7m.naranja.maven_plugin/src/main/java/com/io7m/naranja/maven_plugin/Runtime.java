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

import com.io7m.naranja.core.NArchitecture;
import com.io7m.naranja.core.NOperatingSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A bundled runtime.
 */

public final class Runtime
{
  /**
   * The operating system.
   */

  private String operatingSystem;

  /**
   * The architecture.
   */

  private String architecture;

  /**
   * The unique runtime ID.
   */

  private String id;

  /**
   * Remove all but the given modules from the runtime.
   */

  private List<String> modules = new ArrayList<>();

  /**
   * A bundled runtime.
   */

  public Runtime()
  {

  }

  /**
   * @return The unique runtime ID.
   */

  public String id()
  {
    return this.id;
  }

  /**
   * @return The architecture.
   */

  public NArchitecture architecture()
  {
    return new NArchitecture(this.architecture);
  }

  /**
   * @return The operating system.
   */

  public NOperatingSystem operatingSystem()
  {
    return new NOperatingSystem(this.operatingSystem);
  }

  /**
   * @return The set of modules
   */

  public Set<String> modules()
  {
    return Set.copyOf(this.modules);
  }
}
