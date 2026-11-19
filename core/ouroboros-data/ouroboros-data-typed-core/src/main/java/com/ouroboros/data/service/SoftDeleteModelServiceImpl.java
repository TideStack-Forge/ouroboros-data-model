package com.ouroboros.data.service;

import com.ouroboros.common.service.SoftDelete;
import com.ouroboros.common.service.SoftDeleteModelService;

import java.util.*;

import io.vavr.control.Try;

import com.ouroboros.exception.OuroborosException;
import com.ouroboros.exception.PlatformException;
import com.ouroboros.exception.mapper.OuroborosExceptionMapper;
import com.ouroboros.util.Maps;

/**
 * 带有软删除的业务模型服务基类
 *
 * @param <PK> 主键类型
 * @param <M>  业务模型类型
 * @param <E>  数据模型类型
 * @author liansz
 */
public class SoftDeleteModelServiceImpl<PK, M extends SoftDelete, E extends M> extends ModelServiceImpl<PK, M, E> implements SoftDeleteModelService<PK, M> {

  @Override
  public M add(M model) throws OuroborosException {
    model.setDeleted(false);
    return getDataModel().insert(model)
        .getOrElseThrow(OuroborosExceptionMapper::toOuroborosException);
  }

  @Override
  public List<? extends M> add(Collection<? extends M> models) throws OuroborosException {
    models.forEach(model -> model.setDeleted(false));
    return getDataModel().batchInsert(new ArrayList<>(models))
        .getOrElseThrow(OuroborosExceptionMapper::toOuroborosException);
  }

  @Override
  public Boolean delete(PK id) throws OuroborosException {
    Try<String> deletedFieldNameTry = getDeletedFieldName();
    if (deletedFieldNameTry.isFailure()) {
      throw new PlatformException("获取软删除字段名称失败", deletedFieldNameTry.getCause());
    }

    String deletedFieldName = deletedFieldNameTry.get();

    Map<String, Object> data = Collections.singletonMap(deletedFieldName, true);

    return getDataModel().update(id, data)
        .map(r -> r > 0)
        .getOrElseThrow(OuroborosExceptionMapper::toOuroborosException);
  }

  @Override
  public Boolean delete(Collection<PK> ids) throws OuroborosException {
    Try<String> deletedFieldNameTry = getDeletedFieldName();
    if (deletedFieldNameTry.isFailure()) {
      throw new PlatformException("获取软删除字段名称失败", deletedFieldNameTry.getCause());
    }

    String deletedFieldName = deletedFieldNameTry.get();

    Map<String, Object> data = Collections.singletonMap(deletedFieldName, true);

    return getDataModel().update(ids, data)
        .map(r -> r > 0)
        .getOrElseThrow(OuroborosExceptionMapper::toOuroborosException);
  }

  @Override
  public Boolean drop(PK id) throws OuroborosException {
    return getDataModel().delete(id)
        .map(r -> r > 0)
        .getOrElseThrow(OuroborosExceptionMapper::toOuroborosException);
  }

  @Override
  public Boolean drop(Collection<PK> ids) throws OuroborosException {
    return getDataModel().delete(new ArrayList<>(ids))
        .map(r -> r == ids.size())
        .getOrElseThrow(OuroborosExceptionMapper::toOuroborosException);
  }

  @Override
  public Boolean update(PK id, M model) throws OuroborosException {
    Try<String> deletedFieldNameTry = getDeletedFieldName();
    if (deletedFieldNameTry.isFailure()) {
      throw new PlatformException("获取软删除字段名称失败", deletedFieldNameTry.getCause());
    }

    String deletedFieldName = deletedFieldNameTry.get();

    Map<String, Object> where = new HashMap<>();
    where.put("id", id);
    where.put(deletedFieldName, false);

    return getDataModel().update(where, Maps.fromBean(model))
        .map(r -> r > 0)
        .getOrElseThrow(OuroborosExceptionMapper::toOuroborosException);
  }

  @Override
  public Boolean partialUpdate(PK id, Map<String, Object> data) throws OuroborosException {
    Try<String> deletedFieldNameTry = getDeletedFieldName();
    if (deletedFieldNameTry.isFailure()) {
      throw new PlatformException("获取软删除字段名称失败", deletedFieldNameTry.getCause());
    }

    String deletedFieldName = deletedFieldNameTry.get();

    Map<String, Object> where = new HashMap<>();
    where.put("id", id);
    where.put(deletedFieldName, false);

    return getDataModel().update(where, data)
        .map(r -> r > 0)
        .getOrElseThrow(OuroborosExceptionMapper::toOuroborosException);
  }

  @Override
  public Long count(Map<String, Object> where) throws OuroborosException {
    Try<String> deletedFieldNameTry = getDeletedFieldName();
    if (deletedFieldNameTry.isFailure()) {
      throw new PlatformException("获取软删除字段名称失败", deletedFieldNameTry.getCause());
    }

    String deletedFieldName = deletedFieldNameTry.get();

    Map<String, Object> conditions = where == null ? new HashMap<>() : new HashMap<>(where);
    conditions.put(deletedFieldName, false);
    return getDataModel().count(conditions)
        .getOrElseThrow(OuroborosExceptionMapper::toOuroborosException);
  }

  /**
   * 按主键和附加条件查询未删除记录
   *
   * @param statement
   * @return
   */
  @Override
  public M getByPrimaryKey(PK primaryKey, Map<String, Object> statement) throws OuroborosException {
    return super.getByPrimaryKey(primaryKey, activeRecordStatement(statement));
  }

  /**
   * 通过 Map 查询列表
   *
   * @param statement
   * @return
   */
  @Override
  public List<? extends M> searchList(Map<String, Object> statement) throws OuroborosException {
    return super.searchList(activeRecordStatement(statement));
  }

  private Map<String, Object> activeRecordStatement(Map<String, Object> statement) {
    Try<String> deletedFieldNameTry = getDeletedFieldName();
    if (deletedFieldNameTry.isFailure()) {
      throw new PlatformException("获取软删除字段名称失败", deletedFieldNameTry.getCause());
    }

    String deletedFieldName = deletedFieldNameTry.get();

    Map<String, Object> filtered = statement == null ? new HashMap<>() : new HashMap<>(statement);
    getStatementWhere(filtered).put(deletedFieldName, false);
    return filtered;
  }

  /**
   * 获取statement中的where字段
   *
   * @param statement
   */
  protected Map<String, Object> getStatementWhere(Map<String, Object> statement) {
    String whereKey = statement.keySet().stream()
        .filter(key -> "where".equalsIgnoreCase(key))
        .findFirst()
        .orElse("where");

    Map<String, Object> where = new HashMap<>();
    Object existing = statement.get(whereKey);
    if (existing != null) {
      if (!(existing instanceof Map<?, ?> conditions)) {
        throw new PlatformException("where 必须是 Map", null);
      }
      conditions.forEach((key, value) -> where.put((String) key, value));
    }
    statement.put(whereKey, where);
    return where;
  }

  /**
   * 获取数据模型软删除字段名称
   *
   * @return
   */
  protected Try<String> getDeletedFieldName() {
    Class<E> modelClass = getModelClass();
    return Try.of(() ->
        (String)
            modelClass.getMethod("getDeletedFieldName").invoke(modelClass.getDeclaredConstructor().newInstance())
    );
  }
}
