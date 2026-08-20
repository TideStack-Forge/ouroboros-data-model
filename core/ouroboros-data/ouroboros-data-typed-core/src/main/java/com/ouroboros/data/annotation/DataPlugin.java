package com.ouroboros.data.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Direct runtime plugin descriptor escape hatch for typed models.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@TypedModelPluginAnnotation("DataPlugin")
public @interface DataPlugin {

  String name();

  String[] config() default {};
}
