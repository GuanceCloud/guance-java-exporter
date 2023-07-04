# guance-java-exporter

this is OTEL exporter

## 如何使用

### agent 形式：

该项目作为 exporter jar 包形式在[观测云二开 opentelemetry-java-instrumentation](https://github.com/GuanceCloud/opentelemetry-java-instrumentation)， 版本要求为 最低 v1.26.4-guance

使用命令行启动 otel agent：

```shell
# 必须填写 endpoint 和 token 否则数据无法上传
# 如果想要开启 debug 日志，可以添加 -Dotel.javaagent.debug=true
java  -javaagent:/usr/local/opentelemetry-javaagent-1.26.1-guance.jar \
 -Dotel.traces.exporter=guance \
 -Dotel.metrics.exporter=guance \
 -Dotel.exporter.guance.endpoint=https://openway.guance.com \
 -Dotel.exporter.guance.token=tkn_0d9ebb474d3940xxxxxxxx  \
 -jar app.jar
```

> 注意： endpoint 和 token 必须设置，否则无法上传数据到观测云。 

### 代码集成形式：

引用该 jar 包, pom.xml:

```xml
<dependencies>
    <dependency>
        <groupId>io.opentelemetry</groupId>
        <artifactId>opentelemetry-sdk</artifactId>
        <version>1.26.0</version>
    </dependency>

    <dependency>
        <groupId>io.opentelemetry</groupId>
        <artifactId>opentelemetry-exporter-otlp</artifactId>
       <version>1.26.0</version>
    </dependency>

    <dependency>
        <groupId>io.opentelemetry</groupId>
        <artifactId>opentelemetry-semconv</artifactId>
        <version>1.26.0-alpha</version>
    </dependency>

    <dependency>
        <groupId>com.guance</groupId>
        <artifactId>guance-exporter</artifactId>
       <version>1.3</version>
    </dependency>
  <!--  请确认版本！！ -->
</dependencies>
```

版本可在 maven2 仓库中使用最新版本：[maven2-guance-exporter](https://repo1.maven.org/maven2/com/guance/guance-exporter/)

要在 `SpringBoot` 项目中初始化一个全局的 OpenTelemetry 对象，你可以创建一个单例类来管理它。以下是一个示例：

首先，创建一个名为 `OpenTelemetryManager` 的类：

```java
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;

public class OpenTelemetryManager {
    private static final OpenTelemetry OPEN_TELEMETRY = OpenTelemetryInitializer.initialize(); // 初始化OpenTelemetry

    public static OpenTelemetry getOpenTelemetry() {
        return OPEN_TELEMETRY;
    }

    public static Tracer getTracer(String name) {
        return OPEN_TELEMETRY.getTracer(name);
    }
}
```

然后，在 `OpenTelemetryInitializer` 类中进行 `OpenTelemetry` 的初始化和配置：

```java

import com.guance.exporter.guance.trace.GuanceSpanExporter;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;

public class OpenTelemetryInitializer {
    public static OpenTelemetry initialize() {
        GuanceSpanExporter guanceExporter = new GuanceSpanExporter();
        guanceExporter.setEndpoint("https://openway.guance.com"); // dataway
        guanceExporter.setToken("tkn_0d9ebb47xxxxxxxxx");    // your token

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(guanceExporter).build())
                .setResource(Resource.create(Attributes.builder()
                        .put(ResourceAttributes.SERVICE_NAME, "serviceForJAVA")
                        .build()))
                .build();

        return OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .buildAndRegisterGlobal();
    }
}
```

最后，在你的 Java 文件中，你可以直接通过 `OpenTelemetryManager` 类来获取全局的 `OpenTelemetry` 对象：

```java
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;

public class YourClass {
    private static final OpenTelemetry openTelemetry = OpenTelemetryManager.getOpenTelemetry();
    private static final Tracer tracer = OpenTelemetryManager.getTracer("your-tracer-name");

    public void yourMethod() {
        // 使用tracer进行跟踪
        tracer.spanBuilder("your-span").startSpan().end();

        // ...
    }
}
```

