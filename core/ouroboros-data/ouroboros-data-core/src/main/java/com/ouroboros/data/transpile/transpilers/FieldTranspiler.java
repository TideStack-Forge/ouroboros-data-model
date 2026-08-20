package com.ouroboros.data.transpile.transpilers;

import java.util.Arrays;
import java.util.List;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.ExtOps;
import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.transpile.FieldSource;
import com.ouroboros.data.transpile.SExpressionTranspiler;
import com.ouroboros.data.transpile.TranspileContext;
import com.ouroboros.data.exception.TranspileException;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Projections;

public class FieldTranspiler implements SExpressionTranspiler {

  @Override
  @SuppressWarnings("unchecked")
  public Try<Expression<?>> apply(SExpression<?> sExpr, TranspileContext context) {
    List<?> params = sExpr.getParams();

    if (params.isEmpty()) {
      return Try.failure(new TranspileException("FIELD 表达式缺少参数"));
    }

    // 单参数：使用 context.resolve()
    if (params.size() == 1) {
      return applySingleParam(params.get(0).toString(), context);
    }

    // 多参数：使用 context.resolve(String...)
    return applyMultiParam(params, context);
  }

  /**
   * 处理单参数 FIELD（使用 TranspileContext）
   */
  @SuppressWarnings("unchecked")
  private Try<Expression<?>> applySingleParam(String path, TranspileContext context) {
    // DataModel 查询路径不应再依赖 WildcardExpandAnalyzer 展开 relation.*。
    // 此处的 * 处理仅作为 DataAdapter 最短路径（Normalize → Transpile）下的 fallback。
    if (path.endsWith("*")) {
      if (path.length() == 1) {
        return Try.failure(new TranspileException("不支持使用 * 号，请使用具体字段"));
      }
      var parentPath = path.substring(0, path.length() - 2);
      var fields = context.resolveTable(parentPath)
          .map(FieldSource::getFields);
      if (!fields.isPresent()) {
        return Try.failure(new TranspileException(parentPath + "不存在"));
      }

      return fields
          .filter(list -> !list.isEmpty())
          .map(list -> Projections.list((List<Expression<?>>) (List<?>) list))
          .<Try<Expression<?>>>map(Try::success)
          .orElseGet(() -> Try.failure(new TranspileException(parentPath + "不支持使用 * 号，请使用具体字段")));
    }

    return context.resolve(path)
        .<Try<Expression<?>>>map(Try::success)
        .orElseGet(() -> Try.failure(new TranspileException("字段不存在：" + path)));
  }

  /**
   * 处理多参数 FIELD（使用 TranspileContext）
   */
  private Try<Expression<?>> applyMultiParam(List<?> params, TranspileContext context) {
    // 转换参数为字符串数组
    String[] pathSegments = new String[params.size()];
    for (int i = 0; i < params.size(); i++) {
      Object param = params.get(i);
      if (!(param instanceof String)) {
        return Try.failure(new TranspileException(
            "FIELD 多参数表达式的参数必须为字符串: " + param));
      }
      pathSegments[i] = (String) param;
    }

    // 使用 context.resolve(String, String, ...)
    if (pathSegments.length == 2) {
      return context.resolve(pathSegments[0], pathSegments[1])
          .<Try<Expression<?>>>map(Try::success)
          .orElseGet(() -> Try.failure(new TranspileException(
              "无法解析路径: " + String.join(".", pathSegments))));
    }

    // 多于2个参数，拼接前缀路径
    String prefix = String.join(".", Arrays.copyOfRange(pathSegments, 0, pathSegments.length - 1));
    String lastSegment = pathSegments[pathSegments.length - 1];
    return context.resolve(prefix, lastSegment)
        .<Try<Expression<?>>>map(Try::success)
        .orElseGet(() -> Try.failure(new TranspileException(
            "无法解析多级路径: " + String.join(".", pathSegments))));
  }

  @Override
  public Boolean support(SExpression<?> sExpr) {
    return Operators.FIELD == sExpr.getOperator() || ExtOps.FIELD == sExpr.getOperator();
  }
}
