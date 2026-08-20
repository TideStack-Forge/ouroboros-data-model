package com.ouroboros.data.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author liansz
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Related {

  /**
   * 关联类型（取值范围：RelatedValue实现类getName方法）
   * 目前有：Map、Collection
   *
   * @return
   */
  String type() default "";

  /**
   * 关联模型名称
   *
   * @return
   */
  String model();

  /**
   * 以当前模型为视角关联模型的键字段名称
   *
   * @return
   */
  String key() default "";

  /**
   * 以被关联模型为视角关联模型的键字段名称
   *
   * @return
   */
  String referenceKey() default "";
}
