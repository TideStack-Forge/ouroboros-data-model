package com.ouroboros.data.sql;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.HashMap;

import org.junit.jupiter.api.Test;

import com.querydsl.sql.PostgreSQLTemplates;

class DefaultSqlTemplatesRegisterTest {

  @Test
  void shouldRegisterPostgresqlMetadataAlias() {
    var registry = new HashMap<String, SQLTemplatesSupplier>();

    new DefaultSqlTemplatesRegister().register(registry);

    assertInstanceOf(PostgreSQLTemplates.class, registry.get("POSTGRESQL").get());
  }

  @Test
  void shouldRegisterAllKingbaseAliases() {
    var registry = new HashMap<String, SQLTemplatesSupplier>();

    new KingbaseSqlTemplatesRegister().register(registry);

    assertInstanceOf(PostgreSQLTemplates.class, registry.get("KINGBASE").get());
    assertInstanceOf(PostgreSQLTemplates.class, registry.get("KINGBASEES").get());
    assertInstanceOf(PostgreSQLTemplates.class, registry.get("KINGBASE ES").get());
  }
}
