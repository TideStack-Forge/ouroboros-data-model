package com.ouroboros.data.model.plugins;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Either;
import io.vavr.control.Try;

import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.exception.DataModelException;
import com.ouroboros.data.exception.QueryExecutionException;
import com.ouroboros.data.exception.ReferenceConstraintViolationException;
import com.ouroboros.data.model.*;
import com.ouroboros.data.model.valuetypes.ModelValue;
import com.ouroboros.data.record.RecordList;

public class ReferenceCheckerDataModelPlugin implements DataModelPlugin {
  DataModel dataModel;
  List<Checker> referenceDataChecker;

  public ReferenceCheckerDataModelPlugin(DataModel dataModel, Set<String> referenceDataModelNames) {
    this.dataModel = dataModel;
    if (referenceDataModelNames != null) {
      this.referenceDataChecker = referenceDataModelNames.stream()
          .map(DataModelCenter::getDataModel)
          .filter(Optional::isPresent)
          .map(Optional::get)
          .map(Checker::new)
          .collect(Collectors.toList());
    }
  }

  private List<Checker> getReferenceDataChecker() {
    if (referenceDataChecker == null) {
      referenceDataChecker = DataModelCenter.getDataModelMap()
          .values().stream()
          .filter(model -> model.getFields().stream().anyMatch(field -> field.getValueType() instanceof ModelValue mv && dataModel.getFullName().equalsIgnoreCase(mv.getReferenceModelName())))
          .map(Checker::new)
          .collect(Collectors.toList());
    }
    return referenceDataChecker;
  }

  @Override
  public Try<Long> delete(SExpression<Boolean> where, DataModelPluginContext context) {
    var checkers = getReferenceDataChecker();
    if (checkers.isEmpty()) {
      return DataModelPlugin.super.delete(where, context);
    }

    QueryStatement query = QueryStatement.builder()
        .from(dataModel.getFullName(), dataModel.getRawName())
        .where(where)
        .build();
    var record = DataModelPlugin.super.query(query, context)
        .toEither()
        .mapLeft(e -> (DataModelException) new QueryExecutionException("删除前检查引用数据时查询失败", e))
        .fold(Try::<RecordList>failure, Try::success);

    return record.flatMap(recordList -> {
      if (recordList.isEmpty()) {
        return Try.success(0L);
      }
      var checkResults = checkers.stream()
          .map(checker -> checker.checkReferenceCount(recordList))
          .collect(Collectors.toList());

      var failures = checkResults.stream()
          .filter(Either::isLeft)
          .collect(Collectors.toList());
      if (!failures.isEmpty()) {
        var failure = failures.get(0);
        return Try.failure(failure.getLeft());
      }
      return DataModelPlugin.super.delete(where, context);
    });
  }

  public static class Builder implements DataModelPluginBuilder {

    @Override
    public boolean support(String name) {
      return "referenceChecker".equalsIgnoreCase(name);
    }

    @Override
    public Optional<DataModelPlugin> build(DataModel dataModel, Map<String, Object> config) {
      Set<String> checkModelNames = Optional.ofNullable(config.get("checkModelNames"))
          .filter(Collection.class::isInstance)
          .map(l -> (Collection<?>) l)
          .map(coll -> coll.stream().filter(Objects::nonNull).map(Object::toString).collect(Collectors.toSet()))
          .orElse(null);
      return Optional.of(new ReferenceCheckerDataModelPlugin(dataModel, checkModelNames));
    }
  }

  class Checker {
    private final DataModel referenceDataModel;
    private final Set<DataModelField> referenceFields;

    public Checker(DataModel referenceDataModel) {
      this.referenceDataModel = referenceDataModel;
      this.referenceFields = referenceDataModel.getFields()
          .stream()
          .filter(field -> field.getValueType() instanceof ModelValue mv && dataModel.getFullName().equalsIgnoreCase(mv.getReferenceModelName()))
          .collect(Collectors.toSet());
    }

    public Either<DataModelException, Long> checkReferenceCount(RecordList records) {
      var where = new TreeMap<>(String::compareToIgnoreCase);
      var conditions = referenceFields.stream()
          .map(field -> field.getValueType() instanceof ModelValue mv ? mv.getReferenceKey().map(r -> Tuple.of(field, r)) : Optional.<Tuple2<DataModelField, DataModelField>>empty())
          .filter(Optional::isPresent)
          .map(Optional::get)
          .map(tuple -> {
            var field = tuple._1();
            var refField = tuple._2();
            var valueType = refField.getValueType();
            var values = records.stream()
                .map(record -> record.get(refField.getName()))
                .map(valueType::toPersistentValue)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            if (values.isEmpty()) {
              return Collections.<String, Object>emptyMap();
            }
            if (values.size() == 1) {
              return Collections.singletonMap(field.getName(), values.iterator().next());
            }
            return Collections.<String, Object>singletonMap(field.getName(), values.stream().collect(Collectors.toList()));
          })
          .filter(m -> !m.isEmpty())
          .collect(Collectors.toList());
      if (conditions.isEmpty()) {
        return Either.right(0L);
      }
      if (conditions.size() == 1) {
        where.putAll(conditions.get(0));
      } else {
        where.put("or", conditions);
      }
      return this.referenceDataModel.count(where)
          .toEither()
          .mapLeft(e -> {
            var model = StringUtils.defaultIfBlank(dataModel.getLabel(), dataModel.getFullName());
            var refModel = StringUtils.defaultIfBlank(referenceDataModel.getLabel(), referenceDataModel.getFullName());
            return (DataModelException) new QueryExecutionException("检查" + model + "在" + refModel + "中引用的数据时发生异常", e);
          })
          .flatMap(count -> {
            if (count > 0) {
              var refModel = StringUtils.defaultIfBlank(referenceDataModel.getLabel(), referenceDataModel.getFullName());
              return Either.left(new ReferenceConstraintViolationException("在" + refModel + "中存在" + count + "条被引用的数据", refModel, count));
            }
            return Either.right(count);
          });
    }
  }
}
