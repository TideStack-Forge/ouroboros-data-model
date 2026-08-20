package com.ouroboros.data.model.deletepolicy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import io.vavr.control.Try;

import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelField;

public final class ArchiveModelContract {

  public static final String FIELD_SOURCE_MODEL = "sourceModel";
  public static final String FIELD_SOURCE_DATA_STATION = "sourceDataStation";
  public static final String FIELD_SOURCE_PRIMARY_KEY = "sourcePrimaryKey";
  public static final String FIELD_DELETED_AT = "deletedAt";
  public static final String FIELD_DELETED_BY = "deletedBy";
  public static final String FIELD_DELETE_OPERATION_ID = "deleteOperationId";

  private static final List<String> REQUIRED_METADATA_FIELDS = Collections.unmodifiableList(Arrays.asList(
      FIELD_SOURCE_MODEL,
      FIELD_SOURCE_DATA_STATION,
      FIELD_SOURCE_PRIMARY_KEY,
      FIELD_DELETED_AT,
      FIELD_DELETED_BY,
      FIELD_DELETE_OPERATION_ID
  ));

  private ArchiveModelContract() {
  }

  public static List<String> getRequiredMetadataFields() {
    return REQUIRED_METADATA_FIELDS;
  }

  public static Try<Void> validate(DataModel sourceModel, DataModel archiveModel) {
    return validate(sourceModel, archiveModel, Collections.emptyMap());
  }

  public static Try<Void> validate(DataModel sourceModel, DataModel archiveModel, Map<String, String> sourceFieldMappings) {
    return Try.run(() -> {
      if (sourceModel == null) {
        throw new IllegalArgumentException("sourceModel is required");
      }
      if (archiveModel == null) {
        throw new IllegalArgumentException("archiveModel is required");
      }

      Set<String> archiveFieldNames = new TreeSet<String>(String::compareToIgnoreCase);
      archiveModel.getFields().stream()
          .map(DataModelField::getName)
          .forEach(archiveFieldNames::add);

      for (String requiredField : REQUIRED_METADATA_FIELDS) {
        if (!archiveFieldNames.contains(requiredField)) {
          throw new IllegalArgumentException("Archive model must contain field: " + requiredField);
        }
      }

      List<String> missingSourceFields = new ArrayList<String>();
      for (DataModelField sourceField : sourceModel.getFields()) {
        String archiveFieldName = resolveArchiveFieldName(sourceField.getName(), sourceFieldMappings);
        if (!archiveFieldNames.contains(archiveFieldName)) {
          missingSourceFields.add(archiveFieldName);
        }
      }
      if (!missingSourceFields.isEmpty()) {
        throw new IllegalArgumentException("Archive model is missing source fields: " + String.join(", ", missingSourceFields));
      }
    });
  }

  private static String resolveArchiveFieldName(String sourceFieldName, Map<String, String> sourceFieldMappings) {
    if (sourceFieldMappings == null) {
      return sourceFieldName;
    }
    String mappedName = sourceFieldMappings.get(sourceFieldName);
    if (mappedName == null || mappedName.trim().isEmpty()) {
      return sourceFieldName;
    }
    return mappedName;
  }
}
