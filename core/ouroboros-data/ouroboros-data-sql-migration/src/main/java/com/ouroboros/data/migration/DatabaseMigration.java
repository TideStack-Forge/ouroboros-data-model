package com.ouroboros.data.migration;

import java.io.*;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
    return prepareLiquibase(models).flatMap(lb -> Try.run(() -> {
      lb.update(context);
      connection.commit();
    }).toEither().mapLeft(e -> new DatabaseMigrationException("数据库迁移执行失败", e)));
  }

  /**
   * 获取数据库迁移SQL
   *
   * @return
   */
  public Either<DataModelException, List<String>> getMigrationSqls(List<DataModel> models) {
    return prepareLiquibase(models).flatMap(liquibase -> Try.of(() -> {
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
  private Either<DataModelException, Liquibase> prepareLiquibase(List<DataModel> models) {
    return getCurrentSnapshot(models)
        .flatMap(cs -> getFurtheredSnapshot(models, cs).map(fs -> Tuple.of(cs, fs)))
        .flatMap(tuple -> diffSnapshot(tuple._1(), tuple._2()))
        .flatMap(this::getChangeLog)
        .flatMap(this::buildLiquibase);
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
      return newSnapshot;
    }).toEither().mapLeft(e -> {
      if (e instanceof DataModelException dmException) {
        return dmException;
      }

      return new DatabaseMigrationException("生成目标数据库快照失败", e);
    });
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
  private Either<DataModelException, DatabaseChangeLog> getChangeLog(DiffResult diff) {
    var idRoot = "migration-" + LocalDateTime.now().toEpochSecond(ZoneOffset.of("+8"));
    return Try.of(() -> {
      var diffOutputControl = getDiffOutputControl();
      var diffToChangeLog = new CustomDiffToChangeLog(diff, diffOutputControl);
      diffToChangeLog.setChangeSetContext(context);
      diffToChangeLog.setChangeSetAuthor(author);
      diffToChangeLog.setIdRoot(idRoot);
      diffToChangeLog.setChangeSetPath("in-memory-changelog");

      var databaseChangeLog = new DatabaseChangeLog();
      databaseChangeLog.setLogicalFilePath("in-memory-changelog");
      databaseChangeLog.setPhysicalFilePath("in-memory-changelog.xml");

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
  private DiffOutputControl getDiffOutputControl() {
    var control = new DiffOutputControl();
    if (database instanceof MSSQLDatabase) {
      control.setIncludeCatalog(false);
    }

    control.setObjectChangeFilter(new DiffToChangeLogFilter());
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
          // TODO: 如果 nullable 从 true 变为 false，需要检查是否有数据，现在先一律忽略
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
