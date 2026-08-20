# Data Model Exception System Design

## 异常体系层次结构

```
DataModelException (顶层，RuntimeException)
├── StatementException (语句相关异常基类)
│   ├── NormalizeException (Normalize阶段 - 形式化错误)
│   │   ├── InvalidStatementException (无效语句)
│   │   ├── InvalidEntityNameException (无效实体名)
│   │   ├── StatementSyntaxException (语句语法错误)
│   │   ├── InvalidFieldExpressionException (无效字段表达式)
│   │   └── InvalidConditionException (无效条件表达式)
│   │
│   └── TranspileException (Transpile阶段 - 上下文错误)
│       ├── EntityNotFoundException (实体不存在)
│       ├── FieldNotFoundException (字段不存在)
│       ├── TypeMismatchException (类型不匹配)
│       ├── AmbiguousFieldException (字段歧义)
│       └── InvalidJoinException (无效关联)
│
├── DataValidationException (数据校验异常基类)
│   ├── RecordValidationException (记录校验失败，包含多字段错误)
│   ├── FieldValidationException (单字段校验失败)
│   ├── RequiredFieldMissingException (必填字段缺失)
│   ├── PrimaryKeyValidationException (主键校验失败)
│   ├── PrimaryKeyGenerationException (主键生成失败)
│   ├── UniqueConstraintViolationException (唯一约束冲突)
│   └── ReferenceConstraintViolationException (引用约束冲突)
│
├── MetadataException (元数据异常基类)
│   ├── ModelMetadataException (模型元数据异常)
│   └── FieldMetadataException (字段元数据异常)
│
├── MigrationException (数据库迁移异常基类)
│   └── DatabaseMigrationException (数据库迁移执行异常)
│
└── DataAccessException (底层数据访问异常基类)
    ├── DatabaseException (数据库异常包装)
    ├── ConnectionException (连接异常)
    ├── QueryExecutionException (查询执行异常)
    ├── TransactionException (事务异常)
    └── DataSourceException (数据源异常)
```

## 异常类职责说明

### 1. 语句异常层 (StatementException)

处理DSL语句解析、规范化、转译阶段的错误。

#### NormalizeException

- **职责**: Normalize阶段的形式化错误，如语法错误、格式错误
- **触发时机**: 原始Map → QueryStatement转换阶段
- **典型场景**:
  - 语句结构不符合DSL规范
  - 表达式格式错误
  - 参数类型错误

#### TranspileException

- **职责**: Transpile阶段的上下文错误，如字段不存在、类型不匹配
- **触发时机**: QueryStatement → QueryDSL转换阶段
- **典型场景**:
  - 引用的实体/字段不存在
  - 字段类型与操作不兼容
  - 关联条件错误

### 2. 数据校验异常层 (DataValidationException)

处理用户提交数据的业务规则校验错误。

#### RecordValidationException

- **职责**: 整条记录的多字段校验失败
- **包含信息**: Map<String, List<String>> 字段名 -> 错误列表
- **典型场景**:
  - Insert/Update时多个字段同时校验失败
  - 跨字段业务规则校验

#### FieldValidationException

- **职责**: 单个字段的校验失败
- **包含信息**: 字段名、错误消息列表
- **典型场景**:
  - 字段值格式不正确
  - 字段值超出允许范围
  - 自定义校验规则不通过

### 3. 数据访问异常层 (DataAccessException)

处理底层数据源访问过程中的错误。

#### DatabaseException

- **职责**: 包装底层数据库驱动异常（JDBC SQLException等）
- **保留原始异常**: 保存完整的cause chain
- **典型场景**:
  - SQL执行错误
  - 约束违反（数据库级别）
  - 死锁等数据库错误

## 重构原则

### 1. 脱离OuroborosException依赖

- 所有异常继承自 DataModelException
- 不再使用 BusinessException, PlatformException, InternalException

### 2. 异常携带详细信息

- 每个异常包含足够的上下文信息
- 支持结构化的错误信息（如字段级别错误）
- 便于上层精确处理和展示错误

### 3. 异常层次清晰

- 按处理阶段分层
- 每层职责明确
- 支持按层次捕获和处理

### 4. 兼容性考虑

- 保留现有异常类作为deprecated
- 提供迁移指南
- 逐步替换旧异常使用

## 迁移计划

### Phase 1: 定义新异常类

1. 创建新的异常类层次结构
2. 添加详细的Javadoc说明
3. 标记旧异常类为 @Deprecated

### Phase 2: 替换核心模块

1. DMLStatements.java - 语句构建异常
2. QueryNormalizer.java - Normalize异常
3. QueryTranspiler.java - Transpile异常
4. AbstractDataModel.java - 数据校验异常
5. DataAdapter实现 - 数据访问异常

### Phase 3: 测试和验证

1. 单元测试覆盖所有新异常
2. 集成测试验证异常传播
3. 错误消息国际化支持

### Phase 4: 清理

1. 删除 @Deprecated 的旧异常类
2. 清理对 OuroborosException 的依赖
3. 更新文档
