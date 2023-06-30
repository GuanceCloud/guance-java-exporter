package com.guance.exporter.guance.internal;

import com.guance.exporter.guance.metric.GuanceMetricExporter;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;

public class GuanceMetricExporterProvider implements ConfigurableMetricExporterProvider {

    @Override
    public MetricExporter createExporter(ConfigProperties config) {
        String endpoint = config.getString("otel.exporter.guance.endpoint");
        if (endpoint == null) {
            endpoint = "";
        }

        String token = config.getString("otel.exporter.guance.token");
        if (token == null) {
            token = "";
        }

        return new GuanceMetricExporter().setEndpoint(endpoint).setToken(token);
    }

    @Override
    public String getName() {
        return "guance";
    }
}
