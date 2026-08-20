package com.ouroboros.data.orchestration.step;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ouroboros.data.orchestration.OrchestrationContext;
import com.ouroboros.data.orchestration.OrchestrationException;

/**
 * QueryStep 抽象基类
 *
 * <p>提供：
 * <ul>
 *   <li>日志记录</li>
 *   <li>异常处理</li>
 *   <li>模板方法</li>
 * </ul>
 *
 * <p>子类只需实现 doExecute() 方法
 *
 * @author Claude Code
 */
public abstract class AbstractQueryStep implements QueryStep {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private final String name;

  protected AbstractQueryStep(String name) {
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public final void execute(OrchestrationContext context) {
    logger.debug("Executing step: {}", name);

    try {
      doExecute(context);
      logger.debug("Step completed: {}", name);
    } catch (Exception e) {
      logger.error("Step failed: {}", name, e);
      throw new OrchestrationException("Step execution failed: " + name, e);
    }
  }

  /**
   * 子类实现具体的执行逻辑
   *
   * @param context 编排上下文
   */
  protected abstract void doExecute(OrchestrationContext context);
}
