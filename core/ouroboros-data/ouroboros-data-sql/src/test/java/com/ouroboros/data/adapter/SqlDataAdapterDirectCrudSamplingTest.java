package com.ouroboros.data.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.BadSqlGrammarException;

class SqlDataAdapterDirectCrudSamplingTest {

  private SqlDataAdapter adapter;

  @BeforeEach
  void setUp() {
    var ds = new JdbcDataSource();
    ds.setURL("jdbc:h2:mem:sql_data_adapter_sampling;MODE=MySQL;DB_CLOSE_DELAY=-1");
    ds.setUser("sa");
    ds.setPassword("");
    adapter = new SqlDataAdapter(ds, "H2");

    adapter.execute("DROP TABLE IF EXISTS \"ORDERS\"");
    adapter.execute("CREATE TABLE \"ORDERS\" (\"ID\" BIGINT AUTO_INCREMENT PRIMARY KEY, \"STATUS\" VARCHAR(32), \"AMOUNT\" DECIMAL(18,2), \"SCORE\" DOUBLE)");
  }

  @Test
  void shouldSupportSequentialCrudAndQueryVariants() {
    var id1 = adapter.insert(Long.class,
            "INSERT INTO \"ORDERS\"(\"STATUS\", \"AMOUNT\", \"SCORE\") VALUES(?, ?, ?)",
            "PENDING", new BigDecimal("120.50"), 4.5)
        .get();
    var id2 = adapter.insert(Long.class,
            "INSERT INTO \"ORDERS\"(\"STATUS\", \"AMOUNT\", \"SCORE\") VALUES(?, ?, ?)",
            "COMPLETED", new BigDecimal("201.00"), 3.8)
        .get();
    assertNotNull(id1);
    assertNotNull(id2);

    var batch = adapter.batchInsert(Long.class,
            "INSERT INTO \"ORDERS\"(\"STATUS\", \"AMOUNT\", \"SCORE\") VALUES(?, ?, ?)",
            Arrays.asList(
                Arrays.asList("PENDING", new BigDecimal("80.00"), 2.1),
                Arrays.asList("COMPLETED", new BigDecimal("320.00"), 4.9)))
        .get();
    assertEquals(2, batch.size());

    var updated = adapter.execute(
            "UPDATE \"ORDERS\" SET \"STATUS\" = ? WHERE \"AMOUNT\" >= ?",
            "ARCHIVED", new BigDecimal("200.00"))
        .get();
    assertEquals(2, updated);

    var listUpdated = adapter.execute(
            "UPDATE \"ORDERS\" SET \"STATUS\" = ? WHERE \"SCORE\" < ?",
            Arrays.asList("LOW", 3.0))
        .get();
    assertEquals(1, listUpdated);

    var deleted = adapter.execute("DELETE FROM \"ORDERS\" WHERE \"STATUS\" = ?", "LOW").get();
    assertEquals(1, deleted);

    var queried = adapter.query("SELECT \"ID\", \"STATUS\", \"AMOUNT\" FROM \"ORDERS\" ORDER BY \"ID\" ASC").get();
    assertEquals(3, queried.size());
    assertTrue(queried.get(0).containsKey("ID"));

    var proc = adapter.call("CALL missing_proc()", Collections.emptyList());
    assertTrue(proc.isFailure());
  }

  @Test
  void shouldFailFastForUnsupportedNamedBatchSyntaxOnH2() {
    assertThrows(BadSqlGrammarException.class, () -> {
      var params = new HashMap<String, Object>();
      params.put("A", "PENDING");
      params.put("B", new BigDecimal("1.00"));
      params.put("C", 1.1);
      adapter.batchInsert(Long.class,
          "INSERT INTO \"ORDERS\"(\"STATUS\", \"AMOUNT\", \"SCORE\") VALUES(:{A}, :{B}, :{C})",
          params);
    });
  }
}
