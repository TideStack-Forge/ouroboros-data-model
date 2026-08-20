package com.ouroboros.data.annotation;

import java.lang.annotation.*;

/**
 * Marker for typed data model scan packages.
 *
 * <p>The Spring runtime registry is provided by TypedDataModelAutoConfiguration; this annotation
 * intentionally does not import runtime infrastructure by itself.
 *
 * @author liansz
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataModelScan {

  /**
   * 扫描包路径
   *
   * @return
   */
  String[] value() default {};
}
