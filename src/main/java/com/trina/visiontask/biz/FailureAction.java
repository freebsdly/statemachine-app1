package com.trina.visiontask.biz;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor(onConstructor_ = @Autowired)
public class FailureAction implements Action<FileProcessingState, FileProcessingEvent> {

    @Override
    public void execute(StateContext<FileProcessingState, FileProcessingEvent> context) {
        String errorMessage = (String) context.getMessageHeader("error");
        log.info("处理失败: " + (errorMessage != null ? errorMessage : "未知错误"));

        // 可以在这里添加失败后的处理逻辑，如记录日志、发送通知等
    }
}
