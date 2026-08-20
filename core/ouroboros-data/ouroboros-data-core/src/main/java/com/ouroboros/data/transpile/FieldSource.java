package com.ouroboros.data.transpile;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.ouroboros.data.dsl.ModelFieldPath;
import com.ouroboros.data.model.DataModel;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.sql.RelationalPath;

/**
 * 字段来源接口
 *
 * <p>表示一个可以提供字段的来源（表、子查询、CTE 等）。
 *
 * @since 1.0.0-beta.2
 */
public interface FieldSource {
  static Path<?> transformParent(Path<?> columnPath, Path<?> parent) {
    var finalParent = Optional.ofNullable(parent)
        .map(Path::getMetadata)
        .map(PathMetadata::getName)
        .filter(s -> !s.isEmpty())
        .map(s -> parent)
        .orElse(null);
    if (columnPath instanceof ModelFieldPath<?> modelFieldPath) {
      return modelFieldPath.transformParent(finalParent);
    }
    return Expressions.simplePath(columnPath.getType(), finalParent, columnPath.getMetadata().getName());
  }

  /**
   * 获取指定字段的 Path
   *
   * @param fieldName 字段名
   * @return 字段 Path，如果不存在返回 empty
   */
  Optional<Path<?>> getField(String fieldName);

  /**
   * 获取所有字段的 Path 列表
   *
   * @return 字段 Path 列表
   */
  List<Path<?>> getFields();

  /**
   * 获取自身路径（表路径）
   *
   * @return 表路径
   */
  Path<?> getSelfPath();

  /**
   * 获取来源名称（表名、别名等）
   *
   * <p>默认实现：从 {@link #getSelfPath()} 的 metadata 中提取名称。
   *
   * @return 来源名称
   * @since 1.0.0-beta.2
   */
  default String getName() {
    Path<?> selfPath = getSelfPath();
    if (selfPath == null) {
      return "";
    }
    PathMetadata metadata = selfPath.getMetadata();
    return metadata != null ? metadata.getName() : "";
  }

  /**
   * 获取所有字段名
   *
   * <p>默认实现：从 {@link #getFields()} 中提取字段名。
   *
   * @return 字段名集合
   * @since 1.0.0-beta.2
   */
  default Set<String> getFieldNames() {
    return getFields().stream()
        .map(Path::getMetadata)
        .map(PathMetadata::getName)
        .collect(Collectors.toSet());
  }

  /**
   * 获取 QueryDSL 表对象（用于 FROM/JOIN 子句）
   *
   * <p>默认实现：将 {@link #getSelfPath()} 转换为 RelationalPath。
   *
   * @return QueryDSL 表对象
   * @since 1.0.0-beta.2
   */
  default RelationalPath<?> getTable() {
    Path<?> selfPath = getSelfPath();
    if (selfPath instanceof RelationalPath) {
      return (RelationalPath<?>) selfPath;
    }
    // 如果不是 RelationalPath，返回 null（子查询等场景）
    return null;
  }

  /**
   * 获取关联的 DataModel（可选）
   *
   * <p>用于 Context 访问 DataModel 信息，支持测试场景和特殊需求。
   *
   * @return DataModel，如果此 FieldSource 不关联 DataModel 则返回 empty
   * @since 1.0.0-beta.2
   */
  default Optional<DataModel> getDataModel() {
    return Optional.empty();
  }
}
