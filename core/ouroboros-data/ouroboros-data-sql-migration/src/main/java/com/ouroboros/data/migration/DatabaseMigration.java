package com.ouroboros.data.migration;

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vavr.Tuple;
import io.vavr.control.Either;
import io.vavr.control.Try;

import com.ouroboros.data.exception.ConnectionException;
import com.ouroboros.data.exception.DataModelException;
import com.ouroboros.data.exception.DatabaseMigrationException;
import com.ouroboros.data.model.DataModel;

import liquibase.CatalogAndSchema;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.core.MSSQLDatabase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.diff.DiffGeneratorFactory;
import liquibase.diff.DiffResult;
import liquibase.diff.ObjectDifferences;
import liquibase.diff.compare.CompareControl;
import liquibase.diff.output.DiffOutputControl;
import liquibase.diff.output.ObjectChangeFilter;
import liquibase.resource.AbstractResource;
import liquibase.resource.AbstractResourceAccessor;
import liquibase.resource.Resource;
import liquibase.serializer.core.xml.XMLChangeLogSerializer;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.SnapshotControl;
import liquibase.snapshot.SnapshotGeneratorFactory;
import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGeneratorFactory;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.*;

public class DatabaseMigration {
  private static final String REQUIRED_AFTER_BACKFILL = "requireAfterBackfill";
  private final static Logger logger = LoggerFactory.getLogger(DatabaseMigration.class);

  private final Class<? extends DatabaseObject>[] dataObjectTypes = new Class[]{
      Table.class,
      PrimaryKey.class,
      ForeignKey.class,
      Column.class,
      Sequence.class,
      UniqueConstraint.class,
      Index.class,
//      View.class
  };

  private final Connection connection;
  private final String context;
  private final String author;

  private final Database database;

  public DatabaseMigration(Connection connection, String context, String author) {
    this.connection = connection;
    this.context = context;
    this.author = author;

    this.database = Try.of(() ->
        DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection))
    ).toJavaOptional().orElseThrow(() -> new ConnectionException("数据库连接初始化失败"));

  }

  /**
   * 迁移
   *
   * @return
   */
  @SuppressWarnings("deprecation")
  //TODO: liquibase 升级后需要修改为使用 CommandScope
  public Either<DataModelException, Void> migrate(List<DataModel> models) {
    return prepareLiquibase(models, true).flatMap(lb -> Try.run(() -> {
      lb.update(StringUtils.isBlank(context) ? new Contexts() : new Contexts(context), new LabelExpression());
      connection.commit();
    }).toEither().mapLeft(e -> new DatabaseMigrationException("数据库迁移执行失败", e)));
  }

  /**
   * 获取数据库迁移SQL
   *
   * @return
   */
  public Either<DataModelException, List<String>> getMigrationSqls(List<DataModel> models) {
    return prepareLiquibase(models, false).flatMap(liquibase -> Try.of(() -> {
      return generateMigrationSqls(liquibase.getDatabaseChangeLog(), liquibase.getDatabase());
    }).toEither().mapLeft(e -> new DatabaseMigrationException("生成数据库迁移SQL失败", e)));
  }

  private static List<String> generateMigrationSqls(DatabaseChangeLog changeLog, Database database) {
    var sqlGeneratorFactory = SqlGeneratorFactory.getInstance();
    return changeLog.getChangeSets().stream()
        .flatMap(changeSet -> changeSet.getChanges().stream())
        .flatMap(change -> Arrays.stream(sqlGeneratorFactory.generateSql(change, database)))
        .map(DatabaseMigration::appendDelimiter)
        .collect(Collectors.toList());
  }

  private static String appendDelimiter(Sql sql) {
    var statement = StringUtils.stripEnd(sql.toSql(), null);
    var delimiter = StringUtils.trimToEmpty(sql.getEndDelimiter());
    if (delimiter.isEmpty() || statement.endsWith(delimiter)) {
      return statement;
    }
    return statement + delimiter;
  }

  /**
   * 准备数据库迁移Liquibase
   *
   * @return
   */
  private Either<DataModelException, Liquibase> prepareLiquibase(
      List<DataModel> models, boolean runUniqueConstraintPreflight) {
    return getCurrentSnapshot(models)
        .flatMap(cs -> getFurtheredSnapshot(models, cs).map(fs -> Tuple.of(cs, fs)))
        .flatMap(tuple -> diffSnapshot(tuple._1(), tuple._2()))
        .flatMap(diff -> runUniqueConstraintPreflight
            ? preflightNewUniqueConstraints(models, diff.getComparisonSnapshot(), diff).map(ignored -> diff)
            : Either.right(diff))
        .flatMap(diff -> getChangeLog(diff, models))
        .flatMap(this::buildLiquibase);
  }

  /**
   * Check rows before Liquibase emits an ADD UNIQUE change.  Relying on the
   * database error alone makes the failure opaque and varies by dialect; more
   * importantly, it can leave a partially applied change log on databases that
   * do not execute all DDL atomically.  Only constraints that are actually
   * missing from the current snapshot are checked.
   */
  private Either<DataModelException, Void> preflightNewUniqueConstraints(
      List<DataModel> models, DatabaseSnapshot currentSnapshot, DiffResult diff) {
    try {
      for (UniqueConstraint constraint : diff.getMissingObjects(UniqueConstraint.class)) {
        if (constraint == null || constraint.getRelation() == null
            || constraint.getColumns() == null || constraint.getColumns().isEmpty()) {
          continue;
        }
        String tableName = constraint.getRelation().getName();
        if (StringUtils.isBlank(tableName)
            || models.stream().noneMatch(model -> tableName.equalsIgnoreCase(model.getRawName()))) {
          continue;
        }
        // A missing table is created by the same change set; there cannot be
        // historical rows to validate before that create operation.
        var currentTable = currentSnapshot.get(Table.class).stream()
            .filter(table -> tableName.equalsIgnoreCase(table.getName())).findFirst();
        if (currentTable.isEmpty()) {
          continue;
        }
        // New columns have no historical values to query before Liquibase adds them.
        // Their constraints are applied by the generated migration after column creation.
        if (constraint.getColumns().stream().anyMatch(column -> currentTable.get().getColumns().stream()
            .noneMatch(existing -> column.getName().equalsIgnoreCase(existing.getName())))) {
          continue;
        }
        DuplicateGroup duplicate = findDuplicateGroup(constraint);
        if (duplicate != null) {
          String message = "新增唯一约束前发现重复数据: table=" + tableName
              + ", constraint=" + constraint.getName()
              + ", key=" + duplicate.key() + ", count=" + duplicate.count();
          logger.error(message);
          return Either.left(new DatabaseMigrationException(message));
        }
      }
      return Either.right(null);
    } catch (SQLException ex) {
      return Either.left(new DatabaseMigrationException("唯一约束重复数据预检失败", ex));
    }
  }

  private DuplicateGroup findDuplicateGroup(UniqueConstraint constraint) throws SQLException {
    CatalogAndSchema schema = getCatalogAndSchema();
    String table = database.escapeTableName(
        schema.getCatalogName(), schema.getSchemaName(), constraint.getRelation().getName());
    String columns = constraint.getColumns().stream()
        .map(column -> database.escapeColumnName(
            schema.getCatalogName(), schema.getSchemaName(), constraint.getRelation().getName(),
            column.getName()))
        .collect(Collectors.joining(", "));
    String nonNullPredicate = constraint.getColumns().stream()
        .map(column -> database.escapeColumnName(
            schema.getCatalogName(), schema.getSchemaName(), constraint.getRelation().getName(),
            column.getName()) + " IS NOT NULL")
        .collect(Collectors.joining(" AND "));
    // All identifiers come from Liquibase's dialect-aware escaping helpers and the statement
    // carries no value literals, so there is nothing to bind; the PreparedStatement is used
    // only so the duplicated-check follows the same execution path as the other queries here.
    StringBuilder sql = new StringBuilder("SELECT ");
    sql.append(columns)
        .append(", COUNT(*) AS duplicate_count FROM ")
        .append(table)
        .append(" WHERE ")
        .append(nonNullPredicate)
        .append(" GROUP BY ")
        .append(columns)
        .append(" HAVING COUNT(*) > 1");
    try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
      statement.setMaxRows(1);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return null;
        }
        List<String> values = new ArrayList<>();
        for (int index = 1; index <= constraint.getColumns().size(); index++) {
          values.add(String.valueOf(resultSet.getObject(index)));
        }
        return new DuplicateGroup(String.join(",", values), resultSet.getLong("duplicate_count"));
      }
    }
  }

  private record DuplicateGroup(String key, long count) {
  }

  /**
   * 获取当前数据库快照（根据当前模型获取）
   *
   * @return
   */
  private Either<DataModelException, DatabaseSnapshot> getCurrentSnapshot(List<DataModel> models) {
    return Try.of(() ->
        SnapshotGeneratorFactory.getInstance().createSnapshot(
            getCatalogAndSchema(), database, buildSnapshotControl(models)
        )
    ).toEither().mapLeft(e -> {
      if (e instanceof DataModelException dmException) {
        return dmException;
      }

      return new DatabaseMigrationException("获取当前数据库快照失败", e);
    });
  }

  /**
   * 获取数据库未来快照（并合并数据模型）
   *
   * @param currentSnapshot
   * @return
   */
  private Either<DataModelException, ? extends DatabaseSnapshot> getFurtheredSnapshot(List<DataModel> models, DatabaseSnapshot currentSnapshot) {
    return Try.of(() -> {
      var newSnapshot = new DataModelDatabaseSnapshot(database, models);
      newSnapshot.mergeDatabaseSnapshot(currentSnapshot);
      stageRequiredColumns(models, currentSnapshot, newSnapshot);
      return newSnapshot;
    }).toEither().mapLeft(e -> {
      if (e instanceof DataModelException dmException) {
        return dmException;
      }

      return new DatabaseMigrationException("生成目标数据库快照失败", e);
    });
  }

  private void stageRequiredColumns(List<DataModel> models, DatabaseSnapshot current, DataModelDatabaseSnapshot desired)
      throws SQLException {
    var currentObjects = DataModelDatabaseSnapshot.getDatabaseObjectCollection(current);
    var desiredObjects = DataModelDatabaseSnapshot.getDatabaseObjectCollection(desired);
    for (Column column : desiredObjects.get(Column.class)) {
      if (!Boolean.FALSE.equals(column.isNullable())
          || !DataModelDatabaseSnapshot.hasFieldOption(models, column, REQUIRED_AFTER_BACKFILL)) {
        continue;
      }
      var source = currentObjects.get(Table.class).stream()
          .filter(table -> table.getName().equalsIgnoreCase(column.getRelation().getName()))
          .findFirst();
      if (source.isEmpty()) { continue; } // A new table can be constrained immediately.
      var existing = source.get().getColumns().stream().filter(c -> c.getName().equalsIgnoreCase(column.getName())).findFirst();
      if (existing.isEmpty() || (!Boolean.FALSE.equals(existing.get().isNullable()) && containsNull(source.get(), column))) {
        column.setNullable(true);
      }
    }
  }

  private boolean containsNull(Table table, Column column) throws SQLException {
    var schema = getCatalogAndSchema();
    var qualified = database.escapeTableName(schema.getCatalogName(), schema.getSchemaName(), table.getName());
    var name = database.escapeColumnName(schema.getCatalogName(), schema.getSchemaName(), table.getName(), column.getName());
    // Only dialect-escaped identifiers from the database snapshot enter this statement.
    var sql = String.join(" ", "SELECT 1 FROM", qualified, "WHERE", name, "IS NULL");
    try (var statement = connection.prepareStatement(sql)) {
      statement.setMaxRows(1);
      try (var rows = statement.executeQuery()) {
        return rows.next();
      }
    }
  }

  public Either<DataModelException, Boolean> hasRequiredUniqueColumn(DataModel model, String columnName) {
    return Try.of(() -> {
      var metadata = connection.getMetaData();
      var catalog = database.getDefaultCatalogName();
      var schema = database.getDefaultSchemaName();
      String table = database.correctObjectName(model.getRawName(), Table.class);
      String physicalColumn = database.correctObjectName(columnName, Column.class);
      boolean required = false;
      try (var columns = metadata.getColumns(catalog, schema, table, physicalColumn)) {
        while (columns.next()) {
          if (table.equalsIgnoreCase(columns.getString("TABLE_NAME"))
              && columnName.equalsIgnoreCase(columns.getString("COLUMN_NAME"))
              && "NO".equals(columns.getString("IS_NULLABLE"))) {
            required = true;
            break;
          }
        }
      }
      if (!required) { return false; }
      Map<String, List<String>> uniqueIndexes = new HashMap<>();
      try (var indexes = metadata.getIndexInfo(catalog, schema, table, true, false)) {
        while (indexes.next()) {
          var name = indexes.getString("INDEX_NAME");
          if (name != null && indexes.getString("FILTER_CONDITION") == null) {
            uniqueIndexes.computeIfAbsent(name, ignored -> new ArrayList<>()).add(indexes.getString("COLUMN_NAME"));
          }
        }
      }
      return uniqueIndexes.values().stream()
          .anyMatch(columns -> columns.size() == 1 && columnName.equalsIgnoreCase(columns.getFirst()));
    }).toEither().mapLeft(error -> new DatabaseMigrationException("核验数据库字段约束失败", error));
  }

  /**
   * 对比快照差异
   *
   * @param current
   * @param furthered
   * @return
   */
  private Either<DataModelException, DiffResult> diffSnapshot(DatabaseSnapshot current, DatabaseSnapshot furthered) {
    return Try.of(() -> {
      var compareControl = new CompareControl(new HashSet<>(Arrays.asList(dataObjectTypes)));
      return DiffGeneratorFactory.getInstance().compare(furthered, current, compareControl);
    }).toEither().mapLeft(e -> {
      if (e instanceof DataModelException dmException) {
        return dmException;
      }

      return new DatabaseMigrationException("对比数据库快照差异失败", e);
    });
  }

  /**
   * 生成数据库变更日志（内存存储）
   *
   * @param diff
   * @return
   */
  private Either<DataModelException, DatabaseChangeLog> getChangeLog(DiffResult diff, List<DataModel> models) {
    var idRoot = nextInMemoryChangeSetIdRoot();
    var changeLogPath = idRoot + ".xml";
    return Try.of(() -> {
      var diffOutputControl = getDiffOutputControl(models);
      var diffToChangeLog = new CustomDiffToChangeLog(diff, diffOutputControl);
      diffToChangeLog.setChangeSetContext(context);
      diffToChangeLog.setChangeSetAuthor(author);
      diffToChangeLog.setIdRoot(idRoot);
      diffToChangeLog.setChangeSetPath(changeLogPath);

      var databaseChangeLog = new DatabaseChangeLog();
      databaseChangeLog.setLogicalFilePath(changeLogPath);
      databaseChangeLog.setPhysicalFilePath(changeLogPath);

      diffToChangeLog.generateChangeSets().stream()
          .map(cs -> new ChangeSetWrapper(databaseChangeLog, cs))
          .forEach(databaseChangeLog::addChangeSet);

      return databaseChangeLog;
    }).toEither().mapLeft(e -> {
      if (e instanceof DataModelException dmException) {
        return dmException;
      }
      return new DatabaseMigrationException("生成数据库变更日志失败", e);
    });
  }

  static String nextInMemoryChangeSetIdRoot() {
    return "migration-" + System.currentTimeMillis() + "-" + UUID.randomUUID();
  }

  /**
   * 构建Liquibase实例
   *
   * @param changeLog
   * @return
   */
  private Either<DataModelException, Liquibase> buildLiquibase(DatabaseChangeLog changeLog) {
    return Try.of(() ->
        new Liquibase(changeLog, new InMemoryResourceAccessor(changeLog), database)
    ).toEither().mapLeft(e -> {
      if (e instanceof DataModelException dmException) {
        return dmException;
      }

      return new DatabaseMigrationException("创建Liquibase实例失败", e);
    });
  }

  /**
   * 获取数据库快照控制
   *
   * @return
   */
  private SnapshotControl buildSnapshotControl(List<DataModel> models) {
    var filter = new SnapshotObjectFilter(models, dataObjectTypes);
    return new SnapshotControl(database, filter, dataObjectTypes);
  }

  /**
   * 获取数据库的Catalog或Schema
   *
   * @return
   */
  private CatalogAndSchema getCatalogAndSchema() {
    if (database instanceof MSSQLDatabase) {
      return CatalogAndSchema.DEFAULT;
    }

    return database.getDefaultSchema();
  }

  /**
   * 获取Diff输出控制
   *
   * @return
   */
  private DiffOutputControl getDiffOutputControl(List<DataModel> models) {
    var control = new DiffOutputControl();
    if (database instanceof MSSQLDatabase) {
      control.setIncludeCatalog(false);
    }

    control.setObjectChangeFilter(new DiffToChangeLogFilter(models));
    return control;
  }

  private static class SnapshotObjectFilter implements ObjectChangeFilter {
    private final List<DataModel> models;
    private final List<Class<? extends DatabaseObject>> dataObjectTypes;

    public SnapshotObjectFilter(List<DataModel> models, Class<? extends DatabaseObject>[] dataObjectTypes) {
      this.models = models;
      this.dataObjectTypes = Stream.of(dataObjectTypes).collect(Collectors.toList());
      this.dataObjectTypes.add(Catalog.class);
      this.dataObjectTypes.add(Schema.class);
    }

    @Override
    public boolean includeMissing(DatabaseObject object, Database referenceDatabase, Database comparisionDatabase) {
      return include(object);
    }

    @Override
    public boolean includeUnexpected(DatabaseObject object, Database referenceDatabase, Database comparisionDatabase) {
      return include(object);
    }

    @Override
    public boolean includeChanged(DatabaseObject object, ObjectDifferences differences, Database referenceDatabase, Database comparisionDatabase) {
      return include(object);
    }

    @Override
    public boolean include(DatabaseObject object) {
      var includeType = dataObjectTypes.contains(object.getClass());
      if (includeType && object instanceof Table table) {
        return models.stream().anyMatch(m -> m.getRawName().equalsIgnoreCase(table.getName()));
      }
      return includeType;
    }
  }

  private static class DiffToChangeLogFilter implements ObjectChangeFilter {
    private final List<DataModel> models;

    private DiffToChangeLogFilter() { this(List.of()); }

    private DiffToChangeLogFilter(List<DataModel> models) { this.models = models; }

    private Set<Column> missingColumns = new HashSet<>();
    private Set<Table> missingTables = new HashSet<>();

    @Override
    public boolean includeMissing(DatabaseObject object, Database referenceDatabase, Database comparisionDatabase) {
      if (object instanceof Table table) {
        missingTables.add(table);
        return true;
      }
      if (object instanceof Column column) {
        missingColumns.add(column);
        return true;
      }
      if (object instanceof UniqueConstraint) {
        return true;
      }
      return true;
    }

    @Override
    public boolean includeUnexpected(DatabaseObject object, Database referenceDatabase, Database comparisionDatabase) {
      return true;
    }

    @Override
    public boolean includeChanged(DatabaseObject object, ObjectDifferences differences, Database referenceDatabase, Database comparisionDatabase) {
      if (object instanceof Column column) {
        var nullable = differences.getDifference("nullable");
        if (nullable == null) {
          return true;
        }

        if (nullable.getReferenceValue() instanceof Boolean refValue && !refValue) {
          if (DataModelDatabaseSnapshot.hasFieldOption(models, column, REQUIRED_AFTER_BACKFILL)) {
            return true;
          }
          logger.warn("字段 nullable 属性被忽略: {}", column.getName());
          return false;
        }
      }
      return true;
    }

    @Override
    public boolean include(DatabaseObject object) {
      logger.info("include: {}, {}", object.getObjectTypeName(), object.getName());
      return true;
    }
  }

  private static class InMemoryResourceAccessor extends AbstractResourceAccessor {
    final Resource resource;

    public InMemoryResourceAccessor(DatabaseChangeLog changeLog) {
      this.resource = new InMemoryResource(changeLog);
    }

    @Override
    public List<Resource> search(String path, boolean recursive) throws IOException {
      return Collections.singletonList(resource);
    }

    @Override
    public List<Resource> getAll(String path) {
      return Collections.singletonList(resource);
    }

    @Override
    public List<String> describeLocations() {
      return Collections.emptyList();
    }

    @Override
    public void close() {

    }

    @Override
    public Resource getExisting(String path) throws IOException {
      if (path.equals(resource.getPath())) {
        return resource;
      }
      throw new IOException("Resource not found: " + path);
    }

    @Override
    public Resource get(String path) throws IOException {
      return resource;
    }

    private static class InMemoryResource extends AbstractResource {
      String content;

      public InMemoryResource(DatabaseChangeLog changeLog) {
        super(changeLog.getPhysicalFilePath(), null);
        content = serialize(changeLog);
      }

      String serialize(DatabaseChangeLog changeLog) {
        var xmlSerializer = new XMLChangeLogSerializer();
        var outStream = new ByteArrayOutputStream();
        try {
          xmlSerializer.write(changeLog.getChangeSets(), outStream);
        } catch (IOException e) {
          return null;
        }
        return outStream.toString();
      }

      @Override
      public InputStream openInputStream() {
        return new ByteArrayInputStream(content.getBytes());
      }

      @Override
      public boolean exists() {
        return true;
      }

      @Override
      public Resource resolve(String other) {
        return null;
      }

      @Override
      public Resource resolveSibling(String other) {
        return null;
      }
    }
  }
}
