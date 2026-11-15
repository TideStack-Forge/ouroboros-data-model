package com.ouroboros.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** A completed temporary file, owned by its caller until consumption or explicit close. Never JSON payload data. */
public final class FileResult implements AutoCloseable {
  private final Path path;
  private final String fileName;
  private final String mediaType;
  private final long length;
  private InputStream stream;
  private boolean closed;

  private FileResult(Path path, String fileName, String mediaType) throws IOException {
    if (path == null || fileName == null || fileName.isBlank() || mediaType == null || mediaType.isBlank()
        || fileName.chars().anyMatch(Character::isISOControl) || mediaType.chars().anyMatch(Character::isISOControl)) {
      throw new IllegalArgumentException("文件结果必须提供有效名称、媒体类型和已生成的临时文件");
    }
    if (!Files.isRegularFile(path)) { throw new IOException("文件结果必须引用已生成的普通临时文件"); }
    this.path = path; this.fileName = fileName; this.mediaType = mediaType; this.length = Files.size(path);
  }

  /** Transfers ownership of this already-generated temporary file; closing deletes it. */
  public static FileResult temporary(Path path, String fileName, String mediaType) throws IOException {
    return new FileResult(path, fileName, mediaType);
  }

  public String fileName() { return fileName; }
  public String mediaType() { return mediaType; }
  public long length() { return length; }

  public synchronized InputStream openStream() throws IOException {
    if (closed || stream != null) { throw new IOException("文件结果已关闭或已被消费"); }
    stream = Files.newInputStream(path);
    return new FilterInputStream(stream) {
      @Override public void close() throws IOException { FileResult.this.close(); }
    };
  }

  @Override
  public synchronized void close() throws IOException {
    if (closed) { return; }
    IOException failure = null;
    try { if (stream != null) { stream.close(); } } catch (IOException error) { failure = error; }
    try { Files.deleteIfExists(path); } catch (IOException error) {
      if (failure == null) { failure = error; } else { failure.addSuppressed(error); }
    }
    if (failure != null) { throw failure; }
    closed = true;
  }
}
