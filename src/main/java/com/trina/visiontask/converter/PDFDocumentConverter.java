package com.trina.visiontask.converter;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;


@Component
public class PDFDocumentConverter implements DocumentConverter {

    private final WebClient webClient;
    private final ConverterOptions converterOptions;

    public PDFDocumentConverter(
            @Qualifier("pdfConvertWebClient") WebClient webClient,
            @Qualifier("pdfConverterOptions") ConverterOptions converterOptions) {
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
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData("files", resource))
                .exchangeToFlux(response -> response.bodyToFlux(DataBuffer.class));

    }
}
