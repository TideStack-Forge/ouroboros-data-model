package com.ouroboros.data.model.annotations.plugins;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.ouroboros.data.annotation.TypedModelPluginAnnotation;

/**
 * Archives records into an explicit archive model before delete.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@TypedModelPluginAnnotation("ArchiveDelete")
public @interface ArchiveDelete {

  Class<?> archiveModel() default Void.class;

  String archiveModelName() default "";
}
