package com.ouroboros.data.normalize;

import java.util.*;
import java.util.stream.Collectors;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.exception.DataModelException;
import com.ouroboros.data.exception.NormalizeException;
import com.ouroboros.data.normalize.builders.AggregationExpressionBuilder;
import com.ouroboros.data.normalize.builders.CollectionExpressionBuilder;
import com.ouroboros.data.normalize.builders.ComparisonExpressionBuilder;
import com.ouroboros.data.normalize.builders.ConstantExpressionBuilder;
import com.ouroboros.data.normalize.builders.FieldExpressionBuilder;
import com.ouroboros.data.normalize.builders.LogicCombinationBuilder;
import com.ouroboros.data.normalize.builders.RelationExpressionBuilder;
import com.ouroboros.data.normalize.builders.StringExpressionBuilder;
import com.ouroboros.data.normalize.expressionnormalizers.ListExpressionNormalizer;
import com.ouroboros.data.normalize.expressionnormalizers.MapExpressionNormalizer;
import com.ouroboros.data.normalize.expressionnormalizers.QueryFacadeExpressionNormalizer;
import com.ouroboros.data.normalize.normalizers.DefaultDistinctNormalizer;
import com.ouroboros.data.normalize.normalizers.DefaultFromNormalizer;
import com.ouroboros.data.normalize.normalizers.DefaultGroupNormalizer;
import com.ouroboros.data.normalize.normalizers.DefaultHavingNormalizer;
import com.ouroboros.data.normalize.normalizers.DefaultJoinNormalizer;
import com.ouroboros.data.normalize.normalizers.DefaultOrderNormalizer;
import com.ouroboros.data.normalize.normalizers.DefaultPaginationNormalizer;
import com.ouroboros.data.normalize.normalizers.DefaultSelectNormalizer;
import com.ouroboros.data.normalize.normalizers.DefaultUnionNormalizer;
import com.ouroboros.data.normalize.normalizers.DefaultWhereNormalizer;
import com.ouroboros.data.normalize.normalizers.DefaultWithNormalizer;
import com.querydsl.core.types.Operator;

/**
 * 查询规范化上下文
 * <p>
 * 提供查询规范化过程中需要的上下文信息和扩展点配置。
 * normalizeExpression/normalizeCondition/buildSExpression 委托给 ClauseNormalizeContext。
 */
public class QueryNormalizeContext implements NormalizeContext {

  // === 实例字段 ===

  // 扩展点列表（按添加顺序维护）
  private final List<ClauseNormalizer> clauseNormalizers;
  private final List<RawExpressionNormalizer> expressionNormalizers;
  private final List<SExpressionNormalizer> sExpressionNormalizers;

  // 构造函数
  private QueryNormalizeContext(Builder builder) {
    this.clauseNormalizers = Collections.unmodifiableList(new ArrayList<>(builder.clauseNormalizers));
    this.expressionNormalizers = Collections.unmodifiableList(new ArrayList<>(builder.expressionNormalizers));
    this.sExpressionNormalizers = Collections.unmodifiableList(new ArrayList<>(builder.sExpressionNormalizers));
  }

  // 静态构建器方法
  public static Builder builder() {
    return new Builder();
  }

  // 创建子句规范化上下文
  public ClauseNormalizeContext forClause(String clauseType) {
    return new ClauseNormalizeContext(this, clauseType);
  }

  public List<ClauseNormalizer> getClauseNormalizers() {
    return clauseNormalizers;
  }

  public List<RawExpressionNormalizer> getExpressionNormalizers() {
    return expressionNormalizers;
  }

  public List<SExpressionNormalizer> getSExpressionNormalizers() {
    return sExpressionNormalizers;
  }

  /**
   * @deprecated 使用 {@link #getSExpressionNormalizers()}。
   */
  @Deprecated
  public List<SExpressionBuilder> getSExpressionBuilders() {
    return sExpressionNormalizers.stream()
        .filter(SExpressionBuilder.class::isInstance)
        .map(SExpressionBuilder.class::cast)
        .collect(Collectors.toList());
  }

  // === NormalizeContext 接口实现（委托给默认 EXPRESSION 子句上下文）===

  private static final String DEFAULT_CLAUSE_TYPE = "EXPRESSION";

  @Override
  public Try<SExpression<?>> normalizeExpression(Object rawExpression, String expressionPath) {
    return forClause(DEFAULT_CLAUSE_TYPE).normalizeExpression(rawExpression, expressionPath);
  }

  @Override
  public Try<SExpression<Boolean>> normalizeCondition(Object rawExpression, String expressionPath) {
    return forClause(DEFAULT_CLAUSE_TYPE).normalizeCondition(rawExpression, expressionPath);
  }

  @Override
  public Try<SExpression<?>> buildSExpression(Operator operator, List<SExpression<?>> params, String expressionPath) {
    return forClause(DEFAULT_CLAUSE_TYPE).buildSExpression(operator, params, expressionPath);
  }

  // === 核心规范化方法 ===

  /**
   * 规范化查询Map为QueryStatement（使用默认builder）
   */
  public Try<QueryStatement> normalizeQuery(Map<String, ?> queryMap) {
    return normalizeQuery(queryMap, QueryStatement.builder());
  }

  /**
   * 规范化查询Map为QueryStatement（使用自定义builder）
   */
  public Try<QueryStatement> normalizeQuery(Map<String, ?> queryMap,
                                            QueryStatement.QueryStatementBuilder builder) {
    ClauseNormalizeContext clauseContext = this.forClause("QUERY");

    // 按顺序执行所有子句规范化器
    Try<QueryStatement.QueryStatementBuilder> result = Try.success(builder);
    for (ClauseNormalizer normalizer : this.getClauseNormalizers()) {
      if (normalizer.supports("QUERY")) {
        result = result.flatMap(b -> {
          try {
            return Try.success(normalizer.normalize(queryMap, b, clauseContext));
          } catch (NormalizeException e) {
            return Try.failure(e);
          } catch (DataModelException e) {
            return Try.failure(e);
          } catch (Exception e) {
            return Try.failure(new NormalizeException(
                "ClauseNormalizer " + normalizer.getClass().getSimpleName() + " 内部异常", e));
          }
        });
      }
    }

    return result.map(QueryStatement.QueryStatementBuilder::build);
  }

  /**
   * 查询规范化上下文构建器
   */
  public static class Builder {
    private List<ClauseNormalizer> clauseNormalizers = new ArrayList<>();
    private List<RawExpressionNormalizer> expressionNormalizers = new ArrayList<>();
    private List<SExpressionNormalizer> sExpressionNormalizers = new ArrayList<>();

    // 基础配置

    /**
     * 添加默认的规范化器
     */
    public Builder withDefaultNormalizers() {
      addDefaultExpressionNormalizers();
      addDefaultSExpressionNormalizers();
      addDefaultClauseNormalizers();
      return this;
    }

    /**
     * 添加默认的S表达式构建器
     */
    private void addDefaultSExpressionNormalizers() {
      this.sExpressionNormalizers.add(new FieldExpressionBuilder());
      this.sExpressionNormalizers.add(new ConstantExpressionBuilder());
      this.sExpressionNormalizers.add(new LogicCombinationBuilder());
      this.sExpressionNormalizers.add(new ComparisonExpressionBuilder());
      this.sExpressionNormalizers.add(new StringExpressionBuilder());
      this.sExpressionNormalizers.add(new CollectionExpressionBuilder());
      this.sExpressionNormalizers.add(new RelationExpressionBuilder());
      this.sExpressionNormalizers.add(new AggregationExpressionBuilder());
    }

    /**
     * 添加默认的表达式规范化器
     */
    private void addDefaultExpressionNormalizers() {
      // 按优先级添加（List优先级高于Map）
      this.expressionNormalizers.add(new QueryFacadeExpressionNormalizer());
      this.expressionNormalizers.add(new ListExpressionNormalizer());
      this.expressionNormalizers.add(new MapExpressionNormalizer());
    }

    /**
     * 添加默认的子句规范化器
     * <p>
     * 按照旧代码 DefaultQueryNormalizer 的顺序添加，确保处理顺序正确：
     * With -> Group -> Having -> From -> Join -> Where -> Distinct -> Select -> Order -> Pagination -> Union
     */
    private void addDefaultClauseNormalizers() {
      // 1. WithNormalizer - CTE（Common Table Expression）
      this.clauseNormalizers.add(new DefaultWithNormalizer());

      // 2. GroupNormalizer - GROUP BY子句
      this.clauseNormalizers.add(new DefaultGroupNormalizer());

      // 3. HavingNormalizer - HAVING子句
      this.clauseNormalizers.add(new DefaultHavingNormalizer());

      // 4. FromNormalizer - FROM子句（必需）
      this.clauseNormalizers.add(new DefaultFromNormalizer());

      // 5. JoinNormalizer - JOIN子句
      this.clauseNormalizers.add(new DefaultJoinNormalizer());

      // 6. WhereNormalizer - WHERE子句
      this.clauseNormalizers.add(new DefaultWhereNormalizer());

      // 7. DistinctNormalizer - DISTINCT子句
      this.clauseNormalizers.add(new DefaultDistinctNormalizer());

      // 8. SelectNormalizer - SELECT子句
      this.clauseNormalizers.add(new DefaultSelectNormalizer());

      // 9. OrderNormalizer - ORDER BY子句
      this.clauseNormalizers.add(new DefaultOrderNormalizer());

      // 10. PaginationNormalizer - LIMIT/OFFSET分页
      this.clauseNormalizers.add(new DefaultPaginationNormalizer());

      // 11. UnionNormalizer - UNION/UNION ALL子句
      this.clauseNormalizers.add(new DefaultUnionNormalizer());
    }

    // === ClauseNormalizer 添加方法 ===

    public Builder addClauseNormalizer(ClauseNormalizer normalizer) {
      this.clauseNormalizers.add(normalizer);
      return this;
    }

    public Builder addClauseNormalizerBeforeAll(ClauseNormalizer normalizer) {
      this.clauseNormalizers.add(0, normalizer);
      return this;
    }

    public Builder addClauseNormalizerAfterAll(ClauseNormalizer normalizer) {
      this.clauseNormalizers.add(normalizer);
      return this;
    }

    public Builder addClauseNormalizerBefore(ClauseNormalizer normalizer, String normalizerName) {
      int index = findNormalizerIndexByName(clauseNormalizers, normalizerName);
      if (index >= 0) {
        this.clauseNormalizers.add(index, normalizer);
      } else {
        this.clauseNormalizers.add(0, normalizer); // 找不到就放最前面
      }
      return this;
    }

    public Builder addClauseNormalizerBefore(ClauseNormalizer normalizer, Class<? extends ClauseNormalizer> normalizerClass) {
      int index = findNormalizerIndexByClass(clauseNormalizers, normalizerClass);
      if (index >= 0) {
        this.clauseNormalizers.add(index, normalizer);
      } else {
        this.clauseNormalizers.add(0, normalizer); // 找不到就放最前面
      }
      return this;
    }

    public Builder addClauseNormalizerAfter(ClauseNormalizer normalizer, String normalizerName) {
      int index = findNormalizerIndexByName(clauseNormalizers, normalizerName);
      if (index >= 0) {
        this.clauseNormalizers.add(index + 1, normalizer);
      } else {
        this.clauseNormalizers.add(normalizer); // 找不到就放最后面
      }
      return this;
    }

    public Builder addClauseNormalizerAfter(ClauseNormalizer normalizer, Class<? extends ClauseNormalizer> normalizerClass) {
      int index = findNormalizerIndexByClass(clauseNormalizers, normalizerClass);
      if (index >= 0) {
        this.clauseNormalizers.add(index + 1, normalizer);
      } else {
        this.clauseNormalizers.add(normalizer); // 找不到就放最后面
      }
      return this;
    }

    // === RawExpressionNormalizer 添加方法 ===

    public Builder addExpressionNormalizer(RawExpressionNormalizer normalizer) {
      this.expressionNormalizers.add(normalizer);
      return this;
    }

    public Builder addExpressionNormalizerBeforeAll(RawExpressionNormalizer normalizer) {
      this.expressionNormalizers.add(0, normalizer);
      return this;
    }

    public Builder addExpressionNormalizerAfterAll(RawExpressionNormalizer normalizer) {
      this.expressionNormalizers.add(normalizer);
      return this;
    }

    public Builder addExpressionNormalizerBefore(RawExpressionNormalizer normalizer, String normalizerName) {
      int index = findNormalizerIndexByName(expressionNormalizers, normalizerName);
      if (index >= 0) {
        this.expressionNormalizers.add(index, normalizer);
      } else {
        this.expressionNormalizers.add(0, normalizer);
      }
      return this;
    }

    public Builder addExpressionNormalizerBefore(RawExpressionNormalizer normalizer, Class<? extends RawExpressionNormalizer> normalizerClass) {
      int index = findNormalizerIndexByClass(expressionNormalizers, normalizerClass);
      if (index >= 0) {
        this.expressionNormalizers.add(index, normalizer);
      } else {
        this.expressionNormalizers.add(0, normalizer);
      }
      return this;
    }

    public Builder addExpressionNormalizerAfter(RawExpressionNormalizer normalizer, String normalizerName) {
      int index = findNormalizerIndexByName(expressionNormalizers, normalizerName);
      if (index >= 0) {
        this.expressionNormalizers.add(index + 1, normalizer);
      } else {
        this.expressionNormalizers.add(normalizer);
      }
      return this;
    }

    public Builder addExpressionNormalizerAfter(RawExpressionNormalizer normalizer, Class<? extends RawExpressionNormalizer> normalizerClass) {
      int index = findNormalizerIndexByClass(expressionNormalizers, normalizerClass);
      if (index >= 0) {
        this.expressionNormalizers.add(index + 1, normalizer);
      } else {
        this.expressionNormalizers.add(normalizer);
      }
      return this;
    }

    // === SExpressionNormalizer 添加方法 ===

    public Builder addSExpressionNormalizer(SExpressionNormalizer normalizer) {
      this.sExpressionNormalizers.add(normalizer);
      return this;
    }

    public Builder addSExpressionNormalizerBeforeAll(SExpressionNormalizer normalizer) {
      this.sExpressionNormalizers.add(0, normalizer);
      return this;
    }

    public Builder addSExpressionNormalizerAfterAll(SExpressionNormalizer normalizer) {
      this.sExpressionNormalizers.add(normalizer);
      return this;
    }

    public Builder addSExpressionNormalizerBefore(SExpressionNormalizer normalizer, String normalizerName) {
      int index = findSExpressionNormalizerIndexByName(sExpressionNormalizers, normalizerName);
      if (index >= 0) {
        this.sExpressionNormalizers.add(index, normalizer);
      } else {
        this.sExpressionNormalizers.add(0, normalizer);
      }
      return this;
    }

    public Builder addSExpressionNormalizerBefore(SExpressionNormalizer normalizer, Class<? extends SExpressionNormalizer> normalizerClass) {
      int index = findSExpressionNormalizerIndexByClass(sExpressionNormalizers, normalizerClass);
      if (index >= 0) {
        this.sExpressionNormalizers.add(index, normalizer);
      } else {
        this.sExpressionNormalizers.add(0, normalizer);
      }
      return this;
    }

    public Builder addSExpressionNormalizerAfter(SExpressionNormalizer normalizer, String normalizerName) {
      int index = findSExpressionNormalizerIndexByName(sExpressionNormalizers, normalizerName);
      if (index >= 0) {
        this.sExpressionNormalizers.add(index + 1, normalizer);
      } else {
        this.sExpressionNormalizers.add(normalizer);
      }
      return this;
    }

    public Builder addSExpressionNormalizerAfter(SExpressionNormalizer normalizer, Class<? extends SExpressionNormalizer> normalizerClass) {
      int index = findSExpressionNormalizerIndexByClass(sExpressionNormalizers, normalizerClass);
      if (index >= 0) {
        this.sExpressionNormalizers.add(index + 1, normalizer);
      } else {
        this.sExpressionNormalizers.add(normalizer);
      }
      return this;
    }

    /**
     * @deprecated 使用 {@link #addSExpressionNormalizer(SExpressionNormalizer)}。
     */
    @Deprecated
    public Builder addSExpressionBuilder(SExpressionBuilder builder) {
      return addSExpressionNormalizer(builder);
    }

    /**
     * @deprecated 使用 {@link #addSExpressionNormalizerBeforeAll(SExpressionNormalizer)}。
     */
    @Deprecated
    public Builder addSExpressionBuilderBeforeAll(SExpressionBuilder builder) {
      return addSExpressionNormalizerBeforeAll(builder);
    }

    /**
     * @deprecated 使用 {@link #addSExpressionNormalizerAfterAll(SExpressionNormalizer)}。
     */
    @Deprecated
    public Builder addSExpressionBuilderAfterAll(SExpressionBuilder builder) {
      return addSExpressionNormalizerAfterAll(builder);
    }

    /**
     * @deprecated 使用 {@link #addSExpressionNormalizerBefore(SExpressionNormalizer, String)}。
     */
    @Deprecated
    public Builder addSExpressionBuilderBefore(SExpressionBuilder builder, String builderName) {
      return addSExpressionNormalizerBefore(builder, builderName);
    }

    /**
     * @deprecated 使用 {@link #addSExpressionNormalizerBefore(SExpressionNormalizer, Class)}。
     */
    @Deprecated
    public Builder addSExpressionBuilderBefore(SExpressionBuilder builder, Class<? extends SExpressionBuilder> builderClass) {
      return addSExpressionNormalizerBefore(builder, builderClass);
    }

    /**
     * @deprecated 使用 {@link #addSExpressionNormalizerAfter(SExpressionNormalizer, String)}。
     */
    @Deprecated
    public Builder addSExpressionBuilderAfter(SExpressionBuilder builder, String builderName) {
      return addSExpressionNormalizerAfter(builder, builderName);
    }

    /**
     * @deprecated 使用 {@link #addSExpressionNormalizerAfter(SExpressionNormalizer, Class)}。
     */
    @Deprecated
    public Builder addSExpressionBuilderAfter(SExpressionBuilder builder, Class<? extends SExpressionBuilder> builderClass) {
      return addSExpressionNormalizerAfter(builder, builderClass);
    }

    // === 辅助方法 ===

    private int findNormalizerIndexByName(List<?> normalizers, String name) {
      for (int i = 0; i < normalizers.size(); i++) {
        Object normalizer = normalizers.get(i);
        if (normalizer.getClass().getSimpleName().equals(name) ||
            normalizer.getClass().getName().equals(name)) {
          return i;
        }
      }
      return -1;
    }

    private int findNormalizerIndexByClass(List<?> normalizers, Class<?> targetClass) {
      for (int i = 0; i < normalizers.size(); i++) {
        if (targetClass.isInstance(normalizers.get(i))) {
          return i;
        }
      }
      return -1;
    }

    private int findSExpressionNormalizerIndexByName(List<SExpressionNormalizer> normalizers, String name) {
      for (int i = 0; i < normalizers.size(); i++) {
        SExpressionNormalizer normalizer = normalizers.get(i);
        if (normalizer.getClass().getSimpleName().equals(name) ||
            normalizer.getClass().getName().equals(name)) {
          return i;
        }
      }
      return -1;
    }

    private int findSExpressionNormalizerIndexByClass(List<SExpressionNormalizer> normalizers, Class<?> targetClass) {
      for (int i = 0; i < normalizers.size(); i++) {
        if (targetClass.isInstance(normalizers.get(i))) {
          return i;
        }
      }
      return -1;
    }

    public QueryNormalizeContext build() {
      return new QueryNormalizeContext(this);
    }
  }
}
