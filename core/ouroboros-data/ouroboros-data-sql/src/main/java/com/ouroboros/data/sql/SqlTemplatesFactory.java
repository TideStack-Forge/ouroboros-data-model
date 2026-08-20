package com.ouroboros.data.sql;

import java.util.function.Function;

import javax.sql.DataSource;

import com.ouroboros.data.util.DataServices;
import com.querydsl.sql.SQLTemplates;

public interface SqlTemplatesFactory extends Function<DataSource, SQLTemplates> {
  SqlTemplatesFactory SQL_TEMPLATES_FACTORY_CHAIN = DataServices.getSortedServiceStream(SqlTemplatesFactory.class)
      .reduce((dataSource -> SQLTemplates.DEFAULT), (prev, curr) -> dataSource -> {
        var templates = curr.apply(dataSource);
        if (templates != null) {
          return templates;
        }
        return prev.apply(dataSource);
      });

  static SQLTemplates getSQLTemplates(DataSource dataSource) {
    return SQL_TEMPLATES_FACTORY_CHAIN.apply(dataSource);
  }

  static SQLTemplates getSQLTemplates(DataSource dataSource, String dialect) {
    if (dialect == null || dialect.trim().isEmpty() || "auto".equalsIgnoreCase(dialect)) {
      return SQL_TEMPLATES_FACTORY_CHAIN.apply(dataSource);
    }
    return DataServices.getSortedServiceStream(SqlTemplatesFactory.class)
        .filter(f -> f instanceof DefaultSqlTemplatesFactory)
        .map(f -> ((DefaultSqlTemplatesFactory) f).apply(dataSource, dialect))
        .findFirst()
        .orElseThrow(() -> new SqlTemplatesException(
            "No DefaultSqlTemplatesFactory found via SPI to resolve dialect: " + dialect));
  }
}
