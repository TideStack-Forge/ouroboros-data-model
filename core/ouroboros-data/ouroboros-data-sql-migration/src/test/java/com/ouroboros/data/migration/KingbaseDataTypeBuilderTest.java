package com.ouroboros.data.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.migration.kingbase.KingbaseDatabase;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.ValueType;
import com.ouroboros.data.model.valuetypes.BooleanValue;
import com.ouroboros.data.model.valuetypes.DateTimeValue;
import com.ouroboros.data.model.valuetypes.DoubleValue;
import com.ouroboros.data.model.valuetypes.FloatValue;
import com.ouroboros.data.model.valuetypes.ShortValue;

import liquibase.CatalogAndSchema;
import liquibase.database.Database;
import liquibase.database.core.H2Database;
import liquibase.datatype.DataTypeFactory;
import liquibase.structure.core.Table;

class KingbaseDataTypeBuilderTest {

  @Test
  void shouldApplyKingbaseStrategiesAndPreserveRawTypes() {
    var fields = Arrays.asList(
        field("logical_boolean", new BooleanValue(), null),
        field("logical_float", new FloatValue(), null),
        field("date_time", new DateTimeValue(), null),
        field("double_value", new DoubleValue(), null),
        field("short_value", new ShortValue(), null),
        field("raw_bit", new BooleanValue(), "BIT"),
        field("raw_float", new FloatValue(), "FLOAT"));
    var database = new KingbaseDatabase();

    Table table = TableBuilder.build(database, model(fields), new CatalogAndSchema(null, null));

    assertEquals("BOOLEAN", table.getColumn("logical_boolean").getType().getTypeName());
    assertEquals("REAL", table.getColumn("logical_float").getType().getTypeName());
    assertNativeType(table, database, "date_time", "TIMESTAMP WITHOUT TIME ZONE");
    assertNativeType(table, database, "double_value", "DOUBLE PRECISION");
    assertNativeType(table, database, "short_value", "SMALLINT");
    assertEquals("BIT", table.getColumn("raw_bit").getType().getTypeName());
    assertEquals("FLOAT", table.getColumn("raw_float").getType().getTypeName());
  }

  @Test
  void shouldKeepDefaultBuildersForOtherDatabases() {
    var fields = Arrays.asList(
        field("logical_boolean", new BooleanValue(), null),
        field("logical_float", new FloatValue(), null));

    Table table = TableBuilder.build(new H2Database(), model(fields), new CatalogAndSchema(null, null));

    assertEquals("BIT", table.getColumn("logical_boolean").getType().getTypeName());
    assertEquals("FLOAT", table.getColumn("logical_float").getType().getTypeName());
  }

  private static void assertNativeType(Table table, Database database, String columnName, String expected) {
    var dataType = table.getColumn(columnName).getType();
    var nativeType = DataTypeFactory.getInstance().from(dataType, database).toDatabaseDataType(database);
    assertEquals(expected, nativeType.toString());
  }

  private static DataModelField field(String name, ValueType<?> valueType, String rawType) {
    return (DataModelField) Proxy.newProxyInstance(
        KingbaseDataTypeBuilderTest.class.getClassLoader(),
        new Class[]{DataModelField.class},
        (proxy, method, args) -> switch (method.getName()) {
          case "getName", "getRawName", "getLabel" -> name;
          case "getValueType" -> valueType;
          case "getRawType" -> rawType;
          case "getIsNullable" -> true;
          case "getIsUnsigned", "getIsAutoIncrement", "getIsUnique" -> false;
          case "getRules" -> Collections.emptyList();
          case "getExtraProps" -> Collections.emptyMap();
          default -> defaultValue(method.getReturnType());
        });
  }

  private static DataModel model(List<DataModelField> fields) {
    return (DataModel) Proxy.newProxyInstance(
        KingbaseDataTypeBuilderTest.class.getClassLoader(),
        new Class[]{DataModel.class},
        (proxy, method, args) -> switch (method.getName()) {
          case "getName", "getRawName" -> "kingbase_type_matrix";
          case "getFullName" -> "test.kingbase_type_matrix";
          case "getLabel" -> "Kingbase type matrix";
          case "getFields" -> fields;
          case "getPrimaryKeys" -> Collections.emptyList();
          default -> defaultValue(method.getReturnType());
        });
  }

  private static Object defaultValue(Class<?> returnType) {
    if (returnType == Boolean.TYPE) {
      return false;
    }
    if (returnType == Integer.TYPE) {
      return 0;
    }
    if (returnType == Long.TYPE) {
      return 0L;
    }
    return null;
  }
}
