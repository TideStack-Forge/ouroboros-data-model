package com.ouroboros.data.orchestration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ouroboros.data.dsl.ExtOps;
import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.exception.StatementException;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.FieldPathResolver;
import com.ouroboros.data.model.FieldPathResolver.ResolvedFieldPath;
import com.ouroboros.data.model.valuetypes.CollectionValue;
import com.ouroboros.data.model.valuetypes.ModelValue;
import com.ouroboros.data.model.valuetypes.RelatedValue;
import com.querydsl.core.types.Operator;
/**
 * 关联条件规划器。
 *
 * <p>负责扫描当前 statement 层级中的 relation 条件，并产出 orchestration
 * 后续 rewrite 所需的 {@link AnalysisResult}。
 */
class RelationConditionPlanner {
  private static final Logger logger = LoggerFactory.getLogger(RelationConditionPlanner.class);
  @SuppressWarnings("unchecked")
  AnalysisResult analyze(QueryStatement statement, DataModel rootModel) {
    logger.debug("Analyzing statement for model: {}", rootModel.getName());
    List<CrossSourceCondition> crossSourceConditions = new ArrayList<>();
    List<SameSourceCondition> sameSourceToOneConditions = new ArrayList<>();
    List<SameSourceCondition> sameSourceToManyConditions = new ArrayList<>();
    SExpression<Boolean> where = statement.getWhere();
    if (where.isEmpty()) {
      logger.debug("No WHERE clause, returning empty analysis result");
      return new AnalysisResult(crossSourceConditions, sameSourceToOneConditions, sameSourceToManyConditions);
    }
    collectCurrentLevelRelationConditions(
        where,
        rootModel,
        crossSourceConditions,
        sameSourceToOneConditions,
        sameSourceToManyConditions
    );
    collectImplicitToOneConditions(
        where,
        rootModel,
        null,
        crossSourceConditions,
        sameSourceToOneConditions
    );
    logger.debug("Analysis complete: {} cross-source, {} same-source ToOne, {} same-source ToMany",
        crossSourceConditions.size(),
        sameSourceToOneConditions.size(),
        sameSourceToManyConditions.size());
    return new AnalysisResult(crossSourceConditions, sameSourceToOneConditions, sameSourceToManyConditions);
  }
  @SuppressWarnings("unchecked")
  private void collectCurrentLevelRelationConditions(
      SExpression<?> expr,
      DataModel rootModel,
      List<CrossSourceCondition> crossSourceConditions,
      List<SameSourceCondition> sameSourceToOneConditions,
      List<SameSourceCondition> sameSourceToManyConditions) {
    if (expr == null || expr.isEmpty()) {
      return;
    }
    Operator op = expr.getOperator();
    if (isRelationOperator(op)) {
      Optional<ResolvedRelationField> resolvedFieldOpt = resolveRelationField(rootModel, expr.getParamAsSExpression(0));
      if (!resolvedFieldOpt.isPresent()) {
        logger.warn("无法解析关联字段表达式: {}, 跳过该条件", expr.getParam(0));
        return;
      }
      ResolvedRelationField resolvedField = resolvedFieldOpt.get();
      String fieldPath = resolvedField.fieldPath;
      String sourceFieldPath = getSourceFieldPath(fieldPath);
      SExpression<Boolean> condition = (SExpression<Boolean>) expr.getParamAsSExpression(1);
      logger.debug("Found relation condition: {} with operator {}", fieldPath, op);
      DataModelField field = resolvedField.field;
      Object valueType = field.getValueType();
      if (!(valueType instanceof RelatedValue)) {
        logger.warn("字段 {} 的 ValueType 不是 RelatedValue, 跳过该条件", fieldPath);
        return;
      }
      RelatedValue<?> relatedValue = (RelatedValue<?>) valueType;
      DataModel relatedModel = resolvedField.relatedModel;
      RelationType relationType;
      if (valueType instanceof ModelValue) {
        relationType = RelationType.TO_ONE;
      } else if (valueType instanceof CollectionValue) {
        relationType = RelationType.TO_MANY;
      } else {
        logger.warn("字段 {} 的 ValueType 类型未知: {}, 跳过该条件", fieldPath, valueType.getClass().getName());
        return;
      }
      String localKeyName = relatedValue.getKey()
          .map(DataModelField::getName)
          .orElseThrow(() -> new StatementException(
              "关联键元数据缺失: fieldPath='" + fieldPath + "', model='" + rootModel.getName() + "'"));
      String referenceKeyName = relatedValue.getReferenceKey()
          .map(DataModelField::getName)
          .orElseThrow(() -> new StatementException(
              "关联引用键元数据缺失: fieldPath='" + fieldPath + "', model='" + rootModel.getName() + "'"));
      JoinCapabilityResult joinResult = JoinCapabilities.canJoin(resolvedField.sourceModel, relatedModel);
      if (!joinResult.isJoinable()) {
        crossSourceConditions.add(new CrossSourceCondition(
            fieldPath, sourceFieldPath, relatedModel, condition, localKeyName, referenceKeyName, false));
        logger.debug("Separate-strategy relation condition: {} (reason={})",
            fieldPath, joinResult.getReason());
      } else if (relationType == RelationType.TO_ONE) {
        boolean requiresLeftJoin = detectRequiresLeftJoin(condition);
        sameSourceToOneConditions.add(new SameSourceCondition(
            fieldPath, sourceFieldPath, relatedModel, condition, RelationType.TO_ONE, requiresLeftJoin,
            localKeyName, referenceKeyName));
        logger.debug("Joinable ToOne condition: {} (requiresLeftJoin={})", fieldPath, requiresLeftJoin);
      } else {
        sameSourceToManyConditions.add(new SameSourceCondition(
            fieldPath, sourceFieldPath, relatedModel, condition, RelationType.TO_MANY, false,
            localKeyName, referenceKeyName));
        logger.debug("Joinable ToMany condition: {}", fieldPath);
      }
      return;
    }
    for (Object param : expr.getParams()) {
      if (param instanceof SExpression<?> childExpr) {
        collectCurrentLevelRelationConditions(
            childExpr,
            rootModel,
            crossSourceConditions,
            sameSourceToOneConditions,
            sameSourceToManyConditions
        );
      }
    }
  }
  private void collectImplicitToOneConditions(
      SExpression<?> expr,
      DataModel currentModel,
      String parentFieldPath,
      List<CrossSourceCondition> crossSourceConditions,
      List<SameSourceCondition> sameSourceToOneConditions) {
    if (expr == null || expr.isEmpty() || isRelationOperator(expr.getOperator())) {
      return;
    }
    Optional<BareToOneConditionInfo> bareConditionOpt = extractBareToOneConditionInfo(expr, currentModel, parentFieldPath);
    if (bareConditionOpt.isPresent()) {
      BareToOneConditionInfo bareCondition = bareConditionOpt.get();
      String fieldPath = bareCondition.fieldPath;
      String sourceFieldPath = getSourceFieldPath(fieldPath);
      JoinCapabilityResult joinResult = JoinCapabilities.canJoin(bareCondition.sourceModel, bareCondition.relatedModel);
      if (!joinResult.isJoinable()) {
        crossSourceConditions.add(new CrossSourceCondition(
            fieldPath, sourceFieldPath, bareCondition.relatedModel, bareCondition.condition,
            bareCondition.localKeyName, bareCondition.referenceKeyName, true));
        return;
      }
      boolean requiresLeftJoin = detectRequiresLeftJoin(bareCondition.condition);
      sameSourceToOneConditions.add(new SameSourceCondition(
          fieldPath, sourceFieldPath, bareCondition.relatedModel, bareCondition.condition,
          RelationType.TO_ONE, requiresLeftJoin, bareCondition.localKeyName, bareCondition.referenceKeyName));
      collectImplicitToOneConditions(
          bareCondition.condition,
          bareCondition.relatedModel,
          bareCondition.fieldPath,
          crossSourceConditions,
          sameSourceToOneConditions
      );
      return;
    }
    for (Object param : expr.getParams()) {
      if (param instanceof SExpression<?> childExpr) {
        collectImplicitToOneConditions(childExpr, currentModel, parentFieldPath, crossSourceConditions, sameSourceToOneConditions);
      }
    }
  }
  private Optional<BareToOneConditionInfo> extractBareToOneConditionInfo(
      SExpression<?> expr, DataModel currentModel, String parentFieldPath) {
    if (expr == null || expr.isEmpty() || expr.getDataType() != Boolean.class || isRelationOperator(expr.getOperator())) {
      return Optional.empty();
    }
    List<BareToOneFieldRewrite> rewrites = new ArrayList<>();
    AtomicBoolean unsupported = new AtomicBoolean(false);
    collectBareToOneFieldRewrites(expr, currentModel, rewrites, unsupported);
    if (unsupported.get() || rewrites.isEmpty()) {
      return Optional.empty();
    }
    BareToOneFieldRewrite firstRewrite = rewrites.get(0);
    String relationFieldPath = firstRewrite.relationFieldPath();
    String fullFieldPath = parentFieldPath == null || parentFieldPath.isEmpty()
        ? relationFieldPath
        : parentFieldPath + "." + relationFieldPath;
    boolean sameRelation = rewrites.stream()
        .allMatch(rewrite -> relationFieldPath.equals(rewrite.relationFieldPath()));
    if (!sameRelation) {
      return Optional.empty();
    }
    DataModelField relationField = currentModel.getField(relationFieldPath).orElse(null);
    if (relationField == null || !(relationField.getValueType() instanceof ModelValue modelValue)) {
      return Optional.empty();
    }
    DataModel relatedModel = modelValue.getReferenceModel().orElse(null);
    if (relatedModel == null) {
      return Optional.empty();
    }
    String localKeyName = modelValue.getKey()
        .map(DataModelField::getName)
        .orElseThrow(() -> new StatementException(
            "关联键元数据缺失: fieldPath='" + relationFieldPath + "', model='" + currentModel.getName() + "'"));
    String referenceKeyName = modelValue.getReferenceKey()
        .map(DataModelField::getName)
        .orElseThrow(() -> new StatementException(
            "关联引用键元数据缺失: fieldPath='" + relationFieldPath + "', model='" + currentModel.getName() + "'"));
    @SuppressWarnings("unchecked")
    SExpression<Boolean> rewrittenInner = (SExpression<Boolean>) expr.transform((node, context) -> {
      Optional<BareToOneFieldRewrite> rewriteOpt = resolveBareToOneFieldRewrite(currentModel, node);
      if (rewriteOpt.isPresent() && relationFieldPath.equals(rewriteOpt.get().relationFieldPath())) {
        return rewriteOpt.get().relativeFieldExpr();
      }
      return node;
    });
    return Optional.of(new BareToOneConditionInfo(
        fullFieldPath, currentModel, relatedModel, rewrittenInner, localKeyName, referenceKeyName));
  }
  private Optional<ResolvedRelationField> resolveRelationField(DataModel rootModel, SExpression<?> fieldExpr) {
    Optional<ResolvedFieldPath> resolvedOpt = FieldPathResolver.resolve(fieldExpr, rootModel);
    if (!resolvedOpt.isPresent()) {
      return Optional.empty();
    }
    ResolvedFieldPath resolved = resolvedOpt.get();
    DataModelField currentField = resolved.getTerminalField();
    if (!(currentField.getValueType() instanceof RelatedValue<?> relatedValue)) {
      return Optional.empty();
    }
    DataModel relatedModel = resolved.getTerminalRelatedModel().orElse(null);
    if (relatedModel == null) {
      return Optional.empty();
    }
    return Optional.of(new ResolvedRelationField(
        resolved.getFullPath(),
        resolved.getTerminalSourceModel(),
        currentField,
        relatedModel,
        relatedValue));
  }
  private boolean detectRequiresLeftJoin(SExpression<Boolean> condition) {
    AtomicBoolean result = new AtomicBoolean(false);
    condition.walk((expr, context) -> {
      Operator op = expr.getOperator();
      if (op == Operators.IS_NULL) {
        result.set(true);
      } else if (op == Operators.NOT) {
        result.set(true);
      } else if (op == Operators.IN) {
        for (Object param : expr.getParams()) {
          if (param == null) {
            result.set(true);
            break;
          }
        }
      }
    });
    return result.get();
  }
  private void collectBareToOneFieldRewrites(
      SExpression<?> expr,
      DataModel currentModel,
      List<BareToOneFieldRewrite> rewrites,
      AtomicBoolean unsupported) {
    if (expr == null || expr.isEmpty() || unsupported.get() || isRelationOperator(expr.getOperator())) {
      return;
    }
    if (expr.getOperator() == Operators.FIELD) {
      Optional<BareToOneFieldRewrite> rewriteOpt = resolveBareToOneFieldRewrite(currentModel, expr);
      if (rewriteOpt.isPresent()) {
        rewrites.add(rewriteOpt.get());
      } else {
        unsupported.set(true);
      }
      return;
    }
    for (Object param : expr.getParams()) {
      if (param instanceof SExpression<?> childExpr) {
        collectBareToOneFieldRewrites(childExpr, currentModel, rewrites, unsupported);
      }
    }
  }
  private Optional<BareToOneFieldRewrite> resolveBareToOneFieldRewrite(DataModel currentModel, SExpression<?> expr) {
    if (expr == null || expr.isEmpty() || expr.getOperator() != Operators.FIELD) {
      return Optional.empty();
    }
    if (expr.getParams().size() < 2) {
      return Optional.empty();
    }
    if (!(expr.getParam(0) instanceof String relationFieldName)) {
      return Optional.empty();
    }
    for (Object param : expr.getParams()) {
      if (!(param instanceof String)) {
        return Optional.empty();
      }
    }
    DataModelField relationField = currentModel.getField(relationFieldName).orElse(null);
    if (relationField == null || !(relationField.getValueType() instanceof ModelValue modelValue)) {
      return Optional.empty();
    }
    DataModel relatedModel = modelValue.getReferenceModel().orElse(null);
    if (relatedModel == null) {
      return Optional.empty();
    }
    List<String> remainingSegments = new ArrayList<>();
    expr.getParams().subList(1, expr.getParams().size()).forEach(param -> remainingSegments.add(String.valueOf(param)));
    return Optional.of(new BareToOneFieldRewrite(
        relationFieldName,
        SExpression.field(remainingSegments)
    ));
  }
  private boolean isRelationOperator(Operator operator) {
    return operator == ExtOps.REL_ANY || operator == ExtOps.REL_ALL || operator == ExtOps.REL_NONE;
  }
  private String getSourceFieldPath(String fieldPath) {
    int lastDot = fieldPath.lastIndexOf('.');
    return lastDot < 0 ? null : fieldPath.substring(0, lastDot);
  }
  private static class ResolvedRelationField {
    private final String fieldPath;
    private final DataModel sourceModel;
    private final DataModelField field;
    private final DataModel relatedModel;
    @SuppressWarnings("unused")
    private final RelatedValue<?> relatedValue;
    private ResolvedRelationField(String fieldPath, DataModel sourceModel, DataModelField field,
                                  DataModel relatedModel, RelatedValue<?> relatedValue) {
      this.fieldPath = fieldPath;
      this.sourceModel = sourceModel;
      this.field = field;
      this.relatedModel = relatedModel;
      this.relatedValue = relatedValue;
    }
  }
  private record BareToOneFieldRewrite(
      String relationFieldPath,
      SExpression<?> relativeFieldExpr
  ) {}
  private record BareToOneConditionInfo(
      String fieldPath,
      DataModel sourceModel,
      DataModel relatedModel,
      SExpression<Boolean> condition,
      String localKeyName,
      String referenceKeyName
  ) {}
}
