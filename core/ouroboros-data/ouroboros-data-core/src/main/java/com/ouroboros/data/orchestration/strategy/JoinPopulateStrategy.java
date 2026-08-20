package com.ouroboros.data.orchestration.strategy;

import java.util.Arrays;
import java.util.List;

import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.orchestration.rewriter.PopulateJoinRewriter;
import com.ouroboros.data.orchestration.step.QueryStep;
import com.ouroboros.data.orchestration.step.ResultTransformStep;
import com.ouroboros.data.orchestration.step.StatementRewriteStep;

/**
 * JOIN 填充策略
 *
 * <p>职责：
 * <ul>
 *   <li>创建 ResultTransformStep 处理 JOIN 查询结果</li>
 *   <li>适用于同数据源 ToOne 关联</li>
 * </ul>
 *
 * <p>执行流程：
 * <ul>
 *   <li>主查询已通过 JOIN 获取扁平化数据（dept__id, dept__name）</li>
 *   <li>ResultTransformStep 提取扁平化字段</li>
 *   <li>ResultTransformStep 创建转换器（扁平 → 嵌套）</li>
 *   <li>转换器存入 Context</li>
 * </ul>
 *
 * <p>设计要点：
 * <ul>
 *   <li>不执行额外查询（数据已在主查询中）</li>
 *   <li>只创建结构转换器（扁平 → 嵌套）</li>
 *   <li>支持 NULL 值处理</li>
 * </ul>
 *
 * @author Claude Code
 */
public class JoinPopulateStrategy implements PopulateStrategy {

  @Override
  public List<QueryStep> createSteps(PopulateField populate, String sourceStepName) {
    String relationTargetName = populate.relatedModel().getFullName();
    if (relationTargetName == null || relationTargetName.trim().isEmpty()) {
      relationTargetName = populate.relatedModel().getName();
    }

    // 1. 创建 StatementRewriteStep + PopulateJoinRewriter（改写 SQL：添加 LEFT JOIN + 前缀 SELECT）
    PopulateJoinRewriter rewriter = new PopulateJoinRewriter(
        populate.fieldName(),
        relationTargetName,
        populate.selectFields(),
        populate.localForeignKey(),
        populate.remotePrimaryKey()
    );
    StatementRewriteStep rewriteStep = new StatementRewriteStep(
        "populate_rewrite_" + populate.fieldName(),
        rewriter
    );

    // 2. 创建 ResultTransformStep（将扁平字段提取为嵌套对象）
    String flatFieldPrefix = populate.fieldName() + "__";
    ResultTransformStep resultTransformStep = new ResultTransformStep(
        "result_transform_" + populate.fieldName(),
        populate.fieldName(),
        flatFieldPrefix,
        populate.selectFields(),
        populate.relatedModel().getFields().stream()
            .filter(field -> populate.selectFields().contains(field.getName()))
            .collect(java.util.stream.Collectors.toMap(
                DataModelField::getName,
                DataModelField::getValueType,
                (left, right) -> left
            ))
    );

    return Arrays.asList(rewriteStep, resultTransformStep);
  }
}
