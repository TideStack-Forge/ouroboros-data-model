package com.ouroboros.data.orchestration.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ouroboros.data.orchestration.RelationType;
import com.ouroboros.data.orchestration.JoinCapabilities;
import com.ouroboros.data.orchestration.JoinCapabilityResult;

/**
 * 默认填充策略选择器
 *
 * <p>按优先级选择策略：
 * <ol>
 *   <li>元数据缺失 → Separate（兼容回退）</li>
 *   <li>跨源 → Separate</li>
 *   <li>有子级填充 → Separate</li>
 *   <li>ToMany → Separate</li>
 *   <li>同源 ToOne 可 JOIN → JOIN</li>
 *   <li>默认 → Separate</li>
 * </ol>
 *
 * @author Claude Code
 */
public class DefaultPopulateStrategySelector implements PopulateStrategySelector {

  private static final Logger logger = LoggerFactory.getLogger(DefaultPopulateStrategySelector.class);

  @Override
  public PopulateStrategy select(PopulateField populate) {
    // 1. 元数据缺失 → 回退到 Separate
    if (populate.sourceModel() == null || populate.relationType() == null) {
      logger.debug("Populate field '{}': missing metadata, fallback to Separate", populate.fieldName());
      return new SeparatePopulateStrategy();
    }

    // 2. 跨源 → Separate
    JoinCapabilityResult joinResult = JoinCapabilities.canJoin(populate.sourceModel(), populate.relatedModel());
    if (!joinResult.isJoinable()) {
      logger.debug("Populate field '{}': cross-source ({}), using Separate", populate.fieldName(), joinResult.getReason());
      return new SeparatePopulateStrategy();
    }

    // 3. 有子级填充 → Separate
    if (!populate.children().isEmpty() || populate.nestedPopulate() != null) {
      logger.debug("Populate field '{}': has children, using Separate", populate.fieldName());
      return new SeparatePopulateStrategy();
    }

    // 4. ToMany → Separate
    if (populate.relationType() == RelationType.TO_MANY) {
      logger.debug("Populate field '{}': ToMany relation, using Separate", populate.fieldName());
      return new SeparatePopulateStrategy();
    }

    // 5. 同源 ToOne → JOIN
    if (populate.relationType() == RelationType.TO_ONE) {
      logger.debug("Populate field '{}': same-source ToOne, using JOIN", populate.fieldName());
      return new JoinPopulateStrategy();
    }

    // 6. 默认 → Separate
    logger.debug("Populate field '{}': default fallback to Separate", populate.fieldName());
    return new SeparatePopulateStrategy();
  }
}
