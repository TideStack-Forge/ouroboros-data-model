package com.ouroboros.data.transpile.transpilers;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ouroboros.data.dsl.ModelFieldPath;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.ValueType;
import com.ouroboros.data.transpile.BaseTranspileContext;
import com.ouroboros.data.transpile.DefaultQueryTranspiler;
import com.ouroboros.data.transpile.DefaultOuroborosQueryMetadata;
import com.ouroboros.data.transpile.FieldSource;
import com.querydsl.core.JoinType;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.sql.H2Templates;
import com.querydsl.sql.RelationalPathBase;
import com.querydsl.sql.SQLQuery;

class AliasTranspilerSqlTest {

  @Test
  @DisplayName("ALIAS 转译应该保留模型字段的原始列名")
  void aliasTranspile_shouldKeepModelFieldRawName() {
    RelationalPathBase<Object> tablePath = new RelationalPathBase<>(Object.class, "t_user", "", "t_user");
    FieldSource fieldSource = createFieldSource(tablePath);

    BaseTranspileContext context = new BaseTranspileContext(fieldSource, "User");
    SExpression<?> expr = SExpression.alias(
        SExpression.columns(SExpression.field("User", "departmentId")),
        "departmentId");

    Expression<?> projection = new AliasTranspiler()
        .apply(expr, context)
        .get();

    DefaultOuroborosQueryMetadata metadata = new DefaultOuroborosQueryMetadata();
    metadata.addJoin(JoinType.DEFAULT, Expressions.as(tablePath, "User"));
    metadata.setProjection(Expressions.list(projection));

    String sql = new SQLQuery<>(null, H2Templates.builder().quote().build(), metadata)
        .getSQL()
        .getSQL();

    assertTrue(sql.contains("department_id"), sql);
    assertTrue(sql.contains("departmentId"), sql);
  }

  @Test
  @DisplayName("完整查询转译应该在 SELECT 中使用模型字段原始列名")
  void fullQueryTranspile_shouldKeepModelFieldRawNameInSelect() {
    RelationalPathBase<Object> tablePath = new RelationalPathBase<>(Object.class, "t_user", "", "t_user");
    FieldSource fieldSource = createFieldSource(tablePath);
    BaseTranspileContext context = new BaseTranspileContext(fieldSource, "t_user");

    QueryStatement query = QueryStatement.builder()
        .from("t_user", "User")
        .select(SExpression.alias(
            SExpression.columns(SExpression.field("User", "departmentId")),
            "departmentId"))
        .build();

    String sql = new SQLQuery<>(
        null,
        H2Templates.builder().quote().build(),
        new DefaultQueryTranspiler().applyWithContext(query, context).get())
        .getSQL()
        .getSQL();

    assertTrue(sql.contains("department_id"), sql);
    assertTrue(sql.contains("departmentId"), sql);
  }

  @Test
  @DisplayName("裸 FIELD 选择项应该自动带上逻辑字段别名")
  void fullQueryTranspile_shouldAutoAliasPlainModelFieldSelect() {
    RelationalPathBase<Object> tablePath = new RelationalPathBase<>(Object.class, "t_user", "", "t_user");
    FieldSource fieldSource = createFieldSource(tablePath);
    BaseTranspileContext context = new BaseTranspileContext(fieldSource, "t_user");

    QueryStatement query = QueryStatement.builder()
        .from("t_user", "User")
        .select(SExpression.field("User", "departmentId"))
        .group(SExpression.columns(SExpression.field("User", "departmentId")))
        .build();

    String sql = new SQLQuery<>(
        null,
        H2Templates.builder().quote().build(),
        new DefaultQueryTranspiler().applyWithContext(query, context).get())
        .getSQL()
        .getSQL();

    assertTrue(sql.contains("department_id"), sql);
    assertTrue(sql.contains("departmentId"), sql);
  }

  private FieldSource createFieldSource(RelationalPathBase<Object> tablePath) {
    DataModelField departmentField = new TestDataModelField("departmentId", "department_id");
    return new FieldSource() {
      @Override
      public Optional<Path<?>> getField(String fieldName) {
        if (!"departmentId".equals(fieldName)) {
          return Optional.empty();
        }
        return Optional.of(ModelFieldPath.of(Object.class, departmentField));
      }

      @Override
      public List<Path<?>> getFields() {
        return Collections.singletonList(ModelFieldPath.of(Object.class, departmentField));
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
    public Object getDefaultValue(Map<String, Object> context) {
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
    public Map<String, Object> getExtraProps() {
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
