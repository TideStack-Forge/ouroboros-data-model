package com.ouroboros.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ouroboros.util.jsonschema.JsonSchemaValidationResult;

public class JsonSchemasTest {
  @Test
  public void unconstrainedSchemasAcceptNativeResourcesWithoutReadingOrSerializingThem() {
    var resource = new java.io.ByteArrayInputStream(new byte[] {1, 2, 3});
    var input = java.util.Map.<String, Object>of("file", resource);
    assertTrue(JsonSchemas.validate(java.util.Map.of(), input));
    assertTrue(JsonSchemas.validate("{}", input));
    assertTrue(JsonSchemas.buildValidator(java.util.Map.of()).apply(input));
    assertTrue(JsonSchemas.buildValidatorTry(java.util.Map.of()).apply(input).isSuccess());
    assertTrue(resource.available() == 3);
    assertFalse(JsonSchemas.validate(java.util.Map.of("type", "object", "required", java.util.List.of("id")), input));
  }

  @Test
  public void validateResultReturnsStructuredErrors() {
    var schema = Json.toMap("{\n" +
        "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" +
        "  \"type\": \"object\",\n" +
        "  \"properties\": {\"name\": {\"type\": \"string\"}},\n" +
        "  \"required\": [\"name\"]\n" +
        "}");
    var payload = Json.toMap("{\"age\": 1}");

    JsonSchemaValidationResult result = JsonSchemas.validateResult(schema, payload);

    assertFalse(result.isValid());
    assertFalse(result.getMessages().isEmpty());
    assertTrue(result.toMessageString().contains("required"));
    assertTrue(result.toMessageString().contains("name"));
  }

  @Test
  public void validateKeepsBooleanCompatibility() {
    var schema = Json.toMap("{\"type\":\"integer\"}");

    assertTrue(JsonSchemas.validate(schema, Integer.valueOf(1)));
    assertFalse(JsonSchemas.validate(schema, "1"));
  }

  @Test
  public void isValidSchemaChecksTheSelectedJsonSchemaDialect() {
    assertTrue(JsonSchemas.isValidSchema(Json.toMap("{\"type\":\"object\"}")));
    assertFalse(JsonSchemas.isValidSchema(Json.toMap("{\"type\":\"not-a-json-schema-type\"}")));
  }
}
