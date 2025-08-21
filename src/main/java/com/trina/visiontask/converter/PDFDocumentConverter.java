package com.trina.visiontask.converter;

import com.trina.visiontask.exception.ConversionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.io.ByteArrayOutputStream;
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
    public Flux<DataBuffer> convert(InputStream inputStream, String mimeType, ConversionOptions options) throws ConversionException {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            byte[] byteArray = buffer.toByteArray();

            builder.part("files", new ByteArrayResource(byteArray) {
                        @Override
                        public String getFilename() {
                            return "upload-file"; // 文件名不能为空，根据实际需求修改
                        }
                    })
                    .contentType(MediaType.parseMediaType(mimeType));
        } catch (IOException e) {
            throw new ConversionException("Failed to read input stream", e);
        }

        return webClient.post()
                .uri(converterOptions.getUrl())
                .headers(headers -> converterOptions.headers.forEach(headers::add))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .exchangeToFlux(response -> response.bodyToFlux(DataBuffer.class));

    }

    @Override
    public Flux<DataBuffer> convert(Flux<DataBuffer> inputFlux, String mimeType, ConversionOptions options) throws ConversionException {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.asyncPart("files", inputFlux, DataBuffer.class)
                .contentType(MediaType.parseMediaType(mimeType));

        return webClient.post()
                .uri(converterOptions.getUrl())
                .headers(headers -> converterOptions.headers.forEach(headers::add))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .exchangeToFlux(response -> response.bodyToFlux(DataBuffer.class));
    }

}
