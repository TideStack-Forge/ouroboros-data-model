package com.ouroboros.data.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Method;
import java.sql.Connection;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import liquibase.change.ColumnConfig;
import liquibase.change.ConstraintsConfig;
import liquibase.change.core.CreateTableChange;
import liquibase.changelog.ChangeSet;
import liquibase.diff.DiffResult;
import liquibase.diff.compare.CompareControl;
import liquibase.diff.output.DiffOutputControl;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.SnapshotGeneratorFactory;
import liquibase.structure.core.Column;
import liquibase.structure.core.Table;
import liquibase.structure.core.UniqueConstraint;

class CustomDiffToChangeLogTest {

  @Test
  void shouldPopulateMissingUniqueConstraintNameFromDiffResult() throws Exception {
    var ds = new JdbcDataSource();
    ds.setURL("jdbc:h2:mem:custom_diff_test;MODE=MySQL;DB_CLOSE_DELAY=-1");
    ds.setUser("sa");
    ds.setPassword("");
    var conn = ds.getConnection();
    var database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(conn));
    var reference = SnapshotGeneratorFactory.getInstance().createSnapshot(database.getDefaultSchema(), database, new liquibase.snapshot.SnapshotControl(database));
    var comparison = SnapshotGeneratorFactory.getInstance().createSnapshot(database.getDefaultSchema(), database, new liquibase.snapshot.SnapshotControl(database));
    var diffResult = new DiffResult(reference, comparison, new CompareControl());

    var table = new Table("", "", "USERS");
    var column = new Column();
    column.setName("NAME");
    column.setRelation(table);
    var uniqueConstraint = new UniqueConstraint();
    uniqueConstraint.setName("UK_USERS_NAME");
    uniqueConstraint.setRelation(table);
    uniqueConstraint.addColumn(0, column);
    table.getUniqueConstraints().add(uniqueConstraint);
    diffResult.addMissingObject(table);

    var target = new CustomDiffToChangeLog(diffResult, new DiffOutputControl());

    var create = new CreateTableChange();
    create.setTableName("USERS");
    var columnConfig = new ColumnConfig();
    columnConfig.setName("NAME");
    var constraints = new ConstraintsConfig();
    constraints.setUnique(true);
    columnConfig.setConstraints(constraints);
    create.addColumn(columnConfig);

    var cs = new ChangeSet((liquibase.changelog.DatabaseChangeLog) null);
    cs.addChange(create);

    var peek = CustomDiffToChangeLog.class.getDeclaredMethod("peekChangeSet", ChangeSet.class);
    peek.setAccessible(true);
    peek.invoke(target, cs);

    assertNotNull(columnConfig.getConstraints());
    assertEquals("UK_USERS_NAME", columnConfig.getConstraints().getUniqueConstraintName());
  }
}
