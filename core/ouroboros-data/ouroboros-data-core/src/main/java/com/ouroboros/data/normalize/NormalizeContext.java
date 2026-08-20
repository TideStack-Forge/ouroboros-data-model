package com.ouroboros.data.normalize;

import java.util.List;

import io.vavr.control.Try;

import com.ouroboros.data.dsl.SExpression;
import com.querydsl.core.types.Operator;

/**
 * 归一化上下文共同接口
 */
public interface NormalizeContext {

  Try<SExpression<?>> normalizeExpression(Object rawExpression, String expressionPath);

  Try<SExpression<Boolean>> normalizeCondition(Object rawExpression, String expressionPath);

  Try<SExpression<?>> buildSExpression(Operator operator, List<SExpression<?>> params, String expressionPath);
}
