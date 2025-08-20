package com.trina.visiontask;

import lombok.AllArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@AllArgsConstructor(onConstructor_ = @Autowired)
public class RabbitMQConfig {

    private final Config config;


    // 创建队列
    @Bean
    public Queue uploadQueue() {
        Map<String, Object> args = Map.of("x-max-priority", config.uploadQueueXMaxPriority);
        return QueueBuilder.durable(config.uploadQueueName)
                .withArguments(args)
                .build();
    }

    @Bean
    public Queue pdfConvertQueue() {
        Map<String, Object> args = Map.of("x-max-priority", config.pdfConvertQueueXMaxPriority);
        return QueueBuilder.durable(config.pdfConvertQueueName)
                .withArguments(args)
                .build();
    }

    @Bean
    public Queue mdConvertQueue() {
        Map<String, Object> args = Map.of("x-max-priority", config.mdConvertQueueXMaxPriority);
        return QueueBuilder.durable(config.mdConvertQueueName)
                .withArguments(args)
                .build();
    }

    @Bean
    public Queue aiSliceQueue() {
        Map<String, Object> args = Map.of("x-max-priority", config.aiSliceQueueXMaxPriority);
        return QueueBuilder.durable(config.aiSliceQueueName)
                .withArguments(args)
                .build();
    }

    // 创建交换机
    @Bean
    public TopicExchange uploadExchange() {
        return ExchangeBuilder.topicExchange(config.uploadExchangeName)
                .durable(true)
                .build();
    }

    @Bean
    public TopicExchange pdfConvertExchange() {
        return ExchangeBuilder.topicExchange(config.pdfConvertExchangeName)
                .durable(true)
                .build();
    }

    @Bean
    public TopicExchange mdConvertExchange() {
        return ExchangeBuilder.topicExchange(config.mdConvertExchangeName)
                .durable(true)
                .build();
    }

    @Bean
    public TopicExchange aiSliceExchange() {
        return ExchangeBuilder.topicExchange(config.aiSliceExchangeName)
                .durable(true)
                .build();
    }

    // 绑定队列和交换机
    @Bean
    public Binding uploadBinding() {
        return BindingBuilder.bind(uploadQueue())
                .to(uploadExchange())
                .with(config.uploadRoutingKey);
    }

    @Bean
    public Binding pdfConvertBinding() {
        return BindingBuilder.bind(pdfConvertQueue())
                .to(pdfConvertExchange())
                .with(config.pdfConvertRoutingKey);
    }

    @Bean
    public Binding mdConvertBinding() {
        return BindingBuilder.bind(mdConvertQueue())
                .to(mdConvertExchange())
                .with(config.mdConvertRoutingKey);
    }

    @Bean
    public Binding aiSliceBinding() {
        return BindingBuilder.bind(aiSliceQueue())
                .to(aiSliceExchange())
                .with(config.aiSliceRoutingKey);
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
