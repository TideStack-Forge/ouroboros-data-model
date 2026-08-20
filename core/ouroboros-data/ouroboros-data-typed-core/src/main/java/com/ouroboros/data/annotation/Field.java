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
public @interface Field {

  String type() default "";

  /**
   * 原始名称（存储名称）
   *
   * @return
   */
  String rawName() default "";

  /**
   * 标签（显示名称）
   *
   * @return
   */
  String label() default "";

  /**
   * 原始类型（存储类型）
   *
   * @return
   */
  String rawType() default "";

  /**
   * 是否允许为空
   *
   * @return
   */
  boolean nullable() default false;

  /**
   * 默认值表达式
   *
   * @return
   */
  String defaultValueExpression() default "";

  int decimalDigits() default -1;

  int size() default -1;

  boolean unsigned() default false;

  /**
   * 扩展属性
   *
   * @return
   */
  String[] extraProps() default {};
}
