package com.ouroboros.data.analyze;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.integration.MockTestUtils;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.station.DataStation;

class DefaultQueryTransformerTest {

  @Test
  void transformShouldLeaveRelationWildcardUntouched() {
    DataStation station = mock(DataStation.class);
    DataModel customerModel = MockTestUtils.createMockModel("Customer", "customer", station);
    DataModel orderModel = MockTestUtils.createMockModel("Order", "order", station);
    DataModel rootModel = MockTestUtils.createMockModel("User", "user", station);

    DataModelField customerName = MockTestUtils.createSimpleField("name");
    DataModelField customerCode = MockTestUtils.createSimpleField("code");
    when(customerModel.getFields()).thenReturn(Arrays.asList(customerName, customerCode));

    DataModelField customerRelation = MockTestUtils.createRelationField("customer", customerModel);
    when(orderModel.getFields()).thenReturn(Arrays.asList(customerRelation));

    DataModelField orderRelation = MockTestUtils.createRelationField("order", orderModel);
    when(rootModel.getFields()).thenReturn(Arrays.asList(orderRelation));

    QueryStatement statement = QueryStatement.builder()
        .select(SExpression.create(Operators.FIELD, "order.customer.*"))
        .from("user")
        .build();

    QueryAnalyzeContext context = QueryAnalyzeContext.builder()
        .model(rootModel)
        .build();

    Try<QueryStatement> result = new DefaultQueryTransformer(
        Arrays.asList(new WildcardExpandAnalyzer())
    ).transform(statement, context);

    assertTrue(result.isSuccess());
    assertEquals(1, result.get().getSelect().size());
    assertSame(statement.getSelect().get(0), result.get().getSelect().get(0));
  }

  @Test
  void transformShouldOptimizeBooleanExpression() {
    QueryStatement statement = QueryStatement.builder()
        .from("user")
        .where(SExpression.create(
            Operators.AND,
            SExpression.constant(Boolean.TRUE),
            SExpression.create(
                Operators.EQ,
                SExpression.create(Operators.FIELD, "status"),
                SExpression.constant("active")
            )
        ))
        .build();

    QueryAnalyzeContext context = QueryAnalyzeContext.builder().build();

    Try<QueryStatement> result = new DefaultQueryTransformer(
        Arrays.asList(new OptimizationAnalyzer())
    ).transform(statement, context);

    assertTrue(result.isSuccess());
    assertEquals(Operators.EQ, result.get().getWhere().getOperator());
  }

  @Test
  void transformShouldRespectDisabledOptimizationFlag() {
    QueryStatement statement = QueryStatement.builder()
        .from("user")
        .where(SExpression.create(
            Operators.AND,
            SExpression.constant(Boolean.TRUE),
            SExpression.create(
                Operators.EQ,
                SExpression.create(Operators.FIELD, "status"),
                SExpression.constant("active")
            )
        ))
        .build();

    QueryAnalyzeContext context = QueryAnalyzeContext.builder()
        .enableOptimization(false)
        .build();

    Try<QueryStatement> result = new DefaultQueryTransformer(
        Arrays.asList(new OptimizationAnalyzer())
    ).transform(statement, context);

    assertTrue(result.isSuccess());
    assertSame(statement, result.get(),
        "关闭 enableOptimization 后不应进入 OptimizationAnalyzer");
  }
}
