/*
 * Copyright © 2022 Mark Raynsford <code@io7m.com> https://www.io7m.com
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


package com.io7m.naranja.tests;

import com.io7m.naranja.core.NOperatingSystem;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class NOperatingSystemTest
{
  @TestFactory
  public Stream<DynamicTest> testInvalid()
  {
    return Stream.of(
        " ",
        "a.b.c.d.e.f.g.h.i.a.b.c.d.e.f.g.h.i")
      .map(s -> {
        return DynamicTest.dynamicTest("testInvalid_" + s, () -> {
          assertThrows(IllegalArgumentException.class, () -> {
            new NOperatingSystem(s);
          });
        });
      });
  }

  @Provide
  public Arbitrary<NOperatingSystem> names()
  {
    return Arbitraries.strings()
      .alpha()
      .ofMinLength(1)
      .ofMaxLength(128)
      .map(String::toLowerCase)
      .map(NOperatingSystem::new);
  }

  @Property
  public void testNameLength(
    final @ForAll("names") NOperatingSystem name)
  {
    assertTrue(name.name().length() <= 128);
  }

  @Property
  public void testToStringIdentity(
    final @ForAll("names") NOperatingSystem name)
  {
    assertEquals(
      name,
      new NOperatingSystem(name.name())
    );
  }
}
