package com.trina.visiontask;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class RabbitMQConfig
{

    @Value("${upload.consumer.queue.name}")
    private String queueName;

    @Value("${upload.consumer.queue.x-max-priority}")
    private Integer queueXMaxPriority;

    @Value("${upload.consumer.exchange.name}")
    private String exchangeName;

    @Value("${upload.consumer.routing.key}")
    private String routingKey;

    // 创建队列
    @Bean
    public Queue queue()
    {
        Map<String, Object> args = Map.of("x-max-priority", queueXMaxPriority);
        return QueueBuilder.durable(queueName)
                .withArguments(args)
                .build();
    }

    // 创建交换机
    @Bean
    public TopicExchange exchange()
    {
        return ExchangeBuilder.topicExchange(exchangeName)
                .durable(true)
                .build();
    }

    // 绑定队列和交换机
    @Bean
    public Binding binding()
    {
        return BindingBuilder.bind(queue())
                .to(exchange())
                .with(routingKey);
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
}
