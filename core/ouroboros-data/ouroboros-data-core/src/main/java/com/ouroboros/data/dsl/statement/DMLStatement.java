package com.ouroboros.data.dsl.statement;

import java.util.Map;

public class DMLStatement extends Statement {
  protected DMLStatement(Map<String, ?> statement) {
    super(statement);
  }
}
