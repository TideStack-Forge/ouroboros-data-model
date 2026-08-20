package com.ouroboros.data.orchestration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.ouroboros.data.dsl.statement.PopulateClause;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.exception.StatementException;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.PopulateContext;
import com.ouroboros.data.model.valuetypes.CollectionValue;
import com.ouroboros.data.model.valuetypes.ModelValue;
import com.ouroboros.data.model.valuetypes.RelatedValue;
import com.ouroboros.data.query.DefaultProjectionFieldSupport;
import com.ouroboros.data.orchestration.strategy.PopulateField;
/**
 * populate 规划器。
 *
 * <p>负责从 statement/context 中提取 populate 输入，并转换为 orchestration 使用的
 * {@link PopulateField} 列表。
 */
class PopulatePlanner {
  List<PopulateContext> buildPopulateContexts(PopulateClause clause, DataModel model, String mainAlias) {
    List<PopulateContext> result = clause.getEntries().stream()
        .map(entry -> entry.options() == null
            ? new PopulateContext(model, mainAlias, entry.fieldName())
            : new PopulateContext(model, mainAlias, entry.fieldName(), entry.options()))
        .collect(Collectors.toList());
    List<PopulateContext> errors = result.stream().filter(p -> p.getPopulateField() == null).collect(Collectors.toList());
    if (!errors.isEmpty()) {
      throw new StatementException("Populate子句错误");
    }
    return result;
  }
  List<PopulateField> extractPopulateFields(OrchestrationContext context, DataModel rootModel) {
    List<PopulateContext> populateContexts = context.getPopulateContexts();
    if (populateContexts != null && !populateContexts.isEmpty()) {
      return buildFromPopulateContexts(populateContexts, rootModel);
    }
    QueryStatement statement = context.getMainStatement();
    List<PopulateField> result = new ArrayList<>();
    List<String> populateFieldNames = statement.getPopulate();
    if (populateFieldNames == null || populateFieldNames.isEmpty()) {
      return result;
    }
    for (String fieldName : populateFieldNames) {
      Optional<DataModelField> fieldOpt = rootModel.getField(fieldName);
      if (!fieldOpt.isPresent()) {
        continue;
      }
      DataModelField field = fieldOpt.get();
      Object valueType = field.getValueType();
      if (!(valueType instanceof RelatedValue)) {
        continue;
      }
      RelatedValue<?> relatedValue = (RelatedValue<?>) valueType;
      Optional<ResolvedAssociation> assocOpt = resolveAssociation(
          relatedValue, fieldName, rootModel.getName());
      if (!assocOpt.isPresent()) {
        continue;
      }
      ResolvedAssociation assoc = assocOpt.get();
      DataModel sourceModel = field.getDataModel();
      List<String> selectFields = assoc.relatedModel().getFields().stream()
          .filter(DefaultProjectionFieldSupport::isDirectDefaultProjectionField)
          .map(DataModelField::getName)
          .collect(Collectors.toList());
      PopulateField populateField = new PopulateField(
          fieldName, assoc.relatedModel(), assoc.localForeignKey(), assoc.remotePrimaryKey(),
          selectFields, null, sourceModel, assoc.relationType());
      result.add(populateField);
    }
    return result;
  }
  private List<PopulateField> buildFromPopulateContexts(List<PopulateContext> populateContexts, DataModel rootModel) {
    List<PopulateField> result = new ArrayList<>();
    for (PopulateContext ctx : populateContexts) {
      DataModelField field = ctx.getPopulateField();
      if (field == null) {
        continue;
      }
      Object valueType = field.getValueType();
      if (!(valueType instanceof RelatedValue)) {
        continue;
      }
      RelatedValue<?> relatedValue = (RelatedValue<?>) valueType;
      Optional<ResolvedAssociation> assocOpt = resolveAssociation(
          relatedValue, field.getName(), rootModel.getName());
      if (!assocOpt.isPresent()) {
        continue;
      }
      ResolvedAssociation assoc = assocOpt.get();
      PopulateContext.PopulateConfig config = ctx.getPopulateConfig();
      List<String> selectFields = config.getSelect() != null
          ? config.getSelect().stream().map(DataModelField::getName).collect(Collectors.toList())
          : assoc.relatedModel().getFields().stream()
              .filter(DefaultProjectionFieldSupport::isDirectDefaultProjectionField)
              .map(DataModelField::getName)
              .collect(Collectors.toList());
      PopulateField populateField = new PopulateField(
          field.getName(), assoc.relatedModel(), assoc.localForeignKey(), assoc.remotePrimaryKey(),
          selectFields, null, field.getDataModel(), assoc.relationType(),
          config.getWhere(), config.getLimit(), config.getOffset(), config.getPopulate());
      result.add(populateField);
    }
    return result;
  }
  private Optional<ResolvedAssociation> resolveAssociation(RelatedValue<?> relatedValue, String fieldPath, String modelName) {
    Optional<DataModel> relatedModelOpt = relatedValue.getReferenceModel();
    if (!relatedModelOpt.isPresent()) {
      return Optional.empty();
    }
    DataModel relatedModel = relatedModelOpt.get();
    String localForeignKey = relatedValue.getKey()
        .map(DataModelField::getName)
        .orElseThrow(() -> new StatementException(
            "关联键元数据缺失: fieldPath='" + fieldPath + "', model='" + modelName + "'"));
    String remotePrimaryKey = relatedValue.getReferenceKey()
        .map(DataModelField::getName)
        .orElseThrow(() -> new StatementException(
            "关联引用键元数据缺失: fieldPath='" + fieldPath + "', model='" + modelName + "'"));
    RelationType relationType;
    if (relatedValue instanceof ModelValue) {
      relationType = RelationType.TO_ONE;
    } else if (relatedValue instanceof CollectionValue) {
      relationType = RelationType.TO_MANY;
    } else {
      relationType = null;
    }
    return Optional.of(new ResolvedAssociation(relatedModel, localForeignKey, remotePrimaryKey, relationType));
  }
  private record ResolvedAssociation(
      DataModel relatedModel,
      String localForeignKey,
      String remotePrimaryKey,
      RelationType relationType
  ) {}
}
