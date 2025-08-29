package com.trina.visiontask;

import lombok.Data;

@Data
public class MQConfiguration
{
    /**
     * enable or disable RabbitMQ Consumer
     */
    private boolean enabled;

    /**
     * RabbitMQ Queue Name
     */
    private String queueName;

    /**
     * RabbitMQ Exchange Name
     */
    private String exchangeName;

    /**
     * RabbitMQ Routing Key
     */
    private String routingKey;

    /**
     * RabbitMQ Queue x-max-priority
     */
    private Integer xMaxPriority;

    /**
     * RabbitMQ Consumer max concurrent consumers
     */
    private Integer maxConcurrentConsumers;

    /**
     * RabbitMQ Consumer concurrent consumers
     */
    private Integer concurrentConsumers;

    /**
     * RabbitMQ Consumer prefetch
     */
    private Integer prefetch;

    /**
     * RabbitMQ Consumer acknowledge mode
     */
    private String acknowledgeMode;

    /**
     * RabbitMQ queue count
     */
    private Integer queueCount;
}
