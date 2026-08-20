package com.ouroboros.data.model;

import com.ouroboros.data.util.DataServices;

public interface DataModelMetaDecorator {

  /**
   * 应用装饰器模式来修改和增强数据模型元数据
   * 该方法通过SPI机制获取所有可用的DataModelMetaDecorator实现，并按逆序应用它们
   *
   * @param originalMeta 原始的数据模型元数据
   * @return 经过装饰器处理后的增强版数据模型元数据
   */
  static DataModelMeta applyDecorators(DataModelMeta originalMeta) {
    // 通过SPI机制获取所有DataModelMetaDecorator实现，并按逆序排序
    return DataServices.getCachedReversedServiceStream(DataModelMetaDecorator.class)
        // 筛选出支持当前元数据类型的装饰器
        .filter(decorator -> decorator.supports(originalMeta))
        // 使用reduce操作逐步应用装饰器
        // 第一个参数是初始值(originalMeta)
        // 第二个参数是累积函数，将每个装饰器的应用结果作为下一次的输入
        // 第三个参数是合并函数，在这里实际上不会被执行，因为流是顺序的
        .reduce(originalMeta,
            (meta, decorator) -> decorator.decorate(meta),
            (a, b) -> a);
  }

  /**
   * 是否支持装饰指定的数据模型
   *
   * @param originalMeta 原始元数据
   * @return 是否支持装饰
   */
  boolean supports(DataModelMeta originalMeta);

  /**
   * 装饰数据模型元数据
   *
   * @param originalMeta 原始元数据
   * @return 装饰后的元数据
   */
  DataModelMeta decorate(DataModelMeta originalMeta);
}
