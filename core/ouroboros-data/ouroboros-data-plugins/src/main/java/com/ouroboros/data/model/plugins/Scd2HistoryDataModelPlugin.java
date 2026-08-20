package com.ouroboros.data.model.plugins;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.exception.InvalidStatementException;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelCenter;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.DataModelPlugin;
import com.ouroboros.data.model.DataModelPluginBuilder;
import com.ouroboros.data.model.DataModelPluginContext;
import com.ouroboros.data.model.DataOperationIdentityProviders;
import com.ouroboros.data.record.Record;
import com.ouroboros.data.record.RecordList;
import com.ouroboros.data.util.DataJson;

public class Scd2HistoryDataModelPlugin implements DataModelPlugin {
  private static final String PLUGIN_NAME = "Scd2History";

  private final DataModel sourceModel;
  private final Scd2HistoryConfig config;
  private final Clock clock;
  private final DataModelField primaryKeyField;
  private final List<DataModelField> sourceFields;
  private final Set<String> ignoreFields;
  private final Function<String, Optional<DataModel>> modelResolver;
  private volatile DataModel historyModel;

  Scd2HistoryDataModelPlugin(DataModel sourceModel,
                             Scd2HistoryConfig config,
                             Clock clock,
                             Function<String, Optional<DataModel>> modelResolver) {
    this.sourceModel = sourceModel;
    this.config = config;
    this.clock = clock;
    this.primaryKeyField = extractPrimaryKeyField(sourceModel);
    this.sourceFields = sourceModel.getFields();
    this.ignoreFields = config.getIgnoreFields().stream().collect(Collectors.toSet());
    this.modelResolver = modelResolver;
  }

  @Override
  public Try<Record> insert(Map<String, Object> data, DataModelPluginContext context) {
    return DataModelPlugin.super.insert(data, context)
        .flatMap(insertedRecord -> insertHistoryVersion(insertedRecord, insertedRecord, "INSERT")
            .map(ignored -> insertedRecord));
  }

  @Override
  public Try<RecordList> batchInsert(List<Map<String, Object>> dataList, DataModelPluginContext context) {
    return context.getNextPlugin()
        .batchInsert(dataList, context.getNextPluginContext())
        .flatMap(insertedRecords -> insertHistoryVersions(insertedRecords, "INSERT")
            .map(ignored -> insertedRecords));
  }

  @Override
  public Try<Long> update(SExpression<Boolean> where, Map<String, Object> data, DataModelPluginContext context) {
    return queryOldRecords(where).flatMap(oldRecords -> {
      if (oldRecords.size() > config.getMaxRows()) {
        return Try.failure(new InvalidStatementException("SCD2 历史更新命中的记录数过多，请缩小更新范围"));
      }

      return DataModelPlugin.super.update(where, data, context).flatMap(rows -> {
        if (rows <= 0 || oldRecords.isEmpty()) {
          return Try.success(rows);
        }

        return queryNewRecords(oldRecords).flatMap(newRecords -> synchronizeUpdatedVersions(oldRecords, newRecords).map(ignored -> rows));
      });
    });
  }

  @Override
  public Try<Long> delete(SExpression<Boolean> where, DataModelPluginContext context) {
    return queryOldRecords(where).flatMap(oldRecords ->
        DataModelPlugin.super.delete(where, context).flatMap(rows -> {
          if (rows <= 0 || oldRecords.isEmpty()) {
            return Try.success(rows);
          }

          return oldRecords.stream()
              .map(record -> closeAndInsertDeleteVersion(record))
              .filter(Try::isFailure)
              .map(Try::getCause)
              .findFirst()
              .<Try<Long>>map(Try::failure)
              .orElseGet(() -> Try.success(rows));
        })
    );
  }

  private Try<RecordList> queryOldRecords(SExpression<Boolean> where) {
    QueryStatement statement = QueryStatement.builder()
        .from(sourceModel.getFullName(), sourceModel.getRawName())
        .where(where)
        .build();
    return sourceModel.withoutPlugins().query(statement)
        .map(this::snapshotRecords);
  }

  private Try<RecordList> queryNewRecords(RecordList oldRecords) {
    List<Object> ids = oldRecords.stream()
        .map(record -> Scd2HistoryDiff.readValue(record, primaryKeyField))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    return sourceModel.withoutPlugins().query(ids);
  }

  private RecordList snapshotRecords(RecordList records) {
    List<Map<String, Object>> snapshots = records.stream()
        .map(record -> {
          Map<String, Object> snapshot = new LinkedHashMap<String, Object>();
          record.forEach(snapshot::put);
          return snapshot;
        })
        .collect(Collectors.toList());
    return RecordList.of(snapshots);
  }

  private Try<Void> synchronizeUpdatedVersions(RecordList oldRecords, RecordList newRecords) {
    Map<Object, Record> newRecordMap = new LinkedHashMap<Object, Record>();
    newRecords.forEach(record -> newRecordMap.put(Scd2HistoryDiff.readValue(record, primaryKeyField), record));

    return oldRecords.stream()
        .map(oldRecord -> {
          Record matchedNewRecord = newRecordMap.get(oldRecord.get(primaryKeyField.getName()));
          if (matchedNewRecord == null) {
            matchedNewRecord = newRecordMap.get(Scd2HistoryDiff.readValue(oldRecord, primaryKeyField));
          }
          if (matchedNewRecord == null) {
            return Try.success((Void) null);
          }

          final Record newRecord = matchedNewRecord;
          Scd2HistoryDiff diff = Scd2HistoryDiff.between(sourceFields, oldRecord, newRecord, ignoreFields);
          if (!diff.hasBusinessChanges()) {
            return Try.success((Void) null);
          }

          return closeCurrentVersion(Scd2HistoryDiff.readValue(newRecord, primaryKeyField))
              .flatMap(ignored -> insertHistoryVersion(newRecord, newRecord, "UPDATE", diff))
              .map(inserted -> null);
        })
        .filter(Try::isFailure)
        .map(Try::getCause)
        .findFirst()
        .<Try<Void>>map(Try::failure)
        .orElseGet(() -> Try.success(null));
  }

  private Try<Void> closeAndInsertDeleteVersion(Record oldRecord) {
    return closeCurrentVersion(Scd2HistoryDiff.readValue(oldRecord, primaryKeyField))
        .flatMap(ignored -> insertHistoryVersion(oldRecord, oldRecord, "DELETE", emptyDiff()))
        .map(inserted -> null);
  }

  private Try<Long> closeCurrentVersion(Object businessId) {
    return getHistoryModel().flatMap(historyModel -> {
      Map<String, Object> where = new LinkedHashMap<String, Object>();
      where.put(config.getBusinessKeyField(), businessId);
      where.put(config.getIsCurrentField(), true);

      Map<String, Object> data = new LinkedHashMap<String, Object>();
      data.put(config.getValidToField(), persistedValue(historyModel, config.getValidToField(), now()));
      data.put(config.getIsCurrentField(), persistedValue(historyModel, config.getIsCurrentField(), false));

      return historyModel.update(where, data);
    });
  }

  private Try<Record> insertHistoryVersion(Record snapshot, Record currentRecord, String operation) {
    return insertHistoryVersion(snapshot, currentRecord, operation, emptyDiff());
  }

  private Try<Record> insertHistoryVersion(Record snapshot, Record currentRecord, String operation, Scd2HistoryDiff diff) {
    return getHistoryModel().flatMap(historyModel -> {
      Map<String, Object> version = buildHistoryVersion(historyModel, snapshot, currentRecord, operation, diff);
      return historyModel.insert(version);
    });
  }

  private Try<RecordList> insertHistoryVersions(RecordList records, String operation) {
    if (records.isEmpty()) {
      return Try.success(RecordList.empty());
    }
    List<HistoryVersionInput> versions = records.stream()
        .map(record -> new HistoryVersionInput(record, record, operation, emptyDiff()))
        .collect(Collectors.toList());
    return insertHistoryVersions(versions);
  }

  private Try<RecordList> insertHistoryVersions(List<HistoryVersionInput> versions) {
    if (versions.isEmpty()) {
      return Try.success(RecordList.empty());
    }
    return getHistoryModel().flatMap(historyModel -> {
      List<Map<String, Object>> rows = versions.stream()
          .map(version -> buildHistoryVersion(
              historyModel,
              version.snapshot,
              version.currentRecord,
              version.operation,
              version.diff))
          .collect(Collectors.toList());
      return historyModel.batchInsert(rows);
    });
  }

  private Map<String, Object> buildHistoryVersion(DataModel historyModel,
                                                  Record snapshot,
                                                  Record currentRecord,
                                                  String operation,
                                                  Scd2HistoryDiff diff) {
    Map<String, Object> version = new LinkedHashMap<String, Object>();
    sourceFields.stream()
        .filter(field -> field.getValueType().isPhysical())
        .filter(field -> !field.getName().equals(primaryKeyField.getName()))
        .forEach(field -> version.put(field.getName(), fieldValue(historyModel, snapshot, field.getName())));

    version.put(config.getBusinessKeyField(), persistedValue(historyModel, config.getBusinessKeyField(), Scd2HistoryDiff.readValue(currentRecord, primaryKeyField)));
    version.put(config.getValidFromField(), persistedValue(historyModel, config.getValidFromField(), now()));
    version.put(config.getValidToField(), persistedValue(historyModel, config.getValidToField(), null));
    version.put(config.getIsCurrentField(), persistedValue(historyModel, config.getIsCurrentField(), true));
    version.put(config.getOpField(), persistedValue(historyModel, config.getOpField(), operation));
    version.put(config.getOperatorField(), persistedValue(historyModel, config.getOperatorField(),
        DataOperationIdentityProviders.findCurrentOperator().orElse(null)));
    if (config.isStoreDiff()) {
      version.put(config.getChangedFieldsField(), diffValue(historyModel, config.getChangedFieldsField(), diff.getChangedFields()));
      version.put(config.getChangeSetField(), diffValue(historyModel, config.getChangeSetField(), diff.toChangeSet()));
    }
    return version;
  }

  private Object diffValue(DataModel historyModel, String fieldName, Object rawValue) {
    String jsonValue = DataJson.toJsonString(rawValue);
    return historyModel.getField(fieldName)
        .map(DataModelField::getValueType)
        .map(valueType -> String.class.equals(valueType.getType())
            ? valueType.toPersistentValue(jsonValue)
            : valueType.toPersistentValue(rawValue))
        .orElse(jsonValue);
  }

  private Object fieldValue(DataModel historyModel, Record record, String fieldName) {
    Optional<DataModelField> sourceField = sourceFields.stream()
        .filter(field -> field.getName().equalsIgnoreCase(fieldName))
        .findFirst();
    Object value = sourceField
        .map(field -> Scd2HistoryDiff.readValue(record, field))
        .orElse(record == null ? null : record.get(fieldName));
    return historyModel.getField(fieldName)
        .map(DataModelField::getValueType)
        .map(valueType -> valueType.toPersistentValue(value))
        .orElse(value);
  }

  private Object persistedValue(DataModel historyModel, String fieldName, Object value) {
    return historyModel.getField(fieldName)
        .map(DataModelField::getValueType)
        .map(valueType -> valueType.toPersistentValue(value))
        .orElse(value);
  }

  private LocalDateTime now() {
    return LocalDateTime.now(clock);
  }

  private static DataModelField extractPrimaryKeyField(DataModel sourceModel) {
    if (sourceModel.getPrimaryKeys().size() != 1) {
      throw new IllegalArgumentException("source model must have a single primary key");
    }
    return sourceModel.getPrimaryKeys().get(0);
  }

  private Try<DataModel> getHistoryModel() {
    if (historyModel != null) {
      return Try.success(historyModel);
    }

    synchronized (this) {
      if (historyModel != null) {
        return Try.success(historyModel);
      }

      Optional<DataModel> resolved = modelResolver.apply(config.getHistoryModelFullName());
      if (!resolved.isPresent()) {
        resolved = sourceModel.getDataStation().getDataModel(config.getHistoryModelFullName());
      }
      if (!resolved.isPresent()) {
        resolved = DataModelCenter.getDataModel(config.getHistoryModelFullName());
      }
      if (!resolved.isPresent()) {
        return Try.failure(new InvalidStatementException("SCD2 历史模型不存在: " + config.getHistoryModelFullName()));
      }

      DataModel resolvedModel = resolved.get();
      if (resolvedModel.getExtraProp(Boolean.class, "enableScd2History").orElse(false)) {
        return Try.failure(new InvalidStatementException("SCD2 历史模型自身不能开启 enableScd2History"));
      }

      List<String> missingFields = config.validateAgainst(sourceModel, resolvedModel);
      if (!missingFields.isEmpty()) {
        return Try.failure(new InvalidStatementException("SCD2 历史模型缺少必要字段: " + DataJson.toJsonString(missingFields)));
      }

      historyModel = resolvedModel.withoutPlugins();
      return Try.success(historyModel);
    }
  }

  private static Scd2HistoryDiff emptyDiff() {
    return Scd2HistoryDiff.between(Collections.emptyList(), Record.of(Collections.emptyMap()), Record.of(Collections.emptyMap()), Collections.emptySet());
  }

  private static final class HistoryVersionInput {
    private final Record snapshot;
    private final Record currentRecord;
    private final String operation;
    private final Scd2HistoryDiff diff;

    private HistoryVersionInput(Record snapshot, Record currentRecord, String operation, Scd2HistoryDiff diff) {
      this.snapshot = snapshot;
      this.currentRecord = currentRecord;
      this.operation = operation;
      this.diff = diff;
    }
  }

  public static class Builder implements DataModelPluginBuilder {
    private final Clock clock;
    private final Function<String, Optional<DataModel>> modelResolver;

    public Builder() {
      this(Clock.systemDefaultZone(), DataModelCenter::getDataModel);
    }

    Builder(Clock clock, Function<String, Optional<DataModel>> modelResolver) {
      this.clock = clock;
      this.modelResolver = modelResolver;
    }

    @Override
    public boolean support(String name) {
      return PLUGIN_NAME.equalsIgnoreCase(name);
    }

    @Override
    public Optional<DataModelPlugin> build(DataModel dataModel, Map<String, Object> configMap) {
      if (dataModel.getPrimaryKeys().size() != 1) {
        throw new IllegalArgumentException("SCD2 历史插件仅支持单主键模型");
      }

      Try<Scd2HistoryConfig> config = Scd2HistoryConfig.from(configMap);
      if (config.isFailure()) {
        throw propagateBuildFailure("SCD2 历史插件配置不合法", config.getCause());
      }

      Optional<DataModel> historyModel = modelResolver.apply(config.get().getHistoryModelFullName());
      if (historyModel.isPresent()) {
        validateResolvedHistoryModel(dataModel, config.get(), historyModel.get());
      }

      return Optional.<DataModelPlugin>of(new Scd2HistoryDataModelPlugin(dataModel, config.get(), clock, modelResolver));
    }

    private void validateResolvedHistoryModel(DataModel sourceModel, Scd2HistoryConfig config, DataModel historyModel) {
      List<String> missingFields = config.validateAgainst(sourceModel, historyModel);
      if (!missingFields.isEmpty()) {
        throw new IllegalArgumentException("SCD2 历史模型缺少必要字段: " + DataJson.toJsonString(missingFields));
      }

      if (historyModel.getExtraProp(Boolean.class, "enableScd2History").orElse(false)) {
        throw new IllegalArgumentException("SCD2 历史模型自身不能开启 enableScd2History");
      }
    }

    private IllegalArgumentException propagateBuildFailure(String message, Throwable cause) {
      if (cause instanceof IllegalArgumentException illegalArgumentException) {
        return illegalArgumentException;
      }
      return new IllegalArgumentException(message, cause);
    }
  }
}
