package com.ouroboros.data.model.meta.processor;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TypedModelMetaProcessorTest {

  @TempDir
  private Path tempDir;

  @Test
  void generatedMetaShouldBeAvailableToSameCompilationUnit() throws IOException {
    CompilationResult result = compile(
        source("fixture.UserStatus", """
            package fixture;

            public enum UserStatus {
              ENABLED,
              LOCKED
            }
            """),
        source("fixture.User", """
            package fixture;

            import com.ouroboros.data.annotation.Field;
            import com.ouroboros.data.annotation.Model;

            @Model(fullName = "User")
            public class User {
              @Field
              private String id;

              @Field
              private String username;

              @Field
              private UserStatus status;

              @Field
              private Integer age;
            }
            """),
        source("fixture.UserMetaUsage", """
            package fixture;

            import static fixture.UserMeta.user;

            import com.ouroboros.data.dsl.query.Query;

            public final class UserMetaUsage {
              public Object build() {
                return Query.from(user)
                    .where(
                        user.username.contains("ada"),
                        user.status.eq(UserStatus.ENABLED),
                        user.age.gt(18)
                    )
                    .select(user.id, user.username, user.status, user.age)
                    .build();
              }
            }
            """)
    );

    assertTrue(result.success(), result.diagnosticMessages());
    String generated = Files.readString(result.generatedSource("fixture/UserMeta.java"));
    assertTrue(generated.contains("public final class UserMeta extends com.ouroboros.data.model.meta.TypedModelMeta<fixture.User, UserMeta>"));
    assertTrue(generated.contains("public static final UserMeta user = new UserMeta(\"user\");"));
    assertTrue(generated.contains("public final com.ouroboros.data.model.meta.StringField<UserMeta> username = stringField(\"username\");"));
    assertTrue(generated.contains("public final com.ouroboros.data.model.meta.EnumField<fixture.UserStatus, UserMeta> status = enumField(\"status\", fixture.UserStatus.class);"));
    assertTrue(generated.contains("public final com.ouroboros.data.model.meta.TypedField<java.lang.Integer, UserMeta> age = field(\"age\");"));
    assertFalse(generated.contains("execute("));
    assertFalse(generated.contains("query("));
  }

  @Test
  void missingGeneratedFieldShouldFailCompilation() {
    CompilationResult result = compile(
        source("fixture.UserStatus", """
            package fixture;

            public enum UserStatus {
              ENABLED
            }
            """),
        source("fixture.User", """
            package fixture;

            import com.ouroboros.data.annotation.Field;
            import com.ouroboros.data.annotation.Model;

            @Model(fullName = "User")
            public class User {
              @Field
              private String id;
            }
            """),
        source("fixture.UserMetaUsage", """
            package fixture;

            import static fixture.UserMeta.user;

            public final class UserMetaUsage {
              public Object missing() {
                return user.missingField;
              }
            }
            """)
    );

    assertFalse(result.success());
    assertTrue(result.diagnosticMessages().contains("missingField"), result.diagnosticMessages());
  }

  private CompilationResult compile(JavaFileObject... sources) {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    assertNotNull(compiler, "JDK compiler is required");
    Path classesDir = tempDir.resolve("classes");
    Path generatedDir = tempDir.resolve("generated");
    try {
      Files.createDirectories(classesDir);
      Files.createDirectories(generatedDir);
    } catch (IOException e) {
      fail(e);
    }

    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, Locale.ROOT, null)) {
      JavaCompiler.CompilationTask task = compiler.getTask(
          null,
          fileManager,
          diagnostics,
          List.of(
              "-classpath", System.getProperty("java.class.path"),
              "-d", classesDir.toString(),
              "-s", generatedDir.toString()
          ),
          null,
          List.of(sources)
      );
      task.setProcessors(List.of(new TypedModelMetaProcessor()));
      return new CompilationResult(Boolean.TRUE.equals(task.call()), diagnostics, generatedDir);
    } catch (IOException e) {
      fail(e);
      return new CompilationResult(false, diagnostics, generatedDir);
    }
  }

  private JavaFileObject source(String className, String content) {
    return new StringJavaFileObject(className, content);
  }

  private record CompilationResult(
      boolean success,
      DiagnosticCollector<JavaFileObject> diagnostics,
      Path generatedDir
  ) {
    String diagnosticMessages() {
      StringBuilder builder = new StringBuilder();
      for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
        builder.append(diagnostic.getKind())
            .append(": ")
            .append(diagnostic.getMessage(Locale.ROOT))
            .append('\n');
      }
      return builder.toString();
    }

    Path generatedSource(String path) {
      return generatedDir.resolve(path);
    }
  }

  private static final class StringJavaFileObject extends SimpleJavaFileObject {
    private final String content;

    private StringJavaFileObject(String className, String content) {
      super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
      this.content = content;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
      return content;
    }
  }
}
