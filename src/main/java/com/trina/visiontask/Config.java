package com.trina.visiontask;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.trina.visiontask.service.ObjectStorageOptions;
import com.trina.visiontask.converter.ConverterOptions;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Configuration
public class Config {

    private static final Logger log = Logger.getLogger(Config.class.getName());

    @Bean
    @ConfigurationProperties(prefix = "task.common")
    public TaskConfiguration taskConfiguration() {
        return new TaskConfiguration();
    }

    @Bean
    @ConfigurationProperties(prefix = "pdf-convert.api")
    public ConverterOptions pdfConverterOptions() {
        return new ConverterOptions();
    }

    @Bean
    @ConfigurationProperties(prefix = "md-convert.api")
    public ConverterOptions mdConverterOptions() {
        return new ConverterOptions();
    }

    @Bean
    @ConfigurationProperties(prefix = "ai-slice.api")
    public ConverterOptions aiSliceConverterOptions() {
        return new ConverterOptions();
    }

    @Bean
    @ConfigurationProperties(prefix = "upload.consumer")
    public MQConfiguration uploadConfiguration() {
        return new MQConfiguration();
    }

    @Bean
    @ConfigurationProperties(prefix = "pdf-convert.consumer")
    public MQConfiguration pdfConvertConfiguration() {
        return new MQConfiguration();
    }

    @Bean
    @ConfigurationProperties(prefix = "md-convert.consumer")
    public MQConfiguration mdConvertConfiguration() {
        return new MQConfiguration();
    }

    @Bean
    @ConfigurationProperties(prefix = "ai-slice.consumer")
    public MQConfiguration aiSliceConfiguration() {
        return new MQConfiguration();
    }

    @Bean
    @ConfigurationProperties(prefix = "task-log.consumer")
    public MQConfiguration taskLogConfiguration() {
        return new MQConfiguration();
    }

    @Bean
    public WebClient pdfConvertWebClient(@Qualifier("pdfConverterOptions") ConverterOptions options) {
        return getWebClient(options);
    }

    @Bean
    public WebClient mdConvertWebClient(@Qualifier("mdConverterOptions") ConverterOptions options) {
        return getWebClient(options);
    }

    @Bean
    public WebClient aiSliceWebClient(@Qualifier("aiSliceConverterOptions") ConverterOptions options) {
        return getWebClient(options);
    }

    private WebClient getWebClient(ConverterOptions options) {
        var httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, options.getConnectTimeout())
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(options.getReadTimeout(), TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(options.getWriteTimeout(), TimeUnit.MILLISECONDS))
                );
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeaders(headers -> {
                    if (options.getHeaders() != null) {
                        options.getHeaders().forEach(headers::add);
                    }

                })
                .build();
    }

    @Bean
    @ConfigurationProperties(prefix = "oss")
    public ObjectStorageOptions objectStorageOptions() {
        return new ObjectStorageOptions();
    }

    @Bean
    @ConfigurationProperties(prefix = "oss.client")
    public ClientBuilderConfiguration clientBuilderConfiguration() {
        return new ClientBuilderConfiguration();
    }

    @Bean
    public OSS ossClient(@Qualifier("objectStorageOptions") ObjectStorageOptions options,
                         ClientBuilderConfiguration clientBuilderConfiguration) {
        return new OSSClientBuilder()
                .build(
                        options.getEndpoint(),
                        options.getAccessKey(),
                        options.getAccessSecret(),
                        clientBuilderConfiguration);
    }

    @Bean
    public DataBufferFactory dataBufferFactory() {
        return new DefaultDataBufferFactory();
    }

    private String getHostIp() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                var networkInterface = networkInterfaces.nextElement();
                if (networkInterface.isUp() && !networkInterface.isLoopback()) {
                    var addresses = networkInterface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        var address = addresses.nextElement();
                        if (!address.isLoopbackAddress() && !address.getHostAddress().contains(":")) {
                            return address.getHostAddress();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warning("Failed to get preferred IP address.");

        }
        return "127.0.0.1";
    }

    @Bean
    public String getMdCallbackUrl(@Value("${server.port}") int port) {
        var ip = getHostIp();
        return String.format("http://%s:%d/md-convert/callback", ip, port);
    }

    @Bean
    public String getSliceCallbackUrl(@Value("${server.port}") int port) {
        var ip = getHostIp();
        return String.format("http://%s:%d/slice/callback", ip, port);
    }
}
