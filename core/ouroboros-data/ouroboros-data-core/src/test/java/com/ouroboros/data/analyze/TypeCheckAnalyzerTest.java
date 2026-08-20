package com.ouroboros.data.analyze;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.ExtOps;
import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.valuetypes.RelatedValue;

class TypeCheckAnalyzerTest {

  @Test
  void relationOperatorShouldValidateNestedConditionAgainstRelatedModel() {
    DataModel orderModel = mock(DataModel.class);
    when(orderModel.getName()).thenReturn("Order");

    DataModel orderItemModel = mock(DataModel.class);
    when(orderItemModel.getName()).thenReturn("OrderItem");

    DataModelField orderItemsField = mock(DataModelField.class);
    when(orderItemsField.getName()).thenReturn("orderItems");
    when(orderItemsField.getType()).thenReturn("Collection");
    RelatedValue<?> relationValue = mock(RelatedValue.class);
    when(relationValue.getReferenceModel()).thenReturn(Optional.of(orderItemModel));
    doReturn(relationValue).when(orderItemsField).getValueType();

    DataModelField statusField = mock(DataModelField.class);
    when(statusField.getName()).thenReturn("status");
    when(statusField.getType()).thenReturn("String");

    when(orderModel.getFields()).thenReturn(Arrays.asList(orderItemsField, statusField));
    when(orderModel.getField("orderItems")).thenReturn(Optional.of(orderItemsField));
    when(orderModel.getField("status")).thenReturn(Optional.of(statusField));

    DataModelField priceField = mock(DataModelField.class);
    when(priceField.getName()).thenReturn("price");
    when(priceField.getType()).thenReturn("Number");
    when(orderItemModel.getFields()).thenReturn(Collections.singletonList(priceField));
    when(orderItemModel.getField("price")).thenReturn(Optional.of(priceField));

    QueryStatement statement = QueryStatement.builder()
        .from("Order")
        .where(SExpression.create(
            ExtOps.REL_ANY,
            SExpression.field("orderItems"),
            SExpression.create(
                Operators.GT,
                SExpression.field("price"),
                SExpression.constant(100)
            )
        ))
        .build();

    QueryAnalyzeContext context = QueryAnalyzeContext.builder()
        .model(orderModel)
        .build();

    Try<QueryStatement> result = new DefaultQueryAnalyzer().analyze(statement, context);

    assertTrue(result.isSuccess(), () -> "关联字段条件应使用关联模型做类型检查: "
        + (result.isFailure() ? result.getCause() : ""));
  }

  @Test
  void rootAliasQualifiedFieldShouldBeValidatedBeforeRelationPathFallback() {
    DataModel employeeModel = mock(DataModel.class);
    when(employeeModel.getName()).thenReturn("Employee");

    DataModel departmentModel = mock(DataModel.class);
    when(departmentModel.getName()).thenReturn("Department");

    DataModelField employeeNameField = mock(DataModelField.class);
    when(employeeNameField.getName()).thenReturn("name");
    when(employeeNameField.getType()).thenReturn("String");

    DataModelField departmentField = mock(DataModelField.class);
    when(departmentField.getName()).thenReturn("department");
    when(departmentField.getType()).thenReturn("Model");
    RelatedValue<?> relationValue = mock(RelatedValue.class);
    when(relationValue.getReferenceModel()).thenReturn(Optional.of(departmentModel));
    doReturn(relationValue).when(departmentField).getValueType();

    DataModelField departmentNameField = mock(DataModelField.class);
    when(departmentNameField.getName()).thenReturn("name");
    when(departmentNameField.getType()).thenReturn("String");

    when(employeeModel.getFields()).thenReturn(Arrays.asList(employeeNameField, departmentField));
    when(employeeModel.getField("name")).thenReturn(Optional.of(employeeNameField));
    when(employeeModel.getField("department")).thenReturn(Optional.of(departmentField));
    when(departmentModel.getFields()).thenReturn(Collections.singletonList(departmentNameField));
    when(departmentModel.getField("name")).thenReturn(Optional.of(departmentNameField));

    QueryStatement statement = QueryStatement.builder()
        .from("employee_table", "department")
        .where(SExpression.create(
            Operators.EQ,
            SExpression.field("department", "name"),
            SExpression.constant("Alice")))
        .build();

    QueryAnalyzeContext context = QueryAnalyzeContext.builder()
        .model(employeeModel)
        .build();

    Try<QueryStatement> result = new DefaultQueryAnalyzer().analyze(statement, context);

    assertTrue(result.isSuccess(), () -> "FIELD 第一段命中根别名时应先按表别名解释，再检查模型字段: "
        + (result.isFailure() ? result.getCause() : ""));
  }
}
