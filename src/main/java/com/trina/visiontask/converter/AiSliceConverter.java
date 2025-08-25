package com.trina.visiontask.converter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Set;

@Slf4j
@Component
public class AiSliceConverter implements DocumentConverter {

    private final WebClient webClient;
    private final ConverterOptions converterOptions;

    public AiSliceConverter(
            @Qualifier("aiSliceWebClient") WebClient webClient,
            @Qualifier("aiSliceConverterOptions") ConverterOptions converterOptions) {
        this.webClient = webClient;
        this.converterOptions = converterOptions;
    }

    @Override
    public Set<String> getSupportedInputFormats() {
        return Set.of();
    }

    @Override
    public Set<String> getSupportedOutputFormats() {
        return Set.of();
    }

    @Override
    public <T, F> Mono<F> convert(T dto, Class<F> clazz, ConversionOptions options) throws ConversionException {
        return webClient.post()
                .uri(converterOptions.getUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dto)
                .exchangeToMono(response -> response.bodyToMono(clazz));
    }
}
