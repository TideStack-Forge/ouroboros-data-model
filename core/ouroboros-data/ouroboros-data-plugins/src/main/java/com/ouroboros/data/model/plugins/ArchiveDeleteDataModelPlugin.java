package com.ouroboros.data.model.plugins;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelCenter;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.DataModelPlugin;
import com.ouroboros.data.model.DataModelPluginBuilder;
import com.ouroboros.data.model.DataModelPluginContext;
import com.ouroboros.data.model.deletepolicy.DeleteTransactionCoordinatorCenter;
import com.ouroboros.data.model.deletepolicy.ArchiveModelContract;
import com.ouroboros.data.model.deletepolicy.ArchiveRecordAssembler;
import com.ouroboros.data.normalize.QueryNormalizeContext;
import com.ouroboros.data.record.Record;
import com.ouroboros.data.record.RecordList;
import com.ouroboros.data.util.DataConverters;

public class ArchiveDeleteDataModelPlugin implements DataModelPlugin {

  private static final String PLUGIN_NAME = "ArchiveDelete";
  private static final String CONFIG_ARCHIVE_MODEL = "archiveModel";
  private static final String CONFIG_BATCH_SIZE = "batchSize";
  private static final String CONFIG_SOURCE_FIELD_MAPPINGS = "sourceFieldMappings";
  private static final int DEFAULT_BATCH_SIZE = 200;
  private static final QueryNormalizeContext NORMALIZE_CONTEXT = QueryNormalizeContext.builder()
      .withDefaultNormalizers()
      .build();

  private final DataModel sourceModel;
  private final String archiveModelName;
  private final Supplier<Optional<DataModel>> archiveModelSupplier;
  private final int batchSize;
  private final Map<String, String> sourceFieldMappings;

  public ArchiveDeleteDataModelPlugin(DataModel sourceModel, DataModel archiveModel, int batchSize) {
    this(sourceModel, archiveModel.getFullName(), () -> Optional.of(archiveModel), batchSize, Collections.emptyMap());
  }

  private ArchiveDeleteDataModelPlugin(DataModel sourceModel, String archiveModelName,
                                       Supplier<Optional<DataModel>> archiveModelSupplier,
                                       int batchSize,
                                       Map<String, String> sourceFieldMappings) {
    if (batchSize <= 0) {
      throw new IllegalArgumentException("batchSize must be greater than 0");
    }
    this.sourceModel = sourceModel;
    this.archiveModelName = archiveModelName;
    this.archiveModelSupplier = archiveModelSupplier;
    this.batchSize = batchSize;
    this.sourceFieldMappings = sourceFieldMappings == null
        ? Collections.emptyMap()
        : new LinkedHashMap<String, String>(sourceFieldMappings);
  }

  @Override
  public Try<Long> delete(SExpression<Boolean> where, DataModelPluginContext context) {
    return querySourceRecords(where)
        .flatMap(records -> {
          if (records == null || records.isEmpty()) {
            return Try.success(0L);
          }
          return resolveArchiveModel()
              .flatMap(archiveModel -> {
                Map<String, String> effectiveMappings = resolveEffectiveSourceFieldMappings(archiveModel);
                return ArchiveModelContract.validate(sourceModel, archiveModel, effectiveMappings)
                  .flatMap(ignored -> DeleteTransactionCoordinatorCenter.getCoordinator(sourceModel, archiveModel)
                      .flatMap(coordinator -> coordinator.execute(sourceModel, archiveModel,
                          () -> copyThenDelete(records, archiveModel, effectiveMappings, context))));
              });
        });
  }

  private Try<RecordList> querySourceRecords(SExpression<Boolean> where) {
    DataModel queryModel = sourceModel.withoutPlugins();
    QueryStatement statement = QueryStatement.builder()
        .from(sourceModel.getFullName(), sourceModel.getRawName())
        .where(where)
        .build();
    return queryModel.query(statement);
  }

  private Try<DataModel> resolveArchiveModel() {
    return Try.of(archiveModelSupplier::get)
        .flatMap(optional -> optional
            .map(Try::success)
            .orElseGet(() -> Try.failure(new IllegalArgumentException("Archive model not found: " + archiveModelName))));
  }

  private Try<Long> copyThenDelete(RecordList records, DataModel archiveModel, Map<String, String> effectiveMappings,
                                   DataModelPluginContext context) {
    Date deletedAt = new Date();
    String deleteOperationId = UUID.randomUUID().toString();
    long deletedCount = 0L;

    for (int index = 0; index < records.size(); index += batchSize) {
      int end = Math.min(index + batchSize, records.size());
      List<Record> batch = new ArrayList<Record>(records.subList(index, end));
      List<Map<String, Object>> archiveRows = batch.stream()
          .map(record -> ArchiveRecordAssembler.assemble(sourceModel, record, deletedAt, null, deleteOperationId, effectiveMappings))
          .collect(Collectors.toList());

      Try<RecordList> insertResult = archiveModel.batchInsert(archiveRows);
      if (insertResult.isFailure()) {
        return Try.failure(insertResult.getCause());
      }

      Try<Long> deleteResult = NORMALIZE_CONTEXT.forClause("WHERE")
          .normalizeCondition(buildBatchWhere(batch), "root")
          .flatMap(batchWhere -> DataModelPlugin.super.delete(batchWhere, context));
      if (deleteResult.isFailure()) {
        return Try.failure(deleteResult.getCause());
      }
      deletedCount += deleteResult.get().longValue();
    }

    return Try.success(deletedCount);
  }

  private Map<String, String> resolveEffectiveSourceFieldMappings(DataModel archiveModel) {
    Map<String, String> effectiveMappings = new LinkedHashMap<String, String>(sourceFieldMappings);
    List<String> archiveFieldNames = archiveModel.getFields().stream()
        .map(DataModelField::getName)
        .collect(Collectors.toList());
    for (DataModelField sourceField : sourceModel.getFields()) {
      String sourceFieldName = sourceField.getName();
      if (effectiveMappings.containsKey(sourceFieldName)) {
        continue;
      }
      String inferredName = "source" + upperCaseFirst(sourceFieldName);
      if (containsIgnoreCase(archiveFieldNames, inferredName)) {
        effectiveMappings.put(sourceFieldName, inferredName);
        continue;
      }
      if (containsIgnoreCase(archiveFieldNames, sourceFieldName)) {
        continue;
      }
    }
    return effectiveMappings;
  }

  private boolean containsIgnoreCase(List<String> names, String candidate) {
    return names.stream().anyMatch(name -> name != null && name.equalsIgnoreCase(candidate));
  }

  private String upperCaseFirst(String value) {
    if (value == null || value.isEmpty()) {
      return "";
    }
    return value.length() == 1 ? value.toUpperCase() : value.substring(0, 1).toUpperCase() + value.substring(1);
  }

  private Map<String, Object> buildBatchWhere(List<Record> batch) {
    List<DataModelField> primaryKeys = sourceModel.getPrimaryKeys();
    if (primaryKeys.size() == 1) {
      String primaryKey = primaryKeys.get(0).getName();
      List<Object> ids = batch.stream()
          .map(record -> getRecordValueIgnoreCase(record, primaryKey))
          .collect(Collectors.toList());
      return Collections.<String, Object>singletonMap(primaryKey, ids);
    }

    List<DataModelField> conditionFields = primaryKeys.isEmpty() ? sourceModel.getFields() : primaryKeys;
    List<Map<String, Object>> conditions = batch.stream()
        .map(record -> buildCondition(record, conditionFields))
        .collect(Collectors.toList());
    return Collections.<String, Object>singletonMap("or", (Object) conditions);
  }

  private Map<String, Object> buildCondition(Record record, List<DataModelField> conditionFields) {
    Map<String, Object> condition = new LinkedHashMap<String, Object>();
    for (DataModelField field : conditionFields) {
      condition.put(field.getName(), getRecordValueIgnoreCase(record, field.getName()));
    }
    return condition;
  }

  private Object getRecordValueIgnoreCase(Map<String, ?> record, String fieldName) {
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

  public static class Builder implements DataModelPluginBuilder {

    @Override
    public boolean support(String name) {
      return PLUGIN_NAME.equalsIgnoreCase(name);
    }

    @Override
    public Optional<DataModelPlugin> build(DataModel dataModel, Map<String, Object> config) {
      String archiveModelName = Optional.ofNullable(config)
          .map(c -> c.get(CONFIG_ARCHIVE_MODEL))
          .map(String::valueOf)
          .map(String::trim)
          .orElse("");
      if (archiveModelName.isEmpty()) {
        throw new IllegalArgumentException("ArchiveDelete.archiveModel is required");
      }

      int batchSize = Optional.ofNullable(config)
          .map(c -> c.get(CONFIG_BATCH_SIZE))
          .map(DataConverters::toInteger)
          .filter(value -> value != null)
          .map(Integer::intValue)
          .orElse(DEFAULT_BATCH_SIZE);

      return Optional.of(new ArchiveDeleteDataModelPlugin(
          dataModel,
          archiveModelName,
          () -> DataModelCenter.getDataModel(archiveModelName),
          batchSize,
          readSourceFieldMappings(config)
      ));
    }

    private Map<String, String> readSourceFieldMappings(Map<String, Object> config) {
      Object rawValue = Optional.ofNullable(config)
          .map(c -> c.get(CONFIG_SOURCE_FIELD_MAPPINGS))
          .orElse(null);
      if (!(rawValue instanceof Map<?, ?>)) {
        return Collections.emptyMap();
      }
      Map<String, String> mappings = new LinkedHashMap<String, String>();
      for (Map.Entry<?, ?> entry : ((Map<?, ?>) rawValue).entrySet()) {
        String sourceField = Optional.ofNullable(entry.getKey()).map(String::valueOf).map(String::trim).orElse("");
        String archiveField = Optional.ofNullable(entry.getValue()).map(String::valueOf).map(String::trim).orElse("");
        if (!sourceField.isEmpty() && !archiveField.isEmpty()) {
          mappings.put(sourceField, archiveField);
        }
      }
      return mappings;
    }
  }
}
