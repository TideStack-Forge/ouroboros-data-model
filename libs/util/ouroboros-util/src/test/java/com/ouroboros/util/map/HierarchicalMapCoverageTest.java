package com.ouroboros.util.map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

public class HierarchicalMapCoverageTest {

  @Test
  public void mutableAndImmutableHierarchicalMapsExposeExpectedFacadeBehavior() {
    var mutable = new MutableHierarchicalMap(new LinkedHashMap<>(), value -> Arrays.asList(value.split("/")));
    mutable.setValue("user/name", "Neo").setValue("user/roles/0", "chosen");
    assertTrue(mutable.hasPath("user/name"));
    assertEquals("Neo", mutable.getValue("user/name").orElse(null));
    assertEquals("chosen", mutable.removePath("user/roles/0").orElse(null));
    assertFalse(mutable.removePath(null).isPresent());
    assertFalse(mutable.removePath("missing/path").isPresent());

    var built = mutable.build().getWrappedMap();
    assertTrue(built.containsKey("user"));
    mutable.flatten();
    assertFalse(mutable.getWrappedMap().isEmpty());

    var immutable = mutable.asImmutable();
    assertFalse(immutable.getWrappedMap().isEmpty());
    assertTrue(immutable.getValue("user/name", String.class).isPresent() || immutable.getValue("user.name", String.class).isPresent());
    assertFalse(immutable.build().isEmpty());
    assertEquals(mutable.getWrappedMap(), immutable.toMutableShallow().getWrappedMap());
    assertEquals(mutable.getWrappedMap(), immutable.toMutable().flatten().getWrappedMap());
    assertTrue(immutable.toString().contains("ImmutableHierarchicalMap"));
    assertTrue(mutable.toString().contains("MutableHierarchicalMap"));
  }
}
