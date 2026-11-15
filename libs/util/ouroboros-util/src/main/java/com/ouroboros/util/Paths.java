package com.ouroboros.util;

import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ObjectUtils;

/**
 * @author liansz
 */
public abstract class Paths {

  protected Paths(String... paths) {
    if (paths == null || paths.length == 0) {
      throw new NullPointerException();
    }

    var sb = new StringBuilder();
    Arrays.asList(paths).forEach(str -> {
      if (str != null) {
        sb.append(str);
      }
    });
    if (sb.toString().isEmpty()) {
      throw new NullPointerException();
    }
  }

  protected String resolve(String parent, String child) {
    if (child.isEmpty()) {
      return parent;
    }
    if (child.charAt(0) == getDefaultParent()) {
      if ("/".equals(parent)) {
        return child;
      }
      return parent + child;
    }
    if ("/".equals(parent)) {
      return parent + child;
    }
    return parent + getDefaultParent() + child;
  }

  protected String normalize(String pathname) {
    if (pathname == null || pathname.isEmpty()) {
      return "";
    }
    var n = pathname.length();
    var prevChar = 0;
    for (int i = 0; i < n; i++) {
      var c = pathname.charAt(i);
      if ((prevChar == getDefaultParent()) && (c == getDefaultParent())) {
        return normalize(pathname, n, i - 1);
      }
      prevChar = c;
    }
    if (prevChar == getDefaultParent()) {
      return normalize(pathname, n, n - 1);
    }
    return pathname;
  }

  protected String normalize(String pathname, int len, int off) {
    if (len == 0) {
      return pathname;
    }
    var n = len;
    while ((n > 0) && (pathname.charAt(n - 1) == getDefaultParent())) {
      n--;
    }
    if (n == 0) {
      return "/";
    }
    var sb = new StringBuffer(pathname.length());
    if (off > 0) {
      sb.append(pathname.substring(0, off));
    }
    var prevChar = 0;
    for (int i = off; i < n; i++) {
      var c = pathname.charAt(i);
      if ((prevChar == getDefaultParent()) && (c == getDefaultParent())) {
        continue;
      }
      sb.append(c);
      prevChar = c;
    }
    return sb.toString();
  }

  protected abstract char getDefaultParent();

  public static class File extends Paths {

    private java.io.File file;

    private File(String... params) {
      super(params);

      var path = String.valueOf(getDefaultParent());
      for (int i = 0; i < params.length; i++) {
        path = resolve(path, normalize(params[i]));
      }

      file = new java.io.File(path);
    }

    public static File of(String... params) {
      return new File(params);
    }

    public java.io.File toFile() {
      return file;
    }

    public String getPath() {
      return file.getPath();
    }

    public String getParent() {
      return file.getParent();
    }

    public String getName() {
      return file.getName();
    }

    public String getSuffix() {
      var fileName = getName();
      return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    public boolean exists() {
      return file.exists();
    }

    public boolean isFile() {
      return file.isFile();
    }

    public boolean isDirectory() {
      return file.isDirectory();
    }

    @Override
    protected char getDefaultParent() {
      return java.io.File.separatorChar;
    }

    @Override
    public String toString() {
      return getPath();
    }
  }

  public static class Url extends Paths {

    private String head, url, tail;

    private Url(String... params) {
      super(params);

      var pattern = Pattern.compile("^([a-zA-Z0-9]+://).*");
      var matcher = pattern.matcher(params[0]);
      head = matcher.find() ? matcher.group(1) : "";
      params[0] = params[0].substring(head.length());

      for (int i = params.length - 1; i > 0; i--) {
        if (params[i] != null && !params[i].isEmpty()) {
          tail = Pattern.matches(".+/$", params[i]) ? "/" : "";
          break;
        }
      }

      url = "";
      for (int i = 0; i < params.length; i++) {
        if (params[i] != null && !params[i].isEmpty()) {
          url = resolve(url, normalize(params[i]));
        }
      }
    }

    public static Url of(String... params) {
      return new Url(params);
    }

    public URL toUrl() {
      try {
        return new URL(toString());
      } catch (MalformedURLException e) {
        throw new UncheckedIOException(e);
      }
    }

    @Override
    protected char getDefaultParent() {
      return '/';
    }

    @Override
    public String toString() {
      if (ObjectUtils.isEmpty(head)) {
        return (url.charAt(0) == getDefaultParent() ? url : getDefaultParent() + url) + tail;
      } else {
        return head + (url.charAt(0) == getDefaultParent() ? url.substring(1) : url) + tail;
      }
    }
  }
}
