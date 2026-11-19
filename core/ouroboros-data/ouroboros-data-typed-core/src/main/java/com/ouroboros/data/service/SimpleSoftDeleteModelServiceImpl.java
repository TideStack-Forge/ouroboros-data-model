package com.ouroboros.data.service;

import com.ouroboros.common.service.SoftDelete;
import com.ouroboros.common.service.SoftDeleteModelService;

/**
 * 简单模型服务
 *
 * @param <PK> 主键类型
 * @param <M>  模型类型
 * @author liansz
 */
public class SimpleSoftDeleteModelServiceImpl<PK, M extends SoftDelete>
    extends SoftDeleteModelServiceImpl<PK, M, M> implements SoftDeleteModelService<PK, M> {

  /**
   * 获取模型类
   *
   * @return
   */
  @Override
  protected Class<M> getModelClass() {
    return getSuperclassActualArgumentType(1);
  }
}
