package com.ouroboros.data.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables SCD2 history tracking for a typed data model.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@TypedModelPluginAnnotation("Scd2History")
public @interface Scd2History {

  Class<?> historyModel() default Void.class;

  String historyModelName() default "";
}
