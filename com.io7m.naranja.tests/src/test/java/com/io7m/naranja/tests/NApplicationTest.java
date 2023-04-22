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

package com.io7m.naranja.tests;

import com.io7m.lanark.core.RDottedName;
import com.io7m.naranja.core.NApplications;
import com.io7m.naranja.core.NArchitecture;
import com.io7m.naranja.core.NArtifact;
import com.io7m.naranja.core.NArtifactType;
import com.io7m.naranja.core.NErrorCodes;
import com.io7m.naranja.core.NException;
import com.io7m.naranja.core.NOperatingSystem;
import com.io7m.naranja.core.NScope;
import com.io7m.naranja.core.NAttachedArtifact;
import com.io7m.verona.core.Version;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class NApplicationTest
{
  /**
   * Empty applications are valid.
   *
   * @throws NException On errors
   */

  @Test
  public void testEmpty()
    throws NException
  {
    final var name =
      new RDottedName("com.io7m.example");
    final var version =
      Version.of(1, 0, 0);

    final var app =
      NApplications.create(name, version)
        .build();

    assertEquals(name, app.name());
    assertEquals(version, app.version());
    assertEquals(Map.of(), app.artifacts());
  }

  /**
   * Artifacts cannot be registered twice.
   *
   * @throws NException On errors
   */

  @Test
  public void testArtifactTwice()
    throws NException
  {
    final var name =
      new RDottedName("com.io7m.example");
    final var version =
      Version.of(1, 0, 0);

    final var builder =
      NApplications.create(name, version);

    builder.artifactAdd(
      Paths.get(""),
      NScope.javaClassPath(),
      NOperatingSystem.any(),
      NArchitecture.any(),
      new NArtifact(
        new RDottedName("com.io7m.ex0"),
        new RDottedName("com.io7m.ex0"),
        Version.of(1, 0, 0),
        NArtifactType.jar()
      )
    );

    final var ex = assertThrows(NException.class, () -> {
      builder.artifactAdd(
        Paths.get(""),
        NScope.javaClassPath(),
        NOperatingSystem.any(),
        NArchitecture.any(),
        new NArtifact(
          new RDottedName("com.io7m.ex0"),
          new RDottedName("com.io7m.ex0"),
          Version.of(1, 0, 0),
          NArtifactType.jar()
        )
      );
    });

    assertEquals(NErrorCodes.errorArtifactDuplicate(), ex.errorCode());
  }

  /**
   * Metadata cannot be registered for nonexistent artifacts.
   *
   * @throws NException On errors
   */

  @Test
  public void testArtifactMetadataNonexistent()
    throws NException
  {
    final var name =
      new RDottedName("com.io7m.example");
    final var version =
      Version.of(1, 0, 0);

    final var builder =
      NApplications.create(name, version);

    final var ex = assertThrows(NException.class, () -> {
      builder.artifactMetadataAdd(
        new NArtifact(
          new RDottedName("com.io7m.ex0"),
          new RDottedName("com.io7m.ex0"),
          Version.of(1, 0, 0),
          NArtifactType.jar()
        ),
        "A",
        "B"
      );
    });

    assertEquals(NErrorCodes.errorArtifactNonexistent(), ex.errorCode());
  }

  /**
   * Valid applications are valid.
   *
   * @throws NException On errors
   */

  @Test
  public void testValid0()
    throws NException
  {
    final var name =
      new RDottedName("com.io7m.example");
    final var version =
      Version.of(1, 0, 0);

    final var art0 = new NArtifact(
      new RDottedName("com.io7m.ex0"),
      new RDottedName("com.io7m.ex0"),
      Version.of(1, 0, 0),
      NArtifactType.jar()
    );

    final var art1 = new NArtifact(
      new RDottedName("com.io7m.ex1"),
      new RDottedName("com.io7m.ex1"),
      Version.of(1, 0, 0),
      NArtifactType.jar()
    );

    final var art2 = new NArtifact(
      new RDottedName("com.io7m.ex2"),
      new RDottedName("com.io7m.ex2"),
      Version.of(1, 0, 0),
      NArtifactType.jar()
    );

    final var app =
      NApplications.create(name, version)
        .artifactAdd(
          Paths.get(""),
          NScope.javaModulePath(),
          NOperatingSystem.any(),
          NArchitecture.any(),
          art0
        )
        .artifactAdd(
          Paths.get(""),
          NScope.javaModulePath(),
          NOperatingSystem.any(),
          NArchitecture.any(),
          art1
        )
        .artifactAdd(
          Paths.get(""),
          NScope.javaModulePath(),
          NOperatingSystem.any(),
          NArchitecture.any(),
          art2
        )
        .build();

    assertEquals(name, app.name());
    assertEquals(version, app.version());

    final var art0s =
      new NAttachedArtifact(
        NScope.javaModulePath(),
        NOperatingSystem.any(),
        NArchitecture.any(),
        art0
      );
    final var art1s =
      new NAttachedArtifact(
        NScope.javaModulePath(),
        NOperatingSystem.any(),
        NArchitecture.any(),
        art1
      );
    final var art2s =
      new NAttachedArtifact(
        NScope.javaModulePath(),
        NOperatingSystem.any(),
        NArchitecture.any(),
        art2
      );

    assertEquals(art0s, app.artifact(art0).orElseThrow());
    assertEquals(art1s, app.artifact(art1).orElseThrow());
    assertEquals(art2s, app.artifact(art2).orElseThrow());
    assertEquals(Paths.get(""), app.fileForArtifact(art0));
    assertEquals(Paths.get(""), app.fileForArtifact(art1));
    assertEquals(Paths.get(""), app.fileForArtifact(art2));
    assertEquals(Map.of(), app.artifactMetadata(art0));
    assertEquals(Map.of(), app.artifactMetadata(art1));
    assertEquals(Map.of(), app.artifactMetadata(art2));

    final var ex = assertThrows(NException.class, () -> {
      app.fileForArtifact(new NArtifact(
        new RDottedName("non"),
        new RDottedName("non"),
        Version.of(1,1,1),
        NArtifactType.jar()
      ));
    });

    assertEquals(NErrorCodes.errorArtifactNonexistent(), ex.errorCode());
  }

  /**
   * Metadata works.
   *
   * @throws NException On errors
   */

  @Test
  public void testMetadata0()
    throws NException
  {
    final var name =
      new RDottedName("com.io7m.example");
    final var version =
      Version.of(1, 0, 0);

    final var art0 = new NArtifact(
      new RDottedName("com.io7m.ex0"),
      new RDottedName("com.io7m.ex0"),
      Version.of(1, 0, 0),
      NArtifactType.jar()
    );

    final var app =
      NApplications.create(name, version)
        .artifactAdd(
          Paths.get(""),
          NScope.javaModulePath(),
          NOperatingSystem.any(),
          NArchitecture.any(),
          art0
        )
        .artifactMetadataAdd(art0, "A", "X")
        .artifactMetadataAdd(art0, "B", "Y")
        .artifactMetadataAdd(art0, "C", "Z")
        .build();

    assertEquals(name, app.name());
    assertEquals(version, app.version());

    final var art0s =
      new NAttachedArtifact(
        NScope.javaModulePath(),
        NOperatingSystem.any(),
        NArchitecture.any(),
        art0
      );

    assertEquals(
      Map.ofEntries(
        Map.entry("A", "X"),
        Map.entry("B", "Y"),
        Map.entry("C", "Z")
      ),
      app.artifactMetadata(art0)
    );

    final var ex = assertThrows(NException.class, () -> {
      app.artifactMetadata(new NArtifact(
        new RDottedName("non"),
        new RDottedName("non"),
        Version.of(1,1,1),
        NArtifactType.jar()
      ));
    });

    assertEquals(NErrorCodes.errorArtifactNonexistent(), ex.errorCode());
  }
}
