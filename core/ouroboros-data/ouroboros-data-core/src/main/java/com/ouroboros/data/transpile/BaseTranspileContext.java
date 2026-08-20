package com.ouroboros.data.transpile;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.ouroboros.data.dsl.ModelFieldPath;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelCenter;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.station.DataStation;
import com.ouroboros.data.util.DataServices;
import com.querydsl.core.types.Path;
import com.querydsl.sql.RelationalPathBase;

/**
 * 基础 TranspileContext 实现
 *
 * <p>职责：
 * <ul>
 *   <li>解析主表字段</li>
 *   <li>通过 SPI 查找 SExpressionTranspiler</li>
 * </ul>
 *
 * <p>使用场景：
 * <ul>
 *   <li>FromBuilder 创建，作为 Context 链的起点</li>
 *   <li>是 Context 链的最内层</li>
 * </ul>
 *
 * @since 1.0.0-beta.2
 */
public class BaseTranspileContext implements TranspileContext {

  private final FieldSource mainTable;
  private final String mainAlias;
  private final QueryTranspiler queryTranspiler;
  private final TranspileContext parentContext;

  /**
   * 构造基础 Context
   *
   * @param mainTable 主表 FieldSource
   * @param mainAlias 主表别名
   */
  public BaseTranspileContext(FieldSource mainTable, String mainAlias) {
    this(mainTable, mainAlias, QueryTranspiler.DEFAULT, null);
  }

  /**
   * 构造基础 Context（带 QueryTranspiler）
   *
   * @param mainTable       主表 FieldSource
   * @param mainAlias       主表别名
   * @param queryTranspiler QueryTranspiler 实例
   */
  public BaseTranspileContext(FieldSource mainTable, String mainAlias, QueryTranspiler queryTranspiler) {
    this(mainTable, mainAlias, queryTranspiler, null);
  }

  /**
   * 构造基础 Context（带 QueryTranspiler 和 parent）
   *
   * @param mainTable       主表 FieldSource
   * @param mainAlias       主表别名
   * @param queryTranspiler QueryTranspiler 实例
   * @param parentContext   父 Context，resolveTable 找不到时回退
   */
  public BaseTranspileContext(FieldSource mainTable, String mainAlias,
                              QueryTranspiler queryTranspiler, TranspileContext parentContext) {
    this.mainTable = mainTable;
    this.mainAlias = mainAlias;
    this.queryTranspiler = queryTranspiler;
    this.parentContext = parentContext;
  }

  // ========== 核心 resolve API ==========

  @Override
  public Optional<Path<?>> resolve(String field) {
    // 尝试直接查找
    Optional<Path<?>> result = mainTable.getField(field);
    if (result.isPresent()) {
      return result;
    }
    // 尝试拆分 "table.field" 格式
    int dotIndex = field.indexOf('.');
    if (dotIndex > 0) {
      String table = field.substring(0, dotIndex);
      String fieldName = field.substring(dotIndex + 1);
      return resolve(table, fieldName);
    }
    if (parentContext != null) {
      return parentContext.resolve(field);
    }
    return Optional.empty();
  }

  @Override
  public Optional<Path<?>> resolve(String tableOrAlias, String field) {
    // 检查是否匹配主表别名或表名
    if (matchesMainTable(tableOrAlias)) {
      return mainTable.getField(field);
    }
    if (parentContext != null) {
      var resolved = parentContext.resolve(tableOrAlias, field);
      if (resolved.isPresent()) {
        return resolved;
      }
    }
    return resolveRelatedModelFieldSource(tableOrAlias).flatMap(source -> source.getField(field));
  }

  @Override
  public Optional<FieldSource> resolveTable(String nameOrAlias) {
    if (matchesMainTable(nameOrAlias)) {
      return Optional.of(mainTable);
    }
    if (parentContext != null) {
      var resolved = parentContext.resolveTable(nameOrAlias);
      if (resolved.isPresent()) {
        return resolved;
      }
    }
    return resolveRelatedModelFieldSource(nameOrAlias);
  }

  // ========== SExpressionTranspiler 查找 ==========

  @Override
  public Optional<SExpressionTranspiler> getSExpressionTranspiler(SExpression<?> expr) {
    // 通过 SPI 查找
    return DataServices.getServiceStream(SExpressionTranspiler.class)
        .filter(t -> t.support(expr))
        .max(DataServices::sortByPriority);
  }

  // ========== QueryTranspiler 获取 ==========

  @Override
  public QueryTranspiler getQueryTranspiler() {
    return queryTranspiler;
  }

  // ========== 辅助方法 ==========

  /**
   * 检查给定名称是否匹配主表
   *
   * @param nameOrAlias 表名或别名
   * @return 是否匹配
   */
  private boolean matchesMainTable(String nameOrAlias) {
    if (nameOrAlias == null) {
      return false;
    }
    // 匹配别名
    if (mainAlias != null && mainAlias.equals(nameOrAlias)) {
      return true;
    }
    // 匹配表名
    String tableName = mainTable.getName();
    return tableName != null && tableName.equals(nameOrAlias);
  }

  private Optional<FieldSource> resolveRelatedModelFieldSource(String nameOrAlias) {
    return mainTable.getDataModel()
        .flatMap(mainModel -> DataModelCenter.getDataModel(nameOrAlias)
            .filter(candidate -> isSameDataStation(mainModel, candidate)))
        .map(this::createDataModelFieldSource);
  }

  private boolean isSameDataStation(DataModel mainModel, DataModel candidateModel) {
    DataStation<?> mainStation = mainModel.getDataStation();
    DataStation<?> candidateStation = candidateModel.getDataStation();
    if (mainStation == candidateStation) {
      return mainStation != null;
    }
    if (mainStation == null || candidateStation == null) {
      return false;
    }
    String mainStationName = mainStation.getName();
    String candidateStationName = candidateStation.getName();
    return mainStationName != null && mainStationName.equals(candidateStationName);
  }

  private FieldSource createDataModelFieldSource(DataModel model) {
    Path<?> tablePath = new RelationalPathBase<>(Object.class, model.getRawName(), "", model.getRawName());
    return new FieldSource() {
      @Override
      public Optional<Path<?>> getField(String fieldName) {
        return model.getField(fieldName)
            .map(field -> ModelFieldPath.of(Object.class, field));
      }

      @Override
      public List<Path<?>> getFields() {
        return model.getFields().stream()
            .map(field -> (Path<?>) ModelFieldPath.of(Object.class, field))
            .collect(Collectors.toList());
      }

      @Override
      public Path<?> getSelfPath() {
        return tablePath;
      }

      @Override
      public Optional<DataModel> getDataModel() {
        return Optional.of(model);
      }
    };
  }
}
