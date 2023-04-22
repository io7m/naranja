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

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.io7m.naranja.core.NErrorCodes.errorArtifactDuplicate;
import static com.io7m.naranja.core.NErrorCodes.errorArtifactNonexistent;

/**
 * Application functions.
 */

public final class NApplications
{
  private NApplications()
  {

  }

  /**
   * Create a new application builder.
   *
   * @param name    The name
   * @param version The version
   *
   * @return The builder
   */

  public static NApplicationBuilderType create(
    final RDottedName name,
    final Version version)
  {
    return new Builder(name, version);
  }

  private static final class Builder implements NApplicationBuilderType
  {
    private final RDottedName name;
    private final Version version;
    private final HashMap<NArtifact, NAttachedArtifact> artifacts;
    private final HashMap<NArtifact, Path> artifactFiles;
    private final HashMap<NArtifact, Map<String, String>> artifactMetadata;
    private final HashSet<NRuntime> runtimes;
    private final HashMap<String, Set<String>> runtimesModules;
    private final HashMap<NRuntime, Map<String, String>> runtimeMetadata;
    private final HashMap<String, String> appMetadata;
    private final HashMap<String, NFile> extraFiles;

    Builder(
      final RDottedName inName,
      final Version inVersion)
    {
      this.name =
        Objects.requireNonNull(inName, "name");
      this.version =
        Objects.requireNonNull(inVersion, "version");
      this.artifacts =
        new HashMap<>();
      this.artifactFiles =
        new HashMap<>();
      this.artifactMetadata =
        new HashMap<>();
      this.runtimes =
        new HashSet<>();
      this.runtimesModules =
        new HashMap<>();
      this.runtimeMetadata =
        new HashMap<>();
      this.appMetadata =
        new HashMap<>();
      this.extraFiles =
        new HashMap<>();
    }

    @Override
    public NApplicationBuilderType metadataSet(
      final String metaName,
      final String metaValue)
    {
      Objects.requireNonNull(metaName, "name");
      Objects.requireNonNull(metaValue, "value");

      this.appMetadata.put(metaName, metaValue);
      return this;
    }

    @Override
    public NApplicationBuilderType fileAdd(
      final String fileName,
      final Path input,
      final Executable executable)
    {
      Objects.requireNonNull(fileName, "fileName");
      Objects.requireNonNull(input, "input");
      Objects.requireNonNull(executable, "executable");

      this.extraFiles.put(fileName, new NFile(fileName, input, executable));
      return this;
    }

    @Override
    public NApplicationBuilderType artifactAdd(
      final Path file,
      final NScope scope,
      final NOperatingSystem platform,
      final NArchitecture architecture,
      final NArtifact artifact)
      throws NException
    {
      Objects.requireNonNull(file, "file");
      Objects.requireNonNull(scope, "scope");
      Objects.requireNonNull(platform, "platform");
      Objects.requireNonNull(artifact, "artifact");
      Objects.requireNonNull(architecture, "architecture");

      final var existing = this.artifacts.get(artifact);
      if (existing != null) {
        throw new NException(
          "Artifact %s:%s already exists (scope %s, os %s, arch %s)"
            .formatted(
              artifact.name(),
              artifact.version(),
              existing.scope().name(),
              existing.operatingSystem().name(),
              existing.architecture().name()
            ),
          errorArtifactDuplicate()
        );
      }

      final var scoped =
        new NAttachedArtifact(scope, platform, architecture, artifact);
      this.artifacts.put(artifact, scoped);
      this.artifactFiles.put(artifact, file);
      this.artifactMetadata.put(artifact, new HashMap<>());
      return this;
    }

    @Override
    public NApplicationBuilderType artifactMetadataAdd(
      final NArtifact artifact,
      final String metaName,
      final String metaValue)
      throws NException
    {
      Objects.requireNonNull(artifact, "artifact");
      Objects.requireNonNull(metaName, "metaName");
      Objects.requireNonNull(metaValue, "metaValue");

      if (!this.artifacts.containsKey(artifact)) {
        throw new NException(
          "Nonexistent artifact %s:%s"
            .formatted(artifact.name(), artifact.version()),
          errorArtifactNonexistent()
        );
      }

      final var existing = this.artifactMetadata.get(artifact);
      this.artifactMetadata.put(artifact, existing);
      existing.put(metaName, metaValue);
      return this;
    }

    @Override
    public NApplicationBuilderType runtimeAdd(
      final NRuntime runtime)
      throws NException
    {
      Objects.requireNonNull(runtime, "runtime");

      for (final var existing : this.runtimes) {
        final var os =
          runtime.operatingSystem();
        final var arch =
          runtime.architecture();

        if (Objects.equals(os, existing.operatingSystem())
            && Objects.equals(arch, existing.architecture())) {
          throw new NException(
            "A runtime is already assigned for OS %s, Architecture %s"
              .formatted(os.name(), arch.name()),
            errorArtifactDuplicate()
          );
        }
      }

      this.runtimes.add(runtime);
      this.runtimeMetadata.put(runtime, new HashMap<>());
      return this;
    }

    @Override
    public NApplicationBuilderType runtimeMetadataAdd(
      final NRuntime runtime,
      final String metaName,
      final String metaValue)
      throws NException
    {
      Objects.requireNonNull(runtime, "artifact");
      Objects.requireNonNull(metaName, "metaName");
      Objects.requireNonNull(metaValue, "metaValue");

      if (!this.runtimes.contains(runtime)) {
        throw new NException(
          "Nonexistent runtime %s".formatted(runtime.id()),
          errorArtifactNonexistent()
        );
      }

      final var existing =
        this.runtimeMetadata.get(runtime);
      this.runtimeMetadata.put(runtime, existing);
      existing.put(metaName, metaValue);
      return this;
    }

    @Override
    public NApplicationBuilderType runtimeSetMinimized(
      final NRuntime runtime,
      final Set<String> modules)
      throws NException
    {
      Objects.requireNonNull(runtime, "runtime");
      Objects.requireNonNull(modules, "modules");

      if (!this.runtimes.contains(runtime)) {
        throw new NException(
          "The given runtime has not been added to the application.",
          errorArtifactNonexistent()
        );
      }

      this.runtimesModules.put(runtime.id(), Set.copyOf(modules));
      return this;
    }

    @Override
    public NApplicationType build()
    {

      final var metaCopy =
        this.artifactMetadata.entrySet()
          .stream()
          .map(e -> Map.entry(e.getKey(), Map.copyOf(e.getValue())))
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

      final var runtimeMetaCopy =
        this.runtimeMetadata.entrySet()
          .stream()
          .map(e -> Map.entry(e.getKey(), Map.copyOf(e.getValue())))
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

      return new NApplication(
        this.name,
        this.version,
        Map.copyOf(this.extraFiles),
        Map.copyOf(this.appMetadata),
        Map.copyOf(runtimeMetaCopy),
        Map.copyOf(this.runtimesModules),
        Set.copyOf(this.runtimes),
        Map.copyOf(this.artifacts),
        Map.copyOf(this.artifactFiles),
        Map.copyOf(metaCopy)
      );
    }
  }

  private record NApplication(
    RDottedName name,
    Version version,
    Map<String, NFile> files,
    Map<String, String> appMetadata,
    Map<NRuntime, Map<String, String>> runtimeMedata,
    Map<String, Set<String>> runtimeMinimizeModules,
    Set<NRuntime> runtimes,
    Map<NArtifact, NAttachedArtifact> artifacts,
    Map<NArtifact, Path> artifactFiles,
    Map<NArtifact, Map<String, String>> metadata)
    implements NApplicationType
  {
    private NApplication
    {
      Objects.requireNonNull(appMetadata, "appMetadata");
      Objects.requireNonNull(runtimes, "runtimes");
      Objects.requireNonNull(runtimeMedata, "runtimeMedata");
      Objects.requireNonNull(runtimeMinimizeModules, "runtimeMinimizeModules");
      Objects.requireNonNull(name, "name");
      Objects.requireNonNull(version, "version");
      Objects.requireNonNull(artifacts, "artifacts");
      Objects.requireNonNull(artifactFiles, "artifactFiles");
      Objects.requireNonNull(metadata, "metadata");
    }

    @Override
    public Map<String, String> applicationMetadata()
    {
      return this.appMetadata;
    }

    @Override
    public Optional<Set<String>> runtimeMinimizedModules(
      final NRuntime runtime)
    {
      return Optional.ofNullable(
        this.runtimeMinimizeModules.get(runtime.id())
      );
    }

    @Override
    public Path fileForArtifact(
      final NArtifact artifact)
      throws NException
    {
      Objects.requireNonNull(artifact, "artifact");

      return Optional.ofNullable(this.artifactFiles.get(artifact))
        .orElseThrow(() -> {
          return new NException(
            "Nonexistent artifact %s:%s"
              .formatted(artifact.name(), artifact.version()),
            errorArtifactNonexistent()
          );
        });
    }

    @Override
    public Optional<Map<String, String>> artifactMetadataOptional(
      final NArtifact artifact)
    {
      Objects.requireNonNull(artifact, "artifact");
      return Optional.ofNullable(this.metadata.get(artifact));
    }

    @Override
    public Map<String, String> artifactMetadata(
      final NArtifact artifact)
      throws NException
    {
      Objects.requireNonNull(artifact, "artifact");

      return Optional.ofNullable(this.metadata.get(artifact))
        .orElseThrow(() -> {
          return new NException(
            "Nonexistent artifact %s:%s"
              .formatted(artifact.name(), artifact.version()),
            errorArtifactNonexistent()
          );
        });
    }

    @Override
    public Set<NAttachedArtifact> artifactsFor(
      final NScope scope,
      final NOperatingSystem operatingSystem,
      final NArchitecture architecture)
    {
      return this.artifacts.values()
        .stream()
        .filter(a -> {
          return Objects.equals(a.scope(), scope)
                 && Objects.equals(a.operatingSystem(), operatingSystem)
                 && Objects.equals(a.architecture(), architecture);
        }).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Optional<Map<String, String>> runtimeMetadataOptional(
      final NRuntime runtime)
    {
      Objects.requireNonNull(runtime, "runtime");
      return Optional.ofNullable(this.runtimeMedata.get(runtime));
    }

    @Override
    public Map<String, String> runtimeMetadata(
      final NRuntime runtime)
      throws NException
    {
      Objects.requireNonNull(runtime, "runtime");

      return Optional.ofNullable(this.runtimeMedata.get(runtime))
        .orElseThrow(() -> {
          return new NException(
            "Nonexistent runtime %s"
              .formatted(runtime.id()),
            errorArtifactNonexistent()
          );
        });
    }
  }
}
