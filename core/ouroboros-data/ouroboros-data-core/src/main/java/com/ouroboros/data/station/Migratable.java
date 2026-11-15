package com.ouroboros.data.station;

import io.vavr.control.Either;

import com.ouroboros.data.exception.DataModelException;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.MigrationStrategy;

/**
 * @author liansz
 */
public interface Migratable {
  String getName();

  MigrationStrategy getMigrationStrategy();

  /**
   * 迁移数据库
   *
   * @return right: 成功执行过迁移SQL返回true，反之false left: 异常信息
   */
  Either<DataModelException, Boolean> migrate();

  default Either<DataModelException, Boolean> hasRequiredUniqueColumn(DataModel model, String rawColumnName) {
    return Either.right(false);
  }
}
