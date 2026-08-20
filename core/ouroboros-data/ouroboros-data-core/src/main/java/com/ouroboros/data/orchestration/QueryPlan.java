package com.ouroboros.data.orchestration;

import io.vavr.control.Try;

import com.ouroboros.data.record.RecordList;

/**
 * 查询执行计划接口
 * <p>
 * 定义执行计划的抽象接口，负责管理 QueryStep 的执行顺序。
 * </p>
 */
public interface QueryPlan {

  /**
   * 执行查询计划
   *
   * @param context 编排上下文
   * @return 查询结果，如果执行失败则返回 Failure
   */
  Try<RecordList> execute(OrchestrationContext context);
}
