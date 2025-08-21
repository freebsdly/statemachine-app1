package com.trina.visiontask;

import lombok.Data;

@Data
public class MQConfiguration {
    private boolean enabled;
    private String queueName;
    private String exchangeName;
    private String routingKey;
    private Integer xMaxPriority;
}
