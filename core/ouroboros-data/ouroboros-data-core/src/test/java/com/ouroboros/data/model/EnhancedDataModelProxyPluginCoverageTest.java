package com.ouroboros.data.model;

import static com.ouroboros.data.dsl.query.Query.field;
import static com.ouroboros.data.dsl.query.Query.populate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.format.DateTimeFormatterBuilder;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.vavr.control.Try;

import com.ouroboros.data.adapter.DataAdapter;
import com.ouroboros.data.dsl.Operators;
import com.ouroboros.data.dsl.statement.ModelQueryStatement;
import com.ouroboros.data.dsl.statement.ModelQueryStatementBuilder;
import com.ouroboros.data.dsl.statement.OmitClause;
import com.ouroboros.data.dsl.statement.PopulateClause;
import com.ouroboros.data.exception.InvalidStatementException;
import com.ouroboros.data.model.plugins.ProbeDataModelPlugin;
import com.ouroboros.data.model.valuetypes.LongValue;
import com.ouroboros.data.dsl.SExpression;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.record.Record;
import com.ouroboros.data.record.RecordList;
import com.ouroboros.data.station.DataStation;
import com.ouroboros.data.transpile.TranspileContext;
import com.ouroboros.data.util.DataServices;

public class EnhancedDataModelProxyPluginCoverageTest {

  @BeforeEach
  public void setUp() throws Exception {
    ProbeDataModelPlugin.reset();
    clearSpiCache();
  }

  @Test
  public void updateByIdShouldRouteThroughWherePluginHook() {
    DataModel coreDataModel = createCoreDataModel();
    EnhancedDataModelProxy proxy = createProxy(coreDataModel);

    Try<Long> result = proxy.update(1L, record("name", "after"));

    assertTrue(result.isSuccess(), () -> result.isFailure() ? result.getCause().toString() : "");
    assertTrue(ProbeDataModelPlugin.CALLS.contains("updateWhere"));
  }

  @Test
  public void modelShouldExposeUniqueConstraintsAndFieldUniquenessScope() {
    DataModelMeta meta = new DataModelMeta();
    meta.setName("User");
    meta.setRawName("user");
    meta.setFields(Arrays.asList(fieldMeta("id", "Long"), fieldMeta("code", "String")));
    meta.getFields().get(1).setUniquenessScope(UniquenessScope.ACTIVE_RECORDS);
    meta.setPrimaryKeys(Collections.singletonList("id"));

    DataModelUniqueConstraintMeta uniqueConstraint = new DataModelUniqueConstraintMeta();
    uniqueConstraint.setName("user_code");
    uniqueConstraint.setFields(Arrays.asList("id", "code"));
    meta.setUniqueConstraints(Collections.singletonList(uniqueConstraint));

    DataAdapter adapter = mock(DataAdapter.class);
    DataStation<?> station = mock(DataStation.class);
    when(station.getDataAdapter()).thenReturn(adapter);

    DefaultDataModel core = new DefaultDataModel(meta, station);
    EnhancedDataModelProxy proxy = new EnhancedDataModelProxy(core, Collections.emptyList());

    assertEquals("user_code", core.getUniqueConstraints().get(0).getName());
    assertEquals("user_code", proxy.getUniqueConstraints().get(0).getName());
    assertEquals(UniquenessScope.ACTIVE_RECORDS, core.getField("code").get().getUniquenessScope());
    assertEquals(UniquenessScope.ACTIVE_RECORDS, proxy.getField("code").get().getUniquenessScope());
  }

  @Test
  public void updateByIdsShouldRouteThroughWherePluginHook() {
    DataModel coreDataModel = createCoreDataModel();
    EnhancedDataModelProxy proxy = createProxy(coreDataModel);

    Try<Long> result = proxy.update(Arrays.<Object>asList(1L, 2L), record("status", "archived"));

    assertTrue(result.isSuccess(), () -> result.isFailure() ? result.getCause().toString() : "");
    assertTrue(ProbeDataModelPlugin.CALLS.contains("updateWhere"));
  }

  @Test
  public void deleteByIdShouldRouteThroughWherePluginHook() {
    DataModel coreDataModel = createCoreDataModel();
    EnhancedDataModelProxy proxy = createProxy(coreDataModel);

    Try<Long> result = proxy.delete(1L);

    assertTrue(result.isSuccess());
    assertTrue(ProbeDataModelPlugin.CALLS.contains("deleteWhere"));
  }

  @Test
  public void deleteByIdsShouldRouteThroughWherePluginHook() {
    DataModel coreDataModel = createCoreDataModel();
    EnhancedDataModelProxy proxy = createProxy(coreDataModel);

    Try<Long> result = proxy.delete(Arrays.<Object>asList(1L, 2L));

    assertTrue(result.isSuccess());
    assertTrue(ProbeDataModelPlugin.CALLS.contains("deleteWhere"));
  }

  @Test
  public void insertOrUpdateShouldRouteInsertBranchThroughPluginChain() {
    DataModel coreDataModel = createCoreDataModel();
    EnhancedDataModelProxy proxy = createProxy(coreDataModel);

    Try<Record> result = proxy.insertOrUpdate(record("id", 2L, "name", "new-user"));

    assertTrue(result.isSuccess());
    assertTrue(ProbeDataModelPlugin.CALLS.contains("insert"));
  }

  @Test
  public void batchInsertOrUpdateShouldRouteWritesThroughPluginChain() {
    DataModel coreDataModel = createCoreDataModel();
    EnhancedDataModelProxy proxy = createProxy(coreDataModel);

    Try<RecordList> result = proxy.batchInsertOrUpdate(Arrays.asList(
        record("id", 2L, "name", "new-user"),
        record("id", 1L, "name", "after")
    ));

    assertTrue(result.isSuccess());
    assertTrue(ProbeDataModelPlugin.CALLS.contains("batchInsert"));
    assertTrue(ProbeDataModelPlugin.CALLS.contains("updateWhere"));
  }

  @Test
  public void getByIdShouldRouteConvertedIdThroughQueryPluginHook() {
    EnhancedDataModelProxy proxy = createProxy(createCoreDataModel());

    Try<Record> result = proxy.get("1");

    assertTrue(result.isSuccess());
    assertTrue(ProbeDataModelPlugin.CALLS.contains("query"));
    assertWhereContains(lastQueryStatement(), "id", 1L);
  }

  @Test
  public void getByIdWithStatementShouldRouteMergedStatementThroughQueryPluginHook() {
    EnhancedDataModelProxy proxy = createProxy(createCoreDataModel());
    Map<String, Object> statement = record("SELECT", Collections.singletonList("name"));

    Try<Record> result = proxy.get("1", statement);

    assertTrue(result.isSuccess());
    assertTrue(ProbeDataModelPlugin.CALLS.contains("query"));
    assertSelectContains(lastQueryStatement(), "name");
    assertWhereContains(lastQueryStatement(), "id", 1L);
  }

  @Test
  public void queryByIdsShouldRouteConvertedIdsThroughQueryPluginHook() {
    EnhancedDataModelProxy proxy = createProxy(createCoreDataModel());

    Try<RecordList> result = proxy.query(Arrays.<Object>asList("1", "2"));

    assertTrue(result.isSuccess());
    assertTrue(ProbeDataModelPlugin.CALLS.contains("query"));
    assertWhereContains(lastQueryStatement(), "id", 1L, 2L);
  }

  @Test
  public void queryFacadeWithPluginsShouldRouteThroughRawMapPluginHook() {
    DataModel coreDataModel = createCoreDataModel();
    EnhancedDataModelProxy proxy = new EnhancedDataModelProxy(coreDataModel, Collections.emptyList());

    Try<RecordList> result = proxy.query()
        .withPlugins(new PluginDescriptor("Probe"))
        .where(field("active").eq(true))
        .execute();

    assertTrue(result.isSuccess());
    assertTrue(ProbeDataModelPlugin.CALLS.contains("query"));
    assertWhereContains(lastQueryStatement(), "active", true);
    verify(coreDataModel, never()).query(anyMap());
    verify(coreDataModel).query(any(QueryStatement.class));
  }

  @Test
  public void queryFacadeCanonicalRawShouldReachPluginAndRemainEditable() {
    DataModel coreDataModel = createCoreDataModel();
    EnhancedDataModelProxy proxy = createProxy(coreDataModel);
    ProbeDataModelPlugin.ADD_QUERY_FACADE_DECORATIONS = true;

    Try<RecordList> result = proxy.query()
        .select(field("id"), field("name").as("username"))
        .where(field("age").gte(field("minAge")))
        .populate(populate("department")
            .where(Map.of("age", Map.of("$gte", field("minAge")))))
        .execute();

    assertTrue(result.isSuccess());
    assertTrue(ProbeDataModelPlugin.CALLS.contains("query"));
    assertSelectContains(incomingQueryStatement(), "id", "name");
    assertWhereContains(incomingQueryStatement(), "age");
    assertTrue(containsField(incomingQueryStatement().getWhere(), "minAge"));
    assertTrue(incomingQueryStatement() instanceof ModelQueryStatement);
    assertTrue(((ModelQueryStatement) incomingQueryStatement()).getPopulateClause().getEntries().stream()
        .anyMatch(entry -> "department".equals(entry.fieldName())));

    ArgumentCaptor<QueryStatement> statementCaptor = ArgumentCaptor.forClass(QueryStatement.class);
    verify(coreDataModel).query(statementCaptor.capture());
    assertSelectContains(statementCaptor.getValue(), "pluginName");
    assertWhereContains(statementCaptor.getValue(), "tenantId", "tenant-1");
    assertTrue(statementCaptor.getValue() instanceof ModelQueryStatement);
  }

  @Test
  public void queryFacadeWithoutPluginsShouldUseUnpluggedDataModelView() {
    DataModel coreDataModel = createCoreDataModel();
    EnhancedDataModelProxy proxy = createProxy(coreDataModel);

    Try<RecordList> result = proxy.query()
        .withoutPlugins("Probe")
        .where(field("active").eq(true))
        .execute();

    assertTrue(result.isSuccess());
    assertTrue(!ProbeDataModelPlugin.CALLS.contains("query"));
    verify(coreDataModel, never()).query(anyMap());
    verify(coreDataModel).query(any(QueryStatement.class));
  }

  @Test
  public void typedQueryStatementShouldRouteThroughQueryPluginHook() {
    DataModel coreDataModel = createCoreDataModel();
    EnhancedDataModelProxy proxy = createProxy(coreDataModel);
    SExpression<Boolean> where = SExpression.create(
        com.ouroboros.data.dsl.Operators.EQ,
        SExpression.field("id"),
        SExpression.constant(1L));
    QueryStatement statement = QueryStatement.builder()
        .from("demo_user", "user_root")
        .select(SExpression.field("name"))
        .where(where)
        .build();

    Try<RecordList> result = proxy.query(statement);

    assertTrue(result.isSuccess(), () -> result.isFailure() ? result.getCause().toString() : "");
    assertTrue(ProbeDataModelPlugin.CALLS.contains("query"));
    assertEquals(where, lastQueryStatement().getWhere());
    verify(coreDataModel).query(any(QueryStatement.class));
  }

  @Test
  public void typedQueryStatementShouldKeepOmitAddedByMapPlugin() {
    DataModel coreDataModel = createCoreDataModel();
    EnhancedDataModelProxy proxy = createProxy(coreDataModel);
    ProbeDataModelPlugin.ADD_OMIT = true;
    QueryStatement statement = QueryStatement.builder()
        .from("demo_user", "user_root")
        .select(SExpression.field("name"))
        .build();

    Try<RecordList> result = proxy.query(statement);

    assertTrue(result.isSuccess());
    ArgumentCaptor<QueryStatement> statementCaptor = ArgumentCaptor.forClass(QueryStatement.class);
    verify(coreDataModel).query(statementCaptor.capture());
    assertTrue(statementCaptor.getValue() instanceof ModelQueryStatement);
    ModelQueryStatement normalized = (ModelQueryStatement) statementCaptor.getValue();
    assertEquals(Collections.singleton("secret"), normalized.getOmitClause().getFields());
  }

  @Test
  public void typedRootSubqueryShouldFailClosedWhenMapPluginRewritesWhere() {
    DataModel coreDataModel = createCoreDataModel();
    EnhancedDataModelProxy proxy = createProxy(coreDataModel);
    ProbeDataModelPlugin.ADD_WHERE_FILTER = true;
    QueryStatement subquery = QueryStatement.builder()
        .from("demo_user", "inner_user")
        .select(SExpression.field("name"))
        .build();
    QueryStatement statement = QueryStatement.builder()
        .from(subquery, "derived_user")
        .select(SExpression.field("derived_user", "name"))
        .build();

    Try<RecordList> result = proxy.query(statement);

    assertTrue(result.isFailure());
    assertTrue(result.getCause() instanceof InvalidStatementException);
    verify(coreDataModel, never()).query(any(QueryStatement.class));
  }

  @Test
  public void typedRootSubqueryShouldReachCoreWhenMapPluginPreservesWhere() {
    DataModel coreDataModel = createCoreDataModel();
    EnhancedDataModelProxy proxy = createProxy(coreDataModel);
    QueryStatement subquery = QueryStatement.builder()
        .from("demo_user", "inner_user")
        .select(SExpression.field("name"))
        .build();
    QueryStatement statement = QueryStatement.builder()
        .from(subquery, "derived_user")
        .select(SExpression.field("derived_user", "name"))
        .build();

    Try<RecordList> result = proxy.query(statement);

    assertTrue(result.isSuccess());
    ArgumentCaptor<QueryStatement> statementCaptor = ArgumentCaptor.forClass(QueryStatement.class);
    verify(coreDataModel).query(statementCaptor.capture());
    assertTrue(statementCaptor.getValue().getFrom().isSubQuery());
    assertEquals("derived_user", statementCaptor.getValue().getFrom().getAlias());
  }

  @Test
  public void typedRootSubqueryShouldCompareCanonicalWhereShapes() {
    DataModel coreDataModel = createCoreDataModel();
    EnhancedDataModelProxy proxy = createProxy(coreDataModel);
    QueryStatement subquery = QueryStatement.builder()
        .from("demo_user", "inner_user")
        .select(SExpression.field("name"))
        .build();
    SExpression<Boolean> predicate = SExpression.create(
        Operators.EQ,
        SExpression.field("derived_user", "name"),
        SExpression.constant("original"));
    QueryStatement statement = QueryStatement.builder()
        .from(subquery, "derived_user")
        .select(SExpression.field("derived_user", "name"))
        .where(SExpression.create(Operators.AND, predicate))
        .build();

    Try<RecordList> result = proxy.query(statement);

    assertTrue(result.isSuccess(), () -> result.isFailure() ? result.getCause().toString() : "");
    verify(coreDataModel).query(any(QueryStatement.class));
  }

  @Test
  public void typedRootSubqueryShouldFailClosedWhenMapPluginMutatesWhereInPlace() {
    DataModel coreDataModel = createCoreDataModel();
    EnhancedDataModelProxy proxy = createProxy(coreDataModel);
    ProbeDataModelPlugin.MUTATE_WHERE_IN_PLACE = true;
    QueryStatement subquery = QueryStatement.builder()
        .from("demo_user", "inner_user")
        .select(SExpression.field("name"))
        .build();
    QueryStatement statement = QueryStatement.builder()
        .from(subquery, "derived_user")
        .select(SExpression.field("derived_user", "name"))
        .where(SExpression.create(
            Operators.EQ,
            SExpression.field("derived_user", "name"),
            SExpression.constant("original")))
        .build();

    Try<RecordList> result = proxy.query(statement);

    assertTrue(result.isFailure());
    assertTrue(result.getCause() instanceof InvalidStatementException);
    verify(coreDataModel, never()).query(any(QueryStatement.class));
  }

  @Test
  public void typedRootSubqueryShouldFailClosedWhenMapPluginMutatesSubqueryInPlace() {
    DataModel coreDataModel = createCoreDataModel();
    EnhancedDataModelProxy proxy = createProxy(coreDataModel);
    ProbeDataModelPlugin.MUTATE_ROOT_SUBQUERY_IN_PLACE = true;
    QueryStatement subquery = QueryStatement.builder()
        .from("demo_user", "inner_user")
        .select(SExpression.field("name"))
        .where(SExpression.create(
            Operators.EQ,
            SExpression.field("active"),
            SExpression.constant(Boolean.TRUE)))
        .build();
    QueryStatement statement = QueryStatement.builder()
        .from(subquery, "derived_user")
        .select(SExpression.field("derived_user", "name"))
        .build();

    Try<RecordList> result = proxy.query(statement);

    assertTrue(result.isFailure());
    assertTrue(result.getCause() instanceof InvalidStatementException);
    verify(coreDataModel, never()).query(any(QueryStatement.class));
  }

  @Test
  public void typedRootSubqueryShouldFailClosedWhenModelClausesMutateInPlace() {
    DataModel coreDataModel = createCoreDataModel();
    EnhancedDataModelProxy proxy = createProxy(coreDataModel);
    ProbeDataModelPlugin.MUTATE_ROOT_MODEL_SUBQUERY_IN_PLACE = true;
    QueryStatement statement = QueryStatement.builder()
        .from(modelSubquery(), "derived_user")
        .select(SExpression.field("derived_user", "name"))
        .build();

    Try<RecordList> result = proxy.query(statement);

    assertTrue(result.isFailure());
    assertTrue(result.getCause() instanceof InvalidStatementException);
    verify(coreDataModel, never()).query(any(QueryStatement.class));
  }

  @Test
  public void typedRootSubqueryShouldFailClosedWhenNestedModelClausesMutateInPlace() {
    DataModel coreDataModel = createCoreDataModel();
    EnhancedDataModelProxy proxy = createProxy(coreDataModel);
    ProbeDataModelPlugin.MUTATE_NESTED_MODEL_SUBQUERY_IN_PLACE = true;
    QueryStatement rootSubquery = QueryStatement.builder()
        .from(modelSubquery(), "nested_model")
        .select(SExpression.field("nested_model", "name"))
        .build();
    QueryStatement statement = QueryStatement.builder()
        .from(rootSubquery, "derived_user")
        .select(SExpression.field("derived_user", "name"))
        .build();

    Try<RecordList> result = proxy.query(statement);

    assertTrue(result.isFailure());
    assertTrue(result.getCause() instanceof InvalidStatementException);
    verify(coreDataModel, never()).query(any(QueryStatement.class));
  }

  @Test
  public void typedRootSubqueryShouldDistinguishArrayAndCollectionConstants() {
    DataModel coreDataModel = createCoreDataModel();
    EnhancedDataModelProxy proxy = createProxy(coreDataModel);
    ProbeDataModelPlugin.REPLACE_ARRAY_CONSTANT_WITH_LIST = true;
    QueryStatement statement = rootSubqueryStatement(SExpression.create(
        Operators.EQ,
        SExpression.field("derived_user", "payload"),
        SExpression.constant(new byte[] {1, 2})));

    Try<RecordList> result = proxy.query(statement);

    assertTrue(result.isFailure());
    assertTrue(result.getCause() instanceof InvalidStatementException);
    verify(coreDataModel, never()).query(any(QueryStatement.class));
  }

  @Test
  public void typedRootSubqueryShouldSnapshotMutableNumbersByValue() {
    DataModel coreDataModel = createCoreDataModel();
    EnhancedDataModelProxy proxy = createProxy(coreDataModel);
    ProbeDataModelPlugin.MUTATE_NUMBER_IN_PLACE = true;
    QueryStatement statement = rootSubqueryStatement(SExpression.create(
        Operators.EQ,
        SExpression.field("derived_user", "rank"),
        SExpression.constant(new AtomicInteger(1))));

    Try<RecordList> result = proxy.query(statement);

    assertTrue(result.isFailure());
    assertTrue(result.getCause() instanceof InvalidStatementException);
    verify(coreDataModel, never()).query(any(QueryStatement.class));
  }

  @Test
  public void typedRootSubqueryShouldRejectMutableNumberWithStableText() {
    DataModel coreDataModel = createCoreDataModel();
    EnhancedDataModelProxy proxy = createProxy(coreDataModel);
    ProbeDataModelPlugin.MUTATE_STABLE_TEXT_NUMBER_IN_PLACE = true;
    QueryStatement statement = rootSubqueryStatement(SExpression.create(
        Operators.EQ,
        SExpression.field("derived_user", "rank"),
        SExpression.constant(new ProbeDataModelPlugin.StableTextNumber(1))));

    Try<RecordList> result = proxy.query(statement);

    assertTrue(result.isFailure());
    assertTrue(result.getCause() instanceof InvalidStatementException);
    verify(coreDataModel, never()).query(any(QueryStatement.class));
  }

  @Test
  public void typedRootSubqueryShouldRejectStructurallyCollidingMapKeys() {
    DataModel coreDataModel = createCoreDataModel();
    EnhancedDataModelProxy proxy = createProxy(coreDataModel);
    Map<Object, Object> constant = new LinkedHashMap<>();
    constant.put(new byte[] {1}, new AtomicInteger(1));
    constant.put(new byte[] {1}, new AtomicInteger(2));
    QueryStatement statement = rootSubqueryStatement(SExpression.create(
        Operators.EQ,
        SExpression.field("derived_user", "payload"),
        SExpression.constant(constant)));

    Try<RecordList> result = proxy.query(statement);

    assertTrue(result.isFailure());
    assertTrue(result.getCause() instanceof InvalidStatementException);
    assertTrue(!ProbeDataModelPlugin.CALLS.contains("query"));
    verify(coreDataModel, never()).query(any(QueryStatement.class));
  }

  @Test
  public void typedRootSubqueryShouldRejectMutableJavaTimeValue() {
    DataModel coreDataModel = createCoreDataModel();
    EnhancedDataModelProxy proxy = createProxy(coreDataModel);
    QueryStatement statement = rootSubqueryStatement(SExpression.create(
        Operators.EQ,
        SExpression.field("derived_user", "formatter"),
        SExpression.constant(new DateTimeFormatterBuilder())));

    Try<RecordList> result = proxy.query(statement);

    assertTrue(result.isFailure());
    assertTrue(result.getCause() instanceof InvalidStatementException);
    assertTrue(!ProbeDataModelPlugin.CALLS.contains("query"));
    verify(coreDataModel, never()).query(any(QueryStatement.class));
  }

  @Test
  public void typedRootSubqueryShouldRejectMutableJavaTimeMapKey() {
    DataModel coreDataModel = createCoreDataModel();
    EnhancedDataModelProxy proxy = createProxy(coreDataModel);
    Map<Object, Object> constant = new LinkedHashMap<>();
    constant.put(new DateTimeFormatterBuilder(), "value");
    QueryStatement statement = rootSubqueryStatement(SExpression.create(
        Operators.EQ,
        SExpression.field("derived_user", "formatter_map"),
        SExpression.constant(constant)));

    Try<RecordList> result = proxy.query(statement);

    assertTrue(result.isFailure());
    assertTrue(result.getCause() instanceof InvalidStatementException);
    assertTrue(!ProbeDataModelPlugin.CALLS.contains("query"));
    verify(coreDataModel, never()).query(any(QueryStatement.class));
  }

  @Test
  public void typedRootSubqueryShouldFailClosedWhenFromIsReplaced() {
    DataModel coreDataModel = createCoreDataModel();
    EnhancedDataModelProxy proxy = createProxy(coreDataModel);
    ProbeDataModelPlugin.REPLACE_FROM = true;
    QueryStatement statement = rootSubqueryStatement(SExpression.create(
        Operators.EQ,
        SExpression.field("derived_user", "name"),
        SExpression.constant("original")));

    Try<RecordList> result = proxy.query(statement);

    assertTrue(result.isFailure());
    assertTrue(result.getCause() instanceof InvalidStatementException);
    verify(coreDataModel, never()).query(anyMap());
    verify(coreDataModel, never()).query(any(QueryStatement.class));
  }

  @Test
  public void typedTableQueryShouldFailClosedWhenPluginReplacesFrom() {
    DataModel coreDataModel = createCoreDataModel();
    EnhancedDataModelProxy proxy = createProxy(coreDataModel);
    ProbeDataModelPlugin.REPLACE_FROM = true;
    QueryStatement statement = QueryStatement.builder()
        .from("demo_user", "typed_root")
        .select(SExpression.field("typed_root", "name"))
        .build();

    Try<RecordList> result = proxy.query(statement);

    assertTrue(result.isFailure());
    assertTrue(result.getCause() instanceof InvalidStatementException);
    verify(coreDataModel, never()).query(anyMap());
    verify(coreDataModel, never()).query(any(QueryStatement.class));
  }

  @Test
  public void nestedRawQueryOnSameProxyShouldNotReuseOuterTypedProvenance() {
    DataModel coreDataModel = createCoreDataModel();
    EnhancedDataModelProxy proxy = createProxy(coreDataModel);
    ProbeDataModelPlugin.NESTED_RAW_QUERY_MODEL = proxy;
    ProbeDataModelPlugin.NESTED_RAW_QUERY = record(
        "FROM", new QueryStatement.TableSource("nested_table", "nested"));
    QueryStatement statement = QueryStatement.builder()
        .from("demo_user", "typed_root")
        .build();

    Try<RecordList> result = proxy.query(statement);

    assertTrue(result.isSuccess());
    verify(coreDataModel, never()).query(anyMap());
    verify(coreDataModel, times(2)).query(any(QueryStatement.class));
  }

  @Test
  public void nestedRawQueryOnAnotherProxyShouldNotReuseOuterTypedProvenance() {
    DataModel outerCoreDataModel = createCoreDataModel();
    DataModel nestedCoreDataModel = createCoreDataModel();
    EnhancedDataModelProxy outerProxy = createProxy(outerCoreDataModel);
    EnhancedDataModelProxy nestedProxy = createProxy(nestedCoreDataModel);
    ProbeDataModelPlugin.NESTED_RAW_QUERY_MODEL = nestedProxy;
    ProbeDataModelPlugin.NESTED_RAW_QUERY = record(
        "FROM", new QueryStatement.TableSource("nested_table", "nested"));
    QueryStatement statement = QueryStatement.builder()
        .from("demo_user", "typed_root")
        .build();

    Try<RecordList> result = outerProxy.query(statement);

    assertTrue(result.isSuccess());
    verify(nestedCoreDataModel, never()).query(anyMap());
    verify(nestedCoreDataModel).query(any(QueryStatement.class));
    verify(outerCoreDataModel).query(any(QueryStatement.class));
  }

  @Test
  public void typedQueryStatementShouldPreserveAliasQualifiedFieldsThroughRealCorePath() {
    DataAdapter adapter = mock(DataAdapter.class);
    when(adapter.query(any(QueryStatement.class), any(TranspileContext.class)))
        .thenReturn(Try.success(RecordList.empty()));
    EnhancedDataModelProxy proxy = createProxy(createDefaultDataModel(adapter));
    QueryStatement statement = QueryStatement.builder()
        .from("demo_user", "typed_root")
        .select(SExpression.field("typed_root", "name"))
        .where(SExpression.create(
            com.ouroboros.data.dsl.Operators.EQ,
            SExpression.field("typed_root", "id"),
            SExpression.constant(1L)))
        .build();

    Try<RecordList> result = proxy.query(statement);

    assertTrue(result.isSuccess(), () -> result.isFailure() ? result.getCause().toString() : "");
    ArgumentCaptor<QueryStatement> statementCaptor = ArgumentCaptor.forClass(QueryStatement.class);
    verify(adapter).query(statementCaptor.capture(), any(TranspileContext.class));
    assertEquals("demo_user", statementCaptor.getValue().getFrom().getTableName());
    assertEquals("typed_root", statementCaptor.getValue().getFrom().getAlias());
  }

  @Test
  public void rawQueryShouldStillUseCanonicalModelFromThroughRealCorePath() {
    DataAdapter adapter = mock(DataAdapter.class);
    when(adapter.query(any(QueryStatement.class), any(TranspileContext.class)))
        .thenReturn(Try.success(RecordList.empty()));
    EnhancedDataModelProxy proxy = createProxy(createDefaultDataModel(adapter));
    Map<String, Object> statement = record(
        "FROM", new QueryStatement.TableSource("external_table", "external"),
        "SELECT", Collections.singletonList(SExpression.constant(1))
    );

    Try<RecordList> result = proxy.query(statement);

    assertTrue(result.isSuccess());
    ArgumentCaptor<QueryStatement> statementCaptor = ArgumentCaptor.forClass(QueryStatement.class);
    verify(adapter).query(statementCaptor.capture(), any(TranspileContext.class));
    assertEquals("demo_user", statementCaptor.getValue().getFrom().getTableName());
    assertEquals("demo.User", statementCaptor.getValue().getFrom().getAlias());
  }

  @Test
  public void queryBySelectAndWhereShouldRouteThroughQueryPluginHook() {
    EnhancedDataModelProxy proxy = createProxy(createCoreDataModel());

    Try<RecordList> result = proxy.query(Collections.singletonList("name"), record("active", true));

    assertTrue(result.isSuccess());
    assertTrue(ProbeDataModelPlugin.CALLS.contains("query"));
    assertSelectContains(lastQueryStatement(), "name");
    assertWhereContains(lastQueryStatement(), "active", true);
  }

  @Test
  public void queryBySelectWhereAndOrderShouldRouteThroughQueryPluginHook() {
    EnhancedDataModelProxy proxy = createProxy(createCoreDataModel());

    Try<RecordList> result = proxy.query(
        Collections.singletonList("name"),
        record("active", true),
        record("name", "ASC")
    );

    assertTrue(result.isSuccess());
    assertTrue(ProbeDataModelPlugin.CALLS.contains("query"));
    assertSelectContains(lastQueryStatement(), "name");
    assertWhereContains(lastQueryStatement(), "active", true);
    assertEquals("name", lastQueryStatement().getOrders().get(0).getColumn());
  }

  @Test
  public void pagedQueryShouldRouteThroughQueryPluginHook() {
    EnhancedDataModelProxy proxy = createProxy(createCoreDataModel());

    Try<RecordList> result = proxy.query(
        Collections.singletonList("name"),
        record("active", true),
        record("name", "ASC"),
        10,
        20
    );

    assertTrue(result.isSuccess());
    assertTrue(ProbeDataModelPlugin.CALLS.contains("query"));
    assertSelectContains(lastQueryStatement(), "name");
    assertWhereContains(lastQueryStatement(), "active", true);
    assertEquals("name", lastQueryStatement().getOrders().get(0).getColumn());
    assertEquals(10L, lastQueryStatement().getOffset());
    assertEquals(20L, lastQueryStatement().getLimit());
  }

  private QueryStatement rootSubqueryStatement(SExpression<Boolean> where) {
    QueryStatement subquery = QueryStatement.builder()
        .from("demo_user", "inner_user")
        .select(SExpression.field("name"))
        .build();
    return QueryStatement.builder()
        .from(subquery, "derived_user")
        .select(SExpression.field("derived_user", "name"))
        .where(where)
        .build();
  }

  private ModelQueryStatement modelSubquery() {
    ModelQueryStatementBuilder builder = new ModelQueryStatementBuilder();
    builder.from("demo_user", "inner_user");
    builder.select(SExpression.field("name"));
    builder.populateClause(PopulateClause.fromRaw(Collections.singletonList("roles")));
    builder.omitClause(OmitClause.fromRaw(Collections.singletonList("secret")));
    return builder.build();
  }

  private EnhancedDataModelProxy createProxy(DataModel coreDataModel) {
    return new EnhancedDataModelProxy(
        coreDataModel,
        Collections.singletonList(new PluginDescriptor("Probe"))
    );
  }

  private QueryStatement lastQueryStatement() {
    return (QueryStatement) ProbeDataModelPlugin.lastQueryStatement;
  }

  private QueryStatement incomingQueryStatement() {
    return (QueryStatement) ProbeDataModelPlugin.incomingQueryStatement;
  }

  private void assertSelectContains(QueryStatement statement, String... fieldNames) {
    for (String fieldName : fieldNames) {
      assertTrue(containsField(statement.getSelect(), fieldName), "SELECT should contain field " + fieldName);
    }
  }

  private void assertWhereContains(QueryStatement statement, String fieldName, Object... values) {
    assertTrue(containsField(statement.getWhere(), fieldName), "WHERE should contain field " + fieldName);
    for (Object value : values) {
      assertTrue(containsConstant(statement.getWhere(), value), "WHERE should contain constant " + value);
    }
  }

  private boolean containsField(Object value, String fieldName) {
    if (value instanceof SExpression<?> expression) {
      if (expression.getOperator() == Operators.FIELD && expression.getParams().contains(fieldName)) {
        return true;
      }
      return expression.getParams().stream().anyMatch(param -> containsField(param, fieldName));
    }
    if (value instanceof QueryStatement statement) {
      return statement.values().stream().anyMatch(item -> containsField(item, fieldName));
    }
    if (value instanceof QueryStatement.TableSource tableSource && tableSource.isSubQuery()) {
      return containsField(tableSource.getSubQuery(), fieldName);
    }
    if (value instanceof Iterable<?> iterable) {
      for (Object item : iterable) {
        if (containsField(item, fieldName)) {
          return true;
        }
      }
    }
    if (value instanceof Map<?, ?> map) {
      return map.values().stream().anyMatch(item -> containsField(item, fieldName));
    }
    return false;
  }

  private boolean containsConstant(Object value, Object expected) {
    if (value instanceof SExpression<?> expression) {
      if (expression.getOperator() == Operators.CONSTANT
          && !expression.getParams().isEmpty()
          && containsConstantValue(expression.getParam(0), expected)) {
        return true;
      }
      return expression.getParams().stream().anyMatch(param -> containsConstant(param, expected));
    }
    if (value instanceof Iterable<?> iterable) {
      for (Object item : iterable) {
        if (containsConstant(item, expected)) {
          return true;
        }
      }
    }
    if (value instanceof Map<?, ?> map) {
      return map.values().stream().anyMatch(item -> containsConstant(item, expected));
    }
    return false;
  }

  private boolean containsConstantValue(Object actual, Object expected) {
    if (actual instanceof Iterable<?> iterable) {
      for (Object item : iterable) {
        if (containsConstantValue(item, expected)) {
          return true;
        }
      }
      return false;
    }
    return java.util.Objects.equals(actual, expected);
  }

  private void clearSpiCache() {
    DataServices.clearCache();
  }

  private DataModel createCoreDataModel() {
    DataModel coreDataModel = mock(DataModel.class);
    DataModelField idField = mock(DataModelField.class);

    when(idField.getName()).thenReturn("id");
    when(idField.getValueType()).thenReturn(castValueType(new LongValue()));

    when(coreDataModel.getField("id")).thenReturn(Optional.of(idField));
    when(coreDataModel.getPrimaryKeys()).thenReturn(Collections.singletonList(idField));
    when(coreDataModel.getFullName()).thenReturn("demo.User");
    when(coreDataModel.getRawName()).thenReturn("demo_user");

    when(coreDataModel.update(any(Object.class), anyMap())).thenReturn(Try.success(1L));
    when(coreDataModel.update(anyList(), anyMap())).thenReturn(Try.success(2L));
    when(coreDataModel.update(anyMap(), anyMap())).thenReturn(Try.success(1L));
    when(coreDataModel.update(any(SExpression.class), anyMap())).thenReturn(Try.success(1L));
    when(coreDataModel.delete(any(Object.class))).thenReturn(Try.success(1L));
    when(coreDataModel.delete(anyList())).thenReturn(Try.success(2L));
    when(coreDataModel.delete(anyMap())).thenReturn(Try.success(1L));
    when(coreDataModel.delete(any(SExpression.class))).thenReturn(Try.success(1L));
    when(coreDataModel.count(any(QueryStatement.class))).thenReturn(Try.success(1L));
    when(coreDataModel.insert(anyMap())).thenAnswer(invocation -> Try.success(Record.of(invocation.getArgument(0, Map.class))));
    when(coreDataModel.batchInsert(anyList())).thenAnswer(invocation -> Try.success(RecordList.of(invocation.getArgument(0, java.util.List.class))));
    when(coreDataModel.insertOrUpdate(anyMap())).thenAnswer(invocation -> Try.success(Record.of(invocation.getArgument(0, Map.class))));
    when(coreDataModel.batchInsertOrUpdate(anyList())).thenAnswer(invocation -> Try.success(RecordList.of(invocation.getArgument(0, java.util.List.class))));
    when(coreDataModel.get(eq(1L))).thenReturn(Try.success(Record.of(record("id", 1L, "name", "before"))));
    when(coreDataModel.get(eq(2L))).thenReturn(Try.success(null));
    when(coreDataModel.query(anyList())).thenReturn(Try.success(RecordList.of(Collections.singletonList(record("id", 1L, "name", "before")))));
    when(coreDataModel.query(anyMap())).thenAnswer(invocation -> {
      Map<String, Object> statement = invocation.getArgument(0, Map.class);
      if (record("id", 2L).equals(statement.get("WHERE"))) {
        return Try.success(RecordList.of(Collections.emptyList()));
      }
      return Try.success(RecordList.of(Collections.singletonList(record("id", 1L, "name", "before"))));
    });
    when(coreDataModel.query(any(QueryStatement.class))).thenAnswer(invocation -> {
      QueryStatement statement = invocation.getArgument(0, QueryStatement.class);
      if (containsConstant(statement.getWhere(), 2L) && !containsConstant(statement.getWhere(), 1L)) {
        return Try.success(RecordList.of(Collections.emptyList()));
      }
      return Try.success(RecordList.of(Collections.singletonList(record("id", 1L, "name", "before"))));
    });

    return coreDataModel;
  }

  private DefaultDataModel createDefaultDataModel(DataAdapter adapter) {
    DataModelMeta meta = mock(DataModelMeta.class);
    when(meta.getName()).thenReturn("User");
    when(meta.getFullName()).thenReturn("demo.User");
    when(meta.getRawName()).thenReturn("demo_user");
    DataModelFieldMeta id = fieldMeta("id", "Long");
    DataModelFieldMeta name = fieldMeta("name", "String");
    when(meta.getFields()).thenReturn(Arrays.asList(id, name));
    when(meta.getPrimaryKeys()).thenReturn(Collections.singletonList("id"));
    DataStation<?> station = mock(DataStation.class);
    when(station.getDataAdapter()).thenReturn(adapter);
    return new DefaultDataModel(meta, station);
  }

  private DataModelFieldMeta fieldMeta(String name, String type) {
    DataModelFieldMeta field = new DataModelFieldMeta();
    field.setName(name);
    field.setRawName(name);
    field.setType(type);
    return field;
  }

  private static Map<String, Object> record(Object... pairs) {
    Map<String, Object> data = new LinkedHashMap<String, Object>();
    for (int i = 0; i < pairs.length; i += 2) {
      data.put((String) pairs[i], pairs[i + 1]);
    }
    return data;
  }

  @SuppressWarnings("unchecked")
  private static <T> ValueType<T> castValueType(ValueType<?> valueType) {
    return (ValueType<T>) valueType;
  }
}
