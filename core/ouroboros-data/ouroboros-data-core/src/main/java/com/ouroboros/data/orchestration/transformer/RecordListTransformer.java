package com.ouroboros.data.orchestration.transformer;

import com.ouroboros.data.orchestration.OrchestrationContext;
import com.ouroboros.data.record.RecordList;

/**
 * 记录列表转换器接口
 *
 * <p>用于对查询结果进行转换，如：
 * <ul>
 *   <li>填充关联数据（Populate）</li>
 *   <li>结构变换（扁平 → 嵌套）</li>
 *   <li>字段映射</li>
 * </ul>
 *
 * <p>设计要点：
 * <ul>
 *   <li>转换器是纯函数，不依赖外部状态</li>
 *   <li>可以链式组合多个转换器</li>
 *   <li>支持延迟应用（先创建，后执行）</li>
 * </ul>
 *
 * @author Claude Code
 */
@FunctionalInterface
public interface RecordListTransformer {

  /**
   * 转换记录列表
   *
   * @param recordList 源记录列表
   * @param context    编排上下文
   * @return 转换后的记录列表
   */
  RecordList transform(RecordList recordList, OrchestrationContext context);
}
