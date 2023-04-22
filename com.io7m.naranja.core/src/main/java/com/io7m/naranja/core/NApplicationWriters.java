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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import static com.io7m.naranja.core.NErrorCodes.errorIo;

/**
 * The default factory of application writers.
 */

public final class NApplicationWriters
  implements NApplicationWriterFactoryType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(NApplicationWriters.class);

  /**
   * The default factory of application writers.
   */

  public NApplicationWriters()
  {

  }

  @Override
  public NApplicationWriterType create(
    final NApplicationWriterConfiguration configuration)
  {
    Objects.requireNonNull(configuration, "configuration");

    return new NApplicationWriter(configuration);
  }

  private static final class NApplicationWriter
    implements NApplicationWriterType
  {
    private final NApplicationWriterConfiguration configuration;
    private final Path base;
    private final Path appDirectory;

    private NApplicationWriter(
      final NApplicationWriterConfiguration inConfiguration)
    {
      this.configuration =
        Objects.requireNonNull(inConfiguration, "configuration");

      this.base =
        this.configuration.outputDirectory();
      this.appDirectory =
        this.base.resolve(this.configuration.application().name().value());
    }

    @Override
    public void execute()
      throws NException
    {
      try {
        Files.createDirectories(this.appDirectory);

        final var libDirectory =
          this.appDirectory.resolve("lib");
        final var runtimeDirectory =
          this.appDirectory.resolve("runtime");
        final var confDirectory =
          this.appDirectory.resolve("conf");

        Files.createDirectories(libDirectory);
        Files.createDirectories(runtimeDirectory);
        Files.createDirectories(confDirectory);

        this.copyLibraries(libDirectory);
        this.unpackRuntimes(runtimeDirectory);
        this.copyExtras();

        this.generateManifest(libDirectory, runtimeDirectory);
      } catch (final Exception e) {
        throw new NException(
          Objects.requireNonNullElse(
            e.getMessage(),
            e.getClass().getSimpleName()),
          errorIo()
        );
      }
    }

    private void copyExtras()
      throws IOException
    {
      final var files =
        new TreeMap<>(this.configuration.application().files());

      for (final var entry : files.entrySet()) {
        final var file =
          entry.getValue();
        final var targetFile =
          this.appDirectory.resolve(file.name());

        LOG.info("mkdir {}", targetFile.getParent());
        Files.createDirectories(targetFile.getParent());

        LOG.info("copy {} -> {}", file.source(), targetFile);
        Files.copy(
          file.source(),
          targetFile,
          StandardCopyOption.REPLACE_EXISTING);

        switch (file.executable()) {
          case EXECUTABLE -> {
            LOG.info("executable {}", targetFile);

            final var existing =
              new HashSet<>(Files.getPosixFilePermissions(targetFile));
            existing.add(PosixFilePermission.OWNER_EXECUTE);
            existing.add(PosixFilePermission.GROUP_EXECUTE);
            existing.add(PosixFilePermission.OTHERS_EXECUTE);
            Files.setPosixFilePermissions(targetFile, existing);
          }
          case NOT_EXECUTABLE -> {

          }
        }
      }
    }

    private static final String NS =
      "urn:com.io7m.naranja:1:0";
    private static final String NS_DUBLIN =
      "http://purl.org/dc/elements/1.1/";

    private void generateManifest(
      final Path libDirectory,
      final Path runtimeDirectory)
      throws Exception
    {
      final var application =
        this.configuration.application();
      final var appName =
        application.name();
      final var appVersion =
        application.version();

      final var outputFile =
        this.appDirectory.resolve("application.xml");

      final var documents =
        DocumentBuilderFactory.newDefaultNSInstance();
      final var documentBuilder =
        documents.newDocumentBuilder();
      final var document =
        documentBuilder.newDocument();

      final var eApp =
        document.createElementNS(NS, "Application");
      document.appendChild(eApp);
      eApp.setAttribute("xmlns:dc", NS_DUBLIN);
      eApp.setAttribute("Name", appName.value());
      eApp.setAttribute("Version", appVersion.toString());

      final var eMeta =
        document.createElementNS(NS, "Metadata");
      eApp.appendChild(eMeta);

      {
        final var e =
          document.createElementNS(NS_DUBLIN, "dc:identifier");
        eMeta.appendChild(e);
        e.setTextContent(String.format("%s:%s", appName.value(), appVersion));
      }

      {
        final var sorted =
          new TreeMap<>(application.applicationMetadata());

        for (final var entry : sorted.entrySet()) {
          final var e =
            document.createElementNS(NS, "MetaProperty");
          eMeta.appendChild(e);
          e.setAttribute("Name", entry.getKey());
          e.setTextContent(entry.getValue());
        }
      }

      this.generateFiles(libDirectory, runtimeDirectory, document, eApp);
      this.generateRuntimes(document, eApp);

      final var tr = TransformerFactory.newInstance().newTransformer();
      tr.setOutputProperty(OutputKeys.INDENT, "yes");
      tr.setOutputProperty(OutputKeys.METHOD, "xml");
      tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

      try (var stream = Files.newOutputStream(outputFile)) {
        tr.transform(
          new DOMSource(document),
          new StreamResult(stream)
        );
      }
    }

    private void generateRuntimes(
      final Document document,
      final Element eApp)
    {
      final var eRuntimes =
        document.createElementNS(NS, "Runtimes");
      eApp.appendChild(eRuntimes);

      final var rtElements =
        this.generateManifestRuntimeElements(document);
      for (final var element : rtElements) {
        eRuntimes.appendChild(element);
      }
    }

    private void generateFiles(
      final Path libDirectory,
      final Path runtimeDirectory,
      final Document document,
      final Element eApp)
      throws Exception
    {
      final var eFiles =
        document.createElementNS(NS, "Files");
      eApp.appendChild(eFiles);

      final var libElements =
        this.generateManifestLibraryElements(document, libDirectory);
      for (final var element : libElements) {
        eFiles.appendChild(element);
      }

      final var rtFileElements =
        this.generateManifestRuntimeFileElements(document, runtimeDirectory);
      for (final var element : rtFileElements) {
        eFiles.appendChild(element);
      }

      final var extraFileElements =
        this.generateManifestExtraFileElements(document);
      for (final var element : extraFileElements) {
        eFiles.appendChild(element);
      }
    }

    private List<Element> generateManifestExtraFileElements(
      final Document document)
      throws Exception
    {
      final var files =
        new TreeMap<>(this.configuration.application().files());

      final var results = new ArrayList<Element>();
      for (final var entry : files.entrySet()) {
        final var file =
          entry.getValue();
        final var targetFile =
          this.appDirectory.resolve(file.name());

        final var eFile =
          document.createElementNS(NS, "File");
        eFile.setAttribute("Name", targetFile.toString());

        final var meta = new TreeMap<String, String>();
        meta.put("HashAlgorithm", "SHA-256");
        meta.put("HashValue", NHashing.sha256Of(targetFile));
        generateMetadataElement(document, meta, eFile);
        results.add(eFile);
      }

      return results;
    }

    private List<Element> generateManifestRuntimeFileElements(
      final Document document,
      final Path runtimeDirectory)
      throws IOException, NoSuchAlgorithmException
    {
      final List<Path> files;
      try (var fileStream = Files.walk(runtimeDirectory)) {
        files = fileStream.filter(Files::isRegularFile)
          .map(Path::toAbsolutePath)
          .map(Path::normalize)
          .sorted()
          .toList();
      }

      final var elements = new ArrayList<Element>();
      for (final var file : files) {
        final var relative =
          runtimeDirectory.getParent()
            .relativize(file);

        final var eFile =
          document.createElementNS(NS, "File");
        eFile.setAttribute("Name", relative.toString());

        final var meta = new TreeMap<String, String>();
        meta.put("HashAlgorithm", "SHA-256");
        meta.put("HashValue", NHashing.sha256Of(file));
        generateMetadataElement(document, meta, eFile);
        elements.add(eFile);
      }
      return elements;
    }

    private List<Element> generateManifestRuntimeElements(
      final Document document)
    {
      final var results = new ArrayList<Element>();

      final var runtimes =
        this.configuration.application()
          .runtimes()
          .stream()
          .sorted(Comparator.comparing(NRuntime::id))
          .toList();

      for (final var runtime : runtimes) {
        results.addAll(
          this.generateManifestRuntimeElement(document, runtime)
        );
      }
      return results;
    }

    private List<Element> generateManifestRuntimeElement(
      final Document document,
      final NRuntime runtime)
    {
      final var eRT =
        document.createElementNS(NS, "Runtime");
      eRT.setAttribute("ID", runtime.id());

      final var meta =
        new TreeMap<>(
          this.configuration.application()
            .runtimeMetadataOptional(runtime)
            .orElse(Map.of())
        );

      generateMetadataElement(document, meta, eRT);
      return List.of(eRT);
    }

    private List<Element> generateManifestLibraryElements(
      final Document document,
      final Path libDirectory)
    {
      final var results = new ArrayList<Element>();

      final var artifacts =
        this.configuration.application()
          .artifacts()
          .values()
          .stream()
          .sorted()
          .toList();

      for (final var artifact : artifacts) {
        results.add(
          this.generateManifestArtifactElement(document, libDirectory, artifact)
        );
      }
      return results;
    }

    private Element generateManifestArtifactElement(
      final Document document,
      final Path libDirectory,
      final NAttachedArtifact artifact)
    {
      final var scopeDirectory =
        libDirectory.resolve(artifact.scope().name());
      final var osDirectory =
        scopeDirectory.resolve(artifact.operatingSystem().name());
      final var archDirectory =
        osDirectory.resolve(artifact.architecture().name());

      final var rawArtifact =
        artifact.artifact();
      final var outFile =
        archDirectory.resolve("%s-%s.%s".formatted(
          rawArtifact.name().value(),
          rawArtifact.version(),
          rawArtifact.type().name()
        ));

      final var relative =
        libDirectory.getParent()
          .relativize(outFile);

      final var meta =
        new TreeMap<>(
          this.configuration.application()
            .artifactMetadataOptional(artifact.artifact())
            .orElse(Map.of())
        );

      final var eFile =
        document.createElementNS(NS, "File");
      eFile.setAttribute("Name", relative.toString());
      generateMetadataElement(document, meta, eFile);
      return eFile;
    }

    private static void generateMetadataElement(
      final Document document,
      final TreeMap<String, String> meta,
      final Element parent)
    {
      if (!meta.isEmpty()) {
        final var eMeta =
          document.createElementNS(NS, "Metadata");
        for (final var entry : meta.entrySet()) {
          final var e =
            document.createElementNS(NS, "MetaProperty");
          e.setAttribute("Name", entry.getKey());
          e.setTextContent(entry.getValue());
          eMeta.appendChild(e);
        }
        parent.appendChild(eMeta);
      }
    }

    private void unpackRuntimes(
      final Path runtimeDirectory)
      throws NException
    {
      final var inventory =
        this.configuration.inventory();
      final var application =
        this.configuration.application();
      final var runtimes =
        application.runtimes();

      for (final var runtime : runtimes) {
        final var osDirectory =
          runtimeDirectory.resolve(runtime.operatingSystem().name());
        final var archDirectory =
          osDirectory.resolve(runtime.architecture().name());

        final var modulesOpt =
          application.runtimeMinimizedModules(runtime);

        if (modulesOpt.isPresent()) {
          final var modules = modulesOpt.get();
          inventory.runtimeUnpackAndRelink(runtime, archDirectory, modules);
        } else {
          inventory.runtimeUnpack(runtime, archDirectory);
        }
      }
    }

    private void copyLibraries(
      final Path libDirectory)
      throws IOException, NException
    {
      final var app = this.configuration.application();
      for (final var artifact : app.artifacts().values()) {
        final var scopeDirectory =
          libDirectory.resolve(artifact.scope().name());
        final var osDirectory =
          scopeDirectory.resolve(artifact.operatingSystem().name());
        final var archDirectory =
          osDirectory.resolve(artifact.architecture().name());

        Files.createDirectories(archDirectory);

        final var rawArtifact =
          artifact.artifact();
        final var file =
          app.fileForArtifact(rawArtifact);

        final var outFile =
          archDirectory.resolve("%s-%s.%s".formatted(
            rawArtifact.name().value(),
            rawArtifact.version(),
            rawArtifact.type().name()
          ));

        Files.copy(file, outFile, StandardCopyOption.REPLACE_EXISTING);
      }
    }

    @Override
    public void close()
    {

    }

    @Override
    public Path outputPath()
    {
      return this.appDirectory;
    }
  }
}
