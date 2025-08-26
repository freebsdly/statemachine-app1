package com.trina.visiontask.biz;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

@Component
public class FailureAction implements Action<FileProcessingState, FileProcessingEvent> {
    private static final Logger log = LoggerFactory.getLogger(FailureAction.class);
    private final MessageProducer messageProducer;
    private final String taskInfoKey;
    private final int maxRetryCount;

    public FailureAction(
            MessageProducer messageProducer,
            @Qualifier("taskInfoKey") String taskInfoKey,
            @Qualifier("maxRetryCount") int maxRetryCount
    ) {
        this.messageProducer = messageProducer;
        this.taskInfoKey = taskInfoKey;
        this.maxRetryCount = maxRetryCount;
    }

    @Override
    public void execute(StateContext<FileProcessingState, FileProcessingEvent> context) {
        TaskInfo taskInfo = (TaskInfo) context.getMessage().getHeaders().get(taskInfoKey);
        if (taskInfo == null) {
            log.error("TaskInfo is null");
            return;
        }

        int retryCount = taskInfo.getRetryCount();
        if (retryCount < maxRetryCount) {
            taskInfo.setRetryCount(retryCount + 1);
            taskInfo.setPriority(calculatePriority(taskInfo.getPriority()));
            taskInfo.setMessage(null);

            String errorMessage = (String) context.getMessageHeader("error");
            log.warn("{}, retry {}", errorMessage, taskInfo.getRetryCount());

            FileProcessingEvent event = context.getEvent();
            switch (event) {
                case UPLOAD_FAILURE -> messageProducer.sendToUploadQueue(taskInfo);
                case PDF_CONVERT_FAILURE -> messageProducer.sendToPdfConvertQueue(taskInfo);
                case MD_CONVERT_FAILURE -> messageProducer.sendToMdConvertQueue(taskInfo);
                case AI_SLICE_FAILURE -> messageProducer.sendToAiSliceQueue(taskInfo);
                default -> log.error("unknown event: {}", event);
            }
        }
    }

    public int calculatePriority(int currentPriority) {
        // 使用指数提升: 2^(currentPriority/3)，但不超过最大值10
        int newPriority = (int) Math.round(Math.pow(2, currentPriority / 3.0));
        return Math.min(newPriority, 10);
    }
}
