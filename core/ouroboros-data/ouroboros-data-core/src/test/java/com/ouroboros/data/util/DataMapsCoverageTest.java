package com.ouroboros.data.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;

class DataMapsCoverageTest {

  @Test
  void tryToBeanConvertsBooleanValuesToIntegerFields() {
    var falseBean = DataMaps.tryToBean(IntegerFlagBean.class, Map.of("isDeleted", false)).get();
    var trueBean = DataMaps.tryToBean(IntegerFlagBean.class, Map.of("isDeleted", true)).get();

    assertEquals(0, falseBean.getIsDeleted());
    assertEquals(1, trueBean.getIsDeleted());
  }

  @Test
  void tryToBeanConvertsJdbcTimestampValuesToLocalDateTimeFields() {
    var timestamp = Timestamp.valueOf("2026-07-24 16:50:40");

    var bean = DataMaps.tryToBean(LocalDateTimeBean.class, Map.of("createdAt", timestamp)).get();

    assertEquals(LocalDateTime.of(2026, 7, 24, 16, 50, 40), bean.getCreatedAt());
  }

  @Test
  void tryToBeanKeepsLocalDateTimeFieldValues() {
    var dateTime = LocalDateTime.of(2026, 7, 24, 16, 50, 40);

    var bean = DataMaps.tryToBean(LocalDateTimeBean.class, Map.of("createdAt", dateTime)).get();

    assertEquals(dateTime, bean.getCreatedAt());
  }

  @Test
  void tryToBeanParsesJsonStringValuesForComplexFields() {
    var bean = DataMaps.tryToBean(FormDefinitionBean.class, Map.of(
        "fields", "[{\"name\":\"requestNo\",\"required\":true,\"params\":{\"mode\":\"auto\"}}]",
        "options", "{\"visible\":true}"
    )).get();

    assertEquals(1, bean.getFields().size());
    assertEquals("requestNo", bean.getFields().get(0).getName());
    assertEquals(Boolean.TRUE, bean.getFields().get(0).getRequired());
    assertEquals("auto", bean.getFields().get(0).getParams().get("mode"));
    assertEquals(Boolean.TRUE, bean.getOptions().get("visible"));
  }

  @Test
  void tryToBeanFailsWhenJsonStringFieldCannotBeDeserialized() {
    var result = DataMaps.tryToBean(FormDefinitionBean.class, Map.of("fields", "[{not-json}]"));

    assertTrue(result.isFailure());
  }

  @Test
  void repeatedConcurrentConversionsKeepInheritedAndSetterOnlyJsonPropertiesIsolated() throws Exception {
    try (var pool = Executors.newFixedThreadPool(8)) {
      var tasks = new ArrayList<Callable<Void>>();
      for (int index = 0; index < 1000; index++) {
        int identity = index;
        tasks.add(() -> {
          var value = DataMaps.tryToBean(InheritedBean.class, Map.of(
              "fields", "[{\"name\":\"field-" + identity + "\"}]", "options", "{\"number\":" + identity + "}",
              "supplement", "{\"number\":" + identity + "}")).get();
          assertEquals("field-" + identity, value.getFields().getFirst().getName());
          assertEquals(identity, value.getOptions().get("number"));
          assertEquals(identity, value.supplementValue().get("number"));
          var other = DataMaps.tryToBean(StringFieldsBean.class, Map.of("fields", "literal-" + identity)).get();
          assertEquals("literal-" + identity, other.getFields());
          value.getOptions().put("local", true);
          var next = DataMaps.tryToBean(InheritedBean.class, Map.of("options", "{\"number\":0}")).get();
          assertEquals(Map.of("number", 0), next.getOptions());
          assertTrue(DataMaps.tryToBean(InheritedBean.class, Map.of("options", "{broken}")).isFailure());
          return null;
        });
      }
      for (var result : pool.invokeAll(tasks)) result.get();
    }
  }

  static class InheritedBean extends FormDefinitionBean {
    private Map<String, Object> extra;

    public void setSupplement(Map<String, Object> value) { extra = value; }
    Map<String, Object> supplementValue() { return extra; }
  }

  static class StringFieldsBean {
    private String fields;
    public String getFields() { return fields; }
    public void setFields(String value) { fields = value; }
  }

  static class IntegerFlagBean {
    private Integer isDeleted;

    public Integer getIsDeleted() {
      return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) {
      this.isDeleted = isDeleted;
    }
  }

  static class LocalDateTimeBean {
    private LocalDateTime createdAt;

    public LocalDateTime getCreatedAt() {
      return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
    }
  }

  static class FormDefinitionBean {
    private List<FormFieldBean> fields;
    private Map<String, Object> options;

    public List<FormFieldBean> getFields() {
      return fields;
    }

    public void setFields(List<FormFieldBean> fields) {
      this.fields = fields;
    }

    public Map<String, Object> getOptions() {
      return options;
    }

    public void setOptions(Map<String, Object> options) {
      this.options = options;
    }
  }

  static class FormFieldBean {
    private String name;
    private Boolean required;
    private Map<String, Object> params;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public Boolean getRequired() {
      return required;
    }

    public void setRequired(Boolean required) {
      this.required = required;
    }

    public Map<String, Object> getParams() {
      return params;
    }

    public void setParams(Map<String, Object> params) {
      this.params = params;
    }
  }
}
