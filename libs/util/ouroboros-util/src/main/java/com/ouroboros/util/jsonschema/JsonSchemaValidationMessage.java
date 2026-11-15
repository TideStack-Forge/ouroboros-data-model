package com.ouroboros.util.jsonschema;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class JsonSchemaValidationMessage {
  private final String keyword;
  private final String message;
  private final String instanceLocation;
  private final String evaluationPath;
  private final String schemaLocation;
  private final Map<String, Object> details;

  public JsonSchemaValidationMessage(
      String keyword,
      String message,
      String instanceLocation,
      String evaluationPath,
      String schemaLocation,
      Map<String, Object> details) {
    this.keyword = keyword;
    this.message = message;
    this.instanceLocation = instanceLocation;
    this.evaluationPath = evaluationPath;
    this.schemaLocation = schemaLocation;
    this.details = Collections.unmodifiableMap(details == null ? new LinkedHashMap<>() : new LinkedHashMap<>(details));
  }

  public String getKeyword() {
    return keyword;
  }

  public String getMessage() {
    return message;
  }

  public String getInstanceLocation() {
    return instanceLocation;
  }

  public String getEvaluationPath() {
    return evaluationPath;
  }

  public String getSchemaLocation() {
    return schemaLocation;
  }

  public Map<String, Object> getDetails() {
    return details;
  }
}
