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

package com.io7m.naranja.runtimes;

import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.naranja.core.NArchitecture;
import com.io7m.naranja.core.NException;
import com.io7m.naranja.core.NOperatingSystem;
import com.io7m.naranja.core.NRuntime;
import com.io7m.naranja.core.NRuntimeArchiveType;
import com.io7m.naranja.core.NRuntimeDistribution;
import com.io7m.naranja.core.NRuntimeDownloadType;
import com.io7m.naranja.core.NRuntimeInventoryFactoryType;
import com.io7m.naranja.core.NRuntimeInventoryType;
import eu.hansolo.jdktools.Architecture;
import eu.hansolo.jdktools.ArchiveType;
import eu.hansolo.jdktools.Latest;
import eu.hansolo.jdktools.OperatingSystem;
import eu.hansolo.jdktools.PackageType;
import eu.hansolo.jdktools.ReleaseStatus;
import eu.hansolo.jdktools.TermOfSupport;
import eu.hansolo.jdktools.versioning.VersionNumber;
import io.foojay.api.discoclient.DiscoClient;
import io.foojay.api.discoclient.PropertyManager;
import io.foojay.api.discoclient.pkg.Pkg;
import io.foojay.api.discoclient.util.Constants;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

import static com.io7m.naranja.core.NErrorCodes.errorIo;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.Objects.requireNonNullElse;

/**
 * The default runtime inventory.
 */

public final class NRuntimeInventories
  implements NRuntimeInventoryFactoryType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(NRuntimeInventories.class);

  /**
   * The default runtime inventory.
   */

  public NRuntimeInventories()
  {

  }

  /**
   * Open an inventory with the API URI.
   *
   * @param baseDirectory The base directory
   * @param baseURI       The base URI for the client
   *
   * @return An inventory
   */

  public NRuntimeInventoryType openWithURI(
    final Path baseDirectory,
    final URI baseURI)
  {
    PropertyManager.INSTANCE.set(
      Constants.PROPERTY_KEY_DISCO_URL,
      baseURI.toString()
    );
    final var client = new DiscoClient();
    return new Inventory(baseDirectory, client);
  }

  @Override
  public NRuntimeInventoryType open(
    final Path baseDirectory)
  {
    return new Inventory(baseDirectory, new DiscoClient());
  }

  private record RuntimePaths(
    Path rtFile,
    Path rtFileTmp,
    Path rtiFile
  )
  {
    static RuntimePaths create(
      final Path baseDirectory,
      final NRuntime runtime)
    {
      final var outputRT =
        baseDirectory.resolve(runtime.id() + ".rt");
      final var outputRTI =
        baseDirectory.resolve(runtime.id() + ".rti");
      final var outputRTTmp =
        baseDirectory.resolve(runtime.id() + ".rt.tmp");

      return new RuntimePaths(
        outputRT,
        outputRTTmp,
        outputRTI
      );
    }
  }

  private static final class Inventory implements NRuntimeInventoryType
  {
    private final Path baseDirectory;
    private final DiscoClient client;
    private final Path runtimes;

    Inventory(
      final Path inBaseDirectory,
      final DiscoClient inClient)
    {
      this.baseDirectory =
        Objects.requireNonNull(inBaseDirectory, "baseDirectory");
      this.client =
        Objects.requireNonNull(inClient, "client");
      this.runtimes =
        this.baseDirectory.resolve("runtimes");
    }

    @Override
    public List<NRuntime> runtimesAvailableRemotely(
      final Collection<NOperatingSystem> operatingSystems,
      final Collection<NArchitecture> architectures,
      final Collection<NRuntimeDistribution> distributions,
      final int jdkVersion)
      throws NException
    {
      Objects.requireNonNull(operatingSystems, "operatingSystems");
      Objects.requireNonNull(architectures, "architectures");
      Objects.requireNonNull(distributions, "distributions");

      try {
        final var discoArches =
          architectures.stream()
            .map(a -> Architecture.fromText(a.name()))
            .collect(Collectors.toUnmodifiableSet());

        final var discoOperatingSystems =
          operatingSystems.stream()
            .map(a -> OperatingSystem.fromText(a.name()))
            .collect(Collectors.toUnmodifiableSet());

        final var packages = new ArrayList<Pkg>();
        for (final var os : discoOperatingSystems) {
          for (final var arch : discoArches) {
            packages.addAll(
              findRuntimes(
                this.client,
                arch,
                os,
                jdkVersion,
                name -> {
                  for (final var distribution : distributions) {
                    if (name.equalsIgnoreCase(distribution.name())) {
                      return true;
                    }
                  }
                  return false;
                }
              )
            );
          }
        }

        return packages.stream()
          .map(pack -> {
            try {
              return this.pkgToRuntime(pack);
            } catch (final IOException e) {
              throw new UncheckedIOException(e);
            }
          })
          .toList();

      } catch (final Exception e) {
        throw new NException(
          requireNonNullElse(e.getMessage(), e.getClass().getSimpleName()),
          e,
          errorIo()
        );
      }
    }

    private NRuntime pkgToRuntime(
      final Pkg pack)
      throws IOException
    {
      return new NRuntime(
        pack.getId(),
        new NRuntimeDistribution(pack.getDistributionName().toLowerCase()),
        new NOperatingSystem(pack.getOperatingSystem().name().toLowerCase()),
        new NArchitecture(pack.getArchitecture().name().toLowerCase()),
        pkgArchiveTypeToArchiveType(pack.getArchiveType()),
        URI.create(this.client.getPkgDirectDownloadUri(pack.getId())),
        pack.getMajorVersion().getAsInt(),
        pack.getJavaVersion().toString(),
        pack.getSize()
      );
    }

    private static NRuntimeArchiveType pkgArchiveTypeToArchiveType(
      final ArchiveType archiveType)
      throws IOException
    {
      return switch (archiveType) {
        case ZIP -> NRuntimeArchiveType.RUNTIME_ARCHIVE_ZIP;
        case TAR_GZ -> NRuntimeArchiveType.RUNTIME_ARCHIVE_TAR_GZ;
        default -> {
          throw new IOException("Unsupported archive type: " + archiveType);
        }
      };
    }

    @Override
    public List<NRuntime> runtimesAvailableLocally()
      throws NException
    {
      try {
        final List<Path> files;

        Files.createDirectories(this.runtimes);

        try (var stream = Files.list(this.runtimes)) {
          files = stream.filter(p -> p.getFileName().toString().endsWith(".rti"))
            .sorted()
            .toList();
        }

        final var localRuntimes = new ArrayList<NRuntime>();
        for (final var file : files) {
          try {
            localRuntimes.add(runtimeLoad(file));
          } catch (final Exception e) {
            Files.deleteIfExists(file);
          }
        }

        return List.copyOf(localRuntimes);
      } catch (final Exception e) {
        throw new NException(
          requireNonNullElse(e.getMessage(), e.getClass().getSimpleName()),
          e,
          errorIo()
        );
      }
    }

    @Override
    public NRuntimeDownloadType runtimeDownload(
      final NRuntime runtime)
    {
      Objects.requireNonNull(runtime, "runtime");

      final var paths =
        RuntimePaths.create(this.runtimes, runtime);

      final var future = new CompletableFuture<Path>();
      final var download = new Download(runtime, future, paths);
      final var thread = new Thread(download);
      thread.setName("com.io7m.naranja.runtimes.download." + thread.getId());
      thread.start();
      return download;
    }

    @Override
    public void runtimeUnpack(
      final NRuntime runtime,
      final Path output)
      throws NException
    {
      Objects.requireNonNull(runtime, "runtime");
      Objects.requireNonNull(output, "output");

      final var paths =
        RuntimePaths.create(this.runtimes, runtime);

      try {
        Files.createDirectories(paths.rtFileTmp.getParent());

        try (var file =
               FileChannel.open(paths.rtFileTmp, CREATE, WRITE)) {
          try (var lock = file.lock()) {
            switch (runtime.archiveType()) {
              case RUNTIME_ARCHIVE_TAR_GZ ->
                runtimeUnpackTarGZ(paths.rtFile, output);
              case RUNTIME_ARCHIVE_ZIP ->
                runtimeUnpackZip(paths.rtFile, output);
            }
          }
        }
      } catch (final IOException e) {
        throw new NException(
          requireNonNullElse(e.getMessage(), e.getClass().getSimpleName()),
          e,
          errorIo()
        );
      }
    }

    @Override
    public void runtimeUnpackAndRelink(
      final NRuntime runtime,
      final Path output,
      final Set<String> modules)
      throws NException
    {
      try {
        final var temporaryDirectory =
          Files.createTempDirectory("naranja");

        this.runtimeUnpack(runtime, temporaryDirectory);

        final var jlink =
          ToolProvider.findFirst("jlink")
            .orElseThrow(() -> new NException(
              "No jlink tool is available.",
              errorIo()));

        final var moduleDirectory =
          temporaryDirectory.resolve("jmods")
            .toAbsolutePath()
            .toString();

        final var outputDirectory =
          output.toAbsolutePath()
            .toString();

        final var args =
          new String[]{
            "-p",
            moduleDirectory,
            "--add-modules",
            String.join(",", modules),
            "--output",
            outputDirectory,
          };

        final var r = jlink.run(System.out, System.err, args);
        if (r != 0) {
          throw new NException("jlink tool failed", errorIo());
        }

        try (var walk = Files.walk(temporaryDirectory)) {
          walk.sorted(Comparator.reverseOrder())
            .filter(Files::isRegularFile)
            .forEach(file -> {
              try {
                Files.deleteIfExists(file);
              } catch (final IOException e) {
                LOG.error("delete: {}: ", file, e);
              }
            });
        }

        try (var walk = Files.walk(temporaryDirectory)) {
          walk.sorted(Comparator.reverseOrder())
            .filter(Files::isDirectory)
            .forEach(file -> {
              try {
                Files.deleteIfExists(file);
              } catch (final IOException e) {
                LOG.error("delete: {}: ", file, e);
              }
            });
        }

      } catch (final IOException e) {
        throw new NException(
          requireNonNullElse(e.getMessage(), e.getClass().getSimpleName()),
          e,
          errorIo()
        );
      }
    }

    private static void runtimeUnpackZip(
      final Path rtFile,
      final Path output)
      throws IOException
    {
      Files.createDirectories(output);

      try (var resources = CloseableCollection.create(() -> {
        return new IOException("One or more resources failed to close.");
      })) {
        final var raw =
          resources.add(Files.newInputStream(rtFile));
        final var buffered =
          resources.add(new BufferedInputStream(raw));
        final var zip =
          resources.add(new ZipInputStream(buffered));

        while (true) {
          final var entry = zip.getNextEntry();
          if (entry == null) {
            break;
          }

          final var fullPath =
            List.of(entry.getName().split("/"));
          final var withoutLeading =
            fullPath.subList(1, fullPath.size());

          var outputFile = output;
          for (final var element : withoutLeading) {
            outputFile = outputFile.resolve(element);
          }
          outputFile = outputFile.toAbsolutePath();
          outputFile = outputFile.normalize();

          if (outputFile.equals(output)) {
            continue;
          }

          if (!outputFile.startsWith(output)) {
            throw new IOException(
              "Refusing to unpack entry %s to %s"
                .formatted(entry.getName(), outputFile)
            );
          }

          if (entry.isDirectory()) {
            LOG.info("mkdir {}", outputFile);
            Files.createDirectories(outputFile);
            continue;
          }

          LOG.info("mkdir {}", outputFile.getParent());
          Files.createDirectories(outputFile.getParent());

          /*
           * There's no good reason to include src.zip in runtimes. It's often
           * half the size of the entire runtime.
           */

          if ("src.zip".equals(outputFile.getFileName().toString())) {
            LOG.info("refusing to unpack {}", outputFile);
            continue;
          }

          LOG.info("write {}", outputFile);
          Files.deleteIfExists(outputFile);
          try (var outputStream =
                 Files.newOutputStream(
                   outputFile,
                   CREATE,
                   WRITE,
                   TRUNCATE_EXISTING)) {
            zip.transferTo(outputStream);
          }
        }
      }
    }

    private static void runtimeUnpackTarGZ(
      final Path rtFile,
      final Path output)
      throws IOException
    {
      Files.createDirectories(output);

      try (var resources = CloseableCollection.create(() -> {
        return new IOException("One or more resources failed to close.");
      })) {
        final var raw =
          resources.add(Files.newInputStream(rtFile));
        final var buffered =
          resources.add(new BufferedInputStream(raw));
        final var gzip =
          resources.add(new GZIPInputStream(buffered));
        final var tarStream =
          resources.add(new TarArchiveInputStream(gzip));

        while (true) {
          final var entry = tarStream.getNextTarEntry();
          if (entry == null) {
            break;
          }

          final var fullPath =
            List.of(entry.getName().split("/"));
          final var withoutLeading =
            fullPath.subList(1, fullPath.size());

          var outputFile = output;
          for (final var element : withoutLeading) {
            outputFile = outputFile.resolve(element);
          }
          outputFile = outputFile.toAbsolutePath();
          outputFile = outputFile.normalize();

          if (outputFile.equals(output)) {
            continue;
          }

          if (!outputFile.startsWith(output)) {
            throw new IOException(
              "Refusing to unpack entry %s to %s"
                .formatted(entry.getName(), outputFile)
            );
          }

          if (entry.isDirectory()) {
            LOG.info("mkdir {}", outputFile);
            Files.createDirectories(outputFile);
            continue;
          }

          LOG.info("mkdir {}", outputFile.getParent());
          Files.createDirectories(outputFile.getParent());

          /*
           * There's no good reason to include src.zip in runtimes. It's often
           * half the size of the entire runtime.
           */

          if ("src.zip".equals(outputFile.getFileName().toString())) {
            LOG.info("refusing to unpack {}", outputFile);
            continue;
          }

          LOG.info("write {}", outputFile);
          Files.deleteIfExists(outputFile);
          try (var outputStream =
                 Files.newOutputStream(
                   outputFile,
                   CREATE,
                   WRITE,
                   TRUNCATE_EXISTING)) {
            tarStream.transferTo(outputStream);
          }

          setPermissions(outputFile, entry.getMode());
        }
      }
    }

    private static final int GROUP_EXEC = 0x08;
    private static final int GROUP_READ = 0x20;
    private static final int GROUP_WRITE = 0x10;
    private static final int OTHER_EXEC = 0x01;
    private static final int OTHER_READ = 0x04;
    private static final int OTHER_WRITE = 0x02;
    private static final int OWNER_EXEC = 0x40;
    private static final int OWNER_READ = 0x100;
    private static final int OWNER_WRITE = 0x80;

    private static void setPermissions(
      final Path outputFile,
      final int mode)
      throws IOException
    {
      final var perms = new HashSet<PosixFilePermission>();
      addPermissionsOwner(mode, perms);
      addPermissionsGroup(mode, perms);
      addPermissionsOther(mode, perms);

      try {
        Files.setPosixFilePermissions(outputFile, perms);
      } catch (final UnsupportedOperationException e) {
        // We are on a non-POSIX filesystem
      }
    }

    private static void addPermissionsOther(
      final int mode,
      final HashSet<PosixFilePermission> perms)
    {
      if ((mode & OTHER_EXEC) == OTHER_EXEC) {
        perms.add(PosixFilePermission.OTHERS_EXECUTE);
      }
      if ((mode & OTHER_WRITE) == OTHER_WRITE) {
        perms.add(PosixFilePermission.OTHERS_WRITE);
      }
      if ((mode & OTHER_READ) == OTHER_READ) {
        perms.add(PosixFilePermission.OTHERS_READ);
      }
    }

    private static void addPermissionsGroup(
      final int mode,
      final HashSet<PosixFilePermission> perms)
    {
      if ((mode & GROUP_EXEC) == GROUP_EXEC) {
        perms.add(PosixFilePermission.GROUP_EXECUTE);
      }
      if ((mode & GROUP_WRITE) == GROUP_WRITE) {
        perms.add(PosixFilePermission.GROUP_WRITE);
      }
      if ((mode & GROUP_READ) == GROUP_READ) {
        perms.add(PosixFilePermission.GROUP_READ);
      }
    }

    private static void addPermissionsOwner(
      final int mode,
      final HashSet<PosixFilePermission> perms)
    {
      if ((mode & OWNER_EXEC) == OWNER_EXEC) {
        perms.add(PosixFilePermission.OWNER_EXECUTE);
      }
      if ((mode & OWNER_WRITE) == OWNER_WRITE) {
        perms.add(PosixFilePermission.OWNER_WRITE);
      }
      if ((mode & OWNER_READ) == OWNER_READ) {
        perms.add(PosixFilePermission.OWNER_READ);
      }
    }

    @Override
    public void runtimeDelete(
      final NRuntime runtime)
      throws NException
    {
      Objects.requireNonNull(runtime, "runtime");

      final var paths =
        RuntimePaths.create(this.runtimes, runtime);

      try {
        Files.createDirectories(paths.rtFileTmp.getParent());

        try (var file =
               FileChannel.open(paths.rtFileTmp, CREATE, WRITE)) {
          try (var lock = file.lock()) {
            Files.deleteIfExists(paths.rtFileTmp);
            Files.deleteIfExists(paths.rtiFile);
            Files.deleteIfExists(paths.rtFile);
          }
        }
      } catch (final IOException e) {
        throw new NException(
          requireNonNullElse(e.getMessage(), e.getClass().getSimpleName()),
          e,
          errorIo()
        );
      }
    }

    @Override
    public void close()
      throws NException
    {

    }

    @Override
    public NRuntime runtimeFind(
      final String id)
      throws NException
    {
      Objects.requireNonNull(id, "id");

      try {
        final var locally =
          this.runtimesAvailableLocally();

        for (final var local : locally) {
          if (Objects.equals(local.id(), id)) {
            return local;
          }
        }

        return this.pkgToRuntime(this.client.getPkg(id));
      } catch (final IOException e) {
        throw new NException(
          requireNonNullElse(e.getMessage(), e.getClass().getSimpleName()),
          e,
          errorIo()
        );
      }
    }

    private static List<Pkg> findRuntimes(
      final DiscoClient client,
      final Architecture architecture,
      final OperatingSystem operatingSystem,
      final int version,
      final Predicate<String> distribution)
    {
      final var distributions =
        client.getDistributions()
          .stream()
          .filter(d -> distribution.test(d.getName()))
          .toList();

      final var packages = new ArrayList<Pkg>();
      for (final var archive : List.of(ArchiveType.TAR_GZ, ArchiveType.ZIP)) {
        packages.addAll(
          client.getPkgs(
            distributions,
            VersionNumber.fromText(Integer.toString(version)),
            Latest.OVERALL,
            operatingSystem,
            null,
            architecture,
            null,
            archive,
            PackageType.JDK,
            false,
            true,
            List.of(ReleaseStatus.GA),
            TermOfSupport.LTS,
            null,
            null
          )
        );
      }

      return packages;
    }
  }

  private static final class Download
    implements NRuntimeDownloadType, Runnable
  {
    private final NRuntime runtime;
    private final CompletableFuture<Path> future;
    private final RuntimePaths paths;
    private long sizeReceived;

    Download(
      final NRuntime inRuntime,
      final CompletableFuture<Path> inFuture,
      final RuntimePaths inPaths)
    {
      this.runtime =
        Objects.requireNonNull(inRuntime, "runtime");
      this.future =
        Objects.requireNonNull(inFuture, "future");
      this.paths =
        Objects.requireNonNull(inPaths, "paths");
    }

    @Override
    public long sizeExpected()
    {
      return this.runtime.size();
    }

    @Override
    public long sizeReceived()
    {
      return this.sizeReceived;
    }

    @Override
    public CompletableFuture<Path> future()
    {
      return this.future;
    }

    @Override
    public void run()
    {
      try {
        Files.createDirectories(this.paths.rtFileTmp.getParent());

        try (var file =
               FileChannel.open(this.paths.rtFileTmp, CREATE, WRITE)) {
          try (var lock = file.lock()) {
            this.executeDownload(file);
          }
        }

        this.future.complete(this.paths.rtFile);
      } catch (final Throwable ex) {
        this.future.completeExceptionally(ex);
      }
    }

    private void executeDownload(
      final FileChannel file)
      throws Exception
    {
      if (!this.downloadIsRequired()) {
        this.sizeReceived = this.runtime.size();
        this.future.complete(this.paths.rtFile);
        return;
      }

      file.truncate(0L);
      file.position(0L);

      final var client =
        HttpClient.newBuilder()
          .followRedirects(HttpClient.Redirect.NORMAL)
          .build();

      final var request =
        HttpRequest.newBuilder(this.runtime.source())
          .GET()
          .build();

      final var response =
        client.send(request, HttpResponse.BodyHandlers.ofInputStream());

      if (response.statusCode() >= 400) {
        throw new IOException(
          "%s: %s".formatted(
            this.runtime.source(),
            Integer.valueOf(response.statusCode())
          ));
      }

      final var buffer = new byte[8192];
      try (var stream = response.body()) {
        while (true) {
          final var r = stream.read(buffer);
          if (r == -1) {
            break;
          }
          this.sizeReceived += Integer.toUnsignedLong(r);
          file.write(ByteBuffer.wrap(buffer, 0, r));
        }
      }

      final var fileSize = Files.size(this.paths.rtFileTmp);
      if (fileSize != this.runtime.size()) {
        throw new IOException(
          "Expected to receive %s octets, but received %s"
            .formatted(
              Long.toUnsignedString(this.sizeExpected()),
              Long.toUnsignedString(fileSize))
        );
      }

      Files.move(
        this.paths.rtFileTmp,
        this.paths.rtFile,
        REPLACE_EXISTING,
        ATOMIC_MOVE
      );

      runtimeWrite(this.runtime, this.paths.rtiFile);
    }

    private boolean downloadIsRequired()
    {
      try {
        final var fileSize = Files.size(this.paths.rtFile);
        return (fileSize != this.runtime.size());
      } catch (final IOException e) {
        return true;
      }
    }
  }

  private static NRuntime runtimeLoad(
    final Path file)
    throws Exception
  {
    final var props = new Properties();
    try (var stream = Files.newInputStream(file)) {
      props.load(stream);
    }

    return new NRuntime(
      props.getProperty("id"),
      new NRuntimeDistribution(props.getProperty("distribution")),
      new NOperatingSystem(props.getProperty("operatingSystem")),
      new NArchitecture(props.getProperty("architecture")),
      NRuntimeArchiveType.valueOf(props.getProperty("archiveType")),
      new URI(props.getProperty("uri")),
      Integer.parseUnsignedInt(props.getProperty("version")),
      props.getProperty("versionText"),
      Long.parseUnsignedLong(props.getProperty("size"))
    );
  }

  private static void runtimeWrite(
    final NRuntime runtime,
    final Path file)
    throws Exception
  {
    try (var stream = Files.newOutputStream(file)) {
      final var props = new Properties();
      props.setProperty("architecture", runtime.architecture().name());
      props.setProperty("archiveType", runtime.archiveType().name());
      props.setProperty("distribution", runtime.distribution().name());
      props.setProperty("id", runtime.id());
      props.setProperty("operatingSystem", runtime.operatingSystem().name());
      props.setProperty("size", Long.toUnsignedString(runtime.size()));
      props.setProperty("uri", runtime.source().toString());
      props.setProperty("version", Integer.toUnsignedString(runtime.version()));
      props.setProperty("versionText", runtime.versionText());
      props.store(stream, "");
    }
  }
}
