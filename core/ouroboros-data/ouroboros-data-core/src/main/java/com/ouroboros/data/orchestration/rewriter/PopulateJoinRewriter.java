package com.ouroboros.data.orchestration.rewriter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.orchestration.OrchestrationContext;
/**
 * Populate JOIN 改写器
 *
 * <p>职责：
 * <ul>
 *   <li>为同源 ToOne Populate 字段添加 LEFT JOIN 子句</li>
 *   <li>添加带前缀的 SELECT 字段（如 dept__id, dept__name）</li>
 *   <li>始终使用 LEFT JOIN（Populate 允许 NULL）</li>
 * </ul>
 *
 * <p>与 JoinStatementRewriter 的区别：
 * <ul>
 *   <li>JoinStatementRewriter：改写 WHERE 条件中的字段引用</li>
 *   <li>PopulateJoinRewriter：添加 SELECT 字段（带前缀），不改写 WHERE</li>
 * </ul>
 *
 * @author Claude Code
 */
public record PopulateJoinRewriter(
    String relationField,
    String relationTargetName,
    List<String> selectFields,
    String localForeignKey,
    String remotePrimaryKey
) implements StatementRewriter {
  private static final Logger logger = LoggerFactory.getLogger(PopulateJoinRewriter.class);
  @Override
  public QueryStatement rewrite(QueryStatement statement, OrchestrationContext context) {
    logger.debug("Rewriting Populate JOIN for field: {}", relationField);
    // 1. 生成 JOIN 别名
    String joinAlias = generateJoinAlias(relationField);
    logger.debug("Generated JOIN alias: {}", joinAlias);
    // 2. 添加 LEFT JOIN 子句
    QueryStatement withJoin = addLeftJoin(statement, joinAlias);
    // 3. 添加带前缀的 SELECT 字段
    QueryStatement rewritten = addPrefixedSelectFields(withJoin, joinAlias);
    logger.debug("Populate JOIN rewrite completed for: {}", relationField);
    return rewritten;
  }
  private String generateJoinAlias(String fieldPath) {
    return fieldPath.replace('.', '_');
  }
  private QueryStatement addLeftJoin(QueryStatement statement, String alias) {
    String sourceAlias = statement.getFrom() != null ? statement.getFrom().getName() : null;
    SExpression<Boolean> onCondition = SExpression.create(
        Operators.EQ,
        sourceAlias == null || sourceAlias.trim().isEmpty()
            ? SExpression.field(localForeignKey)
            : SExpression.field(sourceAlias, localForeignKey),
        SExpression.field(alias, remotePrimaryKey)
    );
    return statement.getBuilder()
        .leftJoin(relationTargetName, alias, onCondition)
        .build();
  }
  private QueryStatement addPrefixedSelectFields(QueryStatement statement, String alias) {
    if (selectFields == null || selectFields.isEmpty()) {
      return statement;
    }
    QueryStatement.QueryStatementBuilder builder = statement.getBuilder();
    for (String field : selectFields) {
      String aliasedFieldName = relationField + "__" + field;
      SExpression<?> fieldSelect = SExpression.alias(
          SExpression.field(alias, field),
          aliasedFieldName
      );
      builder.addSelect(fieldSelect);
    }
    return builder.build();
  }
}
