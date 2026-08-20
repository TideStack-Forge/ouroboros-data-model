package com.ouroboros.data.model.policy;

import java.util.List;
import java.util.Map;

import com.ouroboros.data.dsl.statement.DMLStatement;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.model.DataModelMeta;
import com.ouroboros.data.record.RecordList;

/**
 * 模型附加策略
 *
 * @author Song Mingxu
 */
public interface AdditionalPolicy {

  String getName();

  Map<String, ?> getParameters();

  List<DDLEffector> getDDLEffectors();

  List<DMLEffector> getDMLEffectors();

  List<DQLEffector> getDQLEffectors();

  default void applyDDLEffectors(DataModelMeta meta) {
    getDDLEffectors().forEach(effector -> effector.accept(meta, getParameters()));
  }

  default DMLStatement applyDmlEffectors(DataModelMeta meta, DMLStatement dml) {
    return getDMLEffectors().stream()
        .reduce(dml,
            (prev, effector) -> effector.apply(meta, getParameters(), prev),
            (a, b) -> a);
  }

  default QueryStatement applyDQLBeforeEffectors(DataModelMeta meta, QueryStatement dql) {
    return getDQLEffectors().stream()
        .reduce(dql,
            (prev, effector) -> effector.before(meta, getParameters(), prev),
            (a, b) -> a);
  }

  default RecordList applyDQLAfterEffectors(DataModelMeta meta, QueryStatement dql, RecordList results) {
    return getDQLEffectors().stream()
        .reduce(results,
            (prev, effector) -> effector.after(meta, getParameters(), dql, prev),
            (a, b) -> a);
  }
}
