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

import com.io7m.jade.api.ApplicationDirectories;
import com.io7m.jade.api.ApplicationDirectoryConfiguration;
import com.io7m.lanark.core.RDottedName;
import com.io7m.naranja.archivers.NApplicationArchivers;
import com.io7m.naranja.core.NApplicationBuilderType;
import com.io7m.naranja.core.NApplicationType;
import com.io7m.naranja.core.NApplicationWriterConfiguration;
import com.io7m.naranja.core.NApplicationWriters;
import com.io7m.naranja.core.NApplications;
import com.io7m.naranja.core.NArtifact;
import com.io7m.naranja.core.NArtifactType;
import com.io7m.naranja.core.NException;
import com.io7m.naranja.core.NHashing;
import com.io7m.naranja.core.NRuntimeInventoryType;
import com.io7m.naranja.runtimes.NRuntimeInventories;
import com.io7m.verona.core.VersionException;
import com.io7m.verona.core.VersionParser;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.io7m.naranja.core.NApplicationBuilderType.Executable.EXECUTABLE;
import static com.io7m.naranja.core.NApplicationBuilderType.Executable.NOT_EXECUTABLE;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PACKAGE;

/**
 * The "build" mojo.
 */

@Mojo(
  name = "build",
  defaultPhase = PACKAGE,
  requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
  requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)

public final class NBuildMojo extends AbstractMojo
{
  /**
   * Access to the Maven project.
   */

  @Parameter(
    defaultValue = "${project}",
    required = true,
    readonly = true)
  private MavenProject project;

  /**
   * The current repository system.
   */

  @Component
  private RepositorySystem repositorySystem;

  /**
   * The current repository/network configuration of Maven.
   */

  @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
  private RepositorySystemSession repositorySystemSession;

  /**
   * The project's remote repositories to use for the resolution.
   */

  @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true)
  private List<RemoteRepository> remoteRepositories;

  /**
   * The categorizations for artifacts.
   */

  @Parameter(required = false)
  private List<Categorize> categorizedArtifacts = new ArrayList<>();

  /**
   * The ignore expressions for artifacts.
   */

  @Parameter(required = false)
  private List<Ignore> ignoredArtifacts = new ArrayList<>();

  /**
   * The bundled runtimes.
   */

  @Parameter(required = false)
  private List<Runtime> bundledRuntimes = new ArrayList<>();

  /**
   * The application name.
   */

  @Parameter(
    required = true,
    defaultValue = "${project.artifactId}")
  private String applicationName;

  /**
   * The application version.
   */

  @Parameter(
    required = true,
    defaultValue = "${project.version}")
  private String applicationVersion;

  /**
   * The output directory.
   */

  @Parameter(
    required = true,
    defaultValue = "${project.build.directory}/applications")
  private String outputDirectory;

  /**
   * True if the output should be archived.
   */

  @Parameter(
    required = false,
    defaultValue = "true")
  private boolean archive;

  /**
   * Application metadata.
   */

  @Parameter(required = false)
  private Map<String, String> metadata = new HashMap<>();

  /**
   * Extra bundled files.
   */

  @Parameter(required = false)
  private List<ExtraFile> extraFiles = new ArrayList<>();

  /**
   * The "build" mojo.
   */

  public NBuildMojo()
  {

  }

  private enum ProcessResult
  {
    ARTIFACT_MATCHED,
    ARTIFACT_UNMATCHED,
    ARTIFACT_IGNORED
  }

  @Override
  public void execute()
    throws MojoExecutionException
  {
    try {
      final var inventories =
        new NRuntimeInventories();

      final var configuration =
        ApplicationDirectoryConfiguration.builder()
          .setApplicationName("com.io7m.naranja")
          .setOverridePropertyName("com.io7m.naranja.override")
          .setPortablePropertyName("com.io7m.naranja.portable")
          .build();

      final var directories =
        ApplicationDirectories.get(configuration);

      try (var inventory = inventories.open(directories.cacheDirectory())) {
        final var appBuilder =
          NApplications.create(
            new RDottedName(this.applicationName),
            VersionParser.parseLax(this.applicationVersion)
          );

        for (final var entry : this.metadata.entrySet()) {
          appBuilder.metadataSet(entry.getKey(), entry.getValue());
        }

        this.processFiles(appBuilder);
        this.processArtifacts(appBuilder);
        this.processRuntimes(appBuilder, inventory);

        final var app = appBuilder.build();
        final var wrote = this.writeApplication(app, inventory);
        this.archiveApplication(app, wrote);
      }

    } catch (final Exception e) {
      throw new MojoExecutionException(e);
    }
  }

  private void processFiles(
    final NApplicationBuilderType appBuilder)
  {
    for (final var file : this.extraFiles) {
      appBuilder.fileAdd(
        file.fileOutput(),
        file.fileSource(),
        file.isExecutable() ? EXECUTABLE : NOT_EXECUTABLE
      );
    }
  }

  private void archiveApplication(
    final NApplicationType app,
    final Path wrote)
    throws NException
  {
    if (!this.archive) {
      return;
    }

    final var appName =
      app.name().value();
    final var appVersion =
      app.version();

    final var fileName =
      "%s-%s.tgz".formatted(appName, appVersion.toString());
    final var archivePath =
      wrote.getParent().resolve(fileName);

    final var archivers =
      new NApplicationArchivers();

    final var log = this.getLog();
    try (var archiver = archivers.create(appName + "/", wrote, archivePath)) {
      archiver.execute(progress -> {
        log.info("archive (%s/%s) %s".formatted(
          Long.valueOf(progress.fileIndex()),
          Long.valueOf(progress.fileCount()),
          progress.fileName())
        );
      });
    }
  }

  private void processRuntimes(
    final NApplicationBuilderType appBuilder,
    final NRuntimeInventoryType inventory)
    throws NException, InterruptedException, IOException
  {
    final var log = this.getLog();

    for (final var runtime : this.bundledRuntimes) {
      final var actual =
        inventory.runtimeFind(runtime.id());

      final var download =
        inventory.runtimeDownload(actual);

      log.info("Downloading %s".formatted(actual.source()));
      Thread.sleep(10L);
      while (!download.future().isDone()) {
        log.info("Progress: %s/%s".formatted(
          Long.toUnsignedString(download.sizeReceived()),
          Long.toUnsignedString(download.sizeExpected())
        ));
        Thread.sleep(1_000L);
      }

      if (!Objects.equals(
        actual.architecture(),
        runtime.architecture())) {
        throw new IOException(
          "Unexpected architecture: Expected architecture %s but the downloaded runtime has architecture %s"
            .formatted(
              runtime.architecture().name(),
              actual.architecture().name())
        );
      }

      if (!Objects.equals(
        actual.operatingSystem(),
        runtime.operatingSystem())) {
        throw new IOException(
          "Unexpected architecture: Expected operating system %s but the downloaded runtime has operating system %s"
            .formatted(
              runtime.operatingSystem().name(),
              actual.operatingSystem().name())
        );
      }

      appBuilder.runtimeAdd(actual);

      appBuilder.runtimeMetadataAdd(
        actual,
        "RuntimeArchitecture",
        actual.architecture().name());
      appBuilder.runtimeMetadataAdd(
        actual,
        "RuntimeOperatingSystem",
        actual.operatingSystem().name());
      appBuilder.runtimeMetadataAdd(
        actual,
        "RuntimeDistribution",
        actual.distribution().name()
      );
      appBuilder.runtimeMetadataAdd(
        actual,
        "RuntimeVersion",
        actual.versionText()
      );
      appBuilder.runtimeMetadataAdd(
        actual,
        "RuntimeSource",
        actual.source().toString()
      );

      final var modules = runtime.modules();
      if (!modules.isEmpty()) {
        appBuilder.runtimeSetMinimized(actual, modules);
      }
    }
  }

  private Path writeApplication(
    final NApplicationType app,
    final NRuntimeInventoryType inventory)
    throws NException
  {
    final var writers =
      new NApplicationWriters();
    final var configuration =
      new NApplicationWriterConfiguration(
        Paths.get(this.outputDirectory).toAbsolutePath(),
        inventory,
        app
      );

    try (var writer = writers.create(configuration)) {
      writer.execute();
      return writer.outputPath();
    }
  }

  private void processArtifacts(
    final NApplicationBuilderType appBuilder)
    throws
    MojoExecutionException,
    VersionException,
    NException,
    NoSuchAlgorithmException,
    IOException
  {
    final var log =
      this.getLog();

    final var artifacts =
      this.collectArtifacts();

    final var unmatched = new HashSet<Artifact>();
    for (final var artifact : artifacts) {
      switch (this.processArtifact(appBuilder, artifact)) {
        case ARTIFACT_UNMATCHED -> {
          unmatched.add(artifact);
        }
        case ARTIFACT_MATCHED -> {

        }
        case ARTIFACT_IGNORED -> {

        }
      }
    }

    if (!unmatched.isEmpty()) {
      log.error("At least one artifact was unmatched by any categorizations.");
      for (final var artifact : unmatched) {
        log.error("Unmatched: %s".formatted(artifactId(artifact)));
      }
      throw new MojoExecutionException(
        "One or more artifacts were not categorized.");
    }
  }

  private ProcessResult processArtifact(
    final NApplicationBuilderType appBuilder,
    final Artifact artifact)
    throws NException, VersionException, NoSuchAlgorithmException, IOException
  {
    final var log = this.getLog();

    for (final var ignore : this.ignoredArtifacts) {
      if (ignore.matches(artifact)) {
        log.info("Artifact %s is ignored".formatted(artifactId(artifact)));
        return ProcessResult.ARTIFACT_IGNORED;
      }
    }

    final var file = artifact.getFile();
    if (file == null) {
      log.error("Artifact %s has no file".formatted(artifactId(artifact)));
      return ProcessResult.ARTIFACT_UNMATCHED;
    }

    for (final var categorization : this.categorizedArtifacts) {
      if (categorization.matches(artifact)) {
        log.info(
          "Artifact %s categorized as scope %s, operating system %s, arch %s"
            .formatted(
              artifactId(artifact),
              categorization.scope().name(),
              categorization.operatingSystem().name(),
              categorization.architecture().name()
            ));

        final var artifactInfo =
          new NArtifact(
            new RDottedName(artifact.getGroupId()),
            new RDottedName(artifact.getArtifactId()),
            VersionParser.parseLax(artifact.getVersion()),
            new NArtifactType(artifact.getExtension())
          );

        appBuilder.artifactAdd(
          file.toPath(),
          categorization.scope(),
          categorization.operatingSystem(),
          categorization.architecture(),
          artifactInfo
        );

        appBuilder.artifactMetadataAdd(
          artifactInfo,
          "HashAlgorithm",
          "SHA-256");
        appBuilder.artifactMetadataAdd(
          artifactInfo,
          "HashValue",
          NHashing.sha256Of(artifact.getFile()));
        appBuilder.artifactMetadataAdd(
          artifactInfo,
          "MavenGroup",
          artifact.getGroupId()
        );
        appBuilder.artifactMetadataAdd(
          artifactInfo,
          "MavenArtifact",
          artifact.getArtifactId()
        );
        appBuilder.artifactMetadataAdd(
          artifactInfo,
          "MavenClassifier",
          artifact.getClassifier()
        );
        appBuilder.artifactMetadataAdd(
          artifactInfo,
          "MavenVersion",
          artifact.getVersion()
        );
        appBuilder.artifactMetadataAdd(
          artifactInfo,
          "MavenType",
          artifact.getExtension()
        );

        return ProcessResult.ARTIFACT_MATCHED;
      }
    }

    return ProcessResult.ARTIFACT_UNMATCHED;
  }

  private static String artifactId(
    final Artifact artifact)
  {
    return String.format(
      "%s:%s:%s:%s:%s",
      artifact.getGroupId(),
      artifact.getArtifactId(),
      artifact.getVersion(),
      artifact.getClassifier(),
      artifact.getExtension()
    );
  }

  private List<Artifact> collectArtifacts()
    throws MojoExecutionException
  {
    try {
      final var artifact =
        new DefaultArtifact(this.project.getId());

      final CollectRequest collectRequest = new CollectRequest();
      collectRequest.setRoot(new Dependency(artifact, "compile"));
      collectRequest.setRepositories(this.remoteRepositories);

      final DependencyRequest dependencyRequest =
        new DependencyRequest(collectRequest, (dependencyNode, list) -> true);

      final List<ArtifactResult> artifactResults =
        this.repositorySystem.resolveDependencies(
            this.repositorySystemSession,
            dependencyRequest)
          .getArtifactResults();

      return artifactResults
        .stream()
        .map(ArtifactResult::getArtifact)
        .toList();
    } catch (final DependencyResolutionException e) {
      throw new MojoExecutionException(e);
    }
  }
}
