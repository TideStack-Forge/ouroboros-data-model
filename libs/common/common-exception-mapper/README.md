# Ouroboros Exception Mapper

通用异常映射契约。业务调用 `OuroborosExceptionMapper.toOuroborosException`，由正式 SPI 选择适配器；本模块只依赖公共异常和工具，不依赖 data 或其他业务实现。

```java
import com.ouroboros.exception.mapper.OuroborosExceptionMapper;

try {
    executeOperation();
} catch (Exception error) {
    throw OuroborosExceptionMapper.toOuroborosException(error);
}
```

输入已是 `OuroborosException` 时返回原异常。其他输入按 SPI 优先级选择支持的映射器；没有匹配项时使用默认 `InternalException`，也可通过重载指定默认映射。

## 扩展与装配

实现 `supports(Throwable)` 和 `map(Throwable)`，并将实现类注册到 `META-INF/services/com.ouroboros.exception.mapper.OuroborosExceptionMapper`。适配器归拥有外部异常语义的模块，由平台正式装配，不放回公共底座。

数据异常适配位于 `core/ouroboros-data/ouroboros-data-platform-adapter`：`DataModelExceptionMapper` 映射语句、校验和访问异常；`DataWebApiResultExceptionMapper` 提供 Web 结果映射。两者均在该制品中注册 SPI。生产运行时须装配此制品，单独引入本契约不会获得数据异常映射。

详细映射规则、字段错误与异常链边界见 [GUIDE.md](GUIDE.md)。跨模块业务使用通用契约；数据适配的具体类归属为 `com.ouroboros.data.runtime.adapter`。

## Maven 依赖

```xml
<dependency>
    <groupId>com.ouroboros</groupId>
    <artifactId>ouroboros-exception-mapper</artifactId>
</dependency>
```

版本由平台 BOM 管理；实现制品由正式运行时装配入口选择。
