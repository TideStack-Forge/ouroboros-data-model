package com.ouroboros.data.migration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import liquibase.ContextExpression;
import liquibase.Labels;
import liquibase.change.AddColumnConfig;
import liquibase.change.core.AddColumnChange;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.precondition.core.PreconditionContainer;
import liquibase.sql.visitor.AppendSqlVisitor;

class ChangeSetWrapperDeepTest {

  @Test
  void shouldDelegateMostMethodsWithoutError() {
    var log = new DatabaseChangeLog("in-memory.xml");
    log.setIncludeIgnore(true);
    var inner = new ChangeSet(log);
    inner.setFilePath("a.xml");
    inner.setLogicalFilePath("b.xml");
    inner.setStoredFilePath("c.xml");
    inner.setComments("comment");
    inner.setContextFilter(new ContextExpression("ctx"));
    inner.setLabels(new Labels("l1"));
    inner.setIgnore(false);
    inner.setFailOnError(true);
    inner.setValidationFailed(false);
    inner.setCreated("2026-01-01");
    inner.setRunOrder("first");
    inner.setOnValidationFail(ChangeSet.ValidationFailOption.HALT);
    inner.addValidCheckSum("1:abcd");
    inner.setAttribute("x", "y");

    var add = new AddColumnChange();
    add.setTableName("T");
    var col = new AddColumnConfig();
    col.setName("C");
    col.setType("VARCHAR(16)");
    add.addColumn(col);
    inner.addChange(add);
    inner.addRollbackChange(add);
    inner.addRollBackSQL("DELETE FROM T");
    inner.setPreconditions(new PreconditionContainer());
    inner.addSqlVisitor(new AppendSqlVisitor());

    var wrapper = new ChangeSetWrapper(log, inner);

    assertNotNull(wrapper.getFilePath());
    assertNotNull(wrapper.getLogicalFilePath());
    assertNotNull(wrapper.getStoredFilePath());
    assertNotNull(wrapper.getComments());
    assertNotNull(wrapper.getContextFilter());
    assertNotNull(wrapper.getLabels());
    wrapper.getDbmsSet();
    assertNotNull(wrapper.getChanges());
    assertNotNull(wrapper.getRollback());
    assertNotNull(wrapper.getPreconditions());
    assertNotNull(wrapper.getSqlVisitors());
    assertNotNull(wrapper.getChangeLog());
    assertNotNull(wrapper.getDescription());
    assertNotNull(wrapper.getValidCheckSums());
    assertNotNull(wrapper.toString());
    assertNotNull(wrapper.toString(true));
    try {
      wrapper.serialize();
    } catch (RuntimeException expected) {
    }
    assertNotNull(wrapper.getSerializableFields());
    assertNotNull(wrapper.getSerializableFieldType("id"));
    assertNotNull(wrapper.getSerializableFieldNamespace("id"));

    assertTrue(wrapper.isInheritableIgnore());
    assertEquals("y", wrapper.getAttribute("x"));

    assertDoesNotThrow(() -> wrapper.clearCheckSum());
    assertDoesNotThrow(() -> wrapper.setIgnore(true));
    assertDoesNotThrow(() -> wrapper.setComments("c2"));
    assertDoesNotThrow(() -> wrapper.setFilePath("d.xml"));
    assertDoesNotThrow(() -> wrapper.setLogicalFilePath("e.xml"));
    assertDoesNotThrow(() -> wrapper.setStoredFilePath("f.xml"));
    assertDoesNotThrow(() -> wrapper.setLabels(new Labels("l2")));
    assertDoesNotThrow(() -> wrapper.setContextFilter(new ContextExpression("ctx2")));
    assertDoesNotThrow(() -> wrapper.setValidationFailed(false));
    assertDoesNotThrow(() -> wrapper.setFailOnError(false));
    assertDoesNotThrow(() -> wrapper.setCreated("2026-02-02"));
    assertDoesNotThrow(() -> wrapper.setRunOrder("last"));
    assertDoesNotThrow(() -> wrapper.setPreconditions(new PreconditionContainer()));
    assertDoesNotThrow(() -> wrapper.setStoredCheckSum(null));

  }
}
