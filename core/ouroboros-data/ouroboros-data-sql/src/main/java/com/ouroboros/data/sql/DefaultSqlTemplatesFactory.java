package com.ouroboros.data.sql;

import java.sql.DatabaseMetaData;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import jakarta.annotation.Priority;
import javax.sql.DataSource;

import org.springframework.jdbc.support.JdbcUtils;

import com.ouroboros.data.util.DataServices;
import com.querydsl.sql.SQLTemplates;

@Priority(0)
public class DefaultSqlTemplatesFactory implements SqlTemplatesFactory {

  Map<String, SQLTemplates> sqlTemplatesMap = new HashMap<>();
  Map<String, SQLTemplatesSupplier> sqlTemplatesSupplierMap = new HashMap<>();

  public DefaultSqlTemplatesFactory() {
    DataServices.getSortedServiceStream(SqlTemplatesRegister.class)
        .forEach(r -> r.register(sqlTemplatesSupplierMap));
  }

  @Override
  public SQLTemplates apply(DataSource dataSource) {
    try (var conn = dataSource.getConnection()) {
      DatabaseMetaData metaData = conn.getMetaData();
      return getSQLTemplates(metaData);
    } catch (Exception e) {
      throw new SqlTemplatesException("Failed to detect SQL templates from DataSource", e);
    }
  }

  protected SQLTemplates getSQLTemplates(DatabaseMetaData databaseMetaData) {
    try {
      var dbType = JdbcUtils.commonDatabaseName(databaseMetaData.getDatabaseProductName());
      if ("MICROSOFT SQL SERVER".equalsIgnoreCase(dbType.trim())) {
        dbType = "MICROSOFT SQL SERVER " + Math.min(databaseMetaData.getDatabaseMajorVersion(), 11);
      }
      return getSQLTemplates(dbType);
    } catch (Exception e) {
      throw new SqlTemplatesException("Failed to detect SQL templates from DatabaseMetaData", e);
    }
  }

  protected SQLTemplates getSQLTemplates(String typeName) {
    var normalizedTypeName = typeName.trim().toUpperCase(Locale.ROOT);
    var templates = sqlTemplatesMap.get(normalizedTypeName);
    if (templates != null) {
      return templates;
    }

    var supplier = sqlTemplatesSupplierMap.get(normalizedTypeName);
    if (supplier == null) {
      throw new SqlTemplatesException("Unsupported database type: " + normalizedTypeName
          + ". Available dialects: " + sqlTemplatesSupplierMap.keySet()
          + ". Configure explicitly via 'ouroboros.data.sql.dialect' property.");
    }
    templates = supplier.get();
    sqlTemplatesMap.put(normalizedTypeName, templates);
    return templates;
  }

  /**
   * Apply with explicit dialect override.
   * If explicitDialect is null/blank/"auto", falls back to auto-detection from DataSource.
   */
  public SQLTemplates apply(DataSource dataSource, String explicitDialect) {
    if (explicitDialect != null && !explicitDialect.trim().isEmpty() && !"auto".equalsIgnoreCase(explicitDialect)) {
      return getSQLTemplates(explicitDialect);
    }
    return apply(dataSource);
  }
}
