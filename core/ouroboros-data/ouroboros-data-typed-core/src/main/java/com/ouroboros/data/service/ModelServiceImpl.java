package com.ouroboros.data.service;

import com.ouroboros.common.service.ModelService;

import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;

import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.TypedDataModel;
import com.ouroboros.data.model.TypedDataModelCenter;
import com.ouroboros.data.model.valuetypes.RelatedValue;
import com.ouroboros.exception.InternalException;
import com.ouroboros.exception.OuroborosException;
import com.ouroboros.exception.PlatformException;
import com.ouroboros.exception.mapper.OuroborosExceptionMapper;

/**
 * 业务模型服务基类
 *
 * @param <PK> 主键类型
 * @param <M>  业务模型类型
 * @param <E>  数据模型类型
 * @author liansz
 */
public class ModelServiceImpl<PK, M, E extends M> implements ModelService<PK, M> {

  @Override
  public M add(M model) throws OuroborosException {
    var result = getDataModel().insert(model);
    if (result.isSuccess()) {
      return result.get();
    }

    throw OuroborosExceptionMapper.toOuroborosException(result.getCause(), cause -> new InternalException("插入数据失败", cause));
  }

  @Override
  public List<? extends M> add(Collection<? extends M> models) throws OuroborosException {
    var result = getDataModel().batchInsert(new ArrayList<>(models));
    if (result.isSuccess()) {
      return result.get();
    }

    throw OuroborosExceptionMapper.toOuroborosException(result.getCause(), cause -> new InternalException("批量插入数据失败", cause));
  }

  @Override
  public Boolean delete(PK id) throws OuroborosException {
    var result = getDataModel().delete(id);
    if (result.isSuccess()) {
      return result.get() > 0;
    }

    throw OuroborosExceptionMapper.toOuroborosException(result.getCause(), cause -> new InternalException("删除数据失败", cause));
  }

  @Override
  public Boolean delete(Collection<PK> ids) throws OuroborosException {
    var result = getDataModel().delete(new ArrayList<>(ids));
    if (result.isSuccess()) {
      return result.get() == ids.size();
    }

    throw OuroborosExceptionMapper.toOuroborosException(result.getCause(), cause -> new InternalException("批量删除数据失败", cause));
  }

  @Override
  public Boolean update(PK id, M model) throws OuroborosException {
    var result = getDataModel().update(id, model);
    if (result.isSuccess()) {
      return result.get() > 0;
    }

    throw OuroborosExceptionMapper.toOuroborosException(result.getCause(), cause -> new InternalException("更新数据失败", cause));
  }

  @Override
  public Boolean partialUpdate(PK id, Map<String, Object> map) throws OuroborosException {
    var result = getDataModel().update(id, map);
    if (result.isSuccess()) {
      return result.get() > 0;
    }

    throw OuroborosExceptionMapper.toOuroborosException(result.getCause(), cause -> new InternalException("部分更新数据失败", cause));
  }

  @Override
  public M getByPrimaryKey(PK primaryKey) throws OuroborosException {
    TypedDataModel<PK, M> typedDataModel = getDataModel();
    DataModel dataModel = typedDataModel.getDataModel();

    // 获取需要填充的字段
    List<String> populateFieldNames = dataModel.getFields().stream()
        .filter(f -> f.getValueType() instanceof RelatedValue)
        .map(DataModelField::getName)
        .collect(Collectors.toList());

    Map<String, Object> statement = new HashMap<>();
    statement.put("POPULATE", populateFieldNames);

    return getByPrimaryKey(primaryKey, statement);
  }

  @Override
  public M getByPrimaryKey(PK primaryKey, Map<String, Object> statement) throws OuroborosException {
    var result = getDataModel().get(primaryKey, statement != null ? statement : new HashMap<>());
    if (result.isSuccess()) {
      return result.get();
    }

    throw OuroborosExceptionMapper.toOuroborosException(result.getCause(), cause -> new InternalException("根据主键获取数据失败", cause));
  }

  @Override
  public Long count(Map<String, Object> where) throws OuroborosException {
    var result = getDataModel().count(where);
    if (result.isSuccess()) {
      return result.get();
    }

    throw OuroborosExceptionMapper.toOuroborosException(result.getCause(), cause -> new InternalException("统计数据失败", cause));
  }

  /**
   * 通过 Map 查询一条
   *
   * @param statement
   * @return
   */
  @Override
  public M searchOne(Map<String, Object> statement) throws OuroborosException {
    List<? extends M> list = searchList(statement);

    if (ObjectUtils.isEmpty(list)) {
      return null;
    }

    if (list.size() > 1) {
      throw new PlatformException("查询结果不唯一", null);
    }

    return list.get(0);
  }

  /**
   * 通过 Map 查询列表
   *
   * @param statement
   * @return
   */
  @Override
  public List<? extends M> searchList(Map<String, Object> statement) throws OuroborosException {
    var result = getDataModel().query(statement);
    if (result.isSuccess()) {
      return result.get();
    }

    throw OuroborosExceptionMapper.toOuroborosException(result.getCause(), cause -> new InternalException("查询数据失败", cause));
  }

  @Override
  public List<? extends M> searchAll() throws OuroborosException {
    return searchList(new HashMap<>());
  }

  /**
   * 获取数据模型
   *
   * @return
   */
  protected TypedDataModel<PK, M> getDataModel() throws OuroborosException {
    return (TypedDataModel<PK, M>) getDataModel(getPrmaryKeyClass(), getModelClass());
  }

  /**
   * 获取数据模型
   *
   * @param pk
   * @param model
   * @return
   */
  protected <PK1, M1> TypedDataModel<PK1, M1> getDataModel(Class<PK1> pk, Class<M1> model) throws OuroborosException {
    return TypedDataModelCenter.getDataModel(pk, model)
        .orElseThrow(() -> new InternalException(String.format("类型为 %s 的数据模型不存在", model.getName()), null));
  }

  /**
   * 获取模型类
   *
   * @return
   */
  protected Class<PK> getPrmaryKeyClass() {
    return getSuperclassActualArgumentType(0);
  }

  /**
   * 获取模型类
   *
   * @return
   */
  protected Class<E> getModelClass() {
    return getSuperclassActualArgumentType(2);
  }

  /**
   * 获取超类实际参数类型
   *
   * @param index
   * @param <T>
   * @return
   */
  protected <T> T getSuperclassActualArgumentType(int index) {
    var genericSuperclass = getClass().getGenericSuperclass();
    while (!(genericSuperclass instanceof ParameterizedType) && genericSuperclass != null) {
      genericSuperclass = genericSuperclass instanceof Class<?>
          ? ((Class<?>) genericSuperclass).getGenericSuperclass()
          : null;
    }
    if (!(genericSuperclass instanceof ParameterizedType)) {
      throw new InternalException("获取超类实际参数类型失败", null);
    }
    return (T) ((ParameterizedType) genericSuperclass).getActualTypeArguments()[index];
  }
}
