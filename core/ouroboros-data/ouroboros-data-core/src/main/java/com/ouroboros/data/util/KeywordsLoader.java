package com.ouroboros.data.util;

import static io.vavr.API.Try;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.vavr.Tuple;
import io.vavr.Tuple2;

import com.ouroboros.data.util.DataResources;

public class KeywordsLoader {
  public static Map<String, Set<String>> loadKeywords(String fileName) {
    var resourceName = "META-INF/keywords/" + fileName;
    return Try(() -> DataResources.getResourceContentLinesList(resourceName).stream()
        .map(KeywordsLoader::loadKeywords)
        .reduce(new HashMap<>(), (prev, curr) -> {
          curr.forEach((k, v) -> prev.merge(k, v, (prevSet, currSet) -> {
            prevSet.addAll(currSet);
            return prevSet;
          }));
          return prev;
        })).getOrElse(Collections::emptyMap);
  }

  private static Map<String, Set<String>> loadKeywords(Stream<String> lines) {
    return lines.map(String::trim)
        .filter(l -> !l.isEmpty() && !l.startsWith("#"))
        .map(l -> l.split(":"))
        .filter(l -> l.length == 2)
        // 取出所有有效行，映射成 Map Entry
        .map(l -> Tuple.of(l[0].trim().toUpperCase(),
            Arrays.stream(l[1].split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .collect(Collectors.toSet())))
        .collect(Collectors.toMap(Tuple2::_1, Tuple2::_2, (v1, v2) -> {
          v1.addAll(v2);
          return v1;
        }));
  }

}
