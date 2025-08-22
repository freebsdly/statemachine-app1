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
}
