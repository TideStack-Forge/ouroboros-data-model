package com.ouroboros.data.model.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.ouroboros.data.dsl.statement.DMLStatement;
import com.ouroboros.data.dsl.statement.InsertStatement;
import com.ouroboros.data.dsl.statement.QueryStatement;
import com.ouroboros.data.model.DataModelMeta;
import com.ouroboros.data.record.RecordList;

class AdditionalPolicyCoverageTest {

  @Test
  void additionalPolicyDefaultMethodsApplyAllEffectorsInOrder() {
    var parameters = new LinkedHashMap<String, Object>();
    parameters.put("flag", true);

    var meta = new DataModelMeta();
    meta.setName("user");

    var originalDml = InsertStatement.of("user", Collections.<String, Object>singletonMap("name", "alice"));
    var transformedDml = InsertStatement.of("user_archive", Collections.<String, Object>singletonMap("name", "alice"));

    var originalQuery = QueryStatement.builder().from("user", "u").build();
    var transformedQuery = QueryStatement.builder().from("user_archive", "ua").build();

    var originalResults = RecordList.empty();
    var transformedResults = RecordList.of(Collections.singletonList(Collections.<String, Object>singletonMap("name", "alice")));

    var policy = new TrackingPolicy(parameters, transformedDml, transformedQuery, transformedResults);

    policy.applyDDLEffectors(meta);
    assertEquals(Boolean.TRUE, meta.getExtraProp("ddl-applied").orElse(null));

    assertSame(transformedDml, policy.applyDmlEffectors(meta, originalDml));
    assertSame(transformedQuery, policy.applyDQLBeforeEffectors(meta, originalQuery));
    assertSame(transformedResults, policy.applyDQLAfterEffectors(meta, originalQuery, originalResults));
  }

  private static final class TrackingPolicy implements AdditionalPolicy {
    private final Map<String, ?> parameters;
    private final DMLStatement transformedDml;
    private final QueryStatement transformedQuery;
    private final RecordList transformedResults;

    private TrackingPolicy(Map<String, ?> parameters,
                           DMLStatement transformedDml,
                           QueryStatement transformedQuery,
                           RecordList transformedResults) {
      this.parameters = parameters;
      this.transformedDml = transformedDml;
      this.transformedQuery = transformedQuery;
      this.transformedResults = transformedResults;
    }

    @Override
    public String getName() {
      return "tracking";
    }

    @Override
    public Map<String, ?> getParameters() {
      return parameters;
    }

    @Override
    public List<DDLEffector> getDDLEffectors() {
      return Collections.singletonList(new DDLEffector() {
        @Override
        public String getPolicyName() {
          return "tracking";
        }

        @Override
        public void accept(DataModelMeta rawMeta, Map<String, ?> params) {
          rawMeta.setExtraProp("ddl-applied", params.get("flag"));
        }
      });
    }

    @Override
    public List<DMLEffector> getDMLEffectors() {
      return Arrays.asList(
          new DMLEffector() {
            @Override
            public String getPolicyName() {
              return "tracking";
            }

            @Override
            public DMLStatement apply(DataModelMeta meta, Map<String, ?> params, DMLStatement rawClause) {
              return rawClause;
            }
          },
          new DMLEffector() {
            @Override
            public String getPolicyName() {
              return "tracking";
            }

            @Override
            public DMLStatement apply(DataModelMeta meta, Map<String, ?> params, DMLStatement rawClause) {
              return transformedDml;
            }
          });
    }

    @Override
    public List<DQLEffector> getDQLEffectors() {
      return Arrays.asList(
          new DQLEffector() {
            @Override
            public String getPolicyName() {
              return "tracking";
            }

            @Override
            public QueryStatement before(DataModelMeta meta, Map<String, ?> params, QueryStatement rawQuery) {
              return transformedQuery;
            }

            @Override
            public RecordList after(DataModelMeta meta, Map<String, ?> params, QueryStatement query, RecordList rawResults) {
              return rawResults;
            }
          },
          new DQLEffector() {
            @Override
            public String getPolicyName() {
              return "tracking";
            }

            @Override
            public QueryStatement before(DataModelMeta meta, Map<String, ?> params, QueryStatement rawQuery) {
              return rawQuery;
            }

            @Override
            public RecordList after(DataModelMeta meta, Map<String, ?> params, QueryStatement query, RecordList rawResults) {
              return transformedResults;
            }
          });
    }
  }
}
