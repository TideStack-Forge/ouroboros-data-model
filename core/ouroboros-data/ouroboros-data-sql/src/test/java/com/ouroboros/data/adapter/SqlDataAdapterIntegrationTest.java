package com.ouroboros.data.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.RowId;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ouroboros.data.exception.DatabaseException;
import com.ouroboros.data.record.RecordList;
import com.ouroboros.data.transpile.DefaultOuroborosQueryMetadata;
import com.querydsl.core.JoinType;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.sql.RelationalPathBase;

class SqlDataAdapterIntegrationTest {

  private JdbcDataSource dataSource;
  private SqlDataAdapter adapter;

  @BeforeEach
  void setUp() {
    dataSource = new JdbcDataSource();
    dataSource.setURL("jdbc:h2:mem:sql_data_adapter_test;MODE=MySQL;DB_CLOSE_DELAY=-1");
    dataSource.setUser("sa");
    dataSource.setPassword("");
    adapter = new SqlDataAdapter(dataSource, "H2");

    adapter.execute("DROP TABLE IF EXISTS users");
    adapter.execute("CREATE TABLE \"USERS\" (\"ID\" BIGINT AUTO_INCREMENT PRIMARY KEY, \"NAME\" VARCHAR(255), \"AGE\" INT, \"BALANCE\" DECIMAL(18,2), \"ACTIVE\" BOOLEAN)");
  }

  @Test
  void shouldExecuteInsertQueryUpdateDeleteAndCount() {
    var inserted = adapter.execute(
        "INSERT INTO users(name, age, balance, active) VALUES(?, ?, ?, ?)",
        "Alice", 30, new BigDecimal("12.50"), true)
        .get();
    assertEquals(1, inserted);

    var queried = adapter.query("SELECT id, name, age, balance, active FROM users WHERE name = ?", "Alice")
        .get();
    assertEquals(1, queried.size());
    assertEquals("Alice", queried.get(0).get("NAME"));

    var updated = adapter.execute("UPDATE users SET age = ? WHERE name = ?", 31, "Alice")
        .get();
    assertEquals(1, updated);

    var countMetadata = new DefaultOuroborosQueryMetadata();
    countMetadata.addJoin(JoinType.DEFAULT, new RelationalPathBase<>(Object.class, "USERS", "", "USERS"));
    countMetadata.setProjection(Expressions.numberPath(Integer.class, "id"));
    var count = adapter.count(countMetadata).get();
    assertEquals(1L, count);

    var deleted = adapter.execute("DELETE FROM users WHERE name = ?", "Alice")
        .get();
    assertEquals(1, deleted);
  }

  @Test
  void shouldSupportKeyReturningInsertAndBatchInsert() {
    var id = adapter.insert(Long.class,
            "INSERT INTO users(name, age, balance, active) VALUES(?, ?, ?, ?)",
            "Bob", 22, new BigDecimal("88.00"), false)
        .get();
    assertNotNull(id);

    var namedSql = "INSERT INTO \"users\"(\"name\", \"age\", \"balance\", \"active\") VALUES(:{name}, :{age}, :{balance}, :{active})";
    var namedInsertParams = new HashMap<String, Object>();
    namedInsertParams.put("name", "Carol");
    namedInsertParams.put("age", 27);
    namedInsertParams.put("balance", new BigDecimal("91.30"));
    namedInsertParams.put("active", true);
    var namedInsertResult = adapter.insert(Long.class, namedSql, namedInsertParams);
    assertTrue(namedInsertResult.isFailure());

    var batchIds = adapter.batchInsert(Long.class,
            "INSERT INTO users(name, age, balance, active) VALUES(?, ?, ?, ?)",
            Arrays.asList(
                Arrays.asList("Dave", 18, new BigDecimal("1.00"), true),
                Arrays.asList("Eve", 19, new BigDecimal("2.00"), false)))
        .get();
    assertEquals(2, batchIds.size());

    var namedBatchSql = "INSERT INTO \"USERS\"(\"NAME\", \"AGE\", \"BALANCE\", \"ACTIVE\") VALUES(?, ?, ?, ?)";
    var batchMap1 = new HashMap<String, Object>();
    batchMap1.put("P1", "Foo");
    batchMap1.put("P2", 40);
    batchMap1.put("P3", new BigDecimal("10.00"));
    batchMap1.put("P4", true);
    var batchMap2 = new HashMap<String, Object>();
    batchMap2.put("P1", "Bar");
    batchMap2.put("P2", 41);
    batchMap2.put("P3", new BigDecimal("11.00"));
    batchMap2.put("P4", false);
    var namedBatchIds = adapter.batchInsert(Long.class, namedBatchSql,
        batchMap1,
        batchMap2)
        .get();
    assertEquals(2, namedBatchIds.size());
  }

  @Test
  void shouldNormalizeGeneratedKeysAcrossInsertEntryPoints() throws Exception {
    var stringId = adapter.insert(String.class,
            "INSERT INTO users(name, age, balance, active) VALUES(?, ?, ?, ?)",
            "Grace", 26, new BigDecimal("66.00"), true)
        .get();
    assertNotNull(stringId);
    assertFalse(stringId.isEmpty());

    var orderedBatchIds = adapter.batchInsert(String.class,
            "INSERT INTO users(name, age, balance, active) VALUES(?, ?, ?, ?)",
            Arrays.asList(
                Arrays.asList("Heidi", 31, new BigDecimal("12.00"), true),
                Arrays.asList("Ivan", 32, new BigDecimal("13.00"), false)))
        .get();
    assertEquals(2, orderedBatchIds.size());
    assertTrue(orderedBatchIds.stream().allMatch(id -> id != null && !id.isEmpty()));

    var namedBatchSql = "INSERT INTO \"USERS\"(\"NAME\", \"AGE\", \"BALANCE\", \"ACTIVE\") VALUES(?, ?, ?, ?)";
    var batchMap1 = new HashMap<String, Object>();
    batchMap1.put("P1", "Judy");
    batchMap1.put("P2", 28);
    batchMap1.put("P3", new BigDecimal("14.00"));
    batchMap1.put("P4", true);
    var batchMap2 = new HashMap<String, Object>();
    batchMap2.put("P1", "Karl");
    batchMap2.put("P2", 29);
    batchMap2.put("P3", new BigDecimal("15.00"));
    batchMap2.put("P4", false);
    var namedBatchIds = adapter.batchInsert(String.class, namedBatchSql,
        batchMap1,
        batchMap2)
        .get();
    assertEquals(2, namedBatchIds.size());
    assertTrue(namedBatchIds.stream().allMatch(id -> id != null && !id.isEmpty()));
  }

  @Test
  void shouldTreatZeroGeneratedKeysAsMissingDuringNormalization() throws Exception {
    assertNull(invokeNormalizeGeneratedKey(0L, Long.class));
    assertNull(invokeNormalizeGeneratedKey(BigDecimal.ZERO, BigDecimal.class));
    assertNull(invokeNormalizeGeneratedKey(0, String.class));
    assertNull(invokeNormalizeGeneratedKey(serialRowId(), Object.class));
    assertEquals("7", invokeNormalizeGeneratedKey(7L, String.class));
    assertEquals(Long.valueOf(8L), invokeNormalizeGeneratedKey(BigDecimal.valueOf(8), Long.class));
    assertEquals(new BigInteger("9"), invokeNormalizeGeneratedKey("9", BigInteger.class));

    UUID uuid = UUID.randomUUID();
    assertEquals(uuid, invokeNormalizeGeneratedKey(uuid.toString(), UUID.class));
    assertEquals(uuid.toString(), invokeNormalizeGeneratedKey(uuid, String.class));
  }

  @Test
  void shouldMapParametersToJdbcTypesAndCallProcedurePath() {
    var affected = adapter.execute("UPDATE users SET name = ? WHERE id = ?", "Nobody", null)
        .get();
    assertEquals(0, affected);

    var out = adapter.call("CALL missing_proc()", Collections.emptyList());
    assertTrue(out.isFailure());
  }

  @Test
  void shouldRejectFullJoinWhenMysqlDialect() {
    var mysqlAdapter = new SqlDataAdapter(dataSource, "MYSQL");
    var metadata = new DefaultOuroborosQueryMetadata();
    metadata.addJoin(JoinType.DEFAULT, new RelationalPathBase<>(Object.class, "USERS", "", "USERS"));
    metadata.addJoin(JoinType.FULLJOIN, new RelationalPathBase<>(Object.class, "DEPARTMENTS", "", "DEPARTMENTS"));
    metadata.setProjection(Expressions.numberPath(Integer.class, "id"));

    var result = mysqlAdapter.query(metadata);
    assertTrue(result.isFailure());
    assertTrue(result.getCause() instanceof DatabaseException);
  }

  @Test
  void shouldHandleBadSqlAsFailure() {
    var result = adapter.execute("THIS IS BAD SQL");
    assertTrue(result.isFailure());
    assertFalse(result.getCause().getMessage().isEmpty());
  }

  @SuppressWarnings("unchecked")
  private <PK_TYPE> PK_TYPE invokeNormalizeGeneratedKey(Object key, Class<PK_TYPE> keyClass) throws Exception {
    Method method = SqlDataAdapter.class.getDeclaredMethod("normalizeGeneratedKey", Object.class, Class.class);
    method.setAccessible(true);
    return (PK_TYPE) method.invoke(adapter, key, keyClass);
  }

  private static RowId serialRowId() {
    return new RowId() {
      @Override
      public byte[] getBytes() {
        return new byte[]{1, 2, 3};
      }
    };
  }
}
