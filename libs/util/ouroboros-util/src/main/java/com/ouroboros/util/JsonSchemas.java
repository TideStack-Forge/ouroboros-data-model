package com.ouroboros.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SpecificationVersion;
import com.ouroboros.util.jsonschema.JsonSchemaValidationMessage;
import com.ouroboros.util.jsonschema.JsonSchemaValidationResult;
import io.vavr.control.Try;

public class JsonSchemas {
  private static final Map<SpecificationVersion, SchemaRegistry> REGISTRY_BY_VERSION = buildRegistryByVersion();

  public static Boolean validate(Map<String, Object> jsonSchema, Object data) {
    return validateResult(jsonSchema, data).isValid();
  }

  public static Boolean validate(String jsonSchema, Object data) {
    return validateResult(jsonSchema, data).isValid();
  }

  public static JsonSchemaValidationResult validateResult(Map<String, Object> jsonSchema, Object data) {
    return compileSchema(toSchemaNode(jsonSchema))
        .map(schema -> validate(schema, data))
        .getOrElseGet(JsonSchemas::invalidSchemaResult);
  }

  public static JsonSchemaValidationResult validateResult(String jsonSchema, Object data) {
    return parseSchemaNode(jsonSchema)
        .flatMap(JsonSchemas::compileSchema)
        .map(schema -> validate(schema, data))
        .getOrElseGet(JsonSchemas::invalidSchemaResult);
  }

  public static Function<Object, Boolean> buildValidator(Map<String, Object> jsonSchema) {
    final var schema = compileSchema(toSchemaNode(jsonSchema));
    return data -> schema.map(compiled -> validate(compiled, data).isValid()).getOrElse(false);
  }

  public static Function<Object, Boolean> buildValidator(String jsonSchema) {
    final var schema = parseSchemaNode(jsonSchema).flatMap(JsonSchemas::compileSchema);
    return data -> schema.map(compiled -> validate(compiled, data).isValid()).getOrElse(false);
  }

  public static Function<Object, Try<Void>> buildValidatorTry(Map<String, Object> jsonSchema) {
    final var schema = compileSchema(toSchemaNode(jsonSchema)).getOrNull();
    if (schema == null) {
      return null;
    }
    return data -> Try.run(() -> {
      var result = validate(schema, data);
      if (!result.isValid()) {
        throw new IllegalArgumentException(result.toMessageString());
      }
    });
  }

  public static Function<Object, Try<Void>> buildValidatorTry(String jsonSchema) {
    final var schema = parseSchemaNode(jsonSchema).flatMap(JsonSchemas::compileSchema).getOrNull();
    if (schema == null) {
      return null;
    }
    return data -> Try.run(() -> {
      var result = validate(schema, data);
      if (!result.isValid()) {
        throw new IllegalArgumentException(result.toMessageString());
      }
    });
  }

  public static boolean isValidSchema(Map<String, Object> jsonSchema) {
    if (jsonSchema == null) {
      return false;
    }
    JsonNode schemaNode = toSchemaNode(jsonSchema);
    SpecificationVersion version = detectVersion(schemaNode);
    return Try.of(() -> {
      SchemaRegistry registry = REGISTRY_BY_VERSION.get(version);
      Schema metaSchema = registry.getSchema(SchemaLocation.of(version.getDialectId()));
      return validate(metaSchema, jsonSchema).isValid() && compileSchema(schemaNode).isSuccess();
    }).getOrElse(false);
  }

  private static Map<SpecificationVersion, SchemaRegistry> buildRegistryByVersion() {
    var registries = new EnumMap<SpecificationVersion, SchemaRegistry>(SpecificationVersion.class);
    for (var version : SpecificationVersion.values()) {
      registries.put(version, SchemaRegistry.withDefaultDialect(version));
    }
    return Collections.unmodifiableMap(registries);
  }

  private static JsonNode toSchemaNode(Map<String, Object> jsonSchema) {
    return Json.objectMapper.valueToTree(jsonSchema);
  }

  private static Try<JsonNode> parseSchemaNode(String jsonSchema) {
    return Try.of(() -> Json.objectMapper.readTree(jsonSchema));
  }

  private static Try<Schema> compileSchema(JsonNode schemaNode) {
    return Try.of(() -> REGISTRY_BY_VERSION.get(detectVersion(schemaNode))
        .getSchema(Json.objectMapper.writeValueAsString(schemaNode), InputFormat.JSON));
  }

  private static JsonSchemaValidationResult validate(Schema schema, Object data) {
    if (schema.getSchemaNode().isObject() && schema.getSchemaNode().isEmpty()) {
      return JsonSchemaValidationResult.valid();
    }
    return Try.of(() -> schema.validate(Json.objectMapper.writeValueAsString(data), InputFormat.JSON))
        .map(JsonSchemas::toResult)
        .getOrElseGet(JsonSchemas::invalidSchemaResult);
  }

  private static JsonSchemaValidationResult toResult(List<Error> errors) {
    if (errors == null || errors.isEmpty()) {
      return JsonSchemaValidationResult.valid();
    }
    List<JsonSchemaValidationMessage> messages = new ArrayList<>();
    for (var error : errors) {
      messages.add(toMessage(error));
    }
    return new JsonSchemaValidationResult(false, messages);
  }

  private static JsonSchemaValidationMessage toMessage(Error error) {
    return new JsonSchemaValidationMessage(
        error.getKeyword(),
        error.getMessage(),
        error.getInstanceLocation() == null ? null : error.getInstanceLocation().toString(),
        error.getEvaluationPath() == null ? null : error.getEvaluationPath().toString(),
        error.getSchemaLocation() == null ? null : error.getSchemaLocation().toString(),
        error.getDetails() == null ? Collections.emptyMap() : new LinkedHashMap<>(error.getDetails()));
  }

  private static JsonSchemaValidationResult invalidSchemaResult(Throwable throwable) {
    return JsonSchemaValidationResult.invalid(new JsonSchemaValidationMessage(
        "$schema",
        throwable == null ? "JSON Schema 无效" : throwable.getMessage(),
        null,
        null,
        null,
        Collections.emptyMap()));
  }

  static SpecificationVersion detectVersion(JsonNode schemaNode) {
    if (schemaNode == null || !schemaNode.has("$schema")) {
      return SpecificationVersion.DRAFT_7;
    }
    String schemaDialect = schemaNode.path("$schema").asText(null);
    return SpecificationVersion.fromDialectId(schemaDialect).orElse(SpecificationVersion.DRAFT_7);
  }
}
