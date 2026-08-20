package com.ouroboros.data.model.deletepolicy;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.vavr.control.Try;

import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.station.DataStation;

public class ArchiveModelContractTest {

  @Test
  public void testShouldFailWhenRequiredArchiveFieldsAreMissing() {
    DataModel sourceModel = mockModel("demo.User", "default", Arrays.asList("id", "name"), Collections.singletonList("id"));

    for (String requiredField : ArchiveModelContract.getRequiredMetadataFields()) {
      List<String> archiveFields = new ArrayList<String>();
      archiveFields.add("id");
      archiveFields.add("name");
      archiveFields.addAll(ArchiveModelContract.getRequiredMetadataFields());
      archiveFields.remove(requiredField);

      DataModel archiveModel = mockModel("archive.UserArchive", "archive", archiveFields, Collections.singletonList("id"));

      Try<Void> result = ArchiveModelContract.validate(sourceModel, archiveModel);

      assertTrue(result.isFailure(), requiredField);
      assertTrue(result.getCause().getMessage().contains(requiredField), requiredField);
    }
  }

  @Test
  public void testShouldFailWhenArchiveModelDoesNotCoverSourceFields() {
    DataModel sourceModel = mockModel("demo.User", "default", Arrays.asList("id", "name"), Collections.singletonList("id"));
    DataModel archiveModel = mockModel(
        "archive.UserArchive",
        "archive",
        Arrays.asList(
            "id",
            "sourceModel",
            "sourceDataStation",
            "sourcePrimaryKey",
            "deletedAt",
            "deletedBy",
            "deleteOperationId"
        ),
        Collections.singletonList("id")
    );

    Try<Void> result = ArchiveModelContract.validate(sourceModel, archiveModel);

    assertTrue(result.isFailure());
    assertTrue(result.getCause().getMessage().contains("name"));
  }

  @Test
  public void testShouldPassWhenArchiveModelContainsAllSourceAndMetadataFields() {
    DataModel sourceModel = mockModel("demo.User", "default", Arrays.asList("id", "name"), Collections.singletonList("id"));
    DataModel archiveModel = mockModel(
        "archive.UserArchive",
        "archive",
        Arrays.asList(
            "id",
            "name",
            "sourceModel",
            "sourceDataStation",
            "sourcePrimaryKey",
            "deletedAt",
            "deletedBy",
            "deleteOperationId"
        ),
        Collections.singletonList("id")
    );

    Try<Void> result = ArchiveModelContract.validate(sourceModel, archiveModel);

    assertTrue(result.isSuccess());
  }

  private DataModel mockModel(String fullName, String dataStationName, List<String> fieldNames, List<String> primaryKeyNames) {
    DataModel model = mock(DataModel.class);
    @SuppressWarnings("rawtypes")
    DataStation dataStation = mock(DataStation.class);
    List<DataModelField> fields = mockFields(fieldNames);
    List<DataModelField> primaryKeys = mockFields(primaryKeyNames);
    when(dataStation.getName()).thenReturn(dataStationName);
    when(model.getFullName()).thenReturn(fullName);
    when(model.getDataStation()).thenReturn(dataStation);
    when(model.getFields()).thenReturn(fields);
    when(model.getPrimaryKeys()).thenReturn(primaryKeys);
    return model;
  }

  private List<DataModelField> mockFields(List<String> fieldNames) {
    List<DataModelField> fields = new ArrayList<DataModelField>();
    for (String fieldName : fieldNames) {
      DataModelField field = mock(DataModelField.class);
      when(field.getName()).thenReturn(fieldName);
      fields.add(field);
    }
    return fields;
  }
}
