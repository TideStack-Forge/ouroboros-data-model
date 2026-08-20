package com.ouroboros.data.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import com.ouroboros.data.exception.DataModelException;
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.model.DataModelField;
import com.ouroboros.data.model.MigrationStrategy;
import com.ouroboros.data.model.UniquenessScope;
import com.ouroboros.data.model.ValueType;
import com.ouroboros.data.model.valuetypes.BooleanValue;
import com.ouroboros.data.model.valuetypes.IntegerValue;
import com.ouroboros.data.model.valuetypes.StringValue;

import io.vavr.control.Either;

class UniqueConstraintScopeMigrationTest {

  @Test
  void allToActiveShouldDropGeneratedUniqueConstraintAndAllowDeletedDuplicate() throws Exception {
    var tableName = tableName("codex_scope_drop");
    var dataSource = dataSource(tableName);
    var allRecords = model(tableName, UniquenessScope.ALL_RECORDS);
    var activeRecords = model(tableName, activeScope());
    var uniqueName = generatedUniqueName(tableName);

    assertRight(migrate(dataSource, allRecords));
    assertTrue(uniqueExists(dataSource, tableName, uniqueName));

    var preview = migrationSqls(dataSource, activeRecords);
    assertRight(preview);
    assertTrue(containsSql(preview.get(), "DROP", "CONSTRAINT", uniqueName));

    assertRight(migrate(dataSource, activeRecords));
    assertFalse(uniqueExists(dataSource, tableName, uniqueName));

    insertRow(dataSource, tableName, "A", true);
    insertRow(dataSource, tableName, "A", false);

    assertEquals(2, countRows(dataSource, tableName, "A"));
  }

  @Test
  void activeToAllShouldRecreateGeneratedUniqueConstraintWhenDataIsClean() throws Exception {
    var tableName = tableName("codex_scope_recreate");
    var dataSource = dataSource(tableName);
    var activeRecords = model(tableName, activeScope());
    var allRecords = model(tableName, UniquenessScope.ALL_RECORDS);
    var uniqueName = generatedUniqueName(tableName);

    assertRight(migrate(dataSource, activeRecords));
    assertFalse(uniqueExists(dataSource, tableName, uniqueName));
    insertRow(dataSource, tableName, "A", true);
    insertRow(dataSource, tableName, "B", false);

    var preview = migrationSqls(dataSource, allRecords);
    assertRight(preview);
    assertTrue(containsSql(preview.get(), "ADD", "UNIQUE", uniqueName));

    assertRight(migrate(dataSource, allRecords));

    assertTrue(uniqueExists(dataSource, tableName, uniqueName));
  }

  @Test
  void fieldAllRecordsScopeShouldCreateGeneratedUniqueConstraintInActiveModel() throws Exception {
    var tableName = tableName("codex_scope_field_all");
    var dataSource = dataSource(tableName);
    var activeModelWithFieldAllRecords = model(tableName, activeScope(), UniquenessScope.ALL_RECORDS);
    var uniqueName = generatedUniqueName(tableName);

    var preview = migrationSqls(dataSource, activeModelWithFieldAllRecords);
    assertRight(preview);
    assertTrue(containsSql(preview.get(), "ADD", "UNIQUE", uniqueName));

    assertRight(migrate(dataSource, activeModelWithFieldAllRecords));
    assertTrue(uniqueExists(dataSource, tableName, uniqueName));
  }

  @Test
  void fieldActiveRecordsScopeShouldDropGeneratedUniqueConstraintInAllRecordsModel() throws Exception {
    var tableName = tableName("codex_scope_field_active");
    var dataSource = dataSource(tableName);
    var allRecords = model(tableName, UniquenessScope.ALL_RECORDS);
    var allRecordsWithFieldActive = model(tableName, UniquenessScope.ALL_RECORDS, activeScope());
    var uniqueName = generatedUniqueName(tableName);

    assertRight(migrate(dataSource, allRecords));
    assertTrue(uniqueExists(dataSource, tableName, uniqueName));

    var preview = migrationSqls(dataSource, allRecordsWithFieldActive);
    assertRight(preview);
    assertTrue(containsSql(preview.get(), "DROP", "CONSTRAINT", uniqueName));

    assertRight(migrate(dataSource, allRecordsWithFieldActive));
    assertFalse(uniqueExists(dataSource, tableName, uniqueName));
  }

  @Test
  void activeToAllShouldFailOnDuplicateDataWithoutChangingRows() throws Exception {
    var tableName = tableName("codex_scope_duplicate");
    var dataSource = dataSource(tableName);
    var activeRecords = model(tableName, activeScope());
    var allRecords = model(tableName, UniquenessScope.ALL_RECORDS);
    var uniqueName = generatedUniqueName(tableName);

    assertRight(migrate(dataSource, activeRecords));
    insertRow(dataSource, tableName, "A", true);
    insertRow(dataSource, tableName, "A", false);

    var preview = migrationSqls(dataSource, allRecords);
    assertRight(preview);
    assertTrue(containsSql(preview.get(), "ADD", "UNIQUE", uniqueName));

    var migrated = migrate(dataSource, allRecords);
    assertTrue(migrated.isLeft(), "duplicate historical rows must make add-unique migration fail");

    assertEquals(2, countRows(dataSource, tableName, "A"));
    assertFalse(uniqueExists(dataSource, tableName, uniqueName));
  }

  @Test
  void activeMigrationShouldRetainManualUniqueConstraintOwnedByDba() throws Exception {
    var tableName = tableName("codex_scope_manual");
    var dataSource = dataSource(tableName);
    var manualUniqueName = "uk_" + tableName + "_code_manual";
    var generatedUniqueName = generatedUniqueName(tableName);

    executeSql(dataSource, "CREATE TABLE " + tableName + " ("
        + "id INT AUTO_INCREMENT PRIMARY KEY, "
        + "code VARCHAR(64) NOT NULL, "
        + "is_deleted BIT NOT NULL, "
        + "CONSTRAINT " + manualUniqueName + " UNIQUE (code))");

    var activeRecords = model(tableName, activeScope());
    var preview = migrationSqls(dataSource, activeRecords);
    assertRight(preview);
    assertFalse(containsSql(preview.get(), "DROP", manualUniqueName));

    assertRight(migrate(dataSource, activeRecords));

    assertTrue(uniqueExists(dataSource, tableName, manualUniqueName));
    assertFalse(uniqueExists(dataSource, tableName, generatedUniqueName));
  }

  private static JdbcDataSource dataSource(String tableName) {
    var ds = new JdbcDataSource();
    ds.setURL("jdbc:h2:mem:" + tableName + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
    ds.setUser("sa");
    ds.setPassword("");
    return ds;
  }

  private static Either<DataModelException, Void> migrate(DataSource dataSource, DataModel model) throws SQLException {
    try (var connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);
      return new DatabaseMigration(connection, "", "UniqueConstraintScopeMigrationTest")
          .migrate(Collections.singletonList(model));
    }
  }

  private static Either<DataModelException, List<String>> migrationSqls(DataSource dataSource, DataModel model)
      throws SQLException {
    try (var connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);
      return new DatabaseMigration(connection, "", "UniqueConstraintScopeMigrationTest")
          .getMigrationSqls(Collections.singletonList(model));
    }
  }

  private static void assertRight(Either<DataModelException, ?> result) {
    assertTrue(result.isRight(), () -> result.getLeft().getMessage());
  }

  private static DataModel model(String tableName, UniquenessScope uniquenessScope) {
    return model(tableName, uniquenessScope, null);
  }

  private static DataModel model(String tableName, UniquenessScope uniquenessScope, UniquenessScope fieldScope) {
    var owner = new DataModel[1];
    var id = field(owner, "id", "id", new IntegerValue(), false, false, true, null);
    var code = field(owner, "code", "code", new StringValue(), false, true, false, 64, fieldScope);
    var isDeleted = field(owner, "isDeleted", "is_deleted", new BooleanValue(), false, false, false, null);
    var fields = Arrays.asList(id, code, isDeleted);
    var primaryKeys = Collections.singletonList(id);
    Map<String, Object> extraProps = uniquenessScope == UniquenessScope.ALL_RECORDS
        ? Collections.emptyMap()
        : Collections.singletonMap(UniquenessScope.EXTRA_PROP_NAME, uniquenessScope);

    DataModel model = (DataModel) Proxy.newProxyInstance(
        UniqueConstraintScopeMigrationTest.class.getClassLoader(),
        new Class[]{DataModel.class},
        (proxy, method, args) -> switch (method.getName()) {
          case "getVersion" -> "1";
          case "getSource" -> null;
          case "getNamespace" -> "demo";
          case "getName", "getRawName" -> tableName;
          case "getFullName" -> "demo." + tableName;
          case "getLabel" -> tableName;
          case "getDescription" -> null;
          case "getMigrationStrategy" -> MigrationStrategy.AUTO;
          case "getExtraProps" -> extraProps;
          case "getExtraProp" -> Optional.ofNullable(extraProps.get(String.valueOf(args[0])));
          case "getFields" -> fields;
          case "getField" -> fields.stream()
              .filter(field -> field.getName().equals(String.valueOf(args[0])))
              .findFirst();
          case "getPrimaryKeys" -> primaryKeys;
          default -> defaultValue(method.getReturnType());
        });
    owner[0] = model;
    return model;
  }

  private static DataModelField field(DataModel[] owner, String name, String rawName, ValueType<?> valueType,
                                      boolean nullable, boolean unique, boolean autoIncrement, Integer size) {
    return field(owner, name, rawName, valueType, nullable, unique, autoIncrement, size, null);
  }

  private static DataModelField field(DataModel[] owner, String name, String rawName, ValueType<?> valueType,
                                      boolean nullable, boolean unique, boolean autoIncrement, Integer size,
                                      UniquenessScope uniquenessScope) {
    return (DataModelField) Proxy.newProxyInstance(
        UniqueConstraintScopeMigrationTest.class.getClassLoader(),
        new Class[]{DataModelField.class},
        (proxy, method, args) -> switch (method.getName()) {
          case "getName" -> name;
          case "getRawName" -> rawName;
          case "getLabel" -> name;
          case "getDescription", "getRawType" -> null;
          case "getType" -> valueType.getName();
          case "getValueType" -> valueType;
          case "getDefaultValue" -> null;
          case "getRules" -> Collections.emptyList();
          case "getDecimalDigits" -> null;
          case "getSize" -> size;
          case "getIsNullable" -> nullable;
          case "getIsUnsigned" -> false;
          case "getIsUnique" -> unique;
          case "getUniquenessScope" -> uniquenessScope;
          case "getIsAutoIncrement" -> autoIncrement;
          case "getExtraProps" -> Collections.emptyMap();
          case "getExtraProp" -> Optional.empty();
          case "getDataModel" -> owner[0];
          default -> defaultValue(method.getReturnType());
        });
  }

  private static Object defaultValue(Class<?> returnType) {
    if (returnType == Boolean.TYPE) {
      return false;
    }
    if (returnType == Integer.TYPE) {
      return 0;
    }
    if (returnType == Long.TYPE) {
      return 0L;
    }
    return null;
  }

  private static UniquenessScope activeScope() {
    return UniquenessScope.ACTIVE_RECORDS;
  }

  private static String tableName(String prefix) {
    return prefix + "_" + Long.toString(System.nanoTime(), 36);
  }

  private static String generatedUniqueName(String tableName) {
    return "un_demo_" + tableName + "__code";
  }

  private static boolean containsSql(List<String> sqls, String... tokens) {
    return sqls.stream().anyMatch(sql -> {
      var normalized = sql.toUpperCase(Locale.ROOT);
      return Arrays.stream(tokens)
          .map(token -> token.toUpperCase(Locale.ROOT))
          .allMatch(normalized::contains);
    });
  }

  private static boolean uniqueExists(DataSource dataSource, String tableName, String constraintName)
      throws SQLException {
    try (var connection = dataSource.getConnection();
         var statement = connection.prepareStatement(
             "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS "
                 + "WHERE UPPER(TABLE_NAME) = UPPER(?) "
                 + "AND UPPER(CONSTRAINT_NAME) = UPPER(?) "
                 + "AND CONSTRAINT_TYPE = 'UNIQUE'")) {
      statement.setString(1, tableName);
      statement.setString(2, constraintName);
      try (var rs = statement.executeQuery()) {
        rs.next();
        return rs.getInt(1) > 0;
      }
    }
  }

  private static void insertRow(DataSource dataSource, String tableName, String code, boolean isDeleted)
      throws SQLException {
    try (var connection = dataSource.getConnection();
         var statement = connection.prepareStatement(
             "INSERT INTO " + tableName + " (code, is_deleted) VALUES (?, ?)")) {
      statement.setString(1, code);
      statement.setBoolean(2, isDeleted);
      statement.executeUpdate();
    }
  }

  private static int countRows(DataSource dataSource, String tableName, String code) throws SQLException {
    try (var connection = dataSource.getConnection();
         var statement = connection.prepareStatement("SELECT COUNT(*) FROM " + tableName + " WHERE code = ?")) {
      statement.setString(1, code);
      try (var rs = statement.executeQuery()) {
        rs.next();
        return rs.getInt(1);
      }
    }
  }

  private static void executeSql(DataSource dataSource, String sql) throws SQLException {
    try (Connection connection = dataSource.getConnection();
         var statement = connection.createStatement()) {
      statement.execute(sql);
    }
  }
}
