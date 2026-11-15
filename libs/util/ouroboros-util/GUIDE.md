# Ouroboros Util - 工具类指南

本文档描述了 `ouroboros-util` 模块中的常用工具类和方法,在编写代码时应优先使用这些工具类。

## 函数式编程工具 - Functions

路径: `com.ouroboros.util.Functions`

### 常用方法

- **安全执行** (`tryExecute`, `tryApply`)
  ```java
  // 执行可能抛异常的代码,返回 Either
  Either<Throwable, Result> result = Functions.tryExecute(() -> riskyOperation());

  // 应用函数到值,安全处理异常
  Either<Throwable, String> result = Functions.tryApply(value, v -> transform(v));
  ```

- **异常处理** (`ignoreExceptions`)
  ```java
  // 忽略异常,返回 null
  String result = Functions.ignoreExceptions(() -> mayFail());
  ```

## 集合工具 - Maps & Lists

### Maps 工具类

路径: `com.ouroboros.util.Maps`

- **安全获取** (`getValue`)
  ```java
  // 安全获取 Map 中的值,不存在时返回默认值
  String value = Maps.getValue(map, "key", "default");
  ```

- **过滤和转换**
  ```java
  // 按条件过滤 Map
  Map<K, V> filtered = Maps.filter(map, (k, v) -> condition);

  // 转换 Map 的值
  Map<K, V2> transformed = Maps.mapValues(map, value -> transform(value));
  ```

### Lists 工具类

路径: `com.ouroboros.util.Lists`

- **安全访问** (`getValue`, `setValue`)
  ```java
  // 安全获取列表元素,越界返回 null
  String value = Lists.getValue(list, index);

  // 安全设置列表元素,自动扩展列表
  Lists.setValue(list, index, value);
  ```

- **分组和分区**
  ```java
  // 按条件分组
  Map<K, List<V>> grouped = Lists.groupBy(list, item -> key);

  // 分区(满足条件和不满足条件)
  Tuple2<List<V>, List<V>> partitioned = Lists.partition(list, predicate);
  ```

## JSON 工具 - Json

路径: `com.ouroboros.util.Json`

### 核心功能

- **序列化和反序列化**
  ```java
  // 对象转 JSON 字符串
  String json = Json.toJsonString(object);

  // JSON 字符串转对象
  MyClass obj = Json.fromJsonString(json, MyClass.class);

  // JSON 字符串转 Map
  Map<String, Object> map = Json.toMap(json);

  // JSON 字符串转 List
  List<MyClass> list = Json.toList(json, MyClass.class);
  ```

- **JsonBag - 灵活的 JSON 容器**
  ```java
  // 创建 JsonBag
  JsonBag bag = JsonBag.of(jsonObject);

  // 检查类型
  if (bag.isList()) { ... }
  if (bag.isMap()) { ... }

  // 获取值
  Object value = bag.get("key");
  Object value = bag.get(index);

  // 设置值
  bag.put("key", value);
  bag.put(index, value);
  ```

## JSON Schema 验证 - JsonSchemas

路径: `com.ouroboros.util.JsonSchemas`

后端运行时 JSON Schema 校验统一使用 `JsonSchemas`。底层实现由 `com.networknt:json-schema-validator` 承担；上层模块不要直接引入或调用其他 JSON Schema 校验库。

### 验证功能

```java
// 验证数据是否符合 JSON Schema
Boolean valid = JsonSchemas.validate(schemaMap, data);
Boolean valid = JsonSchemas.validate(schemaString, data);

// 返回结构化错误信息
JsonSchemaValidationResult result = JsonSchemas.validateResult(schemaMap, data);
String message = result.toMessageString();

// 构建可重用的验证器
Function<Object, Boolean> validator = JsonSchemas.buildValidator(schemaMap);
boolean isValid = validator.apply(data);

// 构建返回 Try 的验证器(包含详细错误信息)
Function<Object, Try<Void>> validator = JsonSchemas.buildValidatorTry(schemaMap);
Try<Void> result = validator.apply(data);
```

说明:
- 优先使用 Draft 2020-12 schema；当 schema 缺少 `$schema` 时，工具会兼容性地回退到 Draft 7。
- `validateResult(...)` 会保留关键字、实例路径和原始错误消息，适合给上层模块拼接用户可读的失败原因。

## 日期工具 - Dates

路径: `com.ouroboros.util.Dates`

### 日期格式化和解析

```java
// 格式化日期
String formatted = Dates.format(date, "yyyy-MM-dd HH:mm:ss");
String formatted = Dates.format(date); // 默认格式

// 解析日期字符串
Date date = Dates.parse("2025-12-08 10:30:00", "yyyy-MM-dd HH:mm:ss");
Date date = Dates.parse("2025-12-08"); // 自动识别常见格式

// 日期计算
Date tomorrow = Dates.addDays(date, 1);
Date nextWeek = Dates.addWeeks(date, 1);
Date nextMonth = Dates.addMonths(date, 1);

// 日期比较
boolean isBefore = Dates.isBefore(date1, date2);
boolean isAfter = Dates.isAfter(date1, date2);
long daysBetween = Dates.daysBetween(date1, date2);
```

## 路径工具 - Paths

路径: `com.ouroboros.util.Paths`

### 文件路径处理

```java
// 构建文件路径
Paths.File filePath = Paths.File.of("dir1", "dir2", "file.txt");
String path = filePath.getPath();
File file = filePath.toFile();

// 获取文件信息
String name = filePath.getName();
String suffix = filePath.getSuffix();
String parent = filePath.getParent();
boolean exists = filePath.exists();
```

### URL 路径处理

```java
// 构建 URL 路径
Paths.Url urlPath = Paths.Url.of("http://example.com", "api", "v1", "users");
String url = urlPath.toString(); // http://example.com/api/v1/users
URL javaUrl = urlPath.toUrl();
```

## SPI 工具 - SPI

路径: `com.ouroboros.util.SPI`

### 服务加载

```java
// 获取 SPI 服务流
Stream<MyService> services = SPI.getServiceStream(MyService.class);

// 按 @Priority 排序获取服务
Stream<MyService> sorted = SPI.getSortedServiceStream(MyService.class);

// 反向排序(优先级高的在前)
Stream<MyService> reversed = SPI.getReversedServiceStream(MyService.class);

// 获取主服务(优先级最高)
MyService primary = SPI.getPrimaryService(MyService.class);

// 缓存的服务流(性能更好)
Stream<MyService> cached = SPI.getCachedServiceStream(MyService.class);
```

### 泛型服务加载

```java
// 加载特定泛型参数的服务
Stream<Converter<String>> converters =
    SPI.getCachedServiceStream(Converter.class, String.class);

// 加载有两个泛型参数的服务
Stream<Mapper<String, Integer>> mappers =
    SPI.getCachedServiceStream(Mapper.class, String.class, Integer.class);
```

## 使用建议

1. **优先使用这些工具类** - 避免重复实现已有功能
2. **函数式风格** - 使用 `Either` 和 `Try` 进行错误处理,避免直接抛异常
3. **空值安全** - 使用 `Maps.getValue`, `Lists.getValue` 等方法安全访问集合
4. **SPI 缓存** - 频繁使用的 SPI 服务应使用 `getCached*` 方法
5. **JSON 处理** - 使用 `Json` 工具类而不是直接使用 Jackson
6. **路径处理** - 使用 `Paths` 工具类确保跨平台路径兼容性

## 重要提醒

- **不要使用 `Either::toTry`** - 该方法在 Either 为 left 时会抛异常,应使用其他方式转换
- **日期时区** - 注意日期处理时的时区问题
- **JSON Schema 缓存** - 构建验证器后缓存以提高性能
