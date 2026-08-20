package com.ouroboros.data.migration.datatype.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.ValueType;
import com.ouroboros.data.model.valuetypes.BooleanValue;
import com.ouroboros.data.model.valuetypes.DateTimeValue;
import com.ouroboros.data.model.valuetypes.DateValue;
import com.ouroboros.data.model.valuetypes.DecimalValue;
import com.ouroboros.data.model.valuetypes.DoubleValue;
import com.ouroboros.data.model.valuetypes.FloatValue;
import com.ouroboros.data.model.valuetypes.IntegerValue;
import com.ouroboros.data.model.valuetypes.JsonValue;
import com.ouroboros.data.model.valuetypes.ListValue;
import com.ouroboros.data.model.valuetypes.LongValue;
import com.ouroboros.data.model.valuetypes.MapValue;
import com.ouroboros.data.model.valuetypes.ModelValue;
import com.ouroboros.data.model.valuetypes.ShortValue;
import com.ouroboros.data.model.valuetypes.SnowflakeIdValue;
import com.ouroboros.data.model.valuetypes.StringValue;
import com.ouroboros.data.model.valuetypes.TimeValue;
import com.ouroboros.data.validation.Rule;

class DataTypeBuildersTest {

  @Test
  void shouldSupportAndBuildPrimitiveAndTemporalTypes() {
    assertBuilder(new BooleanDataTypeBuilder(), new BooleanValue(), "BIT");
    assertBuilder(new DateDataTypeBuilder(), new DateValue(), "DATE");
    assertBuilder(new DateTimeDataTypeBuilder(), new DateTimeValue(), "DATETIME");
    assertBuilder(new DoubleDataTypeBuilder(), new DoubleValue(), "DOUBLE");
    assertBuilder(new FloatDataTypeBuilder(), new FloatValue(), "FLOAT");
    assertBuilder(new IntegerDataTypeBuilder(), new IntegerValue(), "INTEGER");
    assertBuilder(new LongDataTypeBuilder(), new LongValue(), "BIGINT");
    assertBuilder(new LongDataTypeBuilder(), new SnowflakeIdValue(), "BIGINT");
    assertBuilder(new ShortDataTypeBuilder(), new ShortValue(), "TINYINT");
    assertBuilder(new TimeDataTypeBuilder(), new TimeValue(), "TIME");
  }

  @Test
  void shouldSupportAndBuildTextLikeTypes() {
    assertBuilder(new JsonDataTypeBuilder(), new JsonValue(), "TEXT");
    assertBuilder(new ListDataTypeBuilder(), new ListValue(), "TEXT");
    assertBuilder(new MapDataTypeBuilder(), new MapValue(), "TEXT");
    assertBuilder(new ModelDataTypeBuilder(), new ModelValue(), "TEXT");
  }

  @Test
  void shouldBuildStringTypeBySizeThreshold() {
    var builder = new StringDataTypeBuilder();

    var nvarcharField = field(new StringValue(), 128, null);
    assertTrue(builder.support(null, nvarcharField));
    var nvarchar = builder.build(null, nvarcharField);
    assertEquals("VARCHAR", nvarchar.getTypeName());
    assertEquals(128, nvarchar.getColumnSize());

    var ntextField = field(new StringValue(), 2048, null);
    assertEquals("TEXT", builder.build(null, ntextField).getTypeName());
  }

  @Test
  void shouldBuildDecimalTypeWithDefaultAndCustomScale() {
    var builder = new DecimalDataTypeBuilder();

    var defaultField = field(new DecimalValue(), null, null);
    assertTrue(builder.support(null, defaultField));
    var defaultType = builder.build(null, defaultField);
    assertEquals("DECIMAL", defaultType.getTypeName());
    assertEquals(10, defaultType.getColumnSize());
    assertEquals(2, defaultType.getDecimalDigits());

    var customField = field(new DecimalValue(), 18, 6);
    var customType = builder.build(null, customField);
    assertEquals(18, customType.getColumnSize());
    assertEquals(6, customType.getDecimalDigits());
  }

  @Test
  void shouldReturnFalseForUnsupportedType() {
    assertFalse(new BooleanDataTypeBuilder().support(null, field(new StringValue(), null, null)));
  }

  private static void assertBuilder(Object builder, ValueType<?> valueType, String expectedTypeName) {
    var field = field(valueType, null, null);
    liquibase.structure.core.DataType dataType;
    boolean support;

    if (builder instanceof BooleanDataTypeBuilder) {
      var b = (BooleanDataTypeBuilder) builder;
      support = b.support(null, field);
      dataType = b.build(null, field);
    } else if (builder instanceof DateDataTypeBuilder) {
      var b = (DateDataTypeBuilder) builder;
      support = b.support(null, field);
      dataType = b.build(null, field);
    } else if (builder instanceof DateTimeDataTypeBuilder) {
      var b = (DateTimeDataTypeBuilder) builder;
      support = b.support(null, field);
      dataType = b.build(null, field);
    } else if (builder instanceof DoubleDataTypeBuilder) {
      var b = (DoubleDataTypeBuilder) builder;
      support = b.support(null, field);
      dataType = b.build(null, field);
    } else if (builder instanceof FloatDataTypeBuilder) {
      var b = (FloatDataTypeBuilder) builder;
      support = b.support(null, field);
      dataType = b.build(null, field);
    } else if (builder instanceof IntegerDataTypeBuilder) {
      var b = (IntegerDataTypeBuilder) builder;
      support = b.support(null, field);
      dataType = b.build(null, field);
    } else if (builder instanceof LongDataTypeBuilder) {
      var b = (LongDataTypeBuilder) builder;
      support = b.support(null, field);
      dataType = b.build(null, field);
    } else if (builder instanceof ShortDataTypeBuilder) {
      var b = (ShortDataTypeBuilder) builder;
      support = b.support(null, field);
      dataType = b.build(null, field);
    } else if (builder instanceof TimeDataTypeBuilder) {
      var b = (TimeDataTypeBuilder) builder;
      support = b.support(null, field);
      dataType = b.build(null, field);
    } else if (builder instanceof JsonDataTypeBuilder) {
      var b = (JsonDataTypeBuilder) builder;
      support = b.support(null, field);
      dataType = b.build(null, field);
    } else if (builder instanceof ListDataTypeBuilder) {
      var b = (ListDataTypeBuilder) builder;
      support = b.support(null, field);
      dataType = b.build(null, field);
    } else if (builder instanceof MapDataTypeBuilder) {
      var b = (MapDataTypeBuilder) builder;
      support = b.support(null, field);
      dataType = b.build(null, field);
    } else if (builder instanceof ModelDataTypeBuilder) {
      var b = (ModelDataTypeBuilder) builder;
      support = b.support(null, field);
      dataType = b.build(null, field);
    } else {
      throw new IllegalArgumentException("Unexpected builder: " + builder.getClass());
    }

    assertTrue(support);
    assertEquals(expectedTypeName, dataType.getTypeName());
  }

  private static DataModelField field(ValueType<?> valueType, Integer size, Integer decimalDigits) {
    return new DataModelField() {
      @Override
      public ValueType<?> getValueType() {
        return valueType;
      }

      @Override
      public Integer getSize() {
        return size;
      }

      @Override
      public Integer getDecimalDigits() {
        return decimalDigits;
      }

      @Override
      public String getName() {
        return "field";
      }

      @Override
      public String getLabel() {
        return "field";
      }

      @Override
      public String getDescription() {
        return null;
      }

      @Override
      public String getType() {
        return null;
      }

      @Override
      public String getRawName() {
        return "field";
      }

      @Override
      public String getRawType() {
        return null;
      }

      @Override
      public Object getDefaultValue(Map<String, Object> context) {
        return null;
      }

      @Override
      public List<Rule> getRules() {
        return Collections.emptyList();
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
    };
  }
}
