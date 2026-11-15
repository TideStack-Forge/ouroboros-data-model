package com.ouroboros.data.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.migration.kingbase.KingbaseDatabase;

import liquibase.Scope;
import liquibase.database.Database;
import liquibase.database.core.PostgresDatabase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.diff.DiffResult;
import liquibase.diff.compare.CompareControl;
import liquibase.diff.output.DiffOutputControl;
import liquibase.exception.DatabaseException;
import liquibase.executor.ExecutorService;
import liquibase.executor.jvm.JdbcExecutor;
import liquibase.snapshot.EmptyDatabaseSnapshot;
import liquibase.statement.SqlStatement;
import liquibase.util.DependencyUtil;

class KingbaseDependencyOrderingTest {

  @Test
  void shouldPreserveKingbaseDependencyOrderAndStoredFunctionNames() throws Exception {
    assertDependencyOrder(new KingbaseDatabase());
  }

  @Test
  void shouldPreservePostgresDependencyDiscovery() throws Exception {
    assertDependencyOrder(new PostgresDatabase());
  }

  @Test
  void shouldPropagateDependencyQueryFailures() throws Exception {
    var database = new KingbaseDatabase();
    var failure = new DatabaseException("dependency query failed");
    var executors = Scope.getCurrentScope().getSingleton(ExecutorService.class);
    executors.setExecutor("jdbc", database, new JdbcExecutor() {
      @Override
      public List<Map<String, ?>> queryForList(SqlStatement statement) throws DatabaseException {
        throw failure;
      }
    });
    try {
      var graph = new DependencyUtil.DependencyGraph<String>(new ArrayList<String>()::add);
      assertSame(failure, assertThrows(DatabaseException.class,
          () -> new DependencyReader(database).read(graph, List.of("public"), database)));
    } finally {
      executors.clearExecutor("jdbc", database);
    }
  }

  @Test
  void shouldReadDependenciesWithoutAbortingReadOnlyTransaction() throws Exception {
    var dataSource = KingbaseIntegrationEnvironment.requireDataSource();
    try (var connection = dataSource.getConnection()) {
      connection.setReadOnly(true);
      connection.setAutoCommit(false);
      try {
        var database = new KingbaseDatabase();
        database.setConnection(new JdbcConnection(connection));
        var order = new ArrayList<String>();
        var graph = new DependencyUtil.DependencyGraph<String>(order::add);

        new DependencyReader(database).read(graph, List.of(connection.getSchema()), database);
        graph.computeDependencies();
        assertFalse(order.isEmpty(), "the existing schema should contribute dependency ordering");
        try (var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT 1")) {
          assertTrue(rows.next());
          assertEquals(1, rows.getInt(1), "dependency discovery must leave the transaction usable");
        }
      } finally {
        connection.rollback();
      }
    }
  }

  private void assertDependencyOrder(Database database) throws Exception {
    var rows = List.of(dependency("child", "report(integer)"), dependency("parent", "child"));
    var executors = Scope.getCurrentScope().getSingleton(ExecutorService.class);
    executors.setExecutor("jdbc", database, new JdbcExecutor() {
      @Override
      public List<Map<String, ?>> queryForList(SqlStatement statement) {
        return rows;
      }
    });
    try {
      var order = new ArrayList<String>();
      var graph = new DependencyUtil.DependencyGraph<String>(order::add);
      new DependencyReader(database).read(graph, List.of("public"), database);
      graph.computeDependencies();

      assertTrue(order.containsAll(List.of(
          "public.parent", "public.child", "public.report(integer)", "public.report")));
      assertTrue(order.indexOf("public.parent") < order.indexOf("public.child"));
      assertTrue(order.indexOf("public.child") < order.indexOf("public.report(integer)"));
      assertTrue(order.indexOf("public.child") < order.indexOf("public.report"));
    } finally {
      executors.clearExecutor("jdbc", database);
    }
  }

  private Map<String, ?> dependency(String first, String after) {
    return Map.of("REFERENCING_SCHEMA_NAME", "public", "REFERENCING_NAME", first,
        "REFERENCED_SCHEMA_NAME", "public", "REFERENCED_NAME", after);
  }

  private static class DependencyReader extends CustomDiffToChangeLog {
    DependencyReader(Database database) throws Exception {
      super(new DiffResult(new EmptyDatabaseSnapshot(database), new EmptyDatabaseSnapshot(database), new CompareControl()),
          new DiffOutputControl());
    }

    void read(DependencyUtil.DependencyGraph<String> graph, List<String> schemas, Database database) throws Exception {
      addDependencies(graph, schemas, database);
    }
  }
}
