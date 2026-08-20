package com.ouroboros.data.model.policy;

import java.util.Map;

import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.model.DataModelMeta;
import com.ouroboros.data.record.RecordList;

/**
 * 模型附加策略的DQL效应器
 *
 * @author Song Mingxu
 */
public interface DQLEffector {

  String getPolicyName();

  QueryStatement before(DataModelMeta meta, Map<String, ?> parameters, QueryStatement rawQuery);

  RecordList after(DataModelMeta meta, Map<String, ?> parameters, QueryStatement query,
                   RecordList rawResults);
}
