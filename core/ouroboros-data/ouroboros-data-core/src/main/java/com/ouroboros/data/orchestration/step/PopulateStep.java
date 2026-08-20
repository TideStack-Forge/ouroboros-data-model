package com.ouroboros.data.orchestration.step;

import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.orchestration.RelationType;
import com.ouroboros.data.orchestration.OrchestrationContext;
import com.ouroboros.data.orchestration.strategy.PopulateField;
import com.ouroboros.data.orchestration.transformer.RecordListTransformer;
import com.ouroboros.data.record.RecordList;

/**
 * Populate 执行步骤
 *
 * <p>职责：
 * <ul>
 *   <li>从 Context 获取源数据</li>
 *   <li>执行关联查询（批量查询，避免 N+1）</li>
 *   <li>创建转换器存入 Context（延迟转换）</li>
 * </ul>
 *
 * <p>设计要点：
 * <ul>
 *   <li>不直接修改结果，只创建转换器</li>
 *   <li>支持并发执行（多个 PopulateStep 可并发）</li>
 *   <li>自动分批处理（IN > 1000）</li>
 * </ul>
 *
 * @author Claude Code
 */
public class PopulateStep extends AbstractQueryStep {
  private static final Logger logger = LoggerFactory.getLogger(PopulateStep.class);

  /**
   * 默认批量大小（避免 IN 语句过长）
   */
  private static final int DEFAULT_BATCH_SIZE = 1000;

  private final PopulateField populateField;
  private final String sourceStepName;

  public PopulateStep(String name, PopulateField populateField, String sourceStepName) {
    super(name);
    this.populateField = populateField;
    this.sourceStepName = sourceStepName;
  }

  @Override
  protected void doExecute(OrchestrationContext context) {
    logger.debug("执行 PopulateStep: {}, 源步骤: {}", getName(), sourceStepName);

    // 1. 从 Context 获取源数据
    RecordList sourceData = context.getResult(sourceStepName);
    if (sourceData == null || sourceData.isEmpty()) {
      logger.debug("源数据为空，跳过填充");
      return;
    }

    // 2. 提取外键列表
    List<Object> foreignKeys = extractForeignKeys(sourceData, populateField.localForeignKey());
    if (foreignKeys.isEmpty()) {
      logger.debug("外键列表为空，跳过填充");
      return;
    }

    // 3. 执行批量查询
    RecordList relatedData = executeBatchQuery(foreignKeys, populateField, context);

    // 4. 按外键分组
    Map<Object, List<Map<String, Object>>> grouped = groupByForeignKey(
        relatedData,
        populateField.remotePrimaryKey()
    );

    // 5. 创建转换器并存入 Context
    RecordListTransformer transformer = createTransformer(grouped, populateField);
    context.addTransformer(transformer);

    logger.debug("PopulateStep 完成: {}, 查询到 {} 条关联数据", getName(), relatedData.size());
  }

  /**
   * 提取外键列表
   */
  private List<Object> extractForeignKeys(RecordList sourceData, String foreignKeyField) {
    return sourceData.stream()
        .map(record -> getValueIgnoreCase(record, foreignKeyField))
        .filter(Objects::nonNull)
        .distinct()
        .collect(Collectors.toList());
  }

  /**
   * 执行批量查询
   */
  private RecordList executeBatchQuery(List<Object> foreignKeys, PopulateField field, OrchestrationContext context) {
    DataModel relatedModel = field.relatedModel();

    // Round 5 移除 null 检查
    // 假设 relatedModel 不为 null（由 extractPopulateFields 保证）

    String remotePrimaryKey = field.remotePrimaryKey();
    List<String> selectFields = field.selectFields();

    // 分批查询（避免 IN 列表过长）
    List<RecordList> batches = new ArrayList<>();
    for (int i = 0; i < foreignKeys.size(); i += DEFAULT_BATCH_SIZE) {
      int end = Math.min(i + DEFAULT_BATCH_SIZE, foreignKeys.size());
      List<Object> batch = foreignKeys.subList(i, end);

      // 构建查询语句（使用 Map 方式）
      Map<String, Object> query = buildBatchQueryMap(batch, remotePrimaryKey, selectFields, field);

      // 执行查询
      RecordList batchResult = relatedModel.query(query)
          .getOrElseThrow(e -> new RuntimeException("执行关联查询失败: " + e.getMessage(), e));

      batches.add(batchResult);
    }

    // 合并所有批次结果
    return mergeBatches(batches);
  }

  /**
   * 构建批量查询语句（Map 方式）
   */
  private Map<String, Object> buildBatchQueryMap(List<Object> foreignKeys, String remotePrimaryKey,
                                                   List<String> selectFields, PopulateField field) {
    Map<String, Object> query = new TreeMap<>(String::compareToIgnoreCase);

    // SELECT 字段
    if (selectFields != null && !selectFields.isEmpty()) {
      query.put("SELECT", selectFields);
    }

    // WHERE 条件：remotePrimaryKey IN (foreignKeys)
    Map<String, Object> where = new TreeMap<>(String::compareToIgnoreCase);
    Map<String, Object> inCondition = new HashMap<>();
    inCondition.put("$in", foreignKeys);
    where.put(remotePrimaryKey, inCondition);

    // 合并用户 WHERE 子配置
    if (field.where() != null && !field.where().isEmpty()) {
      where.putAll(field.where());
    }
    query.put("WHERE", where);

    // LIMIT/OFFSET/POPULATE 子配置
    if (field.limit() != null) {
      query.put("LIMIT", field.limit());
    }
    if (field.offset() != null) {
      query.put("OFFSET", field.offset());
    }
    if (field.nestedPopulate() != null) {
      query.put("POPULATE", field.nestedPopulate());
    }

    return query;
  }

  /**
   * 合并批次结果
   */
  private RecordList mergeBatches(List<RecordList> batches) {
    if (batches.isEmpty()) {
      return RecordList.empty();
    }
    if (batches.size() == 1) {
      return batches.get(0);
    }

    // 合并所有批次
    List<Map<String, Object>> allRecords = new ArrayList<>();
    for (RecordList batch : batches) {
      allRecords.addAll(batch);
    }
    return RecordList.of(allRecords);
  }

  /**
   * 按外键分组关联数据
   */
  private Map<Object, List<Map<String, Object>>> groupByForeignKey(RecordList relatedData, String primaryKeyField) {
    Map<Object, List<Map<String, Object>>> grouped = new HashMap<>();

    for (Map<String, Object> record : relatedData) {
      Object key = getValueIgnoreCase(record, primaryKeyField);
      if (key != null) {
        grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(record);
      }
    }

    return grouped;
  }

  /**
   * 创建转换器
   */
  private RecordListTransformer createTransformer(Map<Object, List<Map<String, Object>>> grouped, PopulateField field) {
    return (recordList, context) -> {
      // Record 是不可变的，需要创建新的 RecordList
      List<Map<String, Object>> modified = new ArrayList<>();
      for (Map<String, Object> record : recordList) {
        Object foreignKeyValue = getValueIgnoreCase(record, field.localForeignKey());
        if (foreignKeyValue != null) {
          List<Map<String, Object>> relatedRecords = grouped.get(foreignKeyValue);
          if (relatedRecords != null) {
            Map<String, Object> newRecord = new LinkedHashMap<>(record);
            // 填充到目标字段
            if (field.relationType() == RelationType.TO_MANY) {
              newRecord.put(field.fieldName(), relatedRecords);
            } else if (field.relationType() == RelationType.TO_ONE) {
              newRecord.put(field.fieldName(), relatedRecords.get(0));
            } else if (relatedRecords.size() == 1) {
              newRecord.put(field.fieldName(), relatedRecords.get(0));
            } else {
              newRecord.put(field.fieldName(), relatedRecords);
            }
            modified.add(newRecord);
            continue;
          }
        }
        modified.add(record);
      }
      return RecordList.of(modified);
    };
  }

  private Object getValueIgnoreCase(Map<String, ?> record, String fieldName) {
    if (record.containsKey(fieldName)) {
      return record.get(fieldName);
    }
    for (Map.Entry<String, ?> entry : record.entrySet()) {
      String key = entry.getKey();
      if (key != null && key.equalsIgnoreCase(fieldName)) {
        return entry.getValue();
      }
    }
    return null;
  }
}
