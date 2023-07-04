# 采样

> *作者： songlq*

在 otel 中有两个环境变量配置采样：`OTEL_TRACES_SAMPLER`  和 `OTEL_TRACES_SAMPLER_ARG` :

## 如何选择采样的配置

`OTEL_TRACES_SAMPLER` 的配置是：

*   `"always_on"`: AlwaysOnSampler 默认配置 1.0 也就是不采样
*   `"always_off"`: AlwaysOffSampler 始终采集任何链路
*   `"traceidratio"`: TraceIdRatioBased 根据 trace id 概率采样
*   `"parentbased_always_on"`: ParentBased(root=AlwaysOnSampler)
*   `"parentbased_always_off"`: ParentBased(root=AlwaysOffSampler)
*   `"parentbased_traceidratio"`: ParentBased(root=TraceIdRatioBased)
*   `"parentbased_jaeger_remote"`: ParentBased(root=JaegerRemoteSampler)
*   `"jaeger_remote"`: JaegerRemoteSampler
*   `"xray"`: [AWS X-Ray Centralized Sampling](https://docs.aws.amazon.com/xray/latest/devguide/xray-console-sampling.html) (*third party*)

`OTEL_TRACES_SAMPLER_ARG` 配置

*   1.0  默认情况下是1.0，也就是全部采样
*   \[0-1.0] 概率采样，如 0.25 也就是 25% 的采样率
*   采样的配置只有 `traceidratio` 和 `parentbased_traceidratio` 的情况下，`OTEL_TRACES_SAMPLER_ARG` 才会生效。

### 常用的配置

`OTEL_TRACES_SAMPLER=parentbased_traceidratio`（基于父级的 Trace ID 比率）：这种采样策略基于父级的 Trace ID 来确定子级是否应该被采样。当一个新的请求到达时，它会检查父级的 Trace ID 是否在指定的比率范围内。如果是，则子级也将被采样。这种策略确保了请求和其相关操作之间的关联性。这对于分布式跟踪系统非常有用。

`OTEL_TRACES_SAMPLER=traceidratio`（Trace ID 比率）：这种采样策略根据每个请求的 Trace ID 来确定是否要对该请求进行采样。每个请求都有一个唯一的 Trace ID。使用该策略时，每个请求都会根据指定的比率独立地决定是否被采样。这种策略适用于不需要考虑请求之间的关联性，而只关注每个请求本身的采样率。

综上所述，`OTEL_TRACES_SAMPLER=parentbased_traceidratio` 基于父级的 Trace ID 来决定子级的采样率，而 `OTEL_TRACES_SAMPLER=traceidratio` 则根据每个请求的 Trace ID 独立地决定采样率。选择适合您需求的策略取决于您对请求之间关联性的关注程度。

示例：

```shell
java  -javaagent:/usr/local//opentelemetry-javaagent.jar \
-Dotel.traces.sampler=traceidratio \
-Dotel.traces.sampler.arg=0.1 \
-jar app.jar
```