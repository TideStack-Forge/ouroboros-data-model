package com.ouroboros.data.model.policy;

import java.util.Map;

import com.ouroboros.data.model.DataModelMeta;

/**
 * 模型附加策略的DDL效应器
 *
 * @author Song Mingxu
 */
public interface DDLEffector {

  String getPolicyName();

  void accept(DataModelMeta rawMeta, Map<String, ?> parameters);
}
