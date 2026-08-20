package com.ouroboros.data.model.decorators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.model.DataModelFieldMeta;
import com.ouroboros.data.model.DataModelMeta;
import com.ouroboros.data.model.DataModelMetaDecorator;
import com.ouroboros.data.model.UniquenessScope;

/**
 * 数据模型元数据装饰器测试
 */
public class DataModelMetaDecoratorTest {

  @Test
  public void testAuditFieldsDecorator() {
    // 创建测试数据模型
    DataModelMeta originalMeta = new DataModelMeta();
    originalMeta.setName("TestModel");

    // 启用审计字段功能
    originalMeta.setExtraProp("enableAuditFields", true);

    // 验证装饰器能够识别配置
    AuditFieldsDecorator decorator = new AuditFieldsDecorator();
    assertTrue(decorator.supports(originalMeta));

    // 应用装饰器（现在主要用于配置识别）
    DataModelMeta decoratedMeta = DataModelMetaDecorator.applyDecorators(originalMeta);

    // 验证审计字段是否添加
    assertTrue(decoratedMeta.getFields().stream()
        .anyMatch(field -> "createdBy".equals(field.getName())));
    assertTrue(decoratedMeta.getFields().stream()
        .anyMatch(field -> "createdAt".equals(field.getName())));
    assertTrue(decoratedMeta.getFields().stream()
        .anyMatch(field -> "updatedBy".equals(field.getName())));
    assertTrue(decoratedMeta.getFields().stream()
        .anyMatch(field -> "updatedAt".equals(field.getName())));

    // 验证审计插件是否添加
    assertTrue(decoratedMeta.getPluginDescriptors().stream()
        .anyMatch(plugin -> "BasicAudit".equals(plugin.getName())));
  }

  @Test
  public void testLogicalDeleteDecorator() {
    // 创建测试数据模型
    DataModelMeta originalMeta = new DataModelMeta();
    originalMeta.setName("TestModel");
    DataModelFieldMeta email = new DataModelFieldMeta();
    email.setName("email");
    email.setType("String");
    email.setIsUnique(true);
    originalMeta.setFields(Collections.singletonList(email));

    // 启用逻辑删除功能
    originalMeta.setExtraProp("enableSoftDelete", true);

    // 验证装饰器能够识别配置
    LogicalDeleteDecorator decorator = new LogicalDeleteDecorator();
    assertTrue(decorator.supports(originalMeta));

    // 应用装饰器（现在主要用于配置识别）
    DataModelMeta decoratedMeta = DataModelMetaDecorator.applyDecorators(originalMeta);

    // 验证软删除字段是否添加
    assertTrue(decoratedMeta.getFields().stream()
        .anyMatch(field -> "isDeleted".equals(field.getName())));
    assertTrue(decoratedMeta.getFields().stream()
        .anyMatch(field -> "deletedBy".equals(field.getName())));
    assertTrue(decoratedMeta.getFields().stream()
        .anyMatch(field -> "deletedAt".equals(field.getName())));

    // LogicalDeleteDecorator owns the field decoration and the matching LogicalDelete plugin.
    assertTrue(decoratedMeta.getPluginDescriptors().stream()
        .anyMatch(plugin -> "LogicalDelete".equals(plugin.getName())));
    assertEquals(UniquenessScope.ACTIVE_RECORDS, scopeOf(decoratedMeta));
    assertTrue(decoratedMeta.getFields().stream()
        .filter(field -> "email".equals(field.getName()))
        .findFirst()
        .orElseThrow(AssertionError::new)
        .getIsUnique());

    DataModelMeta decoratedAgain = DataModelMetaDecorator.applyDecorators(decoratedMeta);
    assertEquals(UniquenessScope.ACTIVE_RECORDS, scopeOf(decoratedAgain));
    assertEquals(1, decoratedAgain.getFields().stream()
        .filter(field -> "isDeleted".equals(field.getName()))
        .count());
  }

  @Test
  public void testNoDecoratorAppliedWhenConditionsNotMet() {
    // 创建没有特殊配置的数据模型
    DataModelMeta originalMeta = new DataModelMeta();
    originalMeta.setName("TestModel");

    // 应用装饰器
    DataModelMeta decoratedMeta = DataModelMetaDecorator.applyDecorators(originalMeta);

    // 验证没有额外字段和插件被添加
    assertEquals(0, decoratedMeta.getFields().size());
    assertEquals(0, decoratedMeta.getPluginDescriptors().size());
    assertEquals(UniquenessScope.ALL_RECORDS, scopeOf(decoratedMeta));
  }

  private static UniquenessScope scopeOf(DataModelMeta meta) {
    return UniquenessScope.fromExtraProp(meta.getExtraProp(UniquenessScope.EXTRA_PROP_NAME).orElse(null));
  }
}
