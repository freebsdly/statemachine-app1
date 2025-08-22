package com.trina.visiontask.converter;

import com.trina.visiontask.exception.ConversionException;
import com.trina.visiontask.exception.UnsupportedFormatException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

/**
 * 文档转换器接口，定义文档格式转换的核心能力
 */
public interface DocumentConverter {

    /**
     * 获取支持的输入格式（如 "docx", "pdf", "txt"）
     *
     * @return 不重复的格式集合（小写，无前缀点）
     */
    Set<String> getSupportedInputFormats();

    /**
     * 获取支持的输出格式（如 "pdf", "png", "html"）
     *
     * @return 不重复的格式集合（小写，无前缀点）
     */
    Set<String> getSupportedOutputFormats();

    /**
     * 校验输入格式是否支持
     *
     * @param inputFormat 输入格式（如 "docx"）
     * @throws UnsupportedFormatException 格式不支持时抛出
     */
    default void validateInputFormat(String inputFormat) throws UnsupportedFormatException {
        if (inputFormat == null || !getSupportedInputFormats().contains(inputFormat.toLowerCase())) {
            throw new UnsupportedFormatException(inputFormat);
        }
    }

    /**
     * 校验输出格式是否支持
     *
     * @param outputFormat 输出格式（如 "pdf"）
     * @throws UnsupportedFormatException 格式不支持时抛出
     */
    default void validateOutputFormat(String outputFormat) throws UnsupportedFormatException {
        if (outputFormat == null || !getSupportedOutputFormats().contains(outputFormat.toLowerCase())) {
            throw new UnsupportedFormatException(outputFormat);
        }
    }

    default Flux<DataBuffer> convert(
            MultipartFile file,
            ConversionOptions options
    ) throws ConversionException, IOException {
        throw new ConversionException("Not implemented");
    }

    default Flux<DataBuffer> convert(
            InputStream inputStream,
            String fileName,
            long fileSize,
            ConversionOptions options
    ) throws ConversionException, IOException {
        throw new ConversionException("Not implemented");
    }

    /**
     * 创建一个转换流，将输入流转换为输出流
     *
     * @param inputFlux 输入流（需调用方关闭）
     * @param mimeType  输入格式
     * @param options   转换选项
     * @return 转换结果
     * @throws ConversionException 转换过程中发生异常
     */
    default Flux<DataBuffer> convert(
            Flux<DataBuffer> inputFlux,
            String mimeType,
            ConversionOptions options
    ) throws ConversionException {
        throw new ConversionException("Not implemented");
    }
}
