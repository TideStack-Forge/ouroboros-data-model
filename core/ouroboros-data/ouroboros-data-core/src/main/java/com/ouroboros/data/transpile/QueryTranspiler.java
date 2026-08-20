package com.ouroboros.data.transpile;

import org.apache.commons.lang3.NotImplementedException;

import io.vavr.Tuple2;
import io.vavr.control.Try;

import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.util.DataServices;

/**
 * QueryStatement -> QueryDSL 转译器
 */
public interface QueryTranspiler {

  /**
   * 通过 SPI 加载的默认转译器
   */
  QueryTranspiler DEFAULT = DataServices.getServiceStream(QueryTranspiler.class)
      .max(DataServices::sortByPriority)
      .orElseGet(() -> (query, contexts) -> Try.failure(new NotImplementedException("No QueryTranspiler Implemented")));

  /**
   * 转译
   *
   * @param query   归一化后的QueryStatement
   * @param context 转译上下文
   * @return QueryDSL 查询 metadata
   */
  Try<OuroborosQueryMetadata> applyWithContext(QueryStatement query, TranspileContext context);

  /**
   * @deprecated 使用顶层 {@link ClauseTranspiler} 作为 canonical 契约。
   * 该嵌套接口保留一轮作为兼容桥。
   *
   * 子句构建器接口
   *
   * <p>职责：
   * <ul>
   *   <li>处理特定 SQL 子句（FROM、JOIN、SELECT 等）</li>
   *   <li>可以包装 Context（如 JoinBuilder 包装 JoinTranspileContext）</li>
   * </ul>
   *
   * <p>返回值说明：
   * <ul>
   *   <li>Tuple2&lt;Metadata, Context&gt;：更新后的 metadata 和可能被包装的 context</li>
   * </ul>
   */
  @Deprecated
  interface ClauseBuilder {
    /**
     * 构建子句
     *
     * @param query    查询语句
     * @param metadata 当前 metadata
     * @param context  当前 context
     * @return (更新后的 metadata, 可能被包装的 context)
     */
    Try<Tuple2<OuroborosQueryMetadata, TranspileContext>> applyWithContext(
        QueryStatement query,
        OuroborosQueryMetadata metadata,
        TranspileContext context);
  }
}
