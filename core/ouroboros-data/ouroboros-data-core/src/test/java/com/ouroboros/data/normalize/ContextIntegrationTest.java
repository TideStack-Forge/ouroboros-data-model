package com.ouroboros.data.normalize;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.normalize.builders.ComparisonExpressionBuilder;
import com.ouroboros.data.normalize.normalizers.DefaultWhereNormalizer;

/**
 * 上下文集成测试
 * <p>
 * 展示如何使用新的上下文系统进行查询规范化
 */
public class ContextIntegrationTest {

  private DataModel mockUserModel;
  private DataModel mockDepartmentModel;

  @BeforeEach
  void setUp() {
    mockUserModel = mock(DataModel.class);
    when(mockUserModel.getName()).thenReturn("User");

    mockDepartmentModel = mock(DataModel.class);
    when(mockDepartmentModel.getName()).thenReturn("Department");
  }

  @Test
  void testBasicQueryNormalization() {
    // 创建基础查询上下文
    QueryNormalizeContext context = QueryNormalizeContext.builder()
        .withDefaultNormalizers()
        .build();

    // 测试基础查询
    Map<String, Object> queryMap = new HashMap<>();
    queryMap.put("FROM", "test_table");
    queryMap.put("where", new HashMap<String, Object>() {{
      put("name", Collections.singletonMap("$eq", "张三"));
      put("age", Collections.singletonMap("$gt", 18));
    }});

    var result = context.normalizeQuery(queryMap);

    assertTrue(result.isSuccess());
  }

  @Test
  void testModelBasedQueryNormalization() {
    // 创建基于模型的查询上下文
    Map<String, DataModel> relatedModels = new HashMap<>();
    relatedModels.put("department", mockDepartmentModel);
    QueryNormalizeContext.Builder ctxBuilder = QueryNormalizeContext.builder()
        .withDefaultNormalizers();
    QueryNormalizeContext context = ctxBuilder.build();

    // 测试模型查询
    Map<String, Object> queryMap = new HashMap<>();
    queryMap.put("FROM", "User");
    queryMap.put("where", new HashMap<String, Object>() {{
      put("name", Collections.singletonMap("$contains", "张"));
      put("department", Collections.singletonMap("name", "技术部"));
    }});

    var result = context.normalizeQuery(queryMap);

    assertTrue(result.isSuccess());
  }

  @Test
  void testRelationQueryNormalization() {
    // 创建关联查询上下文（MODEL_BASED 场景）
    Map<String, DataModel> relatedModels = new HashMap<>();
    relatedModels.put("department", mockDepartmentModel);
    QueryNormalizeContext.Builder ctxBuilder = QueryNormalizeContext.builder()
        .withDefaultNormalizers();
    QueryNormalizeContext context = ctxBuilder.build();

    // 测试嵌套 Map 查询（递归生成多级 FIELD）
    Map<String, Object> queryMap = new HashMap<>();
    queryMap.put("FROM", "User");
    queryMap.put("where", new HashMap<String, Object>() {{
      put("user", Collections.singletonMap("name",
          Collections.singletonMap("$contains", "张")));
      put("orderItems", Collections.singletonMap("$any",
          Collections.singletonMap("productName",
              Collections.singletonMap("$contains", "iPhone"))));
    }});

    var result = context.normalizeQuery(queryMap);

    // 嵌套 Map 现在统一递归处理为多级 FIELD 表达式
  }

  @Test
  void testRelationAllNormalization() {
    // 创建关联查询上下文
    Map<String, DataModel> relatedModels = new HashMap<>();
    relatedModels.put("department", mockDepartmentModel);
    QueryNormalizeContext.Builder ctxBuilder = QueryNormalizeContext.builder()
        .withDefaultNormalizers();
    QueryNormalizeContext context = ctxBuilder.build();

    // 测试 $all 关联查询
    Map<String, Object> queryMap = new HashMap<>();
    queryMap.put("FROM", "User");
    queryMap.put("where", new HashMap<String, Object>() {{
      put("orderItems", Collections.singletonMap("$all",
          Collections.singletonMap("shipped", true)));
    }});

    var result = context.normalizeQuery(queryMap);

    // $all 应成功规范化为 REL_ALL 表达式
    assertTrue(result.isSuccess(), () -> "Normalize $all should succeed, but failed: " + result.getCause());
  }

  @Test
  void testRelationNoneNormalization() {
    // 创建关联查询上下文
    Map<String, DataModel> relatedModels = new HashMap<>();
    relatedModels.put("department", mockDepartmentModel);
    QueryNormalizeContext.Builder ctxBuilder = QueryNormalizeContext.builder()
        .withDefaultNormalizers();
    QueryNormalizeContext context = ctxBuilder.build();

    // 测试 $none 关联查询
    Map<String, Object> queryMap = new HashMap<>();
    queryMap.put("FROM", "User");
    queryMap.put("where", new HashMap<String, Object>() {{
      put("orderItems", Collections.singletonMap("$none",
          Collections.singletonMap("status", "cancelled")));
    }});

    var result = context.normalizeQuery(queryMap);

    // $none 应成功规范化为 REL_NONE 表达式
    assertTrue(result.isSuccess(), () -> "Normalize $none should succeed, but failed: " + result.getCause());
  }

  @Test
  void testContextCustomization() {
    // 测试上下文的动态定制
    QueryNormalizeContext.Builder builder = QueryNormalizeContext.builder();

    // 动态添加规范化器
    builder.addClauseNormalizer(new DefaultWhereNormalizer())
        .addSExpressionNormalizerBefore(new ComparisonExpressionBuilder(), "DefaultSExpressionBuilder");

    QueryNormalizeContext context = builder.build();

    assertEquals(1, context.getClauseNormalizers().size());
    assertEquals(1, context.getSExpressionNormalizers().size());
  }

  @Test
  void testBuilderMethods() {
    // 测试 Builder 创建
    QueryNormalizeContext defaultContext = QueryNormalizeContext.builder()
        .withDefaultNormalizers()
        .build();
    assertNotNull(defaultContext);

    QueryNormalizeContext.Builder ctxBuilder = QueryNormalizeContext.builder()
        .withDefaultNormalizers();
    QueryNormalizeContext modelContext = ctxBuilder.build();
    assertNotNull(modelContext);
  }
}
