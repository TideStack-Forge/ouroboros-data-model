package com.ouroboros.data.migration;

import java.util.List;
import java.util.stream.Collectors;

import liquibase.change.core.CreateTableChange;
import liquibase.changelog.ChangeSet;
import liquibase.diff.DiffResult;
import liquibase.diff.output.DiffOutputControl;
import liquibase.diff.output.changelog.DiffToChangeLog;
import liquibase.structure.core.Table;
import liquibase.structure.core.UniqueConstraint;

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
        .peek(this::peekChangeSet)
        .collect(Collectors.toList());
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
