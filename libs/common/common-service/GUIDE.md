# Common Service - 服务层基类指南

本文档描述了 `common-service` 合同与 data 模块中的服务层基类，用于简化基于 Typed Data Model 的 CRUD 服务开发。

## 模块概述

`common-service` 保留 `ModelService`、软删除合同及 outbox 规则；Typed Data 服务层实现现位于 `core/ouroboros-data/ouroboros-data-typed-core`。上层模块应从该模块引入服务基类，而不是继续把数据实现放回 libs。

`ouroboros-data-typed-core` 提供了一套标准的服务层基类，封装了常见的 CRUD 操作和查询功能。支持普通删除和软删除两种模式。

**核心依赖**:
- `ouroboros-data-typed-core`: Typed Data Model 合同与 CRUD 实现（`com.ouroboros.data.service`）
- `common-service`: 与引擎无关的服务合同
- 运行时由正式平台装配 typed runtime 和 data platform adapter
- `common-exception-mapper`: 异常映射

## 核心接口和类

### 1. ModelService<PK, M>

基础服务接口，定义标准 CRUD 操作。

**类型参数**:
- `PK`: 主键类型
- `M`: 模型类型

**主要方法**:
```java
// 增
M add(M model)
List<? extends M> add(Collection<? extends M> models)

// 删
Boolean delete(PK id)
Boolean delete(Collection<PK> ids)

// 改
Boolean update(PK id, M model)
Boolean partialUpdate(PK id, Map<String, Object> map)

// 查
M getByPrimaryKey(PK primaryKey)
M getByPrimaryKey(PK primaryKey, Map<String, Object> statement)
Long count(Map<String, Object> where)
M searchOne(Map<String, Object> statement)
List<? extends M> searchList(Map<String, Object> statement)
List<? extends M> searchAll()
```

### 2. ModelServiceImpl<PK, M, E>

`ModelService` 的完整实现，适用于模型类和实体类不同的场景。

**类型参数**:
- `PK`: 主键类型
- `M`: 业务模型接口类型
- `E`: 数据模型实体类型（必须实现 M）

**使用示例**:
```java
// 业务模型接口
public interface User {
    Long getId();
    String getName();
}

// 数据模型实体
public class UserEntity implements User {
    private Long id;
    private String name;
    // getters and setters...
}

// 服务实现
@Service
public class UserService extends ModelServiceImpl<Long, User, UserEntity> {
    // 自动继承所有 CRUD 方法

    // 可添加自定义业务方法
    public User findByName(String name) throws OuroborosException {
        Map<String, Object> statement = new HashMap<>();
        Map<String, Object> where = new HashMap<>();
        where.put("name", name);
        statement.put("where", where);
        return searchOne(statement);
    }
}
```

### 3. SimpleModelServiceImpl<PK, M>

简化版服务实现，适用于模型类和实体类相同的场景。

**类型参数**:
- `PK`: 主键类型
- `M`: 模型类型（同时作为业务模型和数据模型）

**使用示例**:
```java
// 数据模型
public class Product {
    private Long id;
    private String name;
    private BigDecimal price;
    // getters and setters...
}

// 服务实现
@Service
public class ProductService extends SimpleModelServiceImpl<Long, Product> {
    // 自动继承所有 CRUD 方法
}
```

### 4. SoftDelete 接口

软删除标记接口，模型需实现此接口以支持软删除，并提供可反射调用的无参构造器以解析删除字段名。

**方法**:
```java
// 获取软删除字段名称（默认 "deleted"）
String getDeletedFieldName()

// 获取删除标记
boolean isDeleted()

// 设置删除标记
void setDeleted(boolean deleted)
```

**使用示例**:
```java
public class Article implements SoftDelete {
    private Long id;
    private String title;
    private boolean deleted;

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    @Override
    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    // 可选：自定义软删除字段名
    @Override
    public String getDeletedFieldName() {
        return "is_deleted";  // 默认是 "deleted"
    }
}
```

### 5. SoftDeleteModelService<PK, M>

软删除服务接口，扩展了 `ModelService`，区分软删除和物理删除。

**额外方法**:
```java
// 软删除（覆盖父接口的 delete 方法）
Boolean delete(PK id)
Boolean delete(Collection<PK> ids)

// 物理删除
Boolean drop(PK id)
Boolean drop(Collection<PK> ids)
```

### 6. SoftDeleteModelServiceImpl<PK, M, E>

软删除服务的完整实现。

**类型参数**:
- `PK`: 主键类型
- `M`: 业务模型接口类型（必须实现 SoftDelete）
- `E`: 数据模型实体类型（必须实现 M 和 SoftDelete）

**功能特性**:
- `delete()`: 将 `deleted` 字段设置为 `true`
- `drop()`: 真正删除数据库记录
- `add()`: 自动设置 `deleted` 为 `false`
- `searchAll/searchOne/searchList/count` 及两个 `getByPrimaryKey` 重载均过滤已删除记录

**使用示例**:
```java
// 业务模型接口
public interface Comment extends SoftDelete {
    Long getId();
    String getContent();
}

// 数据模型实体
public class CommentEntity implements Comment {
    private Long id;
    private String content;
    private boolean deleted;

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    @Override
    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}

// 服务实现
@Service
public class CommentService extends SoftDeleteModelServiceImpl<Long, Comment, CommentEntity> {

    public void softDeleteComment(Long id) throws OuroborosException {
        delete(id);  // 软删除，设置 deleted=true
    }

    public void hardDeleteComment(Long id) throws OuroborosException {
        drop(id);    // 物理删除，真正删除记录
    }
}
```

查询和计数先复制调用方的 statement/where，再加入 `deleted=false`；支持缺省、null 和不可变 Map，保留已有条件。主键读取在数据模型的正式查询路径中将主键与附加条件作 AND 合并，因此不会因附加 where 丢失主键约束。`getByPrimaryKey(pk)` 仍自动填充关联字段。

### 7. SimpleSoftDeleteModelServiceImpl<PK, M>

简化版软删除服务，适用于模型类和实体类相同的场景。

**类型参数**:
- `PK`: 主键类型
- `M`: 模型类型（必须实现 SoftDelete）

**使用示例**:
```java
// 数据模型
public class Order implements SoftDelete {
    private Long id;
    private String orderNo;
    private boolean deleted;

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    @Override
    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}

// 服务实现
@Service
public class OrderService extends SimpleSoftDeleteModelServiceImpl<Long, Order> {
    // 自动支持软删除
}
```

## 查询 Statement 参数说明

`searchOne/searchList` 和带参数的 `getByPrimaryKey` 接收 `Map<String, Object> statement`；`count` 只接收 where 条件，用于构建复杂查询。

**常用参数**:

```java
Map<String, Object> statement = new HashMap<>();

// WHERE 条件
Map<String, Object> where = new HashMap<>();
where.put("name", "张三");           // 等值查询
where.put("age", Map.of("$gte", 18));          // 大于等于
where.put("status", Map.of("$in", List.of("ACTIVE", "PENDING")));  // IN 查询
statement.put("where", where);

// 关联查询（填充关联字段）
statement.put("POPULATE", List.of("author", "category"));

// 排序
statement.put("ORDER", Map.of("createTime", "desc"));

// 分页
statement.put("LIMIT", 10);
statement.put("OFFSET", 20);
```

## 使用示例

### 基本 CRUD 操作

```java
@Service
public class BookService extends SimpleModelServiceImpl<Long, Book> {

    public void example() throws OuroborosException {
        // 添加单个
        Book book = new Book();
        book.setTitle("Java 编程思想");
        Book saved = add(book);

        // 批量添加
        List<Book> books = List.of(book1, book2, book3);
        add(books);

        // 更新
        book.setTitle("Java 核心技术");
        update(book.getId(), book);

        // 部分更新
        Map<String, Object> updates = Map.of("price", 99.00);
        partialUpdate(book.getId(), updates);

        // 删除
        delete(book.getId());

        // 批量删除
        delete(List.of(1L, 2L, 3L));
    }
}
```

### 查询操作

```java
@Service
public class BookService extends SimpleModelServiceImpl<Long, Book> {

    public void queryExamples() throws OuroborosException {
        // 根据主键查询（自动填充关联字段）
        Book book = getByPrimaryKey(1L);

        // 根据主键查询（自定义填充）
        Map<String, Object> statement = new HashMap<>();
        statement.put("POPULATE", List.of("author"));
        Book book = getByPrimaryKey(1L, statement);

        // 条件查询单条
        Map<String, Object> where = Map.of("isbn", "978-0134685991");
        Map<String, Object> stmt = Map.of("where", where);
        Book found = searchOne(stmt);

        // 条件查询列表
        Map<String, Object> where = new HashMap<>();
        where.put("price", Map.of("$lte", 100.0));
        where.put("category", "技术");

        Map<String, Object> statement = new HashMap<>();
        statement.put("where", where);
        statement.put("ORDER", Map.of("publishDate", "desc"));
        statement.put("LIMIT", 10);

        List<? extends Book> books = searchList(statement);

        // 统计数量
        Long count = count(where);

        // 查询全部
        List<? extends Book> all = searchAll();
    }
}
```

### 软删除操作

```java
@Service
public class ArticleService extends SimpleSoftDeleteModelServiceImpl<Long, Article> {

    public void softDeleteExample() throws OuroborosException {
        // 软删除（设置 deleted=true）
        delete(1L);

        // 物理删除（真正删除记录）
        drop(1L);

        // 查询时自动过滤已软删除的记录
        List<? extends Article> articles = searchAll();  // 不包含 deleted=true 的记录

        // 添加时自动设置 deleted=false
        Article article = new Article();
        article.setTitle("新文章");
        add(article);  // deleted 自动设置为 false
    }
}
```

### 自定义业务方法

```java
@Service
public class UserService extends SimpleModelServiceImpl<Long, User> {

    // 按用户名查询
    public User findByUsername(String username) throws OuroborosException {
        Map<String, Object> where = Map.of("username", username);
        Map<String, Object> statement = Map.of("where", where);
        return searchOne(statement);
    }

    // 查询活跃用户
    public List<? extends User> findActiveUsers() throws OuroborosException {
        Map<String, Object> where = new HashMap<>();
        where.put("status", "ACTIVE");
        where.put("lastLoginTime", Map.of("$gte", LocalDateTime.now().minusDays(30)));

        Map<String, Object> statement = new HashMap<>();
        statement.put("where", where);
        statement.put("ORDER", Map.of("lastLoginTime", "desc"));

        return searchList(statement);
    }

    // 统计某角色的用户数量
    public Long countByRole(String role) throws OuroborosException {
        Map<String, Object> where = new HashMap<>(Map.of("role", role));
        return count(where);
    }
}
```

## 注意事项

1. **类型参数顺序**:
   - `ModelServiceImpl<PK, M, E>`: 3个类型参数
   - `SimpleModelServiceImpl<PK, M>`: 2个类型参数
   - `SoftDeleteModelServiceImpl<PK, M, E>`: 3个类型参数
   - `SimpleSoftDeleteModelServiceImpl<PK, M>`: 2个类型参数

2. **软删除字段**:
   - 默认字段名为 `deleted`
   - 可通过覆盖 `getDeletedFieldName()` 自定义字段名
   - 必须是 `boolean` 类型

3. **异常处理**:
   - 所有方法都会抛出 `OuroborosException` 或其子类
   - 内部使用 `OuroborosExceptionMapper` 自动转换底层异常

4. **查询自动过滤**:
   - 列表、单条、全部、计数和主键读取均加入删除标记条件
   - 入参可使用不可变 Map；服务不修改调用方的 statement/where

5. **关联字段填充**:
   - `getByPrimaryKey(pk)` 自动填充所有关联字段
   - 可通过 `statement.put("POPULATE", fieldList)` 自定义填充字段

6. **Typed Data Model 依赖**:
   - 服务类依赖 `TypedDataModelCenter` 自动获取数据模型
   - 确保对应的 Typed Data Model 已注册

7. **反射获取类型**:
   - 基类通过反射获取泛型参数的实际类型
   - 实现会越过非泛型中间类，取第一个 ParameterizedType 的实际类型参数；参数必须在预期位置解析为具体 Class。开放类型变量或改变参数顺序的泛型中间层需另行验证

8. **线程安全**:
   - 服务类通常标注 `@Service` 为单例
   - 软删除查询和计数使用独立条件 Map；底层模型、返回记录和调用方后续修改的线程安全仍按各自契约核对
