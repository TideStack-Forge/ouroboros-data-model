package com.ouroboros.data.transpile.clausebuilder;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.ValueType;
import com.ouroboros.data.transpile.BaseTranspileContext;
import com.ouroboros.data.transpile.DefaultQueryTranspiler;
import com.ouroboros.data.transpile.FieldSource;
import com.querydsl.core.types.Path;
import com.querydsl.sql.H2Templates;
import com.querydsl.sql.RelationalPathBase;
import com.querydsl.sql.SQLQuery;

@DisplayName("OrderBuilder alias 测试")
class OrderBuilderAliasTest {

  @Test
  @DisplayName("ORDER BY 应可解析聚合别名")
  void shouldResolveAggregateAliasFromSelectContext() {
    RelationalPathBase<Object> tablePath = new RelationalPathBase<>(Object.class, "orders", "", "orders");
    FieldSource fieldSource = createFieldSource(tablePath);
    BaseTranspileContext context = new BaseTranspileContext(fieldSource, "Order");

    QueryStatement query = QueryStatement.builder()
        .from("orders", "Order")
        .select(
            SExpression.field("Order", "status"),
            SExpression.alias(SExpression.create(com.ouroboros.data.dsl.Operators.COUNT, SExpression.field("*")), "cnt"))
        .group(SExpression.columns(SExpression.field("Order", "status")))
        .order("cnt", "desc")
        .build();

    String sql = assertDoesNotThrow(() -> new SQLQuery<>(
        null,
        H2Templates.builder().quote().build(),
        new DefaultQueryTranspiler().applyWithContext(query, context).get())
        .getSQL()
        .getSQL());

    assertTrue(sql.toLowerCase().contains("order by"),
        "SQL 应成功生成 ORDER BY，而不是在转译阶段因为别名解析失败");
    assertTrue(sql.contains("cnt"), "ORDER BY 应保留聚合别名 cnt");
  }

  private FieldSource createFieldSource(RelationalPathBase<Object> tablePath) {
    DataModelField statusField = new TestDataModelField("status", "status");
    return new FieldSource() {
      @Override
      public Optional<Path<?>> getField(String fieldName) {
        if (!"status".equals(fieldName)) {
          return Optional.empty();
        }
        return Optional.of(com.ouroboros.data.dsl.ModelFieldPath.of(Object.class, statusField));
      }

      @Override
      public List<Path<?>> getFields() {
        return Collections.singletonList(com.ouroboros.data.dsl.ModelFieldPath.of(Object.class, statusField));
      }

      @Override
      public Path<?> getSelfPath() {
        return tablePath;
      }
    };
  }

  private static final class TestDataModelField implements DataModelField {
    private final String name;
    private final String rawName;

    private TestDataModelField(String name, String rawName) {
      this.name = name;
      this.rawName = rawName;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public String getLabel() {
      return name;
    }

    @Override
    public String getDescription() {
      return null;
    }

    @Override
    public String getType() {
      return "string";
    }

    @Override
    public String getRawName() {
      return rawName;
    }

    @Override
    public String getRawType() {
      return "VARCHAR";
    }

    @Override
    public ValueType<?> getValueType() {
      return null;
    }

    @Override
    public Object getDefaultValue(java.util.Map<String, Object> context) {
      return null;
    }

    @Override
    public List<com.ouroboros.data.validation.Rule> getRules() {
      return Collections.emptyList();
    }

    @Override
    public Integer getDecimalDigits() {
      return null;
    }

    @Override
    public Integer getSize() {
      return null;
    }

    @Override
    public Boolean getIsNullable() {
      return true;
    }

    @Override
    public Boolean getIsUnsigned() {
      return false;
    }

    @Override
    public Boolean getIsAutoIncrement() {
      return false;
    }

    @Override
    public Boolean getIsUnique() {
      return false;
    }

    @Override
    public java.util.Map<String, Object> getExtraProps() {
      return Collections.emptyMap();
    }

    @Override
    public Optional<Object> getExtraProp(String name) {
      return Optional.empty();
    }

    @Override
    public DataModel getDataModel() {
      return null;
    }
  }
}
