package com.trina.visiontask.biz;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineBuilder;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class FileProcessingService {

    // 注入各个步骤的处理器
    private final FileUploadAction fileUploadAction;

    private final PdfConvertAction pdfConvertAction;

    private final MdConvertSubmitAction mdConvertSubmitAction;

    private final MdConvertCallbackAction mdConvertCallbackAction;

    private final AiSliceSubmitAction aiSliceSubmitAction;

    private final FailureAction failureAction;

    private final MessageProducer messageProducer;

    private final String taskInfoKey;

    private final long waitTimeout;

    public FileProcessingService(
            FileUploadAction fileUploadAction,
            PdfConvertAction pdfConvertAction,
            MdConvertSubmitAction mdConvertSubmitAction,
            MdConvertCallbackAction mdConvertCallbackAction,
            AiSliceSubmitAction aiSliceSubmitAction,
            FailureAction failureAction,
            MessageProducer messageProducer,
            @Qualifier("taskInfoKey") String taskInfoKey,
            @Qualifier("waitTimeout") long timeout
    ) {
        this.fileUploadAction = fileUploadAction;
        this.pdfConvertAction = pdfConvertAction;
        this.mdConvertSubmitAction = mdConvertSubmitAction;
        this.mdConvertCallbackAction = mdConvertCallbackAction;
        this.aiSliceSubmitAction = aiSliceSubmitAction;
        this.failureAction = failureAction;
        this.messageProducer = messageProducer;
        this.taskInfoKey = taskInfoKey;
        this.waitTimeout = timeout;
    }

    public void processFile(FileProcessingState initState, FileProcessingEvent event, TaskInfo taskInfo)
            throws Exception {
        // 这里使用builder创建状态机，以便从指定的初始状态开始，以便从指定的初始状态开始，例如文件已经上传，从UPLOADED状态开始，发送事件PDF_CONVERT_START事件，开始PDF转换
        StateMachine<FileProcessingState, FileProcessingEvent> stateMachine = buildStateMachine(initState, taskInfo.getId().toString());
        CountDownLatch completionLatch = new CountDownLatch(1);
        if (taskInfo.getStartTime() == null) {
            taskInfo.setStartTime(LocalDateTime.now());
        }
        StateMachineListenerAdapter<FileProcessingState, FileProcessingEvent> listener = new StateMachineListenerAdapter<>() {
            private StateContext<FileProcessingState, FileProcessingEvent> context;

            @Override
            public void stateContext(StateContext<FileProcessingState, FileProcessingEvent> stateContext) {
                this.context = stateContext;
            }

            @Override
            public void transitionEnded(Transition<FileProcessingState, FileProcessingEvent> transition) {
                FileProcessingState source = transition.getSource().getId();
                FileProcessingState target = transition.getTarget().getId();
                FileProcessingEvent event = transition.getTrigger().getEvent();
                TaskInfo taskInfo = (TaskInfo) context.getMessage().getHeaders().get(taskInfoKey);

                if (taskInfo != null) {
                    taskInfo.setPreviousState(source);
                    taskInfo.setCurrentState(target);
                    taskInfo.setEvent(event);
                    if (taskInfo.getEndTime() == null) {
                        taskInfo.setEndTime(LocalDateTime.now());
                    }
                }
                log.info("State changed from {} to {}, triggered by {}", source, target, event);
                logTaskInfo(taskInfo);

                // 检查是否到达最终状态
                if (target == FileProcessingState.UPLOADED
                        || target == FileProcessingState.PDF_CONVERTED
                        || target == FileProcessingState.MARKDOWN_CONVERTED
                        || target == FileProcessingState.COMPLETED
                        || target == FileProcessingState.FAILED
                ) {
                    completionLatch.countDown();
                } else if (target == FileProcessingState.MARKDOWN_CONVERT_SUBMITTED
                        || target == FileProcessingState.AI_SLICE_SUBMITTED
                ) {
                    log.info("waiting for markdown or slice callback");
                }
            }

            private void logTaskInfo(TaskInfo taskInfo) {
                log.info("TaskInfo: {}", taskInfo);
                messageProducer.sendToTaskLogQueue(taskInfo);
            }
        };
        stateMachine.addStateListener(listener);
        Message<FileProcessingEvent> message = MessageBuilder.withPayload(event)
                .setHeader(taskInfoKey, taskInfo)
                .build();
        // 发送初始事件，开始文件处理流程
        stateMachine.sendEvent(Mono.just(message)).blockLast();

        // 等待处理完成
        boolean finished = completionLatch.await(waitTimeout, TimeUnit.SECONDS);
        if (!finished) {
            throw new Exception("state machine completion latch timeout");
        }
    }

    private StateMachine<FileProcessingState, FileProcessingEvent> buildStateMachine(FileProcessingState initState, String id)
            throws Exception {
        StateMachineBuilder.Builder<FileProcessingState, FileProcessingEvent> builder = StateMachineBuilder.builder();
        builder.configureStates()
                .withStates()
                .initial(initState)
                .states(EnumSet.allOf(FileProcessingState.class))
                .end(FileProcessingState.COMPLETED)
                .end(FileProcessingState.FAILED);
        builder
                .configureTransitions()
                .withExternal()
                .source(FileProcessingState.INITIAL)
                .target(FileProcessingState.UPLOADING)
                .event(FileProcessingEvent.UPLOAD_START)
                .action(fileUploadAction)
                .and()
                // 上传中 -> 上传完成
                .withExternal()
                .source(FileProcessingState.UPLOADING)
                .target(FileProcessingState.UPLOADED)
                .event(FileProcessingEvent.UPLOAD_SUCCESS)
                .and()
                // 上传中 -> 失败
                .withExternal()
                .source(FileProcessingState.UPLOADING)
                .target(FileProcessingState.FAILED)
                .event(FileProcessingEvent.UPLOAD_FAILURE)
                .action(failureAction)
                .and()
                // 上传完成 -> PDF转换中
                .withExternal()
                .source(FileProcessingState.UPLOADED)
                .target(FileProcessingState.PDF_CONVERTING)
                .event(FileProcessingEvent.PDF_CONVERT_START)
                .action(pdfConvertAction)
                .and()
                // PDF转换中 -> PDF转换完成
                .withExternal()
                .source(FileProcessingState.PDF_CONVERTING)
                .target(FileProcessingState.PDF_CONVERTED)
                .event(FileProcessingEvent.PDF_CONVERT_SUCCESS)
                .and()
                // PDF转换中 -> 失败
                .withExternal()
                .source(FileProcessingState.PDF_CONVERTING)
                .target(FileProcessingState.FAILED)
                .event(FileProcessingEvent.PDF_CONVERT_FAILURE)
                .action(failureAction)
                .and()
                // PDF转换完成 -> Markdown转换提交中
                .withExternal()
                .source(FileProcessingState.PDF_CONVERTED)
                .target(FileProcessingState.MARKDOWN_CONVERT_SUBMITTING)
                .event(FileProcessingEvent.MD_CONVERT_START)
                .action(mdConvertSubmitAction)
                .and()
                // Markdown转换提交中 -> Markdown转换已提交
                .withExternal()
                .source(FileProcessingState.MARKDOWN_CONVERT_SUBMITTING)
                .target(FileProcessingState.MARKDOWN_CONVERT_SUBMITTED)
                .event(FileProcessingEvent.MD_CONVERT_SUBMIT_SUCCESS)
                .and()
                // Markdown转换提交中 -> Markdown转换提交失败
                .withExternal()
                .source(FileProcessingState.MARKDOWN_CONVERT_SUBMITTING)
                .target(FileProcessingState.FAILED)
                .event(FileProcessingEvent.MD_CONVERT_SUBMIT_FAILURE)
                .action(failureAction)
                .and()
                // Markdown转换已提交 -> Markdown转换完成
                // 由Callback触发
                .withExternal()
                .source(FileProcessingState.MARKDOWN_CONVERT_SUBMITTED)
                .target(FileProcessingState.MARKDOWN_CONVERTED)
                .event(FileProcessingEvent.MD_CONVERT_SUCCESS)
                .action(mdConvertCallbackAction)
                .and()
                // Markdown转换已提交 -> 失败
                .withExternal()
                .source(FileProcessingState.MARKDOWN_CONVERT_SUBMITTED)
                .target(FileProcessingState.FAILED)
                .event(FileProcessingEvent.MD_CONVERT_FAILURE)
                .action(failureAction)
                .and()
                // Markdown转换完成 -> AI切片提交中
                .withExternal()
                .source(FileProcessingState.MARKDOWN_CONVERTED)
                .target(FileProcessingState.AI_SLICE_SUBMITTING)
                .event(FileProcessingEvent.AI_SLICE_START)
                .action(aiSliceSubmitAction)
                .and()
                // AI切片请求提交中 -> AI切片请求已提交
                .withExternal()
                .source(FileProcessingState.AI_SLICE_SUBMITTING)
                .target(FileProcessingState.AI_SLICE_SUBMITTED)
                .event(FileProcessingEvent.AI_SLICE_SUBMIT_SUCCESS)
                .and()
                // AI切片请求提交中 -> 失败
                .withExternal()
                .source(FileProcessingState.AI_SLICE_SUBMITTING)
                .target(FileProcessingState.FAILED)
                .event(FileProcessingEvent.AI_SLICE_SUBMIT_FAILURE)
                .action(failureAction)
                .and()
                // AI切片请求已提交 -> AI处理完成
                // 由Callback触发
                .withExternal()
                .source(FileProcessingState.AI_SLICE_SUBMITTED)
                .target(FileProcessingState.COMPLETED)
                .event(FileProcessingEvent.AI_SLICE_SUCCESS)
                .and()
                // AI切片请求已提交 -> 失败
                .withExternal()
                .source(FileProcessingState.AI_SLICE_SUBMITTED)
                .target(FileProcessingState.FAILED)
                .event(FileProcessingEvent.AI_SLICE_FAILURE)
                .action(failureAction);

        builder.configureConfiguration()
                .withConfiguration()
                .machineId(id)
                .autoStartup(true);

        return builder.build();
    }
}