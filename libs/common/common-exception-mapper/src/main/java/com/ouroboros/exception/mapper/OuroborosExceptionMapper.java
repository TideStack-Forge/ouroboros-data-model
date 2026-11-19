package com.ouroboros.exception.mapper;

import java.util.function.Function;
import java.util.function.Supplier;

import com.ouroboros.exception.InternalException;
import com.ouroboros.exception.OuroborosException;
import com.ouroboros.util.SPI;

public interface OuroborosExceptionMapper {
  static OuroborosException toOuroborosException(Throwable throwable) {
    return toOuroborosException(throwable, () -> new InternalException(throwable));
  }

  static OuroborosException toOuroborosException(Throwable throwable, Supplier<OuroborosException> defaultSupplier) {
    if (throwable instanceof OuroborosException oe) {
      return oe;
    }
    return SPI.getCachedReversedServiceStream(OuroborosExceptionMapper.class)
        .filter(mapper -> mapper.supports(throwable))
        .map(mapper -> mapper.map(throwable))
        .findFirst()
        .orElseGet(defaultSupplier);
  }

  static OuroborosException toOuroborosException(Throwable throwable, Function<Throwable, OuroborosException> defaultMapper) {
    return toOuroborosException(throwable, () -> defaultMapper.apply(throwable));
  }

  OuroborosException map(Throwable throwable);

  boolean supports(Throwable throwable);
}
