package com.ouroboros.data.orchestration.step;

import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ouroboros.data.orchestration.OrchestrationContext;
import com.ouroboros.data.orchestration.transformer.RecordListTransformer;
import com.ouroboros.data.record.RecordList;

/**
 * 应用转换器步骤
 *
 * <p>职责：
 * <ul>
 *   <li>从 Context 获取源数据</li>
 *   <li>依次应用所有转换器</li>
 *   <li>存储转换后的结果</li>
 *   <li>清空转换器队列</li>
 * </ul>
 *
 * <p>设计要点：
 * <ul>
 *   <li>转换器按 FIFO 顺序应用</li>
 *   <li>支持链式转换（多个转换器组合）</li>
 *   <li>转换后清空队列（避免重复应用）</li>
 * </ul>
 *
 * @author Claude Code
 */
public class ApplyTransformersStep extends AbstractQueryStep {
  private static final Logger logger = LoggerFactory.getLogger(ApplyTransformersStep.class);

  private final String sourceStepName;

  public ApplyTransformersStep(String name, String sourceStepName) {
    super(name);
    this.sourceStepName = sourceStepName;
  }

  @Override
  protected void doExecute(OrchestrationContext context) {
    logger.debug("执行 ApplyTransformersStep: {}, 源步骤: {}", getName(), sourceStepName);

    // 1. 从 Context 获取源数据
    RecordList sourceData = context.getResult(sourceStepName);
    if (sourceData == null) {
      logger.warn("源数据不存在: {}", sourceStepName);
      context.setResult(getName(), RecordList.empty());
      return;
    }

    // 2. 获取转换器队列
    Queue<RecordListTransformer> transformers = context.getTransformers();
    if (transformers.isEmpty()) {
      logger.debug("无转换器需要应用，直接返回源数据");
      context.setResult(getName(), sourceData);
      return;
    }

    // 3. 依次应用所有转换器
    RecordList result = sourceData;
    int count = 0;
    while (!transformers.isEmpty()) {
      RecordListTransformer transformer = transformers.poll();
      result = transformer.transform(result, context);
      count++;
    }

    logger.debug("应用了 {} 个转换器", count);

    // 4. 存储结果
    context.setResult(getName(), result);

    logger.debug("ApplyTransformersStep 完成: {}, 结果数量: {}", getName(), result.size());
  }
}
