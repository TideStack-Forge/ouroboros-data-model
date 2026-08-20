package com.ouroboros.data.record;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class DefaultRecordListImplCoverageTest {

  static class BeanView {
    private Integer id;

    public Integer getId() {
      return id;
    }

    public void setId(Integer id) {
      this.id = id;
    }
  }

  @Test
  void exposesListViewAndRecordConversion() {
    Map<String, Object> first = new LinkedHashMap<String, Object>();
    first.put("id", 1);
    Map<String, Object> second = new HashMap<String, Object>();
    second.put("id", 2);

    DefaultRecordListImpl list = new DefaultRecordListImpl(new ArrayList<Map<String, Object>>(Arrays.asList(first, second)));

    assertEquals(2, list.size());
    assertFalse(list.isEmpty());
    assertTrue(list.contains(first));
    assertEquals(0, list.indexOf(first));
    assertEquals(0, list.lastIndexOf(first));
    assertEquals(2, list.get(1).get("id"));
    assertArrayEquals(new Object[]{first, second}, list.toArray());
    assertEquals(2, list.subList(0, 2).size());
    assertEquals(1, list.iterator().next().get("id"));
  }

  @Test
  void mutateRecordUpdatesMutableRecords() {
    Map<String, Object> record = new HashMap<String, Object>();
    record.put("name", "alice");

    DefaultRecordListImpl list = new DefaultRecordListImpl(Arrays.asList(record));
    list.mutateRecord(map -> map.put("name", "bob"));

    assertEquals("bob", list.get(0).get("name"));
  }

  @Test
  void unsupportedMutationsFail() {
    Map<String, Object> record = new HashMap<String, Object>();
    record.put("id", 1);
    DefaultRecordListImpl list = new DefaultRecordListImpl(Arrays.asList(record));

    assertThrows(UnsupportedOperationException.class, () -> list.add(Record.of(new HashMap<>())));
    assertThrows(UnsupportedOperationException.class, () -> list.remove(0));
    assertThrows(UnsupportedOperationException.class, () -> list.clear());
    assertThrows(UnsupportedOperationException.class, () -> list.listIterator().remove());
  }

  @Test
  void recordListStaticFactoriesAndToBeanListWork() {
    var empty = RecordList.empty();
    assertTrue(empty.isEmpty());

    var row = new LinkedHashMap<String, Object>();
    row.put("id", 3);
    var list = RecordList.of(Arrays.asList(row));
    assertEquals(1, list.size());

    var beanViews = list.toBeanList(BeanView.class);
    assertEquals(1, beanViews.size());
    assertEquals(Integer.valueOf(3), beanViews.get(0).getId());

    var unsupported = list.toBeanList(Void.class);
    assertEquals(1, unsupported.size());
    assertNull(unsupported.get(0));
  }
}
