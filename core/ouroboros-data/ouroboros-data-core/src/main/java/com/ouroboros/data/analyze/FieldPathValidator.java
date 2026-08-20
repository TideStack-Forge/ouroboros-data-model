package com.ouroboros.data.analyze;

import java.util.List;

import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.exception.NormalizeException;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.FieldPathResolver;
import com.ouroboros.data.model.FieldPathResolver.ResolvedFieldPath;
import com.ouroboros.data.model.valuetypes.RelatedValue;

/**
 * 多段 FIELD 路径校验工具类
 *
 * <p>逐段验证：字段存在性 → 中间段为关联字段 → 通过关联模型继续验证。
 * 无法获取关联模型时降级为浅层验证（保持 currentModel 不变）。
 *
 * @since 1.0.0-beta.2
 */
public final class FieldPathValidator {

  private FieldPathValidator() {}

  /**
   * 验证多段 FIELD 路径的有效性。
   *
   * @param fieldExpr   多参数 FIELD 表达式
   * @param rootModel   起始数据模型
   * @param clauseType  子句类型（用于错误消息，可为 null）
   */
  public static void validateFieldPath(SExpression<?> fieldExpr, DataModel rootModel, String clauseType) {
    List<Object> pathSegments = fieldExpr.getParams();
    String fullPath = String.join(".", pathSegments.stream().map(Object::toString).toArray(String[]::new));
    ResolvedFieldPath resolved = FieldPathResolver.resolve(fieldExpr, rootModel)
        .orElseThrow(() -> buildInvalidPathException(rootModel, pathSegments, clauseType, fullPath));

    if (resolved.getPathSegments().size() != pathSegments.size()) {
      throw buildInvalidPathException(rootModel, pathSegments, clauseType, fullPath);
    }
  }

  public static void validateFieldPath(SExpression<?> fieldExpr, DataModel rootModel, String clauseType,
                                       QueryStatement statement) {
    validateFieldPath(FieldPathResolver.stripRootSourceQualifier(fieldExpr, statement), rootModel, clauseType);
  }

  private static NormalizeException buildInvalidPathException(
      DataModel rootModel, List<Object> pathSegments, String clauseType, String fullPath) {
    DataModel currentModel = rootModel;

    for (int i = 0; i < pathSegments.size(); i++) {
      String segment = pathSegments.get(i).toString();
      var fieldOpt = currentModel.getField(segment);
      if (!fieldOpt.isPresent()) {
        String msg = "字段路径无效：模型 '" + currentModel.getName() + "' 中不存在字段 '" + segment + "'";
        if (clauseType != null) {
          msg += " (" + clauseType + " 子句, 完整路径: " + fullPath + ")";
        } else {
          msg += "\n完整路径：" + fullPath;
        }
        return new NormalizeException(msg);
      }

      var field = fieldOpt.get();
      if (i < pathSegments.size() - 1) {
        String fieldType = field.getType();
        if (!"Model".equals(fieldType) && !"Collection".equals(fieldType)) {
          String msg = "字段路径无效：'" + segment + "' 不是关联字段（类型: " + fieldType + "），无法继续路径访问";
          if (clauseType != null) {
            msg += " (" + clauseType + " 子句, 完整路径: " + fullPath + ")";
          } else {
            msg += "\n完整路径：" + fullPath;
          }
          return new NormalizeException(msg);
        }

        if (field.getValueType() instanceof RelatedValue<?> relatedValue) {
          currentModel = relatedValue.getReferenceModel().orElse(currentModel);
        }
      }
    }

    return new NormalizeException("字段路径无效\n完整路径：" + fullPath);
  }
}
