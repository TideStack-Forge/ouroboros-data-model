package com.ouroboros.util.jsonschema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class JsonSchemaValidationResult {
  private final boolean valid;
  private final List<JsonSchemaValidationMessage> messages;

  public JsonSchemaValidationResult(boolean valid, List<JsonSchemaValidationMessage> messages) {
    this.valid = valid;
    this.messages = Collections.unmodifiableList(messages == null ? new ArrayList<>() : new ArrayList<>(messages));
  }

  public static JsonSchemaValidationResult valid() {
    return new JsonSchemaValidationResult(true, Collections.emptyList());
  }

  public static JsonSchemaValidationResult invalid(JsonSchemaValidationMessage message) {
    return new JsonSchemaValidationResult(false, Collections.singletonList(message));
  }

  public boolean isValid() {
    return valid;
  }

  public List<JsonSchemaValidationMessage> getMessages() {
    return messages;
  }

  public String toMessageString() {
    return messages.stream()
        .map(JsonSchemaValidationResult::formatMessage)
        .filter(message -> message != null && !message.trim().isEmpty())
        .collect(Collectors.joining("; "));
  }

  private static String formatMessage(JsonSchemaValidationMessage message) {
    if (message == null) {
      return null;
    }
    StringBuilder builder = new StringBuilder();
    append(builder, trimToNull(message.getKeyword()), "");
    append(builder, trimToNull(message.getInstanceLocation()), " @ ");
    append(builder, trimToNull(message.getMessage()), ": ");
    return builder.toString();
  }

  private static void append(StringBuilder builder, String value, String separator) {
    if (value == null) {
      return;
    }
    if (builder.length() > 0) {
      builder.append(separator);
    }
    builder.append(value);
  }

  private static String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
