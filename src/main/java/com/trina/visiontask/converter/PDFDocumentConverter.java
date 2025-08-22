package com.trina.visiontask.converter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

@Slf4j
@Component
public class PDFDocumentConverter implements DocumentConverter {

    private final WebClient webClient;
    private final ConverterOptions converterOptions;

    public PDFDocumentConverter(
            @Qualifier("webClient") WebClient webClient,
            @Qualifier("converterOptions") ConverterOptions converterOptions) {
        this.webClient = webClient;
        this.converterOptions = converterOptions;
    }

    @Override
    public Set<String> getSupportedInputFormats() {
        return Set.of("pptx", "ppt", "doc", "docx");
    }

    @Override
    public Set<String> getSupportedOutputFormats() {
        return Set.of("pdf");
    }

    @Override
    public Flux<DataBuffer> convert(MultipartFile file, ConversionOptions options) throws ConversionException, IOException {
        org.springframework.core.io.InputStreamResource resource =
                new InputStreamResource(file.getInputStream()) {
                    @Override
                    public String getFilename() {
                        return file.getOriginalFilename();
                    }

                    @Override
                    public long contentLength() {
                        return file.getSize();
                    }
                };


        return webClient.post()
                .uri(converterOptions.getUrl())
                .headers(headers -> converterOptions.headers.forEach(headers::add))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData("files", resource))
                .exchangeToFlux(response -> response.bodyToFlux(DataBuffer.class));

    }

    @Override
    public Flux<DataBuffer> convert(InputStream inputStream, String fileName, long fileSize, ConversionOptions options) throws ConversionException, IOException {
        org.springframework.core.io.InputStreamResource resource =
                new InputStreamResource(inputStream) {
                    @Override
                    public String getFilename() {
                        return fileName;
                    }

                    @Override
                    public long contentLength() {
                        return fileSize;
                    }
                };


        return webClient.post()
                .uri(converterOptions.getUrl())
                .headers(headers -> converterOptions.headers.forEach(headers::add))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData("files", resource))
                .exchangeToFlux(response -> response.bodyToFlux(DataBuffer.class));

    }
}
