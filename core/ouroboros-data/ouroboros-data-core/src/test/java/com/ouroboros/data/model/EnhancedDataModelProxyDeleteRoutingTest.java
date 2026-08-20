package com.ouroboros.data.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.exception.InvalidStatementException;
import com.ouroboros.data.model.valuetypes.StringValue;
import com.ouroboros.data.validation.Rule;

public class EnhancedDataModelProxyDeleteRoutingTest {

  @Test
  public void testDeleteByIdShouldRouteThroughDeleteWhere() {
    DataModel coreDataModel = mock(DataModel.class);
    when(coreDataModel.getPrimaryKeys()).thenReturn(Collections.singletonList(primaryKeyField()));
    when(coreDataModel.delete("A1")).thenReturn(Try.success(11L));
    SExpression<Boolean> expectedWhere = SExpression.create(
        Operators.EQ,
        SExpression.field("id"),
        SExpression.constant("A1"));
    when(coreDataModel.delete(expectedWhere)).thenReturn(Try.success(22L));

    DataModel proxy = new EnhancedDataModelProxy(coreDataModel, Collections.emptyList());

    Try<Long> result = proxy.delete("A1");

    assertTrue(result.isSuccess());
    assertEquals(22L, result.get());
    verify(coreDataModel).delete(expectedWhere);
    verify(coreDataModel, never()).delete("A1");
  }

  @Test
  public void testDeleteByIdsShouldRouteThroughDeleteWhere() {
    DataModel coreDataModel = mock(DataModel.class);
    List<String> ids = Arrays.asList("A1", "A2");
    when(coreDataModel.getPrimaryKeys()).thenReturn(Collections.singletonList(primaryKeyField()));
    when(coreDataModel.delete(ids)).thenReturn(Try.success(13L));
    SExpression<Boolean> expectedWhere = SExpression.create(
        Operators.IN,
        SExpression.field("id"),
        SExpression.constant(ids));
    when(coreDataModel.delete(expectedWhere)).thenReturn(Try.success(24L));

    DataModel proxy = new EnhancedDataModelProxy(coreDataModel, Collections.emptyList());

    Try<Long> result = proxy.delete(ids);

    assertTrue(result.isSuccess());
    assertEquals(24L, result.get());
    verify(coreDataModel).delete(expectedWhere);
    verify(coreDataModel, never()).delete(ids);
  }

  @Test
  public void testDeleteByInvalidIdShouldFailWithSameValidationSemantics() {
    DataModel coreDataModel = mock(DataModel.class);
    when(coreDataModel.getPrimaryKeys()).thenReturn(Collections.singletonList(primaryKeyField()));
    when(coreDataModel.getFullName()).thenReturn("demo.TestModel");
    when(coreDataModel.delete("")).thenReturn(Try.success(1L));

    DataModel proxy = new EnhancedDataModelProxy(coreDataModel, Collections.emptyList());

    Try<Long> result = proxy.delete("");

    assertTrue(result.isFailure());
    assertInstanceOf(InvalidStatementException.class, result.getCause());
  }

  private DataModelField primaryKeyField() {
    return new DataModelField() {
      private final StringValue valueType = new StringValue();

      @Override
      public String getName() {
        return "id";
      }

      @Override
      public String getLabel() {
        return "id";
      }

      @Override
      public String getDescription() {
        return "";
      }

      @Override
      public String getType() {
        return "String";
      }

      @Override
      public String getRawName() {
        return "id";
      }

      @Override
      public String getRawType() {
        return "String";
      }

      @Override
      public ValueType<?> getValueType() {
        return valueType;
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
      public Integer getDecimalDigits() {
        return null;
      }

      @Override
      public Integer getSize() {
        return null;
      }

      @Override
      public Boolean getIsNullable() {
        return false;
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
        return true;
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
