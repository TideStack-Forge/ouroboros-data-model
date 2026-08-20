package com.ouroboros.data.migration;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import liquibase.ContextExpression;
import liquibase.Labels;
import liquibase.change.Change;
import liquibase.change.CheckSum;
import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.changelog.RollbackContainer;
import liquibase.changelog.visitor.ChangeExecListener;
import liquibase.database.Database;
import liquibase.database.ObjectQuotingStrategy;
import liquibase.exception.MigrationFailedException;
import liquibase.exception.RollbackFailedException;
import liquibase.parser.core.ParsedNode;
import liquibase.parser.core.ParsedNodeException;
import liquibase.precondition.core.PreconditionContainer;
import liquibase.resource.ResourceAccessor;
import liquibase.sql.visitor.SqlVisitor;

public class ChangeSetWrapper extends ChangeSet {
  ChangeSet innerChangeSet;
  DatabaseChangeLog changeLog;

  public ChangeSetWrapper(DatabaseChangeLog databaseChangeLog, ChangeSet innerChangeSet) {
    super(databaseChangeLog);
    this.changeLog = databaseChangeLog;
    this.innerChangeSet = innerChangeSet;
  }

  public static String lookupExecutor(String executorName) {
    return ChangeSet.lookupExecutor(executorName);
  }

  @Override
  public boolean shouldAlwaysRun() {
    return innerChangeSet.shouldAlwaysRun();
  }

  @Override
  public boolean shouldRunOnChange() {
    return innerChangeSet.shouldRunOnChange();
  }

  @Override
  public String getFilePath() {
    return innerChangeSet.getFilePath();
  }

  @Override
  public void setFilePath(String filePath) {
    innerChangeSet.setFilePath(filePath);
  }

  @Override
  public String getLogicalFilePath() {
    return innerChangeSet.getLogicalFilePath();
  }

  @Override
  public void setLogicalFilePath(String logicalFilePath) {
    innerChangeSet.setLogicalFilePath(logicalFilePath);
  }

  @Override
  public String getStoredFilePath() {
    return innerChangeSet.getStoredFilePath();
  }

  @Override
  public void setStoredFilePath(String storedFilePath) {
    innerChangeSet.setStoredFilePath(storedFilePath);
  }

  @Override
  public String getRunWith() {
    return innerChangeSet.getRunWith();
  }

  @Override
  public void clearCheckSum() {
    innerChangeSet.clearCheckSum();
  }

  @Override
  public void load(ParsedNode node, ResourceAccessor resourceAccessor) throws ParsedNodeException {
    innerChangeSet.load(node, resourceAccessor);
  }

  @Override
  public ParsedNode serialize() {
    return innerChangeSet.serialize();
  }

  @Override
  public ExecType execute(DatabaseChangeLog databaseChangeLog, Database database) throws MigrationFailedException {
    return innerChangeSet.execute(databaseChangeLog, database);
  }

  @Override
  public ExecType execute(DatabaseChangeLog databaseChangeLog, ChangeExecListener listener, Database database) throws MigrationFailedException {
    return innerChangeSet.execute(databaseChangeLog, listener, database);
  }

  @Override
  public void rollback(Database database) throws RollbackFailedException {
    innerChangeSet.rollback(database);
  }

  @Override
  public void rollback(Database database, ChangeExecListener listener) throws RollbackFailedException {
    innerChangeSet.rollback(database, listener);
  }

  @Override
  public boolean hasCustomRollbackChanges() {
    return innerChangeSet.hasCustomRollbackChanges();
  }

  @Override
  public List<Change> getChanges() {
    return innerChangeSet.getChanges();
  }

  @Override
  public void addChange(Change change) {
    innerChangeSet.addChange(change);
  }

  @Override
  public String getId() {
    return innerChangeSet.getId();
  }

  @Override
  public String getAuthor() {
    return innerChangeSet.getAuthor();
  }

  @Override
  @SuppressWarnings("deprecation")
  //TODO: remove when liquibase 4.0.0 is released
  public ContextExpression getContexts() {
    return innerChangeSet.getContexts();
  }

  @Override
  @SuppressWarnings("deprecation")
  //TODO: remove when liquibase 4.0.0 is released
  public ChangeSet setContexts(ContextExpression contexts) {
    return innerChangeSet.setContexts(contexts);
  }

  @Override
  public ContextExpression getContextFilter() {
    return innerChangeSet.getContextFilter();
  }

  @Override
  public ChangeSet setContextFilter(ContextExpression contextFilter) {
    return innerChangeSet.setContextFilter(contextFilter);
  }

  @Override
  public Labels getLabels() {
    return innerChangeSet.getLabels();
  }

  @Override
  public void setLabels(Labels labels) {
    innerChangeSet.setLabels(labels);
  }

  @Override
  public Set<String> getDbmsSet() {
    return innerChangeSet.getDbmsSet();
  }

  @Override
  public boolean isIgnore() {
    return innerChangeSet.isIgnore();
  }

  @Override
  public void setIgnore(boolean ignore) {
    innerChangeSet.setIgnore(ignore);
  }

  @Override
  public boolean isInheritableIgnore() {
    return changeLog.isIncludeIgnore();
  }

  @Override
  public Collection<ContextExpression> getInheritableContextFilter() {
    return innerChangeSet.getInheritableContextFilter();
  }

  @Override
  public Collection<Labels> getInheritableLabels() {
    return innerChangeSet.getInheritableLabels();
  }

  @Override
  public String buildFullContext() {
    return innerChangeSet.buildFullContext();
  }

  @Override
  public String buildFullLabels() {
    return innerChangeSet.buildFullLabels();
  }

  @Override
  public DatabaseChangeLog getChangeLog() {
    return changeLog;
  }

  @Override
  public String toString(boolean includeMD5Sum) {
    return innerChangeSet.toString(includeMD5Sum);
  }

  @Override
  public String toString() {
    return innerChangeSet.toString();
  }

  @Override
  public String getComments() {
    return innerChangeSet.getComments();
  }

  @Override
  public void setComments(String comments) {
    innerChangeSet.setComments(comments);
  }

  @Override
  public boolean isAlwaysRun() {
    return innerChangeSet.isAlwaysRun();
  }

  @Override
  public boolean isRunOnChange() {
    return innerChangeSet.isRunOnChange();
  }

  @Override
  public boolean isRunInTransaction() {
    return innerChangeSet.isRunInTransaction();
  }

  @Override
  public RollbackContainer getRollback() {
    return innerChangeSet.getRollback();
  }

  @Override
  public void addRollBackSQL(String sql) {
    innerChangeSet.addRollBackSQL(sql);
  }

  @Override
  public void addRollbackChange(Change change) {
    innerChangeSet.addRollbackChange(change);
  }

  @Override
  public boolean supportsRollback(Database database) {
    return innerChangeSet.supportsRollback(database);
  }

  @Override
  public String getDescription() {
    return innerChangeSet.getDescription();
  }

  @Override
  public Boolean getFailOnError() {
    return innerChangeSet.getFailOnError();
  }

  @Override
  public void setFailOnError(Boolean failOnError) {
    innerChangeSet.setFailOnError(failOnError);
  }

  @Override
  public ValidationFailOption getOnValidationFail() {
    return innerChangeSet.getOnValidationFail();
  }

  @Override
  public void setOnValidationFail(ValidationFailOption onValidationFail) {
    innerChangeSet.setOnValidationFail(onValidationFail);
  }

  @Override
  public void setValidationFailed(boolean validationFailed) {
    innerChangeSet.setValidationFailed(validationFailed);
  }

  @Override
  public void addValidCheckSum(String text) {
    innerChangeSet.addValidCheckSum(text);
  }

  @Override
  public Set<CheckSum> getValidCheckSums() {
    return innerChangeSet.getValidCheckSums();
  }

  @Override
  public boolean isCheckSumValid(CheckSum storedCheckSum) {
    return innerChangeSet.isCheckSumValid(storedCheckSum);
  }

  @Override
  public PreconditionContainer getPreconditions() {
    return innerChangeSet.getPreconditions();
  }

  @Override
  public void setPreconditions(PreconditionContainer preconditionContainer) {
    innerChangeSet.setPreconditions(preconditionContainer);
  }

  @Override
  public void addSqlVisitor(SqlVisitor sqlVisitor) {
    innerChangeSet.addSqlVisitor(sqlVisitor);
  }

  @Override
  public List<SqlVisitor> getSqlVisitors() {
    return innerChangeSet.getSqlVisitors();
  }

  @Override
  public ChangeLogParameters getChangeLogParameters() {
    return innerChangeSet.getChangeLogParameters();
  }

  @Override
  public void setChangeLogParameters(ChangeLogParameters changeLogParameters) {
    innerChangeSet.setChangeLogParameters(changeLogParameters);
  }

  @Override
  public ObjectQuotingStrategy getObjectQuotingStrategy() {
    return innerChangeSet.getObjectQuotingStrategy();
  }

  @Override
  public String getCreated() {
    return innerChangeSet.getCreated();
  }

  @Override
  public void setCreated(String created) {
    innerChangeSet.setCreated(created);
  }

  @Override
  public String getRunOrder() {
    return innerChangeSet.getRunOrder();
  }

  @Override
  public void setRunOrder(String runOrder) {
    innerChangeSet.setRunOrder(runOrder);
  }

  @Override
  public String getSerializedObjectName() {
    return innerChangeSet.getSerializedObjectName();
  }

  @Override
  public Set<String> getSerializableFields() {
    return innerChangeSet.getSerializableFields();
  }

  @Override
  public Object getSerializableFieldValue(String field) {
    return innerChangeSet.getSerializableFieldValue(field);
  }

  @Override
  public SerializationType getSerializableFieldType(String field) {
    return innerChangeSet.getSerializableFieldType(field);
  }

  @Override
  public String getSerializedObjectNamespace() {
    return innerChangeSet.getSerializedObjectNamespace();
  }

  @Override
  public String getSerializableFieldNamespace(String field) {
    return innerChangeSet.getSerializableFieldNamespace(field);
  }

  @Override
  public boolean equals(Object obj) {
    return innerChangeSet.equals(obj);
  }

  @Override
  public int hashCode() {
    return innerChangeSet.hashCode();
  }

  @Override
  public Object getAttribute(String attribute) {
    return innerChangeSet.getAttribute(attribute);
  }

  @Override
  public ChangeSet setAttribute(String attribute, Object value) {
    return innerChangeSet.setAttribute(attribute, value);
  }

  @Override
  public CheckSum getStoredCheckSum() {
    return innerChangeSet.getStoredCheckSum();
  }

  @Override
  public void setStoredCheckSum(CheckSum storedCheckSum) {
    innerChangeSet.setStoredCheckSum(storedCheckSum);
  }
}
