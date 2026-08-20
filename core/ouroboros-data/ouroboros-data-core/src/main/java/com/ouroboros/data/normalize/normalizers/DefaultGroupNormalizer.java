package com.ouroboros.data.normalize.normalizers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.ouroboros.data.dsl.ExtOps;
import com.ouroboros.data.dsl.Keyword;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.exception.NormalizeException;
import com.ouroboros.data.normalize.ClauseNormalizeContext;
import com.ouroboros.data.normalize.ClauseNormalizer;

/**
 * GROUP BY子句规范化器（上下文感知版本）
 */
public class DefaultGroupNormalizer implements ClauseNormalizer {

  @Override
  public boolean supports(String clauseType) {
    return "QUERY".equalsIgnoreCase(clauseType) || "GROUP".equalsIgnoreCase(clauseType);
  }

  @Override
  public QueryStatement.QueryStatementBuilder normalize(Map<String, ?> clauseData,
                                                        QueryStatement.QueryStatementBuilder builder,
                                                        ClauseNormalizeContext context) {
    // 支持多种key：group, groupBy, group_by
    Object groupRaw = Keyword.GROUP.findIn(clauseData);

    if (groupRaw == null) {
      return builder;
    }

    SExpression<?> groupFields;

    if (groupRaw instanceof SExpression<?> normalizedGroup) {
      groupFields = normalizedGroup;
    } else if (groupRaw instanceof String groupStr) {
      // 字符串形式：field1, field2, field3
      List<SExpression<?>> fields = Arrays.stream(groupStr.split(","))
          .map(String::trim)
          .map(SExpression::field)
          .collect(Collectors.toList());
      groupFields = SExpression.columns(fields);
    } else if (groupRaw instanceof List<?> groupList) {
      // 列表形式
      List<SExpression<?>> fields = new ArrayList<>();
      for (int i = 0; i < groupList.size(); i++) {
        Object item = groupList.get(i);
        if (item instanceof String fieldName) {
          fields.add(SExpression.field(fieldName));
          continue;
        }
        fields.add(context.normalizeExpression(item, "group[" + i + "]").get());
      }
      groupFields = SExpression.columns(fields);
    } else {
      throw new NormalizeException("GROUP BY 子句类型不支持: " + groupRaw.getClass().getName()
          + ", 仅支持 String 或 List");
    }

    builder.group(groupFields);
    return builder;
  }
}
