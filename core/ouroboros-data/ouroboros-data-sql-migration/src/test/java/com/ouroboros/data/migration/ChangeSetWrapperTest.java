package com.ouroboros.data.migration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import liquibase.ContextExpression;
import liquibase.Labels;
import liquibase.change.AddColumnConfig;
import liquibase.change.core.AddColumnChange;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.precondition.core.PreconditionContainer;
import liquibase.sql.visitor.AppendSqlVisitor;

class ChangeSetWrapperTest {

  @Test
  void shouldDelegateCorePropertiesAndCollections() {
    var changeLog = new DatabaseChangeLog("in-memory.xml");
    changeLog.setIncludeIgnore(true);

    var inner = new ChangeSet(changeLog);
    inner.setFilePath("test-file.xml");
    inner.setLogicalFilePath("logical.xml");
    inner.setStoredFilePath("stored.xml");
    inner.setComments("comments");
    inner.setLabels(new Labels("L1"));
    inner.setContextFilter(new ContextExpression("ctx"));
    inner.setIgnore(false);
    inner.setFailOnError(true);
    inner.setOnValidationFail(ChangeSet.ValidationFailOption.HALT);
    inner.setCreated("2026-01-01");
    inner.setRunOrder("first");
    inner.addValidCheckSum("1:abcd");
    inner.setAttribute("k", "v");

    var change = new AddColumnChange();
    change.setTableName("T");
    var col = new AddColumnConfig();
    col.setName("C");
    col.setType("VARCHAR(20)");
    change.addColumn(col);
    inner.addChange(change);
    inner.addRollbackChange(change);
    inner.addRollBackSQL("DELETE FROM T");
    inner.setPreconditions(new PreconditionContainer());
    inner.addSqlVisitor(new AppendSqlVisitor());

    var wrapper = new ChangeSetWrapper(changeLog, inner);

    assertEquals("test-file.xml", wrapper.getFilePath());
    assertEquals("logical.xml", wrapper.getLogicalFilePath());
    assertEquals("stored.xml", wrapper.getStoredFilePath());
    assertEquals("comments", wrapper.getComments());
    assertEquals("ctx", wrapper.getContextFilter().toString());
    assertEquals("l1", wrapper.getLabels().toString());
    assertTrue(wrapper.getValidCheckSums().size() >= 1);
    assertEquals("v", wrapper.getAttribute("k"));
    assertTrue(wrapper.getChanges().size() >= 1);
    assertNotNull(wrapper.getRollback());
    assertNotNull(wrapper.getPreconditions());
    assertTrue(wrapper.getSqlVisitors().size() >= 1);
    assertEquals(ChangeSet.ValidationFailOption.HALT, wrapper.getOnValidationFail());
    assertEquals("2026-01-01", wrapper.getCreated());
    assertEquals("first", wrapper.getRunOrder());
    assertEquals(changeLog, wrapper.getChangeLog());
    assertTrue(wrapper.isInheritableIgnore());
    assertEquals(wrapper.hashCode(), inner.hashCode());
    assertTrue(wrapper.equals(inner));
    assertNotNull(wrapper.toString());
    assertNotNull(wrapper.toString(true));
  }

  @Test
  void shouldDelegateMutatorsWithoutThrowing() {
    var changeLog = new DatabaseChangeLog("in-memory.xml");
    var inner = new ChangeSet(changeLog);
    var wrapper = new ChangeSetWrapper(changeLog, inner);

    assertDoesNotThrow(() -> wrapper.setFilePath("a.xml"));
    assertDoesNotThrow(() -> wrapper.setLogicalFilePath("b.xml"));
    assertDoesNotThrow(() -> wrapper.setStoredFilePath("c.xml"));
    assertDoesNotThrow(() -> wrapper.setComments("comment"));
    assertDoesNotThrow(() -> wrapper.setLabels(new Labels("L2")));
    assertDoesNotThrow(() -> wrapper.setContextFilter(new ContextExpression("CTX2")));
    assertDoesNotThrow(() -> wrapper.setIgnore(true));
    assertDoesNotThrow(() -> wrapper.clearCheckSum());
    assertDoesNotThrow(() -> wrapper.setFailOnError(false));
    assertDoesNotThrow(() -> wrapper.setValidationFailed(false));
    assertDoesNotThrow(() -> wrapper.setOnValidationFail(ChangeSet.ValidationFailOption.MARK_RAN));
    assertDoesNotThrow(() -> wrapper.setPreconditions(new PreconditionContainer()));
    assertDoesNotThrow(() -> wrapper.setChangeLogParameters(null));
    assertDoesNotThrow(() -> wrapper.setCreated("2026-01-02"));
    assertDoesNotThrow(() -> wrapper.setRunOrder("last"));
    assertDoesNotThrow(() -> wrapper.setStoredCheckSum(null));
    assertDoesNotThrow(() -> wrapper.addValidCheckSum("1:efgh"));

    var serializableFields = wrapper.getSerializableFields();
    assertNotNull(serializableFields);
  }

  @Test
  void shouldDelegateAdditionalSafeAccessorsAndFlags() {
    var changeLog = new DatabaseChangeLog("in-memory.xml");
    var inner = new ChangeSet(changeLog);
    inner.setContextFilter(new ContextExpression("ctx-extra"));
    inner.setContexts(new ContextExpression("ctx-legacy"));
    inner.setLabels(new Labels("L3"));
    inner.setAttribute("a1", "v1");
    inner.setStoredFilePath("stored-extra.xml");
    inner.setComments("comment-extra");
    inner.setFailOnError(Boolean.TRUE);
    inner.setRunOrder("first");
    inner.setCreated("2026-03-03");
    inner.setStoredCheckSum(null);
    inner.setFailOnError(Boolean.TRUE);

    var wrapper = new ChangeSetWrapper(changeLog, inner);

    ChangeSetWrapper.lookupExecutor("jdbc");
    wrapper.shouldAlwaysRun();
    wrapper.shouldRunOnChange();
    wrapper.isAlwaysRun();
    wrapper.isRunOnChange();
    wrapper.isRunInTransaction();
    wrapper.getRunWith();
    assertEquals("comment-extra", wrapper.getComments());
    assertEquals("stored-extra.xml", wrapper.getStoredFilePath());
    assertEquals("v1", wrapper.getAttribute("a1"));
    assertEquals(inner.getId(), wrapper.getId());
    assertEquals(inner.getAuthor(), wrapper.getAuthor());
    assertNotNull(wrapper.getContexts());
    assertNotNull(wrapper.setContexts(new ContextExpression("ctx-reset")));
    assertNotNull(wrapper.getInheritableContextFilter());
    assertNotNull(wrapper.getInheritableLabels());
    assertNotNull(wrapper.buildFullContext());
    assertNotNull(wrapper.buildFullLabels());
    wrapper.getDbmsSet();
    assertTrue(wrapper.hasCustomRollbackChanges() == inner.hasCustomRollbackChanges());
    assertNotNull(wrapper.getRollback());
    assertNotNull(wrapper.getDescription());
    assertTrue(wrapper.supportsRollback(null) == inner.supportsRollback(null));
    wrapper.addRollBackSQL("SELECT 1");
    wrapper.addRollbackChange(new AddColumnChange());
    assertNotNull(wrapper.getSerializedObjectName());
    assertNotNull(wrapper.getSerializedObjectNamespace());
    wrapper.getSerializableFieldValue("id");
    assertNotNull(wrapper.getSerializableFieldType("id"));
    assertNotNull(wrapper.getSerializableFieldNamespace("id"));
    wrapper.getObjectQuotingStrategy();
    assertTrue(wrapper.getFailOnError());
    assertEquals("first", wrapper.getRunOrder());
    assertEquals("2026-03-03", wrapper.getCreated());

    var parameters = new ChangeLogParameters();
    wrapper.setChangeLogParameters(parameters);
    assertNotNull(wrapper.getChangeLogParameters());

    wrapper.setAttribute("a2", "v2");
    assertEquals("v2", wrapper.getAttribute("a2"));
    wrapper.getStoredCheckSum();
    wrapper.isCheckSumValid(null);
  }
}
