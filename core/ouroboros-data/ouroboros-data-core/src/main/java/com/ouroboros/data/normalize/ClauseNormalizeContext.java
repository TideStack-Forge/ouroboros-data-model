package com.ouroboros.data.normalize;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.exception.NormalizeException;
import com.querydsl.core.types.Operator;

/**
 * 子句规范化上下文
 * <p>
 * 在特定子句（如WHERE、SELECT等）规范化过程中使用的上下文。
 * 作为子句作用域归一化的主入口，持有归一化核心逻辑。
 */
public class ClauseNormalizeContext implements NormalizeContext {

  private static final Logger logger = LoggerFactory.getLogger(ClauseNormalizeContext.class);

  private final QueryNormalizeContext queryContext;
  private final String clauseType;

  public ClauseNormalizeContext(QueryNormalizeContext queryContext, String clauseType) {
    this.queryContext = queryContext;
    this.clauseType = clauseType;
  }

  // 创建表达式规范化上下文
  public ExpressionNormalizeContext forExpression(String expressionPath) {
    return new ExpressionNormalizeContext(this, expressionPath);
  }

  // 获取父上下文
  public QueryNormalizeContext getQueryContext() {
    return queryContext;
  }

  // 上下文信息
  public String getClauseType() {
    return clauseType;
  }

  // === 归一化主逻辑 ===

  @Override
  public Try<SExpression<?>> normalizeExpression(Object rawExpression, String expressionPath) {
    ExpressionNormalizeContext exprContext = this.forExpression(expressionPath);

    if (rawExpression == null) {
      return Try.success(SExpression.empty());
    }
    if (rawExpression instanceof SExpression<?> sExpr) {
      return Try.success(sExpr);
    }

    for (RawExpressionNormalizer normalizer : queryContext.getExpressionNormalizers()) {
      if (normalizer.supports(clauseType, rawExpression.getClass())) {
        try {
          SExpression<?> result = normalizer.normalize(rawExpression, exprContext);
          if (result != null) {
            return Try.success(result);
          }
        } catch (NormalizeException e) {
          return Try.failure(e);
        } catch (Exception e) {
          logger.warn("Normalizer {} 处理表达式 '{}' 时发生意外异常: {}", normalizer.getClass().getSimpleName(), expressionPath, e.getMessage(), e);
          return Try.failure(new NormalizeException(
              "Normalizer " + normalizer.getClass().getSimpleName() + " 内部异常", e));
        }
      }
    }

    return Try.failure(new NormalizeException("不支持的表达式类型: " + rawExpression.getClass()));
  }

  @Override
  @SuppressWarnings("unchecked")
  public Try<SExpression<Boolean>> normalizeCondition(Object rawExpression, String expressionPath) {
    return Try.of(() -> {
      SExpression<?> sExpr = normalizeExpression(rawExpression, expressionPath).get();
      if (sExpr.isEmpty() && sExpr.getDataType() != Boolean.class) {
        return SExpression.empty(Boolean.class);
      }
      if (!Boolean.class.isAssignableFrom(sExpr.getDataType())) {
        throw new NormalizeException("表达式不是布尔类型");
      }
      return (SExpression<Boolean>) sExpr;
    });
  }

  @Override
  public Try<SExpression<?>> buildSExpression(Operator operator, List<SExpression<?>> params, String expressionPath) {
    ExpressionNormalizeContext exprContext = this.forExpression(expressionPath);

    for (SExpressionNormalizer normalizer : queryContext.getSExpressionNormalizers()) {
      if (normalizer.supports(operator)) {
        try {
          return Try.success(normalizer.normalize(operator, params, exprContext));
        } catch (NormalizeException e) {
          return Try.failure(e);
        } catch (Exception e) {
          logger.warn("SExpressionNormalizer {} 处理表达式 '{}' 时发生意外异常: {}", normalizer.getClass().getSimpleName(), expressionPath, e.getMessage(), e);
          return Try.failure(new NormalizeException(
              "SExpressionNormalizer " + normalizer.getClass().getSimpleName() + " 内部异常", e));
        }
      }
    }

    // 无 builder 匹配时，默认组装
    return Try.success(SExpression.create(operator, params));
  }

  /**
   * 批量规范化表达式列表
   */
  public Try<List<SExpression<?>>> normalizeExpressionList(List<?> rawExpressions, String basePath) {
    List<SExpression<?>> results = new ArrayList<>();
    for (int i = 0; i < rawExpressions.size(); i++) {
      String expressionPath = basePath + "[" + i + "]";
      Try<SExpression<?>> result = normalizeExpression(rawExpressions.get(i), expressionPath);
      if (result.isFailure()) {
        return Try.failure(result.getCause());
      }
      results.add(result.get());
    }
    return Try.success(results);
  }
}
