package com.ouroboros.data.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables logical delete for a typed data model.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@TypedModelPluginAnnotation("LogicalDelete")
public @interface LogicalDelete {

  String isDeletedRawName() default "";

  String deletedByRawName() default "";

  boolean deletedByDisabled() default false;

  String deletedAtRawName() default "";

  boolean deletedAtDisabled() default false;
}
