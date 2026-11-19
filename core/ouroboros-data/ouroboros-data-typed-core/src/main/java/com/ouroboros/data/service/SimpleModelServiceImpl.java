package com.ouroboros.data.service;

import com.ouroboros.common.service.ModelService;

/**
 * 简单模型服务
 *
 * @param <PK> 主键类型
 * @param <M>  模型类型
 * @author liansz
 */
public class SimpleModelServiceImpl<PK, M> extends ModelServiceImpl<PK, M, M> implements ModelService<PK, M> {

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
