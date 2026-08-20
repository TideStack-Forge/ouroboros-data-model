package com.ouroboros.data.orchestration.step;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vavr.control.Try;

import com.ouroboros.data.model.ValueType;
import com.ouroboros.data.orchestration.OrchestrationContext;
import com.ouroboros.data.orchestration.transformer.RecordListTransformer;
import com.ouroboros.data.record.RecordList;

/**
 * 结果变换步骤
 *
 * <p>职责：
 * <ul>
 *   <li>创建转换器，将扁平 JOIN 字段提取为嵌套对象</li>
 *   <li>转换器存入 Context 转换器队列，由 ApplyTransformersStep 统一应用</li>
 * </ul>
 *
 * <p>设计要点：
 * <ul>
 *   <li>不直接修改结果（延迟转换）</li>
 *   <li>支持 NULL 语义（LEFT JOIN 无匹配时嵌套对象为 null）</li>
 *   <li>移除原始扁平字段（如 dept__id, dept__name）</li>
 * </ul>
 *
 * @author Claude Code
 */
public class ResultTransformStep extends AbstractQueryStep {

  private static final Logger logger = LoggerFactory.getLogger(ResultTransformStep.class);

  private final String targetFieldName;
  private final String flatFieldPrefix;
  private final List<String> selectFields;
  private final Map<String, ValueType<?>> valueTypes;

  public ResultTransformStep(String name, String targetFieldName, String flatFieldPrefix, List<String> selectFields) {
    this(name, targetFieldName, flatFieldPrefix, selectFields, Collections.emptyMap());
  }

  public ResultTransformStep(String name, String targetFieldName, String flatFieldPrefix,
                             List<String> selectFields, Map<String, ValueType<?>> valueTypes) {
    super(name);
    this.targetFieldName = targetFieldName;
    this.flatFieldPrefix = flatFieldPrefix;
    this.selectFields = selectFields != null ? selectFields : Collections.emptyList();
    this.valueTypes = valueTypes != null ? valueTypes : Collections.emptyMap();
  }

  @Override
  protected void doExecute(OrchestrationContext context) {
    logger.debug("执行 ResultTransformStep: {}, 目标字段: {}, 前缀: {}", getName(), targetFieldName, flatFieldPrefix);

    if (selectFields.isEmpty()) {
      logger.debug("selectFields 为空，跳过转换器创建");
      return;
    }

    RecordListTransformer transformer = createTransformer();
    context.addTransformer(transformer);

    logger.debug("ResultTransformStep 完成: {}, 创建了转换器", getName());
  }

  private RecordListTransformer createTransformer() {
    return (recordList, context) -> {
      List<Map<String, Object>> modified = new ArrayList<>();
      for (Map<String, Object> record : recordList) {
        Map<String, Object> newRecord = new LinkedHashMap<>(record);
        Map<String, Object> nestedMap = new LinkedHashMap<>();
        boolean allNull = true;

        for (String field : selectFields) {
          String flatKey = flatFieldPrefix + field;
          Object value = removeIgnoreCase(newRecord, flatKey);
          if (value != null) {
            allNull = false;
          }
          nestedMap.put(field, convertValue(field, value));
        }

        newRecord.put(targetFieldName, allNull ? null : nestedMap);
        modified.add(newRecord);
      }
      return RecordList.of(modified);
    };
  }

  private Object convertValue(String field, Object value) {
    ValueType<?> valueType = valueTypes.get(field);
    if (valueType == null) {
      return value;
    }
    var convertResult = Try.of(() -> valueType.convert(value));
    if (convertResult.isFailure()) {
      logger.warn("字段'{}'类型转换时发生错误, 错误信息：{}", field, convertResult.getCause().getMessage());
      return value;
    }
    return convertResult.get();
  }

  private Object removeIgnoreCase(Map<String, Object> record, String fieldName) {
    if (record.containsKey(fieldName)) {
      return record.remove(fieldName);
    }
    for (String key : new ArrayList<>(record.keySet())) {
      if (key != null && key.equalsIgnoreCase(fieldName)) {
        return record.remove(key);
      }
    }
    return null;
  }
}
