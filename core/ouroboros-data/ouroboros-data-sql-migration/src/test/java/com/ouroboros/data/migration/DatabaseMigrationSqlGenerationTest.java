package com.ouroboros.data.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.migration.kingbase.KingbaseDatabase;

import liquibase.change.ColumnConfig;
import liquibase.change.core.CreateTableChange;
import liquibase.change.core.SetColumnRemarksChange;
import liquibase.change.core.SetTableRemarksChange;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.database.Database;

class DatabaseMigrationSqlGenerationTest {

  @Test
  void shouldGenerateOrderedDelimitedSqlFromInternalDiffChanges() throws Exception {
    var changeLog = new DatabaseChangeLog();
    var changeSet = new ChangeSet("structured", "DatabaseMigrationSqlGenerationTest", false, false,
        "in-memory-changelog", null, null, changeLog);
    changeSet.addChange(createTableChange());
    changeSet.addChange(setTableRemarksChange());
    changeSet.addChange(setColumnRemarksChange());
    changeLog.addChangeSet(changeSet);

    List<String> sqls = generateMigrationSqls(changeLog, new KingbaseDatabase());

    assertEquals(3, sqls.size());
    assertTrue(sqls.get(0).startsWith("CREATE TABLE"));
    assertTrue(sqls.get(1).startsWith("COMMENT ON TABLE"));
    assertTrue(sqls.get(2).startsWith("COMMENT ON COLUMN"));
    sqls.forEach(sql -> {
      assertTrue(sql.endsWith(";"));
      assertFalse(sql.endsWith(";;"));
      assertFalse(sql.endsWith("\n"));
    });
  }

  private static List<String> generateMigrationSqls(DatabaseChangeLog changeLog, Database database) throws Exception {
    Method method = DatabaseMigration.class.getDeclaredMethod(
        "generateMigrationSqls", DatabaseChangeLog.class, Database.class);
    method.setAccessible(true);
    var result = (List<?>) method.invoke(null, changeLog, database);
    return result.stream().map(String.class::cast).collect(Collectors.toList());
  }

  private static CreateTableChange createTableChange() {
    var change = new CreateTableChange();
    change.setTableName("kingbase_structured_preview");
    change.addColumn(new ColumnConfig().setName("id").setType("INTEGER"));
    return change;
  }

  private static SetTableRemarksChange setTableRemarksChange() {
    var change = new SetTableRemarksChange();
    change.setTableName("kingbase_structured_preview");
    change.setRemarks("structured table");
    return change;
  }

  private static SetColumnRemarksChange setColumnRemarksChange() {
    var change = new SetColumnRemarksChange();
    change.setTableName("kingbase_structured_preview");
    change.setColumnName("id");
    change.setColumnDataType("INTEGER");
    change.setRemarks("structured column");
    return change;
  }
}
