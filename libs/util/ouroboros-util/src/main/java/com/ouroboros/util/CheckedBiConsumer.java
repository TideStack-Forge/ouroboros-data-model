package com.ouroboros.util;

import java.util.Objects;
import java.util.function.BiConsumer;

import io.vavr.control.Try;

@FunctionalInterface
@SuppressWarnings("unused")
public interface CheckedBiConsumer<T, U> {
  void accept(T t, U u) throws Throwable;

  default BiConsumer<T, U> andThen(CheckedBiConsumer<? super T, ? super U> after) {
    Objects.requireNonNull(after);

    return (l, r) ->
        Try.run(() -> accept(l, r))
            .andThenTry(() -> after.accept(l, r));
  }
}
