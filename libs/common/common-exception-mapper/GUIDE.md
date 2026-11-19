# Common Exception Mapper - 异常映射器指南

本文档描述了 `common-exception-mapper` 模块的功能和使用方法，用于将第三方异常(如 DataModel 异常)统一转换为 Ouroboros 异常体系。

## 模块概述

数据模型异常映射现位于 `core/ouroboros-data/ouroboros-data-platform-adapter`，并在那里注册正式 SPI；`common-exception-mapper` 只保留通用 mapper 合同，不再承载 data 实现。

`common-exception-mapper` 提供了一个可扩展的异常映射机制，通过 SPI 模式将不同来源的异常转换为 Ouroboros 标准异常。

**核心功能:**
- 统一异常转换接口
- 基于 SPI 的可扩展映射器注册
- 数据异常适配由 data-platform-adapter 提供

## 核心接口

### OuroborosExceptionMapper

路径: `com.ouroboros.exception.mapper.OuroborosExceptionMapper`

异常映射器接口，所有自定义映射器必须实现此接口。

**核心方法:**

```java
// 判断是否支持转换该异常
boolean supports(Throwable throwable);

// 将异常转换为 OuroborosException
OuroborosException map(Throwable throwable);

// 静态工具方法：将任意异常转换为 OuroborosException
static OuroborosException toOuroborosException(Throwable throwable);
static OuroborosException toOuroborosException(Throwable throwable, Supplier<OuroborosException> defaultSupplier);
static OuroborosException toOuroborosException(Throwable throwable, Function<Throwable, OuroborosException> defaultMapper);
```

## 数据适配模块提供的映射器

### DataModelExceptionMapper

路径: `com.ouroboros.data.runtime.adapter.DataModelExceptionMapper`（制品 `ouroboros-data-platform-adapter`）

将数据模型层异常转换为 Ouroboros 异常。

**映射规则:**

| DataModel 异常 | 转换为 | 说明 |
|---------------|--------|------|
| `StatementException` | `PlatformException` | 查询语句错误(开发者使用不当) |
| `DataValidationException` | `ValidationException` | 数据验证失败 |
| `RecordValidationException` | `ValidationException` (带字段错误) | 记录验证失败,包含详细字段错误 |
| `DataAccessException` | `InternalException` | 数据访问失败(底层数据库错误) |
| 其他 `DataModelException` | `InternalException` | 未知数据模型异常 |

## 使用示例

### 基本用法

```java
// 直接使用静态方法转换异常
try {
    // 可能抛出 DataModelException 的操作
    dataService.save(record);
} catch (Throwable e) {
    // 自动转换为 OuroborosException
    throw OuroborosExceptionMapper.toOuroborosException(e);
}
```

### 自定义默认映射

```java
// 使用自定义默认映射器
try {
    riskyOperation();
} catch (Throwable e) {
    throw OuroborosExceptionMapper.toOuroborosException(e,
        ex -> new BusinessException("业务操作失败: " + ex.getMessage(), ex)
    );
}
```

### 处理 RecordValidationException

```java
try {
    recordRepository.save(record);
} catch (RecordValidationException e) {
    // 自动转换为 ValidationException,保留字段错误信息
    OuroborosException ex = DataModelExceptionMapper.toOuroborosException(e);

    // 可以获取详细的字段错误
    if (ex instanceof ValidationException validationEx) {
        Map<String, String> errors = validationEx.getErrors();
        // errors: {"name": "名称不能为空,名称长度不能超过50个字符", "age": "年龄必须大于0"}
    }
}
```

## 扩展自定义映射器

### 1. 实现 OuroborosExceptionMapper 接口

```java
package com.example.mymodule;

import com.ouroboros.exception.mapper.OuroborosExceptionMapper;
import com.ouroboros.exception.OuroborosException;
import com.ouroboros.exception.InternalException;

public class MyExceptionMapper implements OuroborosExceptionMapper {

    @Override
    public boolean supports(Throwable throwable) {
        return throwable instanceof MyCustomException;
    }

    @Override
    public OuroborosException map(Throwable throwable) {
        MyCustomException ex = (MyCustomException) throwable;
        return new InternalException("自定义异常: " + ex.getMessage(), ex);
    }
}
```

### 2. 注册 SPI 服务

在 `src/main/resources/META-INF/services/` 目录下创建文件:

**文件名:** `com.ouroboros.exception.mapper.OuroborosExceptionMapper`

**文件内容:**
```
com.example.mymodule.MyExceptionMapper
```

### 3. 使用优先级控制

使用 `@Priority` 注解控制映射器的执行顺序(本映射器调用路径按逆序遍历，数值越大越先执行):

```java
import jakarta.annotation.Priority;

@Priority(100)  // 优先级 100
public class MyExceptionMapper implements OuroborosExceptionMapper {
    // ...
}
```

## 工作原理

1. **SPI 加载:** 通过 Java SPI 机制加载所有 `OuroborosExceptionMapper` 实现
2. **优先级排序:** 按 `@Priority` 注解反向排序(优先级高的先执行)
3. **匹配检查:** 遍历映射器,调用 `supports()` 方法检查是否支持当前异常
4. **转换执行:** 找到第一个支持的映射器,调用 `map()` 方法转换异常
5. **默认处理:** 如果没有匹配的映射器,使用默认映射器或包装为 `InternalException`

## 注意事项

1. **异常已经是 OuroborosException:** 如果异常已经是 `OuroborosException` 类型,会直接返回,不会再次转换
2. **映射器顺序:** 优先级高的映射器先执行,一旦找到支持的映射器就停止查找
3. **性能考虑:** 使用缓存的 SPI 服务流,避免重复加载
4. **null 安全:** `DataModelExceptionMapper.toOuroborosException(null)` 返回 `null`
5. **字段错误合并:** `RecordValidationException` 的多个字段错误会用逗号合并为单个字符串

## 依赖关系

```xml
<dependency>
    <groupId>com.ouroboros</groupId>
    <artifactId>common-exception-mapper</artifactId>
</dependency>
```

**传递依赖:**
- `common-exception`: Ouroboros 异常体系

使用数据映射时，由平台装配 `ouroboros-data-platform-adapter`；通用 mapper 模块不传递引入 data。
