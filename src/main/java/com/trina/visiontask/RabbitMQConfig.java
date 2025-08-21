package com.trina.visiontask;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class RabbitMQConfig {

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

    // 为每个队列创建一个配置Bean，包含队列、交换机和绑定
    @Bean
    public Declarables uploadQueueConfig(@Qualifier("uploadConfiguration") MQConfiguration mqConfig) {
        return getDeclarables(mqConfig);
    }

    @Bean
    public Declarables pdfConvertQueueConfig(@Qualifier("pdfConvertConfiguration") MQConfiguration mqConfig) {
        return getDeclarables(mqConfig);
    }

    @Bean
    public Declarables mdConvertQueueConfig(@Qualifier("mdConvertConfiguration") MQConfiguration mqConfig) {
        return getDeclarables(mqConfig);
    }

    @Bean
    public Declarables aiSliceQueueConfig(@Qualifier("aiSliceConfiguration") MQConfiguration mqConfig) {
        return getDeclarables(mqConfig);
    }

    private Declarables getDeclarables(MQConfiguration mqConfig) {
        Map<String, Object> args = Map.of("x-max-priority", mqConfig.getXMaxPriority());
        Queue queue = QueueBuilder.durable(mqConfig.getQueueName())
                .withArguments(args)
                .build();
        TopicExchange exchange = ExchangeBuilder.topicExchange(mqConfig.getExchangeName())
                .durable(true)
                .build();
        Binding binding = BindingBuilder.bind(queue)
                .to(exchange)
                .with(mqConfig.getRoutingKey());
        return new Declarables(queue, exchange, binding);
    }

    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jackson2JsonMessageConverter());
        return template;
    }
}