package com.ouroboros.data.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.jdbc.core.JdbcTemplate;

class SqlDataAdapterDslDispatchTest {

  private SqlDataAdapter adapter;

  @BeforeEach
  void setUp() {
    var ds = new JdbcDataSource();
    ds.setURL("jdbc:h2:mem:sql_adapter_dsl;MODE=MYSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE");
    ds.setUser("sa");
    ds.setPassword("");
    adapter = new SqlDataAdapter(ds);

    var jdbcTemplate = new JdbcTemplate(ds);
    jdbcTemplate.execute("DROP TABLE IF EXISTS users");
    jdbcTemplate.execute("CREATE TABLE users (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100), age INT, status VARCHAR(32))");
  }

  @Test
  void shouldDispatchInsertAndBatchInsertViaDslMap() {
    Map<String, Object> insert = new HashMap<String, Object>();
    insert.put("insert", "users");
    insert.put("values", mapOf("name", "Alice", "age", 30, "status", "active"));
    var id = adapter.insert(insert).get();
    assertNotNull(id);

    Map<String, Object> batch = new HashMap<String, Object>();
    batch.put("insert", "users");
    batch.put("values", Arrays.asList(
        mapOf("name", "Bob", "age", 28, "status", "active"),
        mapOf("name", "Carol", "age", 35, "status", "inactive")));
    var ids = adapter.batchInsert(batch).get();
    assertEquals(2, ids.size());
  }

  @Test
  void shouldDispatchUpdateDeleteQueryCountViaDslMap() {
    adapter.insert("users", mapOf("name", "Alice", "age", 30, "status", "active")).get();
    adapter.insert("users", mapOf("name", "Bob", "age", 28, "status", "active")).get();
    adapter.insert("users", mapOf("name", "Carol", "age", 40, "status", "inactive")).get();

    Map<String, Object> update = new HashMap<String, Object>();
    update.put("update", "users");
    update.put("set", mapOf("status", "reviewing"));
    update.put("where", mapOf("age", mapOf("$gte", 28)));
    var updated = adapter.update(update).get();
    assertEquals(3L, updated);

    Map<String, Object> query = new HashMap<String, Object>();
    query.put("select", Arrays.asList("id", "name", "status"));
    query.put("from", "users");
    query.put("where", mapOf("status", "reviewing"));
    query.put("order", Arrays.asList("id ASC"));
    var records = adapter.query(query).get();
    assertEquals(3, records.size());

    var count = adapter.count("users", mapOf("status", "reviewing")).get();
    assertEquals(3L, count);

    Map<String, Object> delete = new HashMap<String, Object>();
    delete.put("delete", "users");
    delete.put("where", mapOf("name", "Bob"));
    var deleted = adapter.delete(delete).get();
    assertEquals(1L, deleted);
  }

  @Test
  void shouldDispatchExecuteForInsertUpdateDeleteStatements() {
    Map<String, Object> insert = new HashMap<String, Object>();
    insert.put("insert", "users");
    insert.put("values", mapOf("name", "Eve", "age", 22, "status", "new"));
    var inserted = adapter.execute(insert).get();
    assertNotNull(inserted);

    Map<String, Object> update = new HashMap<String, Object>();
    update.put("update", "users");
    update.put("set", mapOf("status", "done"));
    update.put("where", mapOf("name", "Eve"));
    var affected = (Long) adapter.execute(update).get();
    assertEquals(1L, affected);

    Map<String, Object> delete = new HashMap<String, Object>();
    delete.put("delete", "users");
    delete.put("where", mapOf("name", "Eve"));
    var removed = (Long) adapter.execute(delete).get();
    assertEquals(1L, removed);
  }

  private static Map<String, Object> mapOf(Object... kv) {
    Map<String, Object> map = new HashMap<String, Object>();
    for (int i = 0; i < kv.length; i += 2) {
      map.put((String) kv[i], kv[i + 1]);
    }
    return map;
  }
}
