package com.ouroboros.data.migration;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.ouroboros.data.migration.kingbase.KingbaseDatabase;

import liquibase.Scope;
import liquibase.change.core.AddNotNullConstraintChange;
import liquibase.change.core.CreateTableChange;
import liquibase.change.core.ModifyDataTypeChange;
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import liquibase.database.core.MySQLDatabase;
import liquibase.diff.DiffResult;
import liquibase.diff.output.DiffOutputControl;
import liquibase.diff.output.changelog.DiffToChangeLog;
import liquibase.exception.DatabaseException;
import liquibase.executor.ExecutorService;
import liquibase.structure.core.Column;
import liquibase.structure.core.DataType;
import liquibase.structure.core.Table;
import liquibase.structure.core.UniqueConstraint;
import liquibase.util.DependencyUtil;

public class CustomDiffToChangeLog extends DiffToChangeLog {
  DiffResult diffResult;

  public CustomDiffToChangeLog(DiffResult diffResult, DiffOutputControl diffOutputControl) {
    super(diffResult, diffOutputControl);
    this.diffResult = diffResult;
  }

  @Override
  public List<ChangeSet> generateChangeSets() {
    return super.generateChangeSets()
        .stream()
        .peek(this::preserveMySqlNullability)
        .peek(this::peekChangeSet)
        .collect(Collectors.toList());
  }

  @Override
  protected void addDependencies(DependencyUtil.DependencyGraph<String> graph, List<String> schemas, Database database)
      throws DatabaseException {
    if (!(database instanceof KingbaseDatabase kingbase)) {
      super.addDependencies(graph, schemas, database);
      return;
    }
    var executor = Scope.getCurrentScope().getSingleton(ExecutorService.class).getExecutor("jdbc", database);
    var rows = executor.queryForList(kingbase.getObjectDependenciesStatement(schemas));
    for (Map<String, ?> row : rows) {
      String first = StringUtils.trimToEmpty((String) row.get("REFERENCING_SCHEMA_NAME")) + "."
          + StringUtils.trimToEmpty((String) row.get("REFERENCING_NAME"));
      String after = StringUtils.trimToEmpty((String) row.get("REFERENCED_SCHEMA_NAME")) + "."
          + StringUtils.trimToEmpty((String) row.get("REFERENCED_NAME"));
      // Keep Liquibase's names both with and without stored-function argument lists.
      graph.add(first.replace("\"", ""), after.replace("\"", ""));
      graph.add(first.replace("\"", "").replaceAll("\\s*\\([^)]*\\)\\s*", ""),
          after.replace("\"", "").replaceAll("\\s*\\([^)]*\\)\\s*", ""));
    }
  }

  private void preserveMySqlNullability(ChangeSet changeSet) {
    if (!(diffResult.getComparisonSnapshot().getDatabase() instanceof MySQLDatabase)) {
      return;
    }
    for (var change : List.copyOf(changeSet.getChanges())) {
      if (!(change instanceof ModifyDataTypeChange modify)) {
        continue;
      }
      boolean needsNotNull = diffResult.getChangedObjects(Column.class).entrySet().stream().anyMatch(entry -> {
        var column = entry.getKey();
        var type = entry.getValue().getDifference("type");
        return column.getName().equalsIgnoreCase(modify.getColumnName())
            && column.getRelation().getName().equalsIgnoreCase(modify.getTableName())
            && Boolean.FALSE.equals(column.isNullable())
            && type != null && type.getComparedValue() instanceof DataType existing
            && DataModelDatabaseSnapshot.isShortVarcharToText(existing, column.getType());
      });
      if (needsNotNull) {
        // MySQL MODIFY replaces the column definition, so include NOT NULL in that same statement.
        var replacement = new AddNotNullConstraintChange();
        replacement.setCatalogName(modify.getCatalogName());
        replacement.setSchemaName(modify.getSchemaName());
        replacement.setTableName(modify.getTableName());
        replacement.setColumnName(modify.getColumnName());
        replacement.setColumnDataType(modify.getNewDataType());
        changeSet.removeAllChanges(List.of(change));
        changeSet.addChange(replacement);
      }
    }
  }

  private void peekChangeSet(ChangeSet changeSet) {
    changeSet.getChanges()
        .stream()
        .forEach(change -> {
          if (change instanceof CreateTableChange createTableChange) {
            createTableChange.getColumns().stream().forEach(cc -> {
              var columnConstraint = cc.getConstraints();
              if (columnConstraint == null) {
                return;
              }
              if (columnConstraint.isUnique() == null || !columnConstraint.isUnique()) {
                return;
              }
              if (columnConstraint.getUniqueConstraintName() != null) {
                return;
              }
              if (columnConstraint.isPrimaryKey() != null && columnConstraint.isPrimaryKey()) {
                return;
              }
              var table = diffResult.getMissingObjects(Table.class).stream()
                  .filter(t -> t.getName().equals(createTableChange.getTableName()))
                  .findFirst()
                  .orElse(null);
              if (table == null || table.getUniqueConstraints() == null || table.getUniqueConstraints().isEmpty()) {
                return;
              }
              table.getUniqueConstraints().stream()
                  .filter(uc -> uc.getRelation().getName().equalsIgnoreCase(createTableChange.getTableName()))
                  .filter(uc -> uc.getColumns().size() == 1)
                  .filter(uc -> uc.getColumns().get(0).getName().equalsIgnoreCase(cc.getName()))
                  .map(UniqueConstraint::getName)
                  .findFirst()
                  .ifPresent(columnConstraint::setUniqueConstraintName);
            });
          }
        });
  }
}
