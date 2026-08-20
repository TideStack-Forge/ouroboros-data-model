package com.ouroboros.data.orchestration;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import com.ouroboros.data.dsl.statement.OmitClause;
import com.ouroboros.data.dsl.statement.PopulateClause;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.model.PopulateContext;
import com.ouroboros.data.orchestration.rewriter.StatementRewriter;
import com.ouroboros.data.orchestration.transformer.RecordListTransformer;
import com.ouroboros.data.record.RecordList;

/**
 * 编排上下文
 *
 * <p>职责：
 * <ul>
 *   <li>存储主查询语句</li>
 *   <li>存储各 Step 的执行结果</li>
 *   <li>管理语句改写器队列</li>
 *   <li>管理结果转换器队列</li>
 *   <li>提供线程安全的读写方法</li>
 * </ul>
 *
 * <p>不负责：
 * <ul>
 *   <li>业务逻辑</li>
 *   <li>执行控制</li>
 * </ul>
 *
 * @author Claude Code
 */
public class OrchestrationContext {

  // Step 执行结果（stepName -> RecordList）
  private final Map<String, RecordList> results = new HashMap<>();
  // 语句改写器队列（FIFO）
  private final Queue<StatementRewriter> statementRewriters = new LinkedList<>();
  // 结果转换器队列（FIFO）
  private final Queue<RecordListTransformer> transformers = new LinkedList<>();
  // 别名映射（旧别名 → 新别名，由 JoinDeduplicator 设置）
  private Map<String, String> aliasMapping = new HashMap<>();
  // 主查询语句（可变）
  private QueryStatement mainStatement;
  // Populate 上下文列表（来自 DefaultDataModel.query(Map)）
  private List<PopulateContext> populateContexts = Collections.emptyList();
  // 类型化 POPULATE 子句
  private PopulateClause populateClause;
  // 类型化 OMIT 子句
  private OmitClause omitClause;

  /**
   * 获取主查询语句
   */
  public QueryStatement getMainStatement() {
    return mainStatement;
  }

  /**
   * 设置主查询语句
   */
  public void setMainStatement(QueryStatement statement) {
    this.mainStatement = statement;
  }

  /**
   * 存储 Step 执行结果
   *
   * @param stepName 步骤名称
   * @param result   执行结果
   */
  public void setResult(String stepName, RecordList result) {
    results.put(stepName, result);
  }

  /**
   * 获取 Step 执行结果
   *
   * @param stepName 步骤名称
   * @return 执行结果，如果不存在返回 null
   */
  public RecordList getResult(String stepName) {
    return results.get(stepName);
  }

  /**
   * 检查是否存在指定 Step 的结果
   *
   * @param stepName 步骤名称
   * @return 是否存在
   */
  public boolean hasResult(String stepName) {
    return results.containsKey(stepName);
  }

  /**
   * 添加语句改写器到队列
   *
   * @param rewriter 改写器
   */
  public void addStatementRewriter(StatementRewriter rewriter) {
    statementRewriters.offer(rewriter);
  }

  /**
   * 获取语句改写器队列
   *
   * @return 改写器队列
   */
  public Queue<StatementRewriter> getStatementRewriters() {
    return statementRewriters;
  }

  /**
   * 清空语句改写器队列
   */
  public void clearStatementRewriters() {
    statementRewriters.clear();
  }

  /**
   * 添加结果转换器到队列
   *
   * @param transformer 转换器
   */
  public void addTransformer(RecordListTransformer transformer) {
    transformers.offer(transformer);
  }

  /**
   * 获取结果转换器队列
   *
   * @return 转换器队列
   */
  public Queue<RecordListTransformer> getTransformers() {
    return transformers;
  }

  /**
   * 清空结果转换器队列
   */
  public void clearTransformers() {
    transformers.clear();
  }

  /**
   * 设置别名映射
   *
   * @param mapping 别名映射（旧别名 → 新别名）
   */
  public void setAliasMapping(Map<String, String> mapping) {
    this.aliasMapping = mapping;
  }

  /**
   * 获取别名映射
   *
   * @return 别名映射
   */
  public Map<String, String> getAliasMapping() {
    return aliasMapping;
  }

  /**
   * 获取 Populate 上下文列表
   */
  public List<PopulateContext> getPopulateContexts() {
    return populateContexts;
  }

  /**
   * 设置 Populate 上下文列表
   */
  public void setPopulateContexts(List<PopulateContext> populateContexts) {
    this.populateContexts = populateContexts;
  }

  /**
   * 获取类型化 POPULATE 子句
   */
  public PopulateClause getPopulateClause() {
    return populateClause;
  }

  /**
   * 设置类型化 POPULATE 子句
   */
  public void setPopulateClause(PopulateClause populateClause) {
    this.populateClause = populateClause;
  }

  /**
   * 获取类型化 OMIT 子句
   */
  public OmitClause getOmitClause() {
    return omitClause;
  }

  /**
   * 设置类型化 OMIT 子句
   */
  public void setOmitClause(OmitClause omitClause) {
    this.omitClause = omitClause;
  }
}
