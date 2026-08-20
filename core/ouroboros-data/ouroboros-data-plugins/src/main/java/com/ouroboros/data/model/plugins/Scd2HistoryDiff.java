package com.ouroboros.data.model.plugins;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.record.Record;

public class Scd2HistoryDiff {
  private final List<String> changedFields;
  private final Map<String, Object> changeSet;

  private Scd2HistoryDiff(List<String> changedFields, Map<String, Object> changeSet) {
    this.changedFields = changedFields;
    this.changeSet = changeSet;
  }

  public static Scd2HistoryDiff between(List<DataModelField> sourceFields,
                                          Record oldRecord,
                                          Record newRecord,
                                          Set<String> ignoreFields) {
    List<String> changedFields = new ArrayList<String>();
    Map<String, Object> changeSet = new LinkedHashMap<String, Object>();

    sourceFields.stream()
        .filter(field -> field.getValueType().isPhysical())
        .filter(field -> !ignoreFields.contains(field.getName()))
        .forEach(field -> {
          Object oldValue = field.getValueType().toPersistentValue(readValue(oldRecord, field));
          Object newValue = field.getValueType().toPersistentValue(readValue(newRecord, field));
          if (!Objects.equals(oldValue, newValue)) {
            changedFields.add(field.getName());
            Map<String, Object> fieldChange = new LinkedHashMap<String, Object>();
            fieldChange.put("from", oldValue);
            fieldChange.put("to", newValue);
            changeSet.put(field.getName(), fieldChange);
          }
        });

    return new Scd2HistoryDiff(changedFields, changeSet);
  }

  public boolean hasBusinessChanges() {
    return !changedFields.isEmpty();
  }

  public List<String> getChangedFields() {
    return Collections.unmodifiableList(changedFields);
  }

  public Map<String, Object> toChangeSet() {
    return Collections.unmodifiableMap(changeSet);
  }

  static Object readValue(Record record, DataModelField field) {
    if (record == null) {
      return null;
    }
    if (record.containsKey(field.getName())) {
      return record.get(field.getName());
    }
    if (record.containsKey(field.getRawName())) {
      return record.get(field.getRawName());
    }
    String upperRawName = field.getRawName().toUpperCase();
    if (record.containsKey(upperRawName)) {
      return record.get(upperRawName);
    }
    String upperName = field.getName().toUpperCase();
    if (record.containsKey(upperName)) {
      return record.get(upperName);
    }
    return record.get(field.getName());
  }
}
