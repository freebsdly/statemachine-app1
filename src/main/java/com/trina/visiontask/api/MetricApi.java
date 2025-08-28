package com.trina.visiontask.api;

import io.micrometer.core.annotation.Timed;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MetricApi
{
    private final PrometheusMeterRegistry prometheusMeterRegistry;

    public MetricApi(PrometheusMeterRegistry prometheusMeterRegistry)
    {
        this.prometheusMeterRegistry = prometheusMeterRegistry;
    }

    @Timed(value = "api.metrics", description = "Metrics endpoint")
    @GetMapping(value = "/metrics", produces = MediaType.TEXT_PLAIN_VALUE)
    public String metrics()
    {
        return prometheusMeterRegistry.scrape();
    }
}
