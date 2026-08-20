package com.ouroboros.data.transpile.clausebuilder;

import static com.ouroboros.data.dsl.Order.DESC;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;

import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.transpile.ClauseTranspiler;
import com.ouroboros.data.transpile.OuroborosQueryMetadata;
import com.ouroboros.data.transpile.TranspileContext;
import com.ouroboros.data.exception.TranspileException;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;

public class OrderBuilder implements ClauseTranspiler {
  @Override
  public Try<Tuple2<OuroborosQueryMetadata, TranspileContext>> applyWithContext(QueryStatement query, OuroborosQueryMetadata queryMetadata, TranspileContext context) {
    var orders = query.getOrders();
    for (var order : orders) {
      var field = order.getColumn();

      // 使用新的 resolve API
      var path = field.contains(".")
          ? context.resolve(field.split("\\.", 2)[0], field.split("\\.", 2)[1])
          : context.resolve(field);

      if (!path.isPresent()) {
        return Try.failure(new TranspileException("排序字段不存在: " + field));
      }
      var orderSpec = new OrderSpecifier(toQuerydslOrder(order.getOrder()), path.get());
      queryMetadata.addOrderBy(orderSpec);
    }

    return Try.success(Tuple.of(queryMetadata, context));
  }

  private Order toQuerydslOrder(com.ouroboros.data.dsl.Order order) {
    return order == DESC ? Order.DESC : Order.ASC;
  }
}
