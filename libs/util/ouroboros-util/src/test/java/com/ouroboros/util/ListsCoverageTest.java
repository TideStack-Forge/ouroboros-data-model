package com.ouroboros.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class ListsCoverageTest {

  @Test
  public void detectsStringKeyMapsAndEmptyLists() {
    assertFalse(Lists.isStringKeyMapList(null));
    assertTrue(Lists.isStringKeyMapList(Collections.singletonList(Collections.singletonMap("a", 1))));
    assertFalse(Lists.isStringKeyMapList(Arrays.asList(Collections.singletonMap("a", 1), 2)));

    assertTrue(Lists.isEmpty(null));
    assertTrue(Lists.isEmpty(Collections.emptyList()));
    assertFalse(Lists.isEmpty(Collections.singletonList("x")));
  }

  @Test
  public void fillSetGetRemoveAndPartitionWork() {
    var list = new ArrayList<>();

    Lists.fill(list, 2, "x");
    assertEquals(Arrays.asList("x", "x"), list);

    Lists.fill(list, 4, index -> "v" + index);
    assertEquals(Arrays.asList("x", "x", "v0", "v1"), list);

    Lists.fill(list, 6, () -> "s");
    assertEquals(Arrays.asList("x", "x", "v0", "v1", "s", "s"), list);

    assertEquals("x", Lists.setValue(list, 0, "first"));
    assertEquals("first", list.get(0));

    assertEquals("added", Lists.setValue(list, 6, "added", "pad"));
    assertEquals("added", list.get(6));

    assertEquals("first", Lists.getValue(list, 0));
    assertEquals("fallback", Lists.getValue(list, 99, "fallback"));
    assertEquals("first", Lists.removeValue(list, 0));
    assertEquals(Arrays.asList("x", "v0", "v1", "s", "s", "added"), list);

    assertEquals(Arrays.asList(Arrays.asList("x", "v0"), Arrays.asList("v1", "s"), Arrays.asList("s", "added")),
        Lists.partition(list, 2));
  }

  @Test
  public void deepCloneCopiesNestedListsAndMaps() {
    var source = new ArrayList<>();
    var nestedList = new ArrayList<>(Collections.singletonList("nested"));
    var nestedMap = new LinkedHashMap<>();
    nestedMap.put("k", Collections.singletonList("v"));
    source.add(nestedList);
    source.add(nestedMap);

    var clone = Lists.deepClone(source);

    assertEquals(source, clone);
    assertFalse(source == clone);
    assertFalse(source.get(0) == clone.get(0));
    assertFalse(source.get(1) == clone.get(1));
  }
}
