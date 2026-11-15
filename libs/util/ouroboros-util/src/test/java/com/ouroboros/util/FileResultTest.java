package com.ouroboros.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class FileResultTest {
  @TempDir Path directory;

  @Test
  void completedContentIsConsumedOnceAndClosingItsStreamReleasesTheOwnedTemporaryFile() throws Exception {
    var path = Files.write(directory.resolve("content.tmp"), new byte[]{1, 2, 3});
    var result = FileResult.temporary(path, "订单.xlsx", "application/octet-stream");
    assertEquals(3, result.length()); assertEquals("订单.xlsx", result.fileName());
    assertEquals("application/octet-stream", result.mediaType()); assertTrue(Files.exists(path));
    try (var input = result.openStream()) {
      assertArrayEquals(new byte[]{1, 2, 3}, input.readAllBytes());
      assertThrows(IOException.class, result::openStream);
    }
    assertFalse(Files.exists(path)); result.close(); assertThrows(IOException.class, result::openStream);
  }

  @Test
  void unusedResultCanBeClosedAndDirectoriesMissingFilesAndInvalidMetadataAreRejected() throws Exception {
    var path = Files.writeString(directory.resolve("unused.tmp"), "ready");
    try (var result = FileResult.temporary(path, "file.txt", "text/plain")) { assertTrue(Files.exists(path)); }
    assertFalse(Files.exists(path));
    assertThrows(IOException.class, () -> FileResult.temporary(path, "missing", "text/plain"));
    assertThrows(IOException.class, () -> FileResult.temporary(directory, "directory", "text/plain"));
    assertThrows(IllegalArgumentException.class, () -> FileResult.temporary(directory, "bad\r\nheader", "text/plain"));
  }
}
