package com.ouroboros.data.dsl.statement;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
public final class PopulateClause implements Serializable {
  private static final long serialVersionUID = 1L;
  public record PopulateEntry(String fieldName, Object options) implements Serializable {}
  private final List<PopulateEntry> entries;
  private PopulateClause(List<PopulateEntry> entries) {
    this.entries = entries;
  }
  @SuppressWarnings("unchecked")
  public static PopulateClause fromRaw(Object rawValue) {
    List<PopulateEntry> entries = parseEntries(rawValue).collect(Collectors.toList());
    return new PopulateClause(entries);
  }
  public List<PopulateEntry> getEntries() {
    return entries;
  }
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof PopulateClause other)) return false;
    return Objects.equals(entries, other.entries);
  }
  @Override
  public int hashCode() {
    return Objects.hash(entries);
  }
  @SuppressWarnings("unchecked")
  private static Stream<PopulateEntry> parseEntries(Object value) {
    if (value == null) {
      return Stream.empty();
    }
    if (value instanceof CharSequence cs) {
      return Stream.of(cs.toString().split(","))
          .map(String::trim)
          .filter(s -> !s.isEmpty())
          .map(n -> new PopulateEntry(n, null));
    }
    if (value instanceof Map<?, ?> map) {
      return map.entrySet().stream()
          .filter(e -> e.getKey() instanceof CharSequence)
          .map(e -> new PopulateEntry(e.getKey().toString(), e.getValue()));
    }
    if (value instanceof Collection<?> coll) {
      return coll.stream().flatMap(item -> {
        if (item instanceof CharSequence || item instanceof Map) {
          return parseEntries(item);
        }
        return Stream.empty();
      });
    }
    return Stream.empty();
  }
}
