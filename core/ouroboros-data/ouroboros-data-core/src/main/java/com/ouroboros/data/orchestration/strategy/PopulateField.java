package com.ouroboros.data.orchestration.strategy;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.orchestration.RelationType;
/**
 * Populate 字段信息
 *
 * <p>包含填充所需的所有元数据：
 * <ul>
 *   <li>字段名、关联模型、外键/主键映射</li>
 *   <li>子级 Populate（children）</li>
 *   <li>源模型和关系类型（供 PopulateStrategySelector 使用）</li>
 *   <li>子配置（where/limit/offset/nestedPopulate）</li>
 * </ul>
 *
 * @author Claude Code
 */
public record PopulateField(
    String fieldName,
    DataModel relatedModel,
    String localForeignKey,
    String remotePrimaryKey,
    List<String> selectFields,
    List<PopulateField> children,
    DataModel sourceModel,
    RelationType relationType,
    Map<String, Object> where,
    Integer limit,
    Integer offset,
    Object nestedPopulate
) {
  public PopulateField {
    selectFields = selectFields != null ? selectFields : Collections.emptyList();
    children = children != null ? children : Collections.emptyList();
  }
  /**
   * 兼容构造器（8-arg，无子配置）
   */
  public PopulateField(String fieldName, DataModel relatedModel,
                       String localForeignKey, String remotePrimaryKey,
                       List<String> selectFields, List<PopulateField> children,
                       DataModel sourceModel, RelationType relationType) {
    this(fieldName, relatedModel, localForeignKey, remotePrimaryKey,
        selectFields, children, sourceModel, relationType, null, null, null, null);
  }
  /**
   * 兼容构造器（6-arg，无 sourceModel/relationType/子配置）
   */
  public PopulateField(String fieldName, DataModel relatedModel,
                       String localForeignKey, String remotePrimaryKey,
                       List<String> selectFields, List<PopulateField> children) {
    this(fieldName, relatedModel, localForeignKey, remotePrimaryKey,
        selectFields, children, null, null, null, null, null, null);
  }
  /**
   * 简化构造器（5-arg，无 children/sourceModel/relationType/子配置）
   */
  public PopulateField(String fieldName, DataModel relatedModel,
                       String localForeignKey, String remotePrimaryKey,
                       List<String> selectFields) {
    this(fieldName, relatedModel, localForeignKey, remotePrimaryKey,
        selectFields, Collections.emptyList(), null, null, null, null, null, null);
  }
}
