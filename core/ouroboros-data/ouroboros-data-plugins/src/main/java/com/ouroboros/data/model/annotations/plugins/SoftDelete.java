package com.ouroboros.data.model.annotations.plugins;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.ouroboros.data.annotation.TypedModelPluginAnnotation;

/**
 * Enables logical delete for a typed data model.
 *
 * @deprecated use {@link LogicalDelete}.
 */
@Deprecated
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@TypedModelPluginAnnotation("LogicalDelete")
public @interface SoftDelete {

  String isDeletedRawName() default "";

  String deletedByRawName() default "";

  boolean deletedByDisabled() default false;

  String deletedAtRawName() default "";

  boolean deletedAtDisabled() default false;
}
