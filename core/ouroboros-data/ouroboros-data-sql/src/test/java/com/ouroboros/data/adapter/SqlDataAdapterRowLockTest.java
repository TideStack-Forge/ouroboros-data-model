package com.ouroboros.data.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.util.UUID;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.support.TransactionTemplate;

import com.querydsl.core.DefaultQueryMetadata;
import com.querydsl.core.JoinType;
import com.querydsl.core.QueryMetadata;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.sql.RelationalPathBase;

class SqlDataAdapterRowLockTest {
  private JdbcDataSource dataSource;
  private SqlDataAdapter adapter;
  private JdbcTemplate jdbc;

  @BeforeEach
  void setUp() {
    dataSource = dataSource("row_lock_" + UUID.randomUUID());
    adapter = new SqlDataAdapter(dataSource, "H2");
    jdbc = new JdbcTemplate(dataSource);
    jdbc.execute("CREATE TABLE \"orders\" (\"id\" VARCHAR(32) PRIMARY KEY, \"status\" VARCHAR(32))");
    jdbc.update("INSERT INTO \"orders\" VALUES (?, ?)", "A1", "pending");
    jdbc.update("INSERT INTO \"orders\" VALUES (?, ?)", "A2", "done");
  }

  @Test
  void textFactsRemainReadableAfterTheQueryConnectionIsReleased() {
    jdbc.execute("CREATE TABLE \"facts\" (\"payload\" CLOB)");
    String payload = "{\"orderId\":\"A1\"}";
    jdbc.update("INSERT INTO \"facts\" VALUES (?)", payload);
    var rows = adapter.query("SELECT \"payload\" FROM \"facts\"").get();
    assertEquals(payload, rows.get(0).get("payload"));
  }

  @Test
  void refusesLockingWithoutAnActualTransaction() {
    var result = adapter.queryForUpdate(pendingOrders());

    assertTrue(result.isFailure());
    assertEquals("事务锁定查询必须在实际事务中执行", result.getCause().getMessage());
  }

  @Test
  void refusesAnUnenlistedDataSourceDuringAnotherLocalTransaction() {
    var other = dataSource("unrelated_" + UUID.randomUUID());
    var transaction = new TransactionTemplate(new DataSourceTransactionManager(other));

    transaction.execute(status -> {
      var result = adapter.queryForUpdate(pendingOrders());
      assertTrue(result.isFailure());
      assertEquals("事务管理器未将当前数据源连接绑定到事务", result.getCause().getMessage());
      return null;
    });
  }

  @Test
  void participationProofIdentifiesTheActualCommitConnection() {
    assertTrue(adapter.requireTransactionResource().isFailure());
    var unrelated = new TransactionTemplate(new DataSourceTransactionManager(dataSource("proof_unrelated_" + UUID.randomUUID())));
    unrelated.executeWithoutResult(status -> assertTrue(adapter.requireTransactionResource().isFailure()));

    var transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    transaction.executeWithoutResult(status -> {
      Object resource = adapter.requireTransactionResource().get();
      var connection = DataSourceUtils.getConnection(dataSource);
      try {
        assertSame(connection, resource);
        assertSame(resource, adapter.requireTransactionResource().get());
        adapter.execute("UPDATE \"orders\" SET \"status\" = ? WHERE \"id\" = ?", "changed", "A1").get();
        status.setRollbackOnly();
      } finally {
        DataSourceUtils.releaseConnection(connection, dataSource);
      }
    });
    assertEquals("pending", jdbc.queryForObject("SELECT \"status\" FROM \"orders\" WHERE \"id\" = 'A1'", String.class));
    assertTrue(adapter.requireTransactionResource().isFailure());
  }

  @Test
  void holdsMatchingRowsUntilRollbackAndUsesTheBoundConnectionForWrites() {
    var transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));

    transaction.execute(status -> {
      var locked = adapter.queryForUpdate(pendingOrders()).get();
      assertEquals(1, locked.size());
      assertEquals("A1", locked.get(0).get("id"));

      assertEquals(1L, adapter.execute("UPDATE \"orders\" SET \"status\" = ? WHERE \"id\" = ?",
          "changed", "A1").map(Number.class::cast).map(Number::longValue).get());
      try (var other = dataSource.getConnection(); var statement = other.createStatement()) {
        statement.execute("SET LOCK_TIMEOUT 100");
        assertEquals(1, statement.executeUpdate("UPDATE \"orders\" SET \"status\" = 'changed' WHERE \"id\" = 'A2'"));
        assertThrows(SQLException.class,
            () -> statement.executeUpdate("UPDATE \"orders\" SET \"status\" = 'blocked' WHERE \"id\" = 'A1'"));
      } catch (SQLException error) {
        throw new IllegalStateException(error);
      }
      status.setRollbackOnly();
      return null;
    });

    assertEquals("pending", jdbc.queryForObject("SELECT \"status\" FROM \"orders\" WHERE \"id\" = 'A1'", String.class));
    assertEquals(1, jdbc.update("UPDATE \"orders\" SET \"status\" = 'released' WHERE \"id\" = 'A1'"));
  }

  private QueryMetadata pendingOrders() {
    var table = new RelationalPathBase<>(Object.class, "orders", "", "orders");
    var metadata = new DefaultQueryMetadata();
    metadata.addJoin(JoinType.DEFAULT, table);
    metadata.setProjection(Expressions.stringPath(table, "id"));
    metadata.addWhere(Expressions.stringPath(table, "status").eq("pending"));
    return metadata;
  }

  private JdbcDataSource dataSource(String name) {
    var result = new JdbcDataSource();
    result.setURL("jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1");
    result.setUser("sa");
    result.setPassword("");
    return result;
  }
}
