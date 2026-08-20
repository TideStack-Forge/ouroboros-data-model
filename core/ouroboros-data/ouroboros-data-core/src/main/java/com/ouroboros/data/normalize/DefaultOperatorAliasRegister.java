package com.ouroboros.data.normalize;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import jakarta.annotation.Priority;

import io.vavr.control.Option;
import io.vavr.control.Try;

import com.ouroboros.data.dsl.ExtOps;
import com.ouroboros.data.util.KeywordsLoader;
import com.querydsl.core.types.Operator;
import com.querydsl.core.types.Ops;

@Priority(0)
public final class DefaultOperatorAliasRegister implements OperatorAliasRegister {
  @Override
  @SuppressWarnings(value = {"deprecation"})
  public void register(Map<String, Operator> registry) {
    KeywordsLoader.loadKeywords("OperatorAlias").forEach((key, value) -> {
      var op = Stream.<Function<String, Operator>>of(
              Ops::valueOf,
              Ops.AggOps::valueOf,
              Ops.MathOps::valueOf,
              Ops.StringOps::valueOf,
              Ops.DateTimeOps::valueOf,
              Ops.QuantOps::valueOf,
              ExtOps::valueOf)
          .<Option<Operator>>reduce(Option.none(),
              (acc, curr) -> acc.isEmpty() ? Try.of(() -> curr.apply(key)).toOption() : acc,
              Option::orElse);
      if (op.isEmpty()) {
        return;
      }
      value.forEach(alias -> registry.put(alias, op.get()));
    });
  }
}