package com.ouroboros.data.model.policy;

import java.util.Map;

import com.ouroboros.data.dsl.statement.DMLStatement;
import com.ouroboros.data.model.DataModelMeta;

/**
 * 模型附加策略的DML效应器
 *
 * @author Song Mingxu
 */
public interface DMLEffector {

  String getPolicyName();

  DMLStatement apply(DataModelMeta meta, Map<String, ?> parameters, DMLStatement rawClause);
}
