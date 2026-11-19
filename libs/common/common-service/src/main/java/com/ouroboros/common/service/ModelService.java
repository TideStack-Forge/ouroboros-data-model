package com.ouroboros.common.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.ouroboros.exception.OuroborosException;

/**
 * @author liansz
 */
public interface ModelService<PK, M> {

  /**
   * 添加
   *
   * @param model
   * @return
   * @throws OuroborosException
   */
  M add(M model) throws OuroborosException;

  /**
   * 添加多个
   *
   * @param models
   * @return
   * @throws OuroborosException
   */
  List<? extends M> add(Collection<? extends M> models) throws OuroborosException;

  /**
   * 删除
   *
   * @param id
   * @return
   * @throws OuroborosException
   */
  Boolean delete(PK id) throws OuroborosException;

  /**
   * 删除多个
   *
   * @param ids
   * @return
   * @throws OuroborosException
   */
  Boolean delete(Collection<PK> ids) throws OuroborosException;

  /**
   * 更新
   *
   * @param model
   * @return
   * @throws OuroborosException
   */
  Boolean update(PK id, M model) throws OuroborosException;

  /**
   * 部分更新
   *
   * @param map
   * @return
   * @throws OuroborosException
   */
  Boolean partialUpdate(PK id, Map<String, Object> map) throws OuroborosException;

  /**
   * 通过主键获取（填充所有关联模型）
   *
   * @param primaryKey
   * @return
   * @throws OuroborosException
   */
  M getByPrimaryKey(PK primaryKey) throws OuroborosException;

  /**
   * 通过主键获取
   *
   * @param primaryKey
   * @param statement
   * @return
   * @throws OuroborosException
   */
  M getByPrimaryKey(PK primaryKey, Map<String, Object> statement) throws OuroborosException;

  /**
   * 计算数量
   *
   * @param where
   * @return
   * @throws OuroborosException
   */
  Long count(Map<String, Object> where) throws OuroborosException;

  /**
   * 查询一条
   *
   * @param statement
   * @return
   * @throws OuroborosException
   */
  M searchOne(Map<String, Object> statement) throws OuroborosException;

  /**
   * 查询列表
   *
   * @param statement 查询条件
   * @return
   * @throws OuroborosException
   */
  List<? extends M> searchList(Map<String, Object> statement) throws OuroborosException;

  /**
   * 查询全部
   *
   * @return
   * @throws OuroborosException
   */
  List<? extends M> searchAll() throws OuroborosException;
}
