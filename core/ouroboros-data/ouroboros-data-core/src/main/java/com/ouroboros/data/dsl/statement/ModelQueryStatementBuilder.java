package com.ouroboros.data.dsl.statement;

public class ModelQueryStatementBuilder extends QueryStatement.QueryStatementBuilder {
  private PopulateClause populateClause;
  private OmitClause omitClause;

  public ModelQueryStatementBuilder() {
    super();
  }

  public ModelQueryStatementBuilder(QueryStatement statement) {
    super(statement);
    if (statement instanceof ModelQueryStatement mqs) {
      this.populateClause = mqs.getPopulateClause();
      this.omitClause = mqs.getOmitClause();
    }
  }

  public ModelQueryStatementBuilder populateClause(PopulateClause clause) {
    this.populateClause = clause;
    return this;
  }

  public ModelQueryStatementBuilder omitClause(OmitClause clause) {
    this.omitClause = clause;
    return this;
  }

  @Override
  public ModelQueryStatement build() {
    return new ModelQueryStatement(metaMap, populateClause, omitClause);
  }
}
