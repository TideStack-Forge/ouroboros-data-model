package com.ouroboros.data.model;

import java.util.*;
import java.util.stream.Collectors;

public class ModelMetaBuilder {

  private final DataModelMeta modelMeta;

  private ModelMetaBuilder() {
    modelMeta = new DataModelMeta();
  }

  public static ModelMetaBuilder create(String modelName) {
    return new ModelMetaBuilder().name(modelName);
  }

  private ModelMetaBuilder name(String name) {
    modelMeta.setName(name);
    return this;
  }

  public ModelMetaBuilder table(String name) {
    modelMeta.setRawName(name);
    return this;
  }

  public ModelMetaBuilder rawName(String rawName) {
    modelMeta.setRawName(rawName);
    return this;
  }

  public ModelMetaBuilder label(String label) {
    modelMeta.setLabel(label);
    return this;
  }

  public ModelMetaBuilder description(String description) {
    modelMeta.setDescription(description);
    return this;
  }

  public ModelMetaBuilder withFields(DataModelFieldMeta... fields) {
    modelMeta.setFields(Arrays.asList(fields));
    return this;
  }

  public ModelMetaBuilder withFields(Collection<DataModelFieldMeta> fields) {
    modelMeta.setFields(fields.stream().collect(Collectors.toList()));
    return this;
  }

  public FieldsBuilder fields() {
    return new FieldsBuilder();
  }

  public ModelMetaBuilder primaryKey(String... primaryKey) {
    modelMeta.getPrimaryKeys().addAll(Arrays.asList(primaryKey));
    return this;
  }

  public ModelMetaBuilder primaryKey(DataModelFieldMeta... primaryKey) {
    modelMeta.getPrimaryKeys().addAll(Arrays.asList(primaryKey).stream().map(DataModelFieldMeta::getName).collect(Collectors.toList()));
    return this;
  }

  public ModelMetaBuilder uniqueConstraint(String name, String... fields) {
    return uniqueConstraint(name, null, fields);
  }

  public ModelMetaBuilder uniqueConstraint(String name, UniquenessScope scope, String... fields) {
    var uniqueConstraint = new DataModelUniqueConstraintMeta();
    uniqueConstraint.setName(name);
    uniqueConstraint.setScope(scope);
    uniqueConstraint.setFields(Arrays.asList(fields));
    modelMeta.addUniqueConstraint(uniqueConstraint);
    return this;
  }

  public ModelMetaBuilder extraProps(String key, Object value) {
    modelMeta.getExtraProps().put(key, value);
    return this;
  }

  public ModelMetaBuilder extraProps(Map<String, Object> extraProps) {
    modelMeta.getExtraProps().putAll(extraProps);
    return this;
  }

  public ModelMetaBuilder primaryKeyGenerator(String primaryKeyGenerator) {
    modelMeta.setPrimaryKeyGenerator(primaryKeyGenerator);
    return this;
  }

  public ModelMetaBuilder migrationStrategy(MigrationStrategy migrationStrategy) {
    modelMeta.setMigrationStrategy(migrationStrategy);
    return this;
  }

  public DataModelMeta build() {
    return modelMeta;
  }

  public class FieldsBuilder {
    List<DataModelFieldMeta> fields = new ArrayList<>();

    public FieldBuilder field(String name) {
      return new FieldBuilder(name);
    }

    public FieldBuilder integerField(String name) {
      return field(name).type("Integer");
    }

    public FieldBuilder stringField(String name) {
      return field(name).type("String");
    }

    public FieldBuilder longField(String name) {
      return field(name).type("Long");
    }

    public FieldBuilder doubleField(String name) {
      return field(name).type("Double");
    }

    public FieldBuilder floatField(String name) {
      return field(name).type("Float");
    }

    public FieldBuilder booleanField(String name) {
      return field(name).type("Boolean");
    }

    public FieldBuilder dateField(String name) {
      return field(name).type("Date");
    }

    public FieldBuilder dateTimeField(String name) {
      return field(name).type("DateTime");
    }

    public FieldBuilder timeField(String name) {
      return field(name).type("Time");
    }

    public FieldBuilder jsonField(String name) {
      return field(name).type("Json");
    }

    public FieldBuilder listField(String name) {
      return field(name).type("List");
    }

    public FieldBuilder mapField(String name) {
      return field(name).type("Map");
    }

    public FieldBuilder modelField(String name, String model, String referenceKey) {
      return field(name).type("Model").extraProp("model", model).extraProp("referenceKey", referenceKey);
    }

    public FieldBuilder collectionField(String name, String model, String referenceKey) {
      return field(name).type("Collection").extraProp("model", model).extraProp("referenceKey", referenceKey);
    }

    public FieldsBuilder add(DataModelFieldMeta field) {
      fields.add(field);
      return this;
    }

    public ModelMetaBuilder end() {
      var oldFields = (List<DataModelFieldMeta>) modelMeta.getFields();
      var mergedFields = new ArrayList<DataModelFieldMeta>();
      mergedFields.addAll(oldFields);
      mergedFields.addAll(fields);
      modelMeta.setFields(mergedFields);
      return ModelMetaBuilder.this;
    }

    public class FieldBuilder {
      private final DataModelFieldMeta fieldMeta;

      private FieldBuilder(String name) {
        fieldMeta = new DataModelFieldMeta();
        fieldMeta.setName(name);
        FieldsBuilder.this.add(fieldMeta);
      }

      public FieldBuilder rawName(String rawName) {
        fieldMeta.setRawName(rawName);
        return this;
      }

      public FieldBuilder type(String type) {
        fieldMeta.setType(type);
        return this;
      }

      public FieldBuilder autoIncrement() {
        fieldMeta.setIsAutoIncrement(true);
        return this;
      }

      public FieldBuilder unique() {
        fieldMeta.setIsUnique(true);
        return this;
      }

      public FieldBuilder unique(UniquenessScope scope) {
        fieldMeta.setIsUnique(true);
        fieldMeta.setUniquenessScope(scope);
        return this;
      }

      public FieldBuilder nullable() {
        fieldMeta.setIsNullable(true);
        return this;
      }

      public FieldBuilder isUnsigned() {
        fieldMeta.setIsUnsigned(true);
        return this;
      }

      public FieldBuilder label(String label) {
        fieldMeta.setLabel(label);
        return this;
      }

      public FieldBuilder description(String description) {
        fieldMeta.setDescription(description);
        return this;
      }

      public FieldBuilder rawType(String rawType) {
        fieldMeta.setRawType(rawType);
        return this;
      }

      public FieldBuilder decimalDigits(Integer decimalDigits) {
        fieldMeta.setDecimalDigits(decimalDigits);
        return this;
      }

      public FieldBuilder size(Integer size) {
        fieldMeta.setSize(size);
        return this;
      }

      public FieldBuilder rules(List<String> rules) {
        fieldMeta.setRules(rules);
        return this;
      }

      public FieldBuilder rules(String... rules) {
        fieldMeta.setRules(Arrays.asList(rules));
        return this;
      }

      public FieldBuilder defaultValue(String defaultValue) {
        fieldMeta.setDefaultValue(defaultValue);
        return this;
      }

      public FieldBuilder extraProp(String key, Object value) {
        fieldMeta.getExtraProps().put(key, value);
        return this;
      }

      public FieldBuilder extraProps(Map<String, Object> extraProps) {
        fieldMeta.getExtraProps().putAll(extraProps);
        return this;
      }

      public FieldBuilder isPrimaryKey() {
        ModelMetaBuilder.this.primaryKey(fieldMeta.getName());
        return this;
      }

      public FieldsBuilder end() {
        return FieldsBuilder.this;
      }
    }
  }
}
