package com.ouroboros.data.migration.kingbase;

import java.util.List;

import liquibase.database.DatabaseConnection;
import liquibase.database.core.PostgresDatabase;
import liquibase.exception.DatabaseException;
import liquibase.statement.core.RawParameterizedSqlStatement;
import liquibase.util.StringUtil;

public final class KingbaseDatabase extends PostgresDatabase {
  public static final String SHORT_NAME = "kingbase";
  public static final String DRIVER_CLASS_NAME = "com.kingbase8.Driver";
  public static final String JDBC_URL_PREFIX = "jdbc:kingbase8:";
  private static final String PRODUCT_NAME = "KingbaseES";
  private static final int DEFAULT_PORT = 54321;

  @Override
  public String getShortName() {
    return SHORT_NAME;
  }

  @Override
  public String getDisplayName() {
    return PRODUCT_NAME;
  }

  @Override
  protected String getDefaultDatabaseProductName() {
    return PRODUCT_NAME;
  }

  @Override
  public Integer getDefaultPort() {
    return DEFAULT_PORT;
  }

  @Override
  public int getPriority() {
    return PRIORITY_DATABASE + 1;
  }

  @Override
  public boolean isCorrectDatabaseImplementation(DatabaseConnection connection) throws DatabaseException {
    return PRODUCT_NAME.equalsIgnoreCase(connection.getDatabaseProductName());
  }

  @Override
  public String getDefaultDriver(String url) {
    if (url != null && url.startsWith(JDBC_URL_PREFIX)) {
      return DRIVER_CLASS_NAME;
    }
    return super.getDefaultDriver(url);
  }

  /**
   * Liquibase 4.31.1 keeps its PostgreSQL dependency query private. Preserve its object mapping here,
   * with integer substring bounds: Kingbase MySQL mode can promote these expressions to bigint,
   * selecting substring(text, text, text) and treating the length as an escape character.
   * Quoted aliases also preserve the keys expected by the dependency reader on case-sensitive connections.
   */
  public RawParameterizedSqlStatement getObjectDependenciesStatement(List<String> schemas) {
    // Preserve Liquibase's PostgreSQL catalog mapping, excluding PK/FK added later in the changelog.
    String schemaPredicate = StringUtil.join(schemas, " OR ",
        schema -> " REFERENCED_NAME LIKE ? OR REFERENCED_NAME NOT LIKE '%.%'");
    String sql = """
        WITH RECURSIVE preference AS (
            SELECT 10 AS max_depth  -- The deeper the recursion goes, the slower it performs.
                 , 16384 AS min_oid -- user objects only
                 , '^(londiste|pgq|pg_toast)'::text AS schema_exclusion
                 , '^pg_(conversion|language|ts_(dict|template))'::text AS class_exclusion
                 , '{"SCHEMA":"00", "TABLE":"01", "CONSTRAINT":"02", "DEFAULT":"03",
              "INDEX":"05", "SEQUENCE":"06", "TRIGGER":"07", "FUNCTION":"08",
              "VIEW":"10", "MVIEW":"11", "FOREIGN":"12"}'::json AS type_ranks),
                       dependency_pair AS (
                           WITH relation_object AS ( SELECT oid, oid::regclass::text AS object_name  FROM pg_class )
                           SELECT DISTINCT
                                 substring(pg_identify_object(classid, objid, 0)::text, E'(\\\\w+?)\\\\.') as referenced_schema_name,
                                 CASE classid
                                      WHEN 'pg_constraint'::regclass THEN (SELECT CONTYPE::text FROM pg_constraint WHERE oid = objid)
                                      ELSE objid::text
                                      END AS CONTYPE,
                                 CASE classid
                                      WHEN 'pg_attrdef'::regclass THEN (SELECT attname FROM pg_attrdef d JOIN pg_attribute c ON (c.attrelid,c.attnum)=(d.adrelid,d.adnum) WHERE d.oid = objid)
                                      WHEN 'pg_cast'::regclass THEN (SELECT concat(castsource::regtype::text, ' AS ', casttarget::regtype::text,' WITH ', castfunc::regprocedure::text) FROM pg_cast WHERE oid = objid)
                                      WHEN 'pg_class'::regclass THEN rel.object_name
                                      WHEN 'pg_constraint'::regclass THEN (SELECT conname FROM pg_constraint WHERE oid = objid)
                                      WHEN 'pg_extension'::regclass THEN (SELECT extname FROM pg_extension WHERE oid = objid)
                                      WHEN 'pg_namespace'::regclass THEN (SELECT nspname FROM pg_namespace WHERE oid = objid)
                                      WHEN 'pg_opclass'::regclass THEN (SELECT opcname FROM pg_opclass WHERE oid = objid)
                                      WHEN 'pg_operator'::regclass THEN (SELECT oprname FROM pg_operator WHERE oid = objid)
                                      WHEN 'pg_opfamily'::regclass THEN (SELECT opfname FROM pg_opfamily WHERE oid = objid)
                                      WHEN 'pg_proc'::regclass THEN objid::regprocedure::text
                                      WHEN 'pg_rewrite'::regclass THEN (SELECT ev_class::regclass::text FROM pg_rewrite WHERE oid = objid)
                                      WHEN 'pg_trigger'::regclass THEN (SELECT tgname FROM pg_trigger WHERE oid = objid)
                                      WHEN 'pg_type'::regclass THEN objid::regtype::text
                                      ELSE objid::text
                                      END AS REFERENCED_NAME,
                                  substring(pg_identify_object(refclassid, refobjid, 0)::text, E'(\\\\w+?)\\\\.') as referencing_schema_name,
                                 CASE refclassid
                                      WHEN 'pg_namespace'::regclass THEN (SELECT nspname FROM pg_namespace WHERE oid = refobjid)
                                      WHEN 'pg_class'::regclass THEN rrel.object_name
                                      WHEN 'pg_opfamily'::regclass THEN (SELECT opfname FROM pg_opfamily WHERE oid = refobjid)
                                      WHEN 'pg_proc'::regclass THEN refobjid::regprocedure::text
                                      WHEN 'pg_type'::regclass THEN refobjid::regtype::text
                                      ELSE refobjid::text
                                      END AS REFERENCING_NAME
                           FROM pg_depend dep
                                    LEFT JOIN relation_object rel ON rel.oid = dep.objid
                                    LEFT JOIN relation_object rrel ON rrel.oid = dep.refobjid, preference
                           WHERE deptype = ANY('{n,a}')
                             AND objid >= preference.min_oid
                             AND (refobjid >= preference.min_oid OR refobjid = 2200) -- need public schema as root node
                             AND classid::regclass::text !~ preference.class_exclusion
                             AND refclassid::regclass::text !~ preference.class_exclusion
                             AND COALESCE(SUBSTRING(objid::regclass::text, E'^(\\\\\\\\w+)\\\\\\\\.'),'') !~ preference.schema_exclusion
                             AND COALESCE(SUBSTRING(refobjid::regclass::text, E'^(\\\\\\\\w+)\\\\\\\\.'),'') !~ preference.schema_exclusion
                           GROUP BY classid, objid, refclassid, refobjid, deptype, rel.object_name, rrel.object_name
                       )
         select referenced_schema_name AS "REFERENCED_SCHEMA_NAME",
            (CASE
              WHEN position('.' in referenced_name) >0 THEN substring(referenced_name from CAST(position('.' in referenced_name)+1 AS integer) for CAST(length(referenced_name) AS integer))
              ELSE referenced_name
            END) AS "REFERENCED_NAME",
           referencing_schema_name AS "REFERENCING_SCHEMA_NAME",
           (CASE
              WHEN position('.' in referencing_name) >0 THEN substring(referencing_name from CAST(position('.' in referencing_name)+1 AS integer) for CAST(length(referencing_name) AS integer))
              ELSE referencing_name
            END) AS "REFERENCING_NAME" from dependency_pair where REFERENCED_NAME != REFERENCING_NAME  AND (%s)
         AND (CONTYPE::text != 'p' AND CONTYPE::text != 'f')
         AND referencing_schema_name is not null and referencing_name is not null
        """.formatted(schemaPredicate);
    return new RawParameterizedSqlStatement(sql, schemas.stream().map(schema -> schema + ".%").toArray());
  }
}
