package com.ouroboros.data.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.ouroboros.data.model.MigrationStrategy;

/**
 * @author liansz
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Model {

  /**
   * 名称（模型名称，默认使用简单类名）
   *
   * @return
   */
  String fullName() default "";

  /**
   * 标签名称（用于展示）
   *
   * @return
   */
  String label() default "";

  /**
   * 原始名称（存储名称）
   *
   * @return
   */
  String rawName() default "";

  /**
   * 数据站
   *
   * @return
   */
  String dataStation() default "default";

  /**
   * 迁移策略
   *
   * @return
   */
  MigrationStrategy migrationStrategy() default MigrationStrategy.AUTO;

  /**
   * 扩展属性
   *
   * @return
   */
  String[] extraProps() default {};
}
