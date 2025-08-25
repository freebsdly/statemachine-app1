package com.trina.visiontask;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

import java.util.Map;

@Configuration
public class RabbitMQConfig
{

    @Bean
    @ConfigurationProperties(prefix = "upload.consumer")
    public MQConfiguration uploadConfiguration()
    {
        return new MQConfiguration();
    }

    @Bean
    @ConfigurationProperties(prefix = "pdf-convert.consumer")
    public MQConfiguration pdfConvertConfiguration()
    {
        return new MQConfiguration();
    }

    @Bean
    @ConfigurationProperties(prefix = "md-convert.consumer")
    public MQConfiguration mdConvertConfiguration()
    {
        return new MQConfiguration();
    }

    @Bean
    @ConfigurationProperties(prefix = "ai-slice.consumer")
    public MQConfiguration aiSliceConfiguration()
    {
        return new MQConfiguration();
    }

    @Bean
    @ConfigurationProperties(prefix = "task-log.consumer")
    public MQConfiguration taskLogConfiguration()
    {
        return new MQConfiguration();
    }

    // 为每个队列创建一个配置Bean，包含队列、交换机和绑定
    @Bean
    public Declarables uploadQueueConfig(@Qualifier("uploadConfiguration") MQConfiguration mqConfig)
    {
        Map<String, Object> args = Map.of("x-max-priority", mqConfig.getXMaxPriority());
        return getDeclarables(mqConfig, args);
    }

    @Bean
    public Declarables pdfConvertQueueConfig(@Qualifier("pdfConvertConfiguration") MQConfiguration mqConfig)
    {
        Map<String, Object> args = Map.of("x-max-priority", mqConfig.getXMaxPriority());
        return getDeclarables(mqConfig, args);
    }

    @Bean
    public Declarables mdConvertQueueConfig(@Qualifier("mdConvertConfiguration") MQConfiguration mqConfig)
    {
        Map<String, Object> args = Map.of("x-max-priority", mqConfig.getXMaxPriority());
        return getDeclarables(mqConfig, args);
    }

    @Bean
    public Declarables aiSliceQueueConfig(@Qualifier("aiSliceConfiguration") MQConfiguration mqConfig)
    {
        Map<String, Object> args = Map.of("x-max-priority", mqConfig.getXMaxPriority());
        return getDeclarables(mqConfig, args);
    }

    @Bean
    public Declarables taskLogQueueConfig(@Qualifier("taskLogConfiguration") MQConfiguration mqConfig)
    {
        return getDeclarables(mqConfig, Map.of());
    }

    private Declarables getDeclarables(MQConfiguration mqConfig, Map<String, Object> args)
    {
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
    public MessageConverter jackson2JsonMessageConverter()
    {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory)
    {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jackson2JsonMessageConverter());
        return template;
    }

    @Bean("taskLogContainerFactory")
    public SimpleRabbitListenerContainerFactory taskLogContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            @Qualifier("taskLogConfiguration") MQConfiguration mqConfig)
    {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMaxConcurrentConsumers(mqConfig.getMaxConcurrentConsumers());
        factory.setConcurrentConsumers(mqConfig.getConcurrentConsumers());
        factory.setPrefetchCount(mqConfig.getPrefetch());
        factory.setAcknowledgeMode(AcknowledgeMode.valueOf(mqConfig.getAcknowledgeMode()));
        return factory;
    }
}