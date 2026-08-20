package com.ouroboros.data.dsl.statement;

import java.util.Map;
import java.util.Objects;

public class ModelQueryStatement extends QueryStatement {
  private static final long serialVersionUID = 1L;

  private final PopulateClause populateClause;
  private final OmitClause omitClause;

  ModelQueryStatement(Map<String, Object> metaMap,
                      PopulateClause populateClause,
                      OmitClause omitClause) {
    super(metaMap);
    this.populateClause = populateClause;
    this.omitClause = omitClause;
  }

  public PopulateClause getPopulateClause() {
    return populateClause;
  }

  public OmitClause getOmitClause() {
    return omitClause;
  }

  @Override
  public QueryStatementBuilder getBuilder() {
    return new ModelQueryStatementBuilder(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof ModelQueryStatement other)) return false;
    if (!super.equals(obj)) return false;
    return Objects.equals(populateClause, other.populateClause)
        && Objects.equals(omitClause, other.omitClause);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), populateClause, omitClause);
  }
}
