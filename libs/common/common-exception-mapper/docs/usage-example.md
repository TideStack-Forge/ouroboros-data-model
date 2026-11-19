# DataModelExceptionMapper 使用示例

## 完整示例代码

```java
import java.util.*;
import com.ouroboros.data.exception.*;
import com.ouroboros.exception.*;
import com.ouroboros.exception.mapper.DataModelExceptionMapper;

public class ExceptionMapperExample {

  public static void main(String[] args) {
    // 示例 1: 转换语句异常
    exampleStatementException();

    // 示例 2: 转换简单数据验证异常
    exampleSimpleValidationException();

    // 示例 3: 转换带字段错误的记录验证异常
    exampleRecordValidationException();

    // 示例 4: 转换数据访问异常
    exampleDataAccessException();
  }

  /**
   * 示例 1: 语句异常转换为平台异常
   */
  private static void exampleStatementException() {
    try {
      // 模拟抛出语句异常
      throw new InvalidStatementException("查询语句格式不正确");
    } catch (DataModelException e) {
      // 转换为 Ouroboros 异常
      OuroborosException ouroborosException =
          DataModelExceptionMapper.toOuroborosException(e);

      System.out.println("异常类型: " + ouroborosException.getClass().getSimpleName());
      // 输出: PlatformException

      System.out.println("错误消息: " + ouroborosException.getMessage());
      // 输出: 查询语句格式不正确

      System.out.println("错误代码: " + ouroborosException.getErrorCode());
      // 输出: -6000 (PlatformException 的默认错误代码)
    }
  }

  /**
   * 示例 2: 简单数据验证异常转换
   */
  private static void exampleSimpleValidationException() {
    try {
      // 模拟抛出数据验证异常
      throw new DataValidationException("数据验证失败");
    } catch (DataModelException e) {
      // 转换为 Ouroboros 异常
      OuroborosException ouroborosException =
          DataModelExceptionMapper.toOuroborosException(e);

      System.out.println("异常类型: " + ouroborosException.getClass().getSimpleName());
      // 输出: ValidationException

      System.out.println("错误消息: " + ouroborosException.getMessage());
      // 输出: 数据验证失败

      System.out.println("错误代码: " + ouroborosException.getErrorCode());
      // 输出: 422 (ValidationException 的错误代码)
    }
  }

  /**
   * 示例 3: 记录验证异常（带字段错误）转换
   */
  private static void exampleRecordValidationException() {
    try {
      // 构造字段错误信息
      Map<String, List<String>> fieldErrors = new HashMap<>();
      fieldErrors.put("username", Arrays.asList(
          "用户名不能为空",
          "用户名长度必须在3-20个字符之间"
      ));
      fieldErrors.put("password", Arrays.asList(
          "密码强度不够",
          "密码长度至少8位",
          "密码必须包含数字和字母"
      ));
      fieldErrors.put("email", Arrays.asList("邮箱格式不正确"));

      // 模拟抛出记录验证异常
      throw new RecordValidationException(fieldErrors);

    } catch (DataModelException e) {
      // 转换为 Ouroboros 异常
      OuroborosException ouroborosException =
          DataModelExceptionMapper.toOuroborosException(e);

      System.out.println("异常类型: " + ouroborosException.getClass().getSimpleName());
      // 输出: ValidationException

      // 获取字段错误详情
      ValidationException validationException = (ValidationException) ouroborosException;
      Map<String, String> errors = validationException.getErrors();

      System.out.println("字段错误数量: " + errors.size());
      // 输出: 3

      // 打印每个字段的错误
      for (Map.Entry<String, String> entry : errors.entrySet()) {
        System.out.println(entry.getKey() + ": " + entry.getValue());
      }
      // 输出:
      // username: 用户名不能为空,用户名长度必须在3-20个字符之间
      // password: 密码强度不够,密码长度至少8位,密码必须包含数字和字母
      // email: 邮箱格式不正确
    }
  }

  /**
   * 示例 4: 数据访问异常转换为内部异常
   */
  private static void exampleDataAccessException() {
    try {
      // 模拟抛出数据访问异常
      throw new QueryExecutionException("数据库连接超时",
          new java.sql.SQLException("Connection timeout"));
    } catch (DataModelException e) {
      // 转换为 Ouroboros 异常
      OuroborosException ouroborosException =
          DataModelExceptionMapper.toOuroborosException(e);

      System.out.println("异常类型: " + ouroborosException.getClass().getSimpleName());
      // 输出: InternalException

      System.out.println("错误消息: " + ouroborosException.getMessage());
      // 输出: 数据库连接超时

      System.out.println("错误代码: " + ouroborosException.getErrorCode());
      // 输出: -5000 (InternalException 的默认错误代码)

      // 可以获取原始异常
      System.out.println("原始异常类型: " + ouroborosException.getCause().getClass().getSimpleName());
      // 输出: QueryExecutionException
    }
  }
}
```

## 在业务代码中的实际应用

### 场景：在 Service 层捕获并转换异常

```java
import com.ouroboros.data.model.DataModel;
import com.ouroboros.data.exception.DataModelException;
import com.ouroboros.exception.OuroborosException;
import com.ouroboros.exception.mapper.DataModelExceptionMapper;

public class UserService {

  private final DataModel userModel;

  public void createUser(Map<String, Object> userData) {
    try {
      // 调用数据模型层
      userModel.insert(userData);
    } catch (DataModelException e) {
      // 转换为 Ouroboros 异常并重新抛出
      throw DataModelExceptionMapper.toOuroborosException(e);
    }
  }

  public List<Record> queryUsers(Map<String, Object> condition) {
    try {
      // 调用数据模型层
      return userModel.query(condition).get();
    } catch (DataModelException e) {
      // 转换为 Ouroboros 异常并重新抛出
      throw DataModelExceptionMapper.toOuroborosException(e);
    }
  }
}
```

### 场景：在全局异常处理器中使用

```java
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.ouroboros.exception.ValidationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(ValidationException e) {
    // 获取字段错误详情
    Map<String, String> fieldErrors = e.getErrors();

    ErrorResponse response = new ErrorResponse();
    response.setCode(e.getErrorCode());
    response.setMessage(e.getMessage());
    response.setFieldErrors(fieldErrors);

    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
  }
}
```

## 错误响应示例

当客户端提交的数据验证失败时，API 返回的响应示例：

```json
{
  "code": 422,
  "message": "Record validation failed: 3 field(s) have errors",
  "fieldErrors": {
    "username": "用户名不能为空,用户名长度必须在3-20个字符之间",
    "password": "密码强度不够,密码长度至少8位,密码必须包含数字和字母",
    "email": "邮箱格式不正确"
  }
}
```

## 最佳实践

1. **在 Service 层边界转换异常**：在 Service 层捕获 `DataModelException`，立即转换为 `OuroborosException`
2. **异常链边界**：语句、访问和默认包装分支保留 cause；数据校验分支当前只传递消息或字段错误。需要诊断原始校验堆栈时，在映射前保留原异常的日志证据
3. **统一异常处理**：使用全局异常处理器统一处理 `OuroborosException`
4. **字段错误展示**：对于 `ValidationException`，提取 errors map 并返回给前端，提供友好的字段级错误提示
