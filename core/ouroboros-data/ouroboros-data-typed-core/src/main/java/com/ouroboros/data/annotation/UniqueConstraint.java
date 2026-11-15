package com.ouroboros.data.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.ouroboros.data.model.UniquenessScope;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(UniqueConstraints.class)
public @interface UniqueConstraint {
  String name() default "";

  String[] fields();

  UniquenessScope scope() default UniquenessScope.DEFAULT;

  /** When true, a complete key containing null does not claim uniqueness. */
  boolean nullsDistinct() default false;
}
