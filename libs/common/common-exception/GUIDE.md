# Common Exception - 异常体系指南

本文档描述了 `common-exception` 模块中的异常类体系,提供统一的异常处理规范。

## 异常层次结构

```
OuroborosException (抽象基类)
├── BusinessException (业务异常, -4000)
│   └── ValidationException (验证异常, 422)
├── PlatformException (平台异常, -6000)
└── InternalException (内部异常, -5000)
```

## 核心异常类

### OuroborosException

**路径**: `com.ouroboros.exception.OuroborosException`

所有 Ouroboros 异常的抽象基类,继承自 `RuntimeException`。

**核心特性**:
- 支持自定义错误代码 (`errorCode`)
- 默认错误代码为 `-1`
- 支持 `ErrorCodeReference` 与消息参数，保留错误码引用和参数副本
- 所有子类都应继承此类

**构造方法**:
```java
// 仅消息
OuroborosException(String message)

// 消息 + 原因
OuroborosException(String message, Throwable cause)

// 错误代码 + 消息
OuroborosException(Integer errorCode, String message)

// 错误代码 + 消息 + 原因
OuroborosException(Integer errorCode, String message, Throwable cause)
```

### BusinessException

**路径**: `com.ouroboros.exception.BusinessException`

**用途**: 用户输入与业务逻辑不符的异常

**默认错误代码**: `-4000`

**适用场景**:
- 缺少必填字段
- 格式校验失败
- 关联业务实体不存在
- 业务状态不正确
- 业务规则违反

**使用示例**:
```java
// 简单消息
throw new BusinessException("订单状态不允许此操作");

// 自定义错误代码
throw new BusinessException(-4001, "库存不足");

// 包含原因
throw new BusinessException("支付失败", paymentException);

// 完整参数
throw new BusinessException(-4002, "用户未实名认证", verificationException);
```

### PlatformException

**路径**: `com.ouroboros.exception.PlatformException`

**用途**: 开发者不当使用导致的异常

**默认错误代码**: `-6000`

**适用场景**:
- 参数不合规
- 脚本语法错误
- 访问的模型不存在
- 配置错误
- API 使用不当

**使用示例**:
```java
// 模型不存在
throw new PlatformException("模型 'User' 不存在");

// 脚本错误
throw new PlatformException(-6001, "脚本语法错误: " + syntaxError);

// 配置错误
throw new PlatformException("缺少必要的配置项: database.url", cause);
```

### ValidationException

**路径**: `com.ouroboros.exception.ValidationException`

**用途**: 数据验证失败时抛出的异常 (继承自 `BusinessException`)

**错误代码**: `422`（异常中的业务码；HTTP 响应状态由 Web 处理器决定）

该类在源码中已标记 `@Deprecated`，仍保留供现有调用兼容。数据内核的记录校验使用 `RecordValidationException`，对外边界按异常映射规则转换；不要为了迁移让 common 层反向依赖 data。仓库业务异常规范仍包含此类，以下示例用于理解存量调用，不能据历史 Javadoc 的移除版本推断它已被删除。

**核心特性**:
- 支持单个错误消息
- 支持字段级别的错误信息 (`Map<String, String>`)

**使用示例**:
```java
// 单个错误消息
throw new ValidationException("数据验证失败");

// 字段级别错误
Map<String, String> errors = new HashMap<>();
errors.put("username", "用户名长度必须在 3-20 个字符之间");
errors.put("email", "邮箱格式不正确");
errors.put("age", "年龄必须大于 18");
throw new ValidationException(errors);

// 获取错误信息
try {
    validate(data);
} catch (ValidationException e) {
    Map<String, String> fieldErrors = e.getErrors();
    fieldErrors.forEach((field, error) -> {
        System.out.println(field + ": " + error);
    });
}
```

### InternalException

**路径**: `com.ouroboros.exception.InternalException`

**用途**: 平台内部错误

**默认错误代码**: `-5000`

**适用场景**:
- 平台 BUG
- 底层无法处理的错误
- 系统资源问题
- 意外的运行时错误

**使用示例**:
```java
// 简单内部错误
throw new InternalException("数据库连接池已耗尽");

// 包装底层异常
try {
    database.execute(sql);
} catch (SQLException e) {
    throw new InternalException(e);
}

// 自定义错误代码
throw new InternalException(-5001, "缓存服务不可用", redisException);
```

## 异常使用指南

### 选择合适的异常类型

| 异常类型 | 使用场景 | 责任方 | 示例 |
|---------|---------|--------|------|
| `BusinessException` | 业务规则违反 | 用户/客户端 | 订单已取消、库存不足 |
| `ValidationException` | 数据验证失败 | 用户/客户端 | 邮箱格式错误、必填字段缺失 |
| `PlatformException` | 开发者使用不当 | 开发者 | 模型不存在、脚本语法错误 |
| `InternalException` | 系统内部错误 | 平台/系统 | 数据库连接失败、内存溢出 |

### 错误代码规范

优先按 [统一错误码设计](../../../docs/design/modules/libs/common-error-code.md) 定义和引用正式错误码，通过 `BusinessException(ErrorCodeReference, Object...)` 或带 cause 的重载传入消息参数。`OuroborosException` 用引用中的数值码、fallback 模板和参数构造消息；Web 边界再解析本地化等响应信息。

`BusinessException`、`InternalException`、`PlatformException` 的默认数值分别为 `-4000`、`-5000`、`-6000`；旧的整数构造器仍可调用。新增业务码遵循统一分段和定义规则，不以任意 `-4xxx` 常量替代正式定义。`ValidationException` 的 `422` 也不自动决定 HTTP 状态。

### 最佳实践

1. **明确异常类型**
   ```java
   // 好的做法
   if (stock < quantity) {
       throw new BusinessException("库存不足,当前库存: " + stock);
   }

   // 避免使用通用异常
   // throw new RuntimeException("库存不足");
   ```

2. **提供清晰的错误消息**
   ```java
   // 好的做法
   throw new BusinessException("订单 #12345 已取消,无法进行支付操作");

   // 避免模糊消息
   // throw new BusinessException("操作失败");
   ```

3. **保留原始异常**
   ```java
   // 好的做法 - 保留堆栈信息
   try {
       parseJson(jsonString);
   } catch (JsonProcessingException e) {
       throw new PlatformException("JSON 解析失败", e);
   }

   // 避免丢失原始异常
   // throw new PlatformException("JSON 解析失败");
   ```

4. **兼容存量字段校验调用**（该类已弃用，新增数据内核校验采用 `RecordValidationException`）
   ```java
   // 好的做法 - 一次性返回所有字段错误
   Map<String, String> errors = new HashMap<>();
   if (user.getUsername() == null) {
       errors.put("username", "用户名不能为空");
   }
   if (user.getEmail() == null) {
       errors.put("email", "邮箱不能为空");
   }
   if (!errors.isEmpty()) {
       throw new ValidationException(errors);
   }

   // 避免多次抛出异常
   // if (user.getUsername() == null) throw new ValidationException("用户名不能为空");
   // if (user.getEmail() == null) throw new ValidationException("邮箱不能为空");
   ```

5. **旧整数错误码调用方式**（新定义优先采用上面的 `ErrorCodeReference` 机制）
   ```java
   public class OrderErrorCodes {
       public static final int ORDER_NOT_FOUND = -4100;
       public static final int ORDER_CANCELLED = -4101;
       public static final int INSUFFICIENT_STOCK = -4102;
   }

   throw new BusinessException(OrderErrorCodes.ORDER_CANCELLED, "订单已取消");
   ```

## 异常处理建议

1. **在控制器层统一处理异常** - 使用 `@ExceptionHandler` 或全局异常处理器
2. **服务层抛出具体异常** - 不要捕获异常后返回 `null` 或错误标志
3. **记录异常日志** - `InternalException` 应记录详细日志,`BusinessException` 可记录警告级别
4. **向用户返回友好消息** - 不要将技术细节直接暴露给最终用户
5. **使用 common-exception-mapper** - 在边界显式调用 mapper，将已注册的第三方异常转换为 Ouroboros 异常

## 与其他模块集成

- **common-exception-mapper**: 通过显式映射入口转换常见异常，详见 [mapper 指南](../common-exception-mapper/GUIDE.md)
- **common-web**: 提供 Web 层的异常处理器
- **common-service**: 服务层基类使用这些异常

## 注意事项

- 所有异常都是 `RuntimeException`,无需在方法签名中声明
- 不要捕获 `OuroborosException` 后再包装成其他 `OuroborosException`
- 数据内核校验走 `RecordValidationException`；业务边界遵循异常体系与正式 mapper，存量 `ValidationException` 仍可识别但已弃用
- 平台内部错误应抛出 `InternalException`,不要抛出 `BusinessException`
