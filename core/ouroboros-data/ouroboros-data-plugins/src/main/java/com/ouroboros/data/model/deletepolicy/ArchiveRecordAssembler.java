package com.ouroboros.data.model.deletepolicy;

import java.util.Date;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.record.Record;
import com.ouroboros.data.util.DataJson;

public final class ArchiveRecordAssembler {

  private ArchiveRecordAssembler() {
  }

  public static Map<String, Object> assemble(DataModel sourceModel, Record sourceRecord, Date deletedAt, Object deletedBy,
                                             String deleteOperationId) {
    return assemble(sourceModel, sourceRecord, deletedAt, deletedBy, deleteOperationId, Collections.emptyMap());
  }

  public static Map<String, Object> assemble(DataModel sourceModel, Record sourceRecord, Date deletedAt, Object deletedBy,
                                             String deleteOperationId, Map<String, String> sourceFieldMappings) {
    Map<String, Object> payload = new LinkedHashMap<String, Object>();
    for (DataModelField field : sourceModel.getFields()) {
      payload.put(resolveArchiveFieldName(field.getName(), sourceFieldMappings), getValueIgnoreCase(sourceRecord, field.getName()));
    }
    payload.put(ArchiveModelContract.FIELD_SOURCE_MODEL, sourceModel.getFullName());
    payload.put(ArchiveModelContract.FIELD_SOURCE_DATA_STATION, resolveDataStationName(sourceModel));
    payload.put(ArchiveModelContract.FIELD_SOURCE_PRIMARY_KEY, DataJson.toJsonString(buildPrimaryKeyMap(sourceModel, sourceRecord)));
    payload.put(ArchiveModelContract.FIELD_DELETED_AT, deletedAt);
    payload.put(ArchiveModelContract.FIELD_DELETED_BY, deletedBy);
    payload.put(ArchiveModelContract.FIELD_DELETE_OPERATION_ID, deleteOperationId);
    return payload;
  }

  private static String resolveArchiveFieldName(String sourceFieldName, Map<String, String> sourceFieldMappings) {
    return Optional.ofNullable(sourceFieldMappings)
        .map(mappings -> mappings.get(sourceFieldName))
        .filter(mappedName -> mappedName != null && !mappedName.trim().isEmpty())
        .orElse(sourceFieldName);
  }

  private static Map<String, Object> buildPrimaryKeyMap(DataModel sourceModel, Map<String, Object> sourceRecord) {
    Map<String, Object> primaryKeyMap = new LinkedHashMap<String, Object>();
    for (DataModelField field : sourceModel.getPrimaryKeys()) {
      primaryKeyMap.put(field.getName(), getValueIgnoreCase(sourceRecord, field.getName()));
    }
    return primaryKeyMap;
  }

  private static Object getValueIgnoreCase(Map<String, ?> record, String fieldName) {
    if (record.containsKey(fieldName)) {
      return record.get(fieldName);
    }
    for (Map.Entry<String, ?> entry : record.entrySet()) {
      String key = entry.getKey();
      if (key != null && key.equalsIgnoreCase(fieldName)) {
        return entry.getValue();
      }
    }
    return null;
  }

  private static String resolveDataStationName(DataModel sourceModel) {
    return Optional.ofNullable(sourceModel.getDataStation())
        .map(dataStation -> dataStation.getName())
        .orElse(sourceModel.getSource());
  }
}
