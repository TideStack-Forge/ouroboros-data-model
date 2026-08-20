package com.ouroboros.data.transpile.clausebuilder;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
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
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.sql.H2Templates;
import com.querydsl.sql.RelationalPathBase;
import com.querydsl.sql.SQLQuery;

@DisplayName("SelectBuilder wildcard 测试")
class SelectBuilderWildcardTest {

  @Test
  @DisplayName("纯 wildcard SELECT 应展开主表列并生成有效投影")
  void shouldExpandPureWildcardProjection() {
    RelationalPathBase<Object> tablePath = new RelationalPathBase<>(Object.class, "orders", "", "orders");
    FieldSource fieldSource = createFieldSource(tablePath);
    BaseTranspileContext context = new BaseTranspileContext(fieldSource, "Order");

    QueryStatement query = QueryStatement.builder()
        .from("orders", "Order")
        .select(SExpression.create(com.ouroboros.data.dsl.Operators.COLUMNS, "*"))
        .build();

    String sql = assertDoesNotThrow(() -> new SQLQuery<>(
        null,
        H2Templates.builder().quote().build(),
        new DefaultQueryTranspiler().applyWithContext(query, context).get())
        .getSQL()
        .getSQL());

    assertTrue(sql.toLowerCase().contains("select"), "纯 wildcard 选择不应生成缺失 SELECT 的 SQL");
    assertTrue(sql.contains("id"), "纯 wildcard 应展开主表字段");
    assertTrue(sql.contains("status"), "纯 wildcard 应包含主表字段列表");
  }

  @Test
  @DisplayName("混合 wildcard SELECT 应展开主表列并保留显式别名列")
  void shouldExpandMixedWildcardAndKeepExplicitAliasProjection() {
    RelationalPathBase<Object> tablePath = new RelationalPathBase<>(Object.class, "orders", "", "orders");
    FieldSource fieldSource = createFieldSource(tablePath);
    BaseTranspileContext context = new BaseTranspileContext(fieldSource, "Order");

    QueryStatement query = QueryStatement.builder()
        .from("orders", "Order")
        .select(SExpression.create(
            com.ouroboros.data.dsl.Operators.COLUMNS,
            "*",
            SExpression.alias(SExpression.field("Order", "status"), "statusLabel")))
        .build();

    String sql = assertDoesNotThrow(() -> new SQLQuery<>(
        null,
        H2Templates.builder().quote().build(),
        new DefaultQueryTranspiler().applyWithContext(query, context).get())
        .getSQL()
        .getSQL());

    assertTrue(sql.contains("id"), "混合 wildcard 应展开主表字段而不是忽略 *");
    assertTrue(sql.contains("statusLabel"), "显式别名列不应因 wildcard 展开而丢失");
  }

  @Test
  @DisplayName("隐式默认 SELECT 应忽略无法直接解析的点号字段")
  void shouldIgnoreUnresolvableDottedFieldsForImplicitDefaultSelect() {
    RelationalPathBase<Object> tablePath = new RelationalPathBase<>(Object.class, "orders", "", "orders");
    FieldSource fieldSource = createFieldSourceWithUnresolvableDottedField(tablePath);
    BaseTranspileContext context = new BaseTranspileContext(fieldSource, "Order");

    QueryStatement query = QueryStatement.builder()
        .from("orders", "Order")
        .build();

    String sql = assertDoesNotThrow(() -> new SQLQuery<>(
        null,
        H2Templates.builder().quote().build(),
        new DefaultQueryTranspiler().applyWithContext(query, context).get())
        .getSQL()
        .getSQL());

    assertTrue(sql.contains("id"), "默认 SELECT 应保留可解析字段");
    assertTrue(sql.contains("status"), "默认 SELECT 应保留普通主表字段");
    assertTrue(!sql.contains("aEntryDisplayId.id"), "默认 SELECT 不应包含无法解析的点号字段");
  }

  private FieldSource createFieldSource(RelationalPathBase<Object> tablePath) {
    DataModelField idField = new TestDataModelField("id", "id");
    DataModelField statusField = new TestDataModelField("status", "status");
    return new FieldSource() {
      @Override
      public Optional<Path<?>> getField(String fieldName) {
        return switch (fieldName) {
          case "id" -> Optional.of(com.ouroboros.data.dsl.ModelFieldPath.of(Object.class, idField));
          case "status" -> Optional.of(com.ouroboros.data.dsl.ModelFieldPath.of(Object.class, statusField));
          default -> Optional.empty();
        };
      }

      @Override
      public List<Path<?>> getFields() {
        return Arrays.asList(
            com.ouroboros.data.dsl.ModelFieldPath.of(Object.class, idField),
            com.ouroboros.data.dsl.ModelFieldPath.of(Object.class, statusField));
      }

      @Override
      public Path<?> getSelfPath() {
        return tablePath;
      }
    };
  }

  private FieldSource createFieldSourceWithUnresolvableDottedField(RelationalPathBase<Object> tablePath) {
    DataModelField idField = new TestDataModelField("id", "id");
    DataModelField statusField = new TestDataModelField("status", "status");
    Path<?> unresolvedDottedField = Expressions.path(Object.class, tablePath, "aEntryDisplayId.id");
    return new FieldSource() {
      @Override
      public Optional<Path<?>> getField(String fieldName) {
        return switch (fieldName) {
          case "id" -> Optional.of(com.ouroboros.data.dsl.ModelFieldPath.of(Object.class, idField));
          case "status" -> Optional.of(com.ouroboros.data.dsl.ModelFieldPath.of(Object.class, statusField));
          default -> Optional.empty();
        };
      }

      @Override
      public List<Path<?>> getFields() {
        return Arrays.asList(
            com.ouroboros.data.dsl.ModelFieldPath.of(Object.class, idField),
            com.ouroboros.data.dsl.ModelFieldPath.of(Object.class, statusField),
            unresolvedDottedField);
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
    private final ValueType<Object> valueType = new ValueType<>() {
      @Override
      public String getName() {
        return "test";
      }

      @Override
      public String getLabel() {
        return "test";
      }

      @Override
      public Class<Object> getType() {
        return Object.class;
      }

      @Override
      public Object convert(Object value) {
        return value;
      }
    };

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
      return valueType;
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
