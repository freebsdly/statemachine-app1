package com.trina.visiontask.statemachine;


import com.trina.visiontask.FileProcessingEvent;
import com.trina.visiontask.FileProcessingState;
import com.trina.visiontask.TaskConfiguration;
import com.trina.visiontask.service.MessageProducer;
import com.trina.visiontask.service.TaskDTO;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

@Component
public class FailureAction implements Action<FileProcessingState, FileProcessingEvent> {
    private static final Logger log = LoggerFactory.getLogger(FailureAction.class);
    private final MessageProducer messageProducer;
    private final TaskConfiguration taskConfiguration;

    public FailureAction(
            MessageProducer messageProducer,
            TaskConfiguration taskConfiguration
    ) {
        this.messageProducer = messageProducer;
        this.taskConfiguration = taskConfiguration;
    }

    @Timed(value = "failure.retry", description = "failure action")
    @Override
    public void execute(StateContext<FileProcessingState, FileProcessingEvent> context) {
        TaskDTO taskInfo = (TaskDTO) context.getMessage().getHeaders().get(taskConfiguration.getTaskInfoKey());
        if (taskInfo == null) {
            log.error("taskInfo is null");
            return;
        }

        int retryCount = taskInfo.getRetryCount();
        if (retryCount < taskConfiguration.getMaxRetryCount()) {
            taskInfo.setRetryCount(retryCount + 1);
            taskInfo.setPriority(calculatePriority(taskInfo.getPriority()));
            taskInfo.setMessage(null);

            String errorMessage = (String) context.getMessageHeader("error");
            log.warn("{}, retry {}", errorMessage, taskInfo.getRetryCount());

            FileProcessingEvent event = context.getEvent();
            switch (event) {
                case UPLOAD_FAILURE -> messageProducer.sendToUploadQueue(taskInfo);
                case PDF_CONVERT_FAILURE -> messageProducer.sendToPdfConvertQueue(taskInfo);
                case MD_CONVERT_SUBMIT_FAILURE, MD_CONVERT_FAILURE -> messageProducer.sendToMdConvertQueue(taskInfo);
                case AI_SLICE_SUBMIT_FAILURE, AI_SLICE_FAILURE -> messageProducer.sendToAiSliceQueue(taskInfo);
                default -> log.error("unknown event: {}", event);
            }
        }
    }

    public int calculatePriority(int currentPriority) {
        // 使用偏移量确保从0开始也能递增
        double exponent = (currentPriority + 1) / 3.0;
        int newPriority = (int) Math.round(Math.pow(2, exponent));
        // 确保结果至少比输入值大（除非达到上限）
        return Math.min(Math.max(newPriority, currentPriority + 1), 10);
    }
}
