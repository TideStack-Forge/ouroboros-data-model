package com.ouroboros.data.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.model.valuetypes.RelatedValue;

/**
 * 字段路径解析工具。
 *
 * <p>负责基于模型元数据解析多段 FIELD 路径，输出终点字段及其所在模型。
 * Analyze 可用它做合法性校验，Orchestration 可用它读取受控路径的结构信息。
 */
public final class FieldPathResolver {

  private FieldPathResolver() {
  }

  public static Optional<ResolvedFieldPath> resolve(SExpression<?> fieldExpr, DataModel rootModel) {
    if (fieldExpr == null || fieldExpr.isEmpty() || fieldExpr.getOperator() != Operators.FIELD) {
      return Optional.empty();
    }

    List<String> pathSegments = new ArrayList<>();
    DataModel currentModel = rootModel;
    DataModel terminalSourceModel = rootModel;
    DataModelField terminalField = null;
    DataModel terminalRelatedModel = null;

    for (int index = 0; index < fieldExpr.getParams().size(); index++) {
      Object param = fieldExpr.getParam(index);
      if (!(param instanceof String)) {
        return Optional.empty();
      }

      String segment = (String) param;
      pathSegments.add(segment);
      terminalSourceModel = currentModel;
      terminalField = currentModel.getField(segment).orElse(null);
      if (terminalField == null) {
        return Optional.empty();
      }

      terminalRelatedModel = null;
      if (terminalField.getValueType() instanceof RelatedValue<?> relatedValue) {
        terminalRelatedModel = relatedValue.getReferenceModel().orElse(null);
      }

      if (index < fieldExpr.getParams().size() - 1) {
        if (terminalRelatedModel == null) {
          return Optional.empty();
        }
        currentModel = terminalRelatedModel;
      }
    }

    if (terminalField == null) {
      return Optional.empty();
    }

    return Optional.of(new ResolvedFieldPath(
        Collections.unmodifiableList(pathSegments),
        terminalSourceModel,
        terminalField,
        terminalRelatedModel
    ));
  }

  public static Optional<ResolvedFieldPath> resolve(SExpression<?> fieldExpr, DataModel rootModel,
                                                    QueryStatement statement) {
    return resolve(stripRootSourceQualifier(fieldExpr, statement), rootModel);
  }

  public static SExpression<?> stripRootSourceQualifier(SExpression<?> fieldExpr, QueryStatement statement) {
    if (fieldExpr == null || fieldExpr.isEmpty() || fieldExpr.getOperator() != Operators.FIELD
        || statement == null || statement.getFrom() == null || fieldExpr.getParams().size() <= 1) {
      return fieldExpr;
    }

    Object first = fieldExpr.getParam(0);
    if (!(first instanceof String firstSegment) || !isRootSourceName(firstSegment, statement.getFrom())) {
      return fieldExpr;
    }

    List<String> relativeSegments = fieldExpr.getParams().stream()
        .skip(1)
        .map(Object::toString)
        .toList();
    return SExpression.field(relativeSegments);
  }

  private static boolean isRootSourceName(String value, QueryStatement.TableSource from) {
    return value.equals(from.getAlias())
        || value.equals(from.getName())
        || value.equals(from.getTableName());
  }

  public static final class ResolvedFieldPath {
    private final List<String> pathSegments;
    private final DataModel terminalSourceModel;
    private final DataModelField terminalField;
    private final DataModel terminalRelatedModel;

    private ResolvedFieldPath(
        List<String> pathSegments,
        DataModel terminalSourceModel,
        DataModelField terminalField,
        DataModel terminalRelatedModel) {
      this.pathSegments = pathSegments;
      this.terminalSourceModel = terminalSourceModel;
      this.terminalField = terminalField;
      this.terminalRelatedModel = terminalRelatedModel;
    }

    public List<String> getPathSegments() {
      return pathSegments;
    }

    public String getFullPath() {
      return String.join(".", pathSegments);
    }

    public DataModel getTerminalSourceModel() {
      return terminalSourceModel;
    }

    public DataModelField getTerminalField() {
      return terminalField;
    }

    public Optional<DataModel> getTerminalRelatedModel() {
      return Optional.ofNullable(terminalRelatedModel);
    }
  }
}
