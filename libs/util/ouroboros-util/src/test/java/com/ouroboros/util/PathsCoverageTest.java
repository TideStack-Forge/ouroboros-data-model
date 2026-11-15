package com.ouroboros.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PathsCoverageTest {

  @Test
  public void buildsNormalizedFilePaths() {
    var file = Paths.File.of("alpha", "beta.txt");

    assertTrue(file.getPath().endsWith(java.io.File.separator + "alpha" + java.io.File.separator + "beta.txt"));
    assertEquals("alpha", file.getParent().substring(file.getParent().lastIndexOf(java.io.File.separator) + 1));
    assertEquals("beta.txt", file.getName());
    assertEquals("txt", file.getSuffix());
  }

  @Test
  public void buildsUrlsWithAndWithoutSchemeAndTrailingSlash() {
    var withScheme = Paths.Url.of("https://example.com", "api", "v1/");
    assertEquals("https://example.com/api/v1/", withScheme.toString());
    assertEquals("https", withScheme.toUrl().getProtocol());
    assertEquals("example.com", withScheme.toUrl().getHost());
    assertEquals("/api/v1/", withScheme.toUrl().getPath());

    assertEquals("/example.com/api", Paths.Url.of("example.com", "api").toString());
  }
}
