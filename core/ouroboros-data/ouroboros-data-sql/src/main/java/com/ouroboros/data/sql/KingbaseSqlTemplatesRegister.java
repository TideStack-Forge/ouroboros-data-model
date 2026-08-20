package com.ouroboros.data.sql;

import java.util.Map;

import com.querydsl.sql.PostgreSQLTemplates;

public final class KingbaseSqlTemplatesRegister implements SqlTemplatesRegister {

  @Override
  public void register(Map<String, SQLTemplatesSupplier> registry) {
    registry.put("KINGBASE", PostgreSQLTemplates.builder().quote()::build);
    registry.put("KINGBASEES", PostgreSQLTemplates.builder().quote()::build);
    registry.put("KINGBASE ES", PostgreSQLTemplates.builder().quote()::build);
  }
}
