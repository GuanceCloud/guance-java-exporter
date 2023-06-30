package com.guance.exporter.guance.internal;

import com.guance.exporter.guance.trace.GuanceSpanExporter;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.trace.export.SpanExporter;

public class GuanceSpanExporterProvider implements ConfigurableSpanExporterProvider {
    @SuppressWarnings("SystemOut")
    @Override
    public String getName() {
        return "guance";
    }

    @SuppressWarnings("SystemOut")
    @Override
    public SpanExporter createExporter(ConfigProperties config) {
        GuanceSpanExporter exporter = new GuanceSpanExporter();

        String endpoint = config.getString("otel.exporter.guance.endpoint");
        if (endpoint != null) {
            exporter.setEndpoint(endpoint);
        }

        String token = config.getString("otel.exporter.guance.token");
        if (token != null) {
            exporter.setToken(token);
        }

        return exporter;
    }
}
