package com.ouroboros.util.map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.Test;

public class LazyMergedMapCoverageTest {

  @Test
  public void supportsOverlayCachingViewsAndPolicies() {
    var supplierCalls = new AtomicInteger();
    Map<String, Object> first = new LinkedHashMap<String, Object>();
    first.put("a", 1);
    first.put("shared", "first");
    Map<String, Object> second = new LinkedHashMap<String, Object>();
    second.put("b", 2);
    second.put("shared", "second");
    Map<String, Supplier<Object>> lazy = new LinkedHashMap<String, Supplier<Object>>();
    lazy.put("lazy", () -> {
      supplierCalls.incrementAndGet();
      return 3;
    });

    LazyMergedMap map = new LazyMergedMap.Builder()
        .addSources(first, second)
        .addLazySource(lazy)
        .addResolver((String key) -> key.equals("resolved") ? 4 : null)
        .addLazyEntry("sum", current -> (Integer) current.get("a") + (Integer) current.get("b"))
        .addDynamicKeyHint("resolved")
        .materializeOnView(false)
        .build();

    assertEquals(1, map.get("a"));
    assertEquals("first", map.get("shared"));
    assertEquals(3, map.get("lazy"));
    assertEquals(1, supplierCalls.get());
    assertEquals(3, map.get("lazy"));
    assertEquals(1, supplierCalls.get());
    assertEquals(4, map.get("resolved"));
    assertEquals(3, map.get("sum"));
    assertTrue(map.containsKey("resolved"));
    assertTrue(map.containsValue(4));
    assertFalse(map.isEmpty());
    assertTrue(map.keySet().containsAll(Arrays.asList("a", "b", "lazy", "resolved", "sum", "shared")));
    assertTrue(map.entrySet().stream().anyMatch(entry -> entry.getKey().equals("sum") && entry.getValue().equals(3)));
    assertTrue(map.values().contains(4));

    assertEquals(null, map.put("overlay", 5));
    assertEquals(5, map.get("overlay"));
    map.putAll(Collections.singletonMap("overlay2", 6));
    assertEquals(6, map.get("overlay2"));
    assertEquals(5, map.remove("overlay"));
    map.clearCache();
    assertEquals(3, map.get("lazy"));
    map.clear();
    assertFalse(map.overlayView().containsKey("overlay2"));

    Map<String, Object> materialized = new LazyMergedMap.Builder()
        .addSource(first)
        .addSource(second)
        .addLazyEntry("computed", current -> (String) current.get("shared") + "!")
        .materializeOnView(true)
        .build()
        .materialize();
    assertEquals("first!", materialized.get("computed"));

    assertEquals("second", new LazyMergedMap.Builder().addSources(first, second).mergePolicy(LazyMergedMap.MergePolicy.LAST_WINS).build().get("shared"));
    assertEquals("same", new LazyMergedMap.Builder().addSources(Collections.<String, Object>singletonMap("k", "same"), Collections.<String, Object>singletonMap("k", "same")).mergePolicy(LazyMergedMap.MergePolicy.FAIL_ON_CONFLICT).build().get("k"));
  }

  @Test(expected = IllegalStateException.class)
  public void failOnConflictThrowsForDifferentValues() {
    new LazyMergedMap.Builder()
        .addSources(Collections.singletonMap("k", "left"), Collections.singletonMap("k", "right"))
        .mergePolicy(LazyMergedMap.MergePolicy.FAIL_ON_CONFLICT)
        .build()
        .get("k");
  }

  @Test(expected = UnsupportedOperationException.class)
  public void readOnlyOverlayRejectsWrites() {
    new LazyMergedMap.Builder().mutableOverlay(false).build().put("x", 1);
  }

  @Test
  public void supportsConvenienceFactories() {
    LazyMergedMap map = LazyMergedMap.of(Collections.<String, Object>singletonMap("a", 1));
    assertEquals(1, map.get("a"));
    assertEquals(2, LazyMergedMap.fromReduced(Collections.<String, Object>singletonMap("b", 2)).get("b"));
    assertEquals(3, LazyMergedMap.fromStreamReduced(Collections.<String, Object>singletonMap("c", 3), LazyMergedMap.MergePolicy.FIRST_WINS).get("c"));
    var values = map.values();
    assertTrue(values.contains(1));
  }
}
