package com.trina.visiontask.biz;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FailureAction implements Action<FileProcessingState, FileProcessingEvent>
{

    private final MessageProducer messageProducer;
    private final String taskInfoKey;

    public FailureAction(MessageProducer messageProducer, @Qualifier("taskInfoKey") String taskInfoKey)
    {
        this.messageProducer = messageProducer;
        this.taskInfoKey = taskInfoKey;
    }

    @Override
    public void execute(StateContext<FileProcessingState, FileProcessingEvent> context)
    {
        String errorMessage = (String) context.getMessageHeader("error");
        log.info("处理失败: " + (errorMessage != null ? errorMessage : "未知错误"));
        TaskInfo taskInfo = (TaskInfo) context.getMessage().getHeaders().get(taskInfoKey);

        // 可以在这里添加失败后的处理逻辑，如记录日志、发送通知等
    }

    public int calculatePriority(int currentPriority)
    {
        // 使用指数提升: 2^(currentPriority/3)，但不超过最大值10
        int newPriority = (int) Math.pow(2, currentPriority / 3.0);
        return Math.min(newPriority, 10);
    }
}
