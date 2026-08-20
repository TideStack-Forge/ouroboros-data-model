package com.ouroboros.data.model.meta.processor;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import com.ouroboros.data.annotation.Field;
import com.ouroboros.data.annotation.Model;

/**
 * Generates same-package typed meta classes for {@link Model} classes.
 */
public final class TypedModelMetaProcessor extends AbstractProcessor {
  private static final String MODEL_ANNOTATION = "com.ouroboros.data.annotation.Model";
  private static final String TYPED_MODEL_META = "com.ouroboros.data.model.meta.TypedModelMeta";
  private static final String STRING_FIELD = "com.ouroboros.data.model.meta.StringField";
  private static final String ENUM_FIELD = "com.ouroboros.data.model.meta.EnumField";
  private static final String TYPED_FIELD = "com.ouroboros.data.model.meta.TypedField";
  private static final Set<String> RESERVED_FIELD_NAMES = Set.of("MODEL_NAME", "SCHEMA_HASH");

  private final Set<String> generatedTypes = new LinkedHashSet<>();
  private Elements elements;
  private Types types;

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    elements = processingEnv.getElementUtils();
    types = processingEnv.getTypeUtils();
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Set.of(MODEL_ANNOTATION);
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    for (Element element : roundEnv.getElementsAnnotatedWith(Model.class)) {
      if (element.getKind() != ElementKind.CLASS) {
        error(element, "@Model can only generate typed meta for classes");
        continue;
      }
      generateMeta((TypeElement) element);
    }
    return true;
  }

  private void generateMeta(TypeElement modelElement) {
    PackageElement packageElement = elements.getPackageOf(modelElement);
    String packageName = packageElement.isUnnamed() ? "" : packageElement.getQualifiedName().toString();
    String modelSimpleName = modelElement.getSimpleName().toString();
    String metaSimpleName = modelSimpleName + "Meta";
    String metaQualifiedName = packageName.isBlank() ? metaSimpleName : packageName + "." + metaSimpleName;

    if (!generatedTypes.add(metaQualifiedName)) {
      return;
    }

    Model model = modelElement.getAnnotation(Model.class);
    String modelName = model.fullName().isBlank() ? modelSimpleName : model.fullName();
    String rootName = lowerCamel(modelSimpleName);
    List<FieldSpec> fields = fieldSpecs(modelElement);
    if (hasFieldNameConflict(modelElement, fields, rootName)) {
      return;
    }

    try {
      JavaFileObject source = processingEnv.getFiler().createSourceFile(metaQualifiedName, modelElement);
      try (Writer writer = source.openWriter()) {
        writer.write(renderSource(packageName, modelElement, metaSimpleName, modelName, rootName, fields));
      }
    } catch (IOException e) {
      error(modelElement, "Failed to generate " + metaQualifiedName + ": " + e.getMessage());
    }
  }

  private List<FieldSpec> fieldSpecs(TypeElement modelElement) {
    List<FieldSpec> fields = new ArrayList<>();
    for (VariableElement field : ElementFilter.fieldsIn(modelElement.getEnclosedElements())) {
      if (field.getAnnotation(Field.class) == null) {
        continue;
      }
      fields.add(toFieldSpec(field));
    }
    fields.sort(Comparator.comparing(FieldSpec::name));
    return fields;
  }

  private FieldSpec toFieldSpec(VariableElement field) {
    TypeMirror type = field.asType();
    String fieldName = field.getSimpleName().toString();
    if (isString(type)) {
      return new FieldSpec(fieldName, STRING_FIELD + "<%s>", "stringField(\"" + fieldName + "\")");
    }
    if (isEnum(type)) {
      String enumType = sourceTypeName(type);
      return new FieldSpec(fieldName, ENUM_FIELD + "<" + enumType + ", %s>", "enumField(\"" + fieldName + "\", " + enumType + ".class)");
    }
    return new FieldSpec(fieldName, TYPED_FIELD + "<" + sourceTypeName(type) + ", %s>", "field(\"" + fieldName + "\")");
  }

  private boolean hasFieldNameConflict(TypeElement modelElement, List<FieldSpec> fields, String rootName) {
    Set<String> generatedFieldNames = new LinkedHashSet<>(RESERVED_FIELD_NAMES);
    generatedFieldNames.add(rootName);
    for (FieldSpec field : fields) {
      if (generatedFieldNames.contains(field.name())) {
        error(modelElement, "@Field name '" + field.name() + "' conflicts with generated meta member names");
        return true;
      }
      generatedFieldNames.add(field.name());
    }
    return false;
  }

  private String renderSource(
      String packageName,
      TypeElement modelElement,
      String metaSimpleName,
      String modelName,
      String rootName,
      List<FieldSpec> fields
  ) {
    String modelType = modelElement.getQualifiedName().toString();
    StringBuilder source = new StringBuilder();
    if (!packageName.isBlank()) {
      source.append("package ").append(packageName).append(";\n\n");
    }
    source.append("public final class ").append(metaSimpleName)
        .append(" extends ").append(TYPED_MODEL_META).append("<").append(modelType).append(", ").append(metaSimpleName).append("> {\n")
        .append("  public static final String MODEL_NAME = \"").append(escape(modelName)).append("\";\n")
        .append("  public static final String SCHEMA_HASH = \"").append(schemaHash(modelName, fields)).append("\";\n")
        .append("  public static final ").append(metaSimpleName).append(" ").append(rootName)
        .append(" = new ").append(metaSimpleName).append("(\"").append(rootName).append("\");\n\n");
    for (FieldSpec field : fields) {
      source.append("  public final ").append(field.declarationType(metaSimpleName))
          .append(" ").append(field.name()).append(" = ").append(field.initializer()).append(";\n");
    }
    source.append("\n")
        .append("  private ").append(metaSimpleName).append("(String alias) {\n")
        .append("    super(MODEL_NAME, ").append(modelType).append(".class, alias);\n")
        .append("  }\n\n")
        .append("  @Override\n")
        .append("  public ").append(metaSimpleName).append(" as(String alias) {\n")
        .append("    return new ").append(metaSimpleName).append("(alias);\n")
        .append("  }\n")
        .append("}\n");
    return source.toString();
  }

  private boolean isString(TypeMirror type) {
    TypeElement stringElement = elements.getTypeElement("java.lang.String");
    return types.isSameType(types.erasure(type), types.erasure(stringElement.asType()));
  }

  private boolean isEnum(TypeMirror type) {
    if (type.getKind() != TypeKind.DECLARED) {
      return false;
    }
    Element element = ((DeclaredType) type).asElement();
    return element.getKind() == ElementKind.ENUM;
  }

  private String sourceTypeName(TypeMirror type) {
    if (type.getKind().isPrimitive()) {
      return types.boxedClass((PrimitiveType) type).getQualifiedName().toString();
    }
    return type.toString();
  }

  private String schemaHash(String modelName, List<FieldSpec> fields) {
    StringBuilder payload = new StringBuilder(modelName);
    for (FieldSpec field : fields) {
      payload.append('|').append(field.name()).append(':').append(field.declarationType(""));
    }
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(payload.toString().getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder(hash.length * 2);
      for (byte b : hash) {
        hex.append(String.format("%02x", b));
      }
      return "sha256:" + hex;
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 digest is unavailable", e);
    }
  }

  private String lowerCamel(String value) {
    if (value.length() == 1) {
      return value.toLowerCase();
    }
    return Character.toLowerCase(value.charAt(0)) + value.substring(1);
  }

  private String escape(String value) {
    return value.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  private void error(Element element, String message) {
    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
  }

  private record FieldSpec(String name, String declarationTemplate, String initializer) {
    String declarationType(String metaSimpleName) {
      return declarationTemplate.formatted(metaSimpleName);
    }
  }
}
