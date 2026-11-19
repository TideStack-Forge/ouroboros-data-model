# Data Model Exception System Design

## 异常体系层次结构

```text
DataModelException (RuntimeException)
├── DataAccessException
│   ├── ConnectionException
│   ├── DatabaseException
│   └── QueryExecutionException
├── DataValidationException
│   ├── PrimaryKeyGenerationException
│   ├── PrimaryKeyValidationException
│   ├── RecordValidationException
│   │   └── ValidationException
│   ├── ReferenceConstraintViolationException
│   └── UniqueConstraintViolationException
├── ExecuteException
├── FieldValidationException
├── MetadataException
│   ├── FieldMetadataException
│   └── ModelMetadataException
├── MigrationException
│   └── DatabaseMigrationException
└── StatementException
    ├── NormalizeException
    │   ├── InvalidEntityNameException
    │   ├── InvalidStatementException
    │   ├── StatementError
    │   │   ├── InvalidEntityName
    │   │   ├── StatementCheckFailure
    │   │   ├── ValuesError
    │   │   └── ValuesListError
    │   └── StatementSyntaxException
    └── TranspileException
        ├── EntityNotFoundException
        ├── FieldNotFoundException
        └── TypeMismatchException
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

处理用户提交数据的业务规则校验错误。`FieldValidationException` 属于同一业务场景，但继承边界独立于本层。

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

## 维护与边界

继承关系以本包源类型为准；其中 `FieldValidationException` 直接继承 `DataModelException`，不能仅捕获 `DataValidationException` 就假定覆盖全部字段错误。旧 `ValidationException` 仍继承 `RecordValidationException`，是否继续迁移应按实际调用点安排。

原阶段性迁移清单不代表当前完成状态，已退出本页；历史可从本文件 Git 记录追溯。本页只维护当前异常合同。内核异常在平台 API 边界的映射见 [exception-mapper](../../../../../../../../../../libs/common/common-exception-mapper/README.md)。
