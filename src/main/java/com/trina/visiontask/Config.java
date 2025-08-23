package com.trina.visiontask;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.trina.visiontask.biz.ObjectStorageOptions;
import com.trina.visiontask.converter.ConverterOptions;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.TimeUnit;

@Configuration
public class Config
{

    @Bean
    public String taskInfoKey()
    {
        return "task.info";
    }

    @Bean
    public long waitTimeout()
    {
        return 30;
    }

    @Bean
    public long uploadTaskTimeout()
    {
        return 300;
    }

    @Bean
    public long pdfConvertTaskTimeout()
    {
        return 300;
    }

    @Bean
    public long mdConvertTaskTimeout()
    {
        return 300;
    }

    @Bean
    public long aiSliceTaskTimeout()
    {
        return 300;
    }

    @Bean
    @ConfigurationProperties(prefix = "pdf-convert.api")
    public ConverterOptions converterOptions()
    {
        return new ConverterOptions();
    }

    @Bean
    @ConfigurationProperties(prefix = "oss")
    public ObjectStorageOptions objectStorageOptions()
    {
        return new ObjectStorageOptions();
    }

    @Bean
    @ConfigurationProperties(prefix = "oss.client")
    public ClientBuilderConfiguration clientBuilderConfiguration()
    {
        return new ClientBuilderConfiguration();
    }

    @Bean
    public WebClient webClient(@Qualifier("converterOptions") ConverterOptions options)
    {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, options.getConnectTimeout())
                .doOnConnected(conn -> conn
                                       .addHandlerLast(new ReadTimeoutHandler(options.getReadTimeout(), TimeUnit.MILLISECONDS))
                                       .addHandlerLast(new WriteTimeoutHandler(options.getWriteTimeout(), TimeUnit.MILLISECONDS))
                              );
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Bean
    public OSS ossClient(@Qualifier("objectStorageOptions") ObjectStorageOptions options,
                         ClientBuilderConfiguration clientBuilderConfiguration)
    {
        return new OSSClientBuilder()
                .build(
                        options.getEndpoint(),
                        options.getAccessKey(),
                        options.getAccessSecret(),
                        clientBuilderConfiguration);
    }

    @Bean
    public DataBufferFactory dataBufferFactory()
    {
        return new DefaultDataBufferFactory();
    }
}
