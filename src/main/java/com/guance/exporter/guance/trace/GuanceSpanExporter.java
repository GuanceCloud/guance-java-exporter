package com.guance.exporter.guance.trace;

import com.guance.exporter.guance.http.OKHTTPClient;
import com.guance.exporter.guance.utils.GuanceUtils;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.internal.ThrottlingLogger;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.influxdb.dto.Point;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static com.guance.exporter.guance.http.OKHTTPClient.TRACE_CATEGORY;
import static java.util.Objects.requireNonNull;
import static java.util.logging.Level.WARNING;

public class GuanceSpanExporter implements SpanExporter {

    private static final Logger internalLogger = Logger.getLogger(GuanceSpanExporter.class.getName());

    private final ThrottlingLogger logger = new ThrottlingLogger(internalLogger);
    private static final String SERVICE_NAME = "service.name";
    private static final String NAME = "opentelemetry";
    private static final AttributeKey<String> SERVICE_NAME_KEY = AttributeKey.stringKey(SERVICE_NAME);
    private final OKHTTPClient delegate;

    public GuanceSpanExporter() {
        delegate = new OKHTTPClient();
    }

    public void setEndpoint(String endpoint) {
        requireNonNull(endpoint, "endpoint");
        if (endpoint.equals("")) {
            logger.log(
                    WARNING,
                    "guance endpoing is not set. please set otel.exporter.guance.endpoint=<openway>");
        } else {
            delegate.setEndpoint(endpoint);
        }
    }

    public void setToken(String token) {
        requireNonNull(token, "token");
        if (token.equals("")) {
            logger.log(WARNING, "openway token is null !!! please set otel.exporter.guance.token=xxx");
        } else {
            delegate.setToken(token);
        }
    }

    @SuppressWarnings("SystemOut")
    @Override
    public CompletableResultCode export(Collection<SpanData> collection) {
        System.out.println("-------------------export-----spans---------");
        // todo
        StringBuilder sb = new StringBuilder();

        for (SpanData span : collection) {
            TraceFlags flags = span.getSpanContext().getTraceFlags();
            if (flags != null) {
                if (!flags.isSampled()) {
                    System.out.println("sampled .....");
                    continue;
                }
            }
            sb.append(convertSpanToInfluxDBPoint(span).lineProtocol()).append('\n');
        }
        sb.deleteCharAt(sb.length() - 1); // delete last \n

        delegate.write(sb.toString(), collection.size(), TRACE_CATEGORY);
        System.out.println("post " + collection.size() + " to dataway");

        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {

        return CompletableResultCode.ofSuccess();
    }

    public Point convertSpanToInfluxDBPoint(SpanData span) {
        String serviceName = span.getResource().getAttributes().get(SERVICE_NAME_KEY);
        if (serviceName == null) {
            serviceName = "name";
        }
        String name = span.getName();
        long startTime = TimeUnit.NANOSECONDS.toMicros(span.getStartEpochNanos());
        long endTime = TimeUnit.NANOSECONDS.toMicros(span.getEndEpochNanos());
        long duration = endTime - startTime;
        String sourceType = getSourceType(span.getAttributes());
        String spanType = Objects.equals(span.getParentSpanId(), "") ? "entry" : "local";
        Point.Builder pointBuilder =
                Point.measurement(NAME)
                        .tag("service", serviceName)
                        .tag("service_name", serviceName)
                        .tag("operation", name)
                        .tag("source_type", sourceType)
                        .tag("span_type", spanType)
                        .tag("status", "ok")
                        .tag("host", GuanceUtils.getHostName())
                        .addField("trace_id", span.getSpanContext().getTraceId())
                        .addField("span_id", span.getSpanContext().getSpanId())
                        .addField("parent_id", span.getParentSpanId())
                        .addField("start", startTime)
                        .addField("resource", name)
                        .addField("message", span.toString())
                        .addField("duration", duration);

        // Add tags as fields if required
        span.getSpanContext()
                .getTraceState()
                .forEach((key, value) -> pointBuilder.addField(key, value));

        span.getAttributes()
                .forEach(
                        (key, value) ->
                                pointBuilder.tag(key.getKey().replaceAll("\\.", "_"), value.toString()));

        pointBuilder.time(span.getStartEpochNanos(), TimeUnit.NANOSECONDS);

        return pointBuilder.build();
    }

  /*  @SuppressWarnings("SystemOut")
  public String toPoint(SpanData span){
    // tag: service operation source_type span_type status
    // field: trace_id parent_id span_id resource start duration message
    StringBuilder sb = new StringBuilder();
    // source 是 guance_exporter
    String name = span.getName();
    String  serviceName = span.getResource().getAttributes().get(SERVICE_NAME_KEY);
    if (serviceName==null){
      serviceName = "name";
    }
    String traceId = span.getTraceId();
    String spanId = span.getSpanId();
    long startTimeNano = span.getStartEpochNanos();

    long startTime = TimeUnit.NANOSECONDS.toMicros(span.getStartEpochNanos());
    long endTime = TimeUnit.NANOSECONDS.toMicros(span.getEndEpochNanos());
    long duration = endTime - startTime;
    String spanType = Objects.equals(span.getParentSpanId(), "") ?"entry":"local";
    String sourceType = getSourceType(span.getAttributes());
    sb.append(NAME)
        .append(",service=").append(escapeSpaces(serviceName))
        .append(",operation=").append(escapeSpaces(name))
        .append(",source_type=").append(sourceType)
        .append(",span_type=").append(spanType)
        .append(",status=").append(span.getStatus().getStatusCode().toString()).append(' ') // tags
        .append("trace_id=").append(traceId)
        .append(",span_id=").append(spanId)
        .append(",parent_id=").append(span.getParentSpanId())
        .append(",duration=").append(duration)
        .append(",resource=").append(escapeSpaces(name))
        .append(",start=").append(startTime)
       // .append(",message=").append("{}")
        .append(' ').append(startTimeNano).append('\n');
    String point = sb.toString();
    System.out.println(point);
    return point;
  }*/

    public static String getSourceType(Attributes attributes) {
        AttributeKey<String> httpMethodKey = AttributeKey.stringKey("http.method");
        AttributeKey<String> dbSystemKey = AttributeKey.stringKey("db.system");
        AttributeKey<String> messagingSystemKey = AttributeKey.stringKey("messaging.system");

        if (attributes.get(httpMethodKey) != null) {
            return "web";
        } else if (attributes.get(dbSystemKey) != null) {
            return "db";
        } else if (attributes.get(messagingSystemKey) != null) {
            return "message";
        } else {
            return "custom";
        }
    }

    public String escapeSpaces(String input) {
        return input.replaceAll(" ", "\\\\ ");
    }
}
