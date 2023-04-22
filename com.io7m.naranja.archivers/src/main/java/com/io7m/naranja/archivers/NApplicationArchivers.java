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


package com.io7m.naranja.archivers;

import com.io7m.naranja.core.NApplicationArchiverFactoryType;
import com.io7m.naranja.core.NApplicationArchiverProgressType;
import com.io7m.naranja.core.NApplicationArchiverType;
import com.io7m.naranja.core.NException;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipParameters;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.zip.Deflater;

import static com.io7m.naranja.core.NErrorCodes.errorIo;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.Objects.requireNonNullElse;

/**
 * The default archivers.
 */

public final class NApplicationArchivers
  implements NApplicationArchiverFactoryType
{
  /**
   * The default archivers.
   */

  public NApplicationArchivers()
  {

  }

  @Override
  public NApplicationArchiverType create(
    final String prefix,
    final Path sourceDirectory,
    final Path outputFile)
  {
    return new NArchiver(prefix, sourceDirectory, outputFile);
  }

  private static final class NArchiver
    implements NApplicationArchiverType, NApplicationArchiverProgressType
  {
    private static final FileTime BASE_TIME =
      FileTime.from(Instant.parse("2020-01-01T00:00:00Z"));

    private final String prefix;
    private final Path sourceDirectory;
    private final Path outputFile;
    private long fileIndex;
    private String fileName;
    private long fileCount;

    NArchiver(
      final String inPrefix,
      final Path inSourceDirectory,
      final Path inOutputFile)
    {
      this.prefix =
        Objects.requireNonNull(inPrefix, "prefix");
      this.sourceDirectory =
        Objects.requireNonNull(inSourceDirectory, "sourceDirectory");
      this.outputFile =
        Objects.requireNonNull(inOutputFile, "outputFile");

      this.fileIndex = 0L;
      this.fileCount = 0L;
      this.fileName = "";
    }

    @Override
    public void execute(
      final Consumer<NApplicationArchiverProgressType> progress)
      throws NException
    {
      try (var output =
             Files.newOutputStream(
               this.outputFile, WRITE, CREATE, TRUNCATE_EXISTING)) {
        try (var buffered = new BufferedOutputStream(output)) {
          final var gzipParams = new GzipParameters();
          gzipParams.setCompressionLevel(Deflater.BEST_COMPRESSION);
          gzipParams.setOperatingSystem(255);
          gzipParams.setModificationTime(BASE_TIME.toMillis());

          try (var gzip = new GzipCompressorOutputStream(buffered, gzipParams)) {
            try (var tar = new TarArchiveOutputStream(gzip, UTF_8.displayName())) {
              tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
              this.buildArchive(tar, progress);
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

    private void buildArchive(
      final TarArchiveOutputStream output,
      final Consumer<NApplicationArchiverProgressType> progress)
      throws IOException
    {
      final List<Path> files;
      try (var walkStream = Files.walk(this.sourceDirectory)) {
        files = walkStream.filter(Files::isRegularFile)
          .map(this.sourceDirectory::relativize)
          .map(Path::normalize)
          .sorted()
          .toList();
      }

      this.fileCount =
        Integer.toUnsignedLong(files.size());

      for (final var file : files) {
        this.fileName = "%s%s".formatted(this.prefix, file.toString());
        progress.accept(this);

        final var fileActual =
          this.sourceDirectory.resolve(file)
            .toAbsolutePath()
            .normalize();

        final var entry = new TarArchiveEntry(this.fileName);
        entry.setCreationTime(BASE_TIME);
        entry.setLastModifiedTime(BASE_TIME);
        entry.setLastAccessTime(BASE_TIME);
        entry.setGroupId(0L);
        entry.setUserId(0L);
        entry.setMode(modeOfFile(fileActual));
        entry.setSize(Files.size(fileActual));

        output.putArchiveEntry(entry);
        try (var input = Files.newInputStream(fileActual)) {
          input.transferTo(output);
        }
        output.closeArchiveEntry();
        ++this.fileIndex;
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

    private static int modeOfFile(
      final Path fileActual)
      throws IOException
    {
      final var permissions =
        Files.getPosixFilePermissions(fileActual);

      var mode = 0;
      mode = modeOfFileOwner(permissions, mode);
      mode = modeOfFileGroup(permissions, mode);
      mode = modeOfFileOther(permissions, mode);
      return mode;
    }

    private static int modeOfFileOther(
      final Set<PosixFilePermission> permissions,
      final int mode)
    {
      int r = mode;
      if (permissions.contains(PosixFilePermission.OTHERS_EXECUTE)) {
        r |= OTHER_EXEC;
      }
      if (permissions.contains(PosixFilePermission.OTHERS_WRITE)) {
        r |= OTHER_WRITE;
      }
      if (permissions.contains(PosixFilePermission.OTHERS_READ)) {
        r |= OTHER_READ;
      }
      return r;
    }

    private static int modeOfFileGroup(
      final Set<PosixFilePermission> permissions,
      final int mode)
    {
      int r = mode;
      if (permissions.contains(PosixFilePermission.GROUP_EXECUTE)) {
        r |= GROUP_EXEC;
      }
      if (permissions.contains(PosixFilePermission.GROUP_WRITE)) {
        r |= GROUP_WRITE;
      }
      if (permissions.contains(PosixFilePermission.GROUP_READ)) {
        r |= GROUP_READ;
      }
      return r;
    }

    private static int modeOfFileOwner(
      final Set<PosixFilePermission> permissions,
      final int mode)
    {
      int r = mode;
      if (permissions.contains(PosixFilePermission.OWNER_EXECUTE)) {
        r |= OWNER_EXEC;
      }
      if (permissions.contains(PosixFilePermission.OWNER_WRITE)) {
        r |= OWNER_WRITE;
      }
      if (permissions.contains(PosixFilePermission.OWNER_READ)) {
        r |= OWNER_READ;
      }
      return r;
    }

    @Override
    public void close()
    {

    }

    @Override
    public long fileCount()
    {
      return this.fileCount;
    }

    @Override
    public long fileIndex()
    {
      return this.fileIndex;
    }

    @Override
    public String fileName()
    {
      return this.fileName;
    }
  }
}
