package com.ouroboros.data.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.querydsl.sql.types.Null;

class SqlDataAdapterTypeMappingTest {

  private SqlDataAdapter adapter;

  @BeforeEach
  void setUp() {
    var ds = new JdbcDataSource();
    ds.setURL("jdbc:h2:mem:sql_type_mapping;MODE=MySQL;DB_CLOSE_DELAY=-1");
    ds.setUser("sa");
    ds.setPassword("");
    adapter = new SqlDataAdapter(ds, "H2");
  }

  @Test
  void shouldMapCommonJavaTypesToJdbcTypes() throws Exception {
    var m = SqlDataAdapter.class.getDeclaredMethod("toJdbcType", Object.class);
    m.setAccessible(true);

    assertEquals(Types.DECIMAL, ((Integer) m.invoke(adapter, new BigDecimal("1.2"))).intValue());
    assertEquals(Types.NUMERIC, ((Integer) m.invoke(adapter, 1)).intValue());
    assertEquals(Types.BOOLEAN, ((Integer) m.invoke(adapter, Boolean.TRUE)).intValue());
    assertEquals(Types.TIME, ((Integer) m.invoke(adapter, LocalTime.now())).intValue());
    assertEquals(Types.DATE, ((Integer) m.invoke(adapter, LocalDate.now())).intValue());
    assertEquals(Types.TIMESTAMP, ((Integer) m.invoke(adapter, LocalDateTime.now())).intValue());
    assertEquals(Types.NULL, ((Integer) m.invoke(adapter, Null.DEFAULT)).intValue());
    assertEquals(Types.NULL, ((Integer) m.invoke(adapter, new Object[]{null}[0])).intValue());
    assertNotNull(adapter.getJdbcTemplate());
  }
}
