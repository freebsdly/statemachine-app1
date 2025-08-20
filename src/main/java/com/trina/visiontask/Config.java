package com.trina.visiontask;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Config {

    @Value("${upload.consumer.queue.name}")
    public String uploadQueueName;

    @Value("${upload.consumer.queue.x-max-priority}")
    public Integer uploadQueueXMaxPriority;

    @Value("${upload.consumer.exchange.name}")
    public String uploadExchangeName;

    @Value("${upload.consumer.routing.key}")
    public String uploadRoutingKey;

    @Value("${pdf-convert.consumer.queue.name}")
    public String pdfConvertQueueName;

    @Value("${pdf-convert.consumer.queue.x-max-priority}")
    public Integer pdfConvertQueueXMaxPriority;

    @Value("${pdf-convert.consumer.exchange.name}")
    public String pdfConvertExchangeName;

    @Value("${pdf-convert.consumer.routing.key}")
    public String pdfConvertRoutingKey;

    @Value("${md-convert.consumer.queue.name}")
    public String mdConvertQueueName;

    @Value("${md-convert.consumer.queue.x-max-priority}")
    public Integer mdConvertQueueXMaxPriority;

    @Value("${md-convert.consumer.exchange.name}")
    public String mdConvertExchangeName;

    @Value("${md-convert.consumer.routing.key}")
    public String mdConvertRoutingKey;

    @Value("${ai-slice.consumer.queue.name}")
    public String aiSliceQueueName;

    @Value("${ai-slice.consumer.queue.x-max-priority}")
    public Integer aiSliceQueueXMaxPriority;

    @Value("${ai-slice.consumer.exchange.name}")
    public String aiSliceExchangeName;

    @Value("${ai-slice.consumer.routing.key}")
    public String aiSliceRoutingKey;
}
