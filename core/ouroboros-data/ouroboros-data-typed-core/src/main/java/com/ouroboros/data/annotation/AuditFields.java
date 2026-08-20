package com.ouroboros.data.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables audit fields for a typed data model.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@TypedModelPluginAnnotation("BasicAudit")
public @interface AuditFields {

  String createdByRawName() default "";

  String createdAtRawName() default "";

  String updatedByRawName() default "";

  String updatedAtRawName() default "";
}
