package com.ouroboros.common.service;

import java.util.Collection;

import com.ouroboros.exception.OuroborosException;

/**
 * @author liansz
 */
public interface SoftDeleteModelService<PK, M> extends ModelService<PK, M> {

  /**
   * 删除（软删除）
   *
   * @param id
   * @return
   * @throws OuroborosException
   */
  @Override
  Boolean delete(PK id) throws OuroborosException;

  /**
   * 删除多个（软删除）
   *
   * @param ids
   * @return
   * @throws OuroborosException
   */
  @Override
  Boolean delete(Collection<PK> ids) throws OuroborosException;

  /**
   * 删除（物理删除）
   *
   * @param id
   * @return
   * @throws OuroborosException
   */
  Boolean drop(PK id) throws OuroborosException;

  /**
   * 删除多个（物理删除）
   *
   * @param ids
   * @return
   * @throws OuroborosException
   */
  Boolean drop(Collection<PK> ids) throws OuroborosException;
}
