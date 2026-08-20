package com.ouroboros.data.orchestration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
/**
 * 分析结果
 * <p>
 * 封装 QueryStatement 分析的结果，包含跨源条件和同源条件列表。
 * </p>
 */
public record AnalysisResult(List<CrossSourceCondition> crossSourceConditions,
                             List<SameSourceCondition> sameSourceToOneConditions,
                             List<SameSourceCondition> sameSourceToManyConditions) {
  public AnalysisResult(
      List<CrossSourceCondition> crossSourceConditions,
      List<SameSourceCondition> sameSourceToOneConditions,
      List<SameSourceCondition> sameSourceToManyConditions) {
    this.crossSourceConditions = new ArrayList<>(crossSourceConditions);
    this.sameSourceToOneConditions = new ArrayList<>(sameSourceToOneConditions);
    this.sameSourceToManyConditions = new ArrayList<>(sameSourceToManyConditions);
  }
  @Override
  public List<CrossSourceCondition> crossSourceConditions() {
    return Collections.unmodifiableList(crossSourceConditions);
  }
  @Override
  public List<SameSourceCondition> sameSourceToOneConditions() {
    return Collections.unmodifiableList(sameSourceToOneConditions);
  }
  @Override
  public List<SameSourceCondition> sameSourceToManyConditions() {
    return Collections.unmodifiableList(sameSourceToManyConditions);
  }
  public boolean hasConditions() {
    return !crossSourceConditions.isEmpty()
        || !sameSourceToOneConditions.isEmpty()
        || !sameSourceToManyConditions.isEmpty();
  }
}
