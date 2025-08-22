package com.trina.visiontask.biz;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineBuilder;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.EnumSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class FileProcessingService
{

    // 注入各个步骤的处理器
    private final FileUploadAction fileUploadAction;

    private final PdfConvertAction pdfConvertAction;

    private final MarkdownConvertAction markdownConvertAction;

    private final AiSliceAction aiSliceAction;

    private final FailureAction failureAction;

    private final String taskInfoKey;

    private final long waitTimeout;

    public FileProcessingService(FileUploadAction fileUploadAction,
                                 PdfConvertAction pdfConvertAction,
                                 MarkdownConvertAction markdownConvertAction,
                                 AiSliceAction aiSliceAction,
                                 FailureAction failureAction,
                                 @Qualifier("taskInfoKey") String taskInfoKey,
                                 @Qualifier("waitTimeout") long timeout)
    {
        this.fileUploadAction = fileUploadAction;
        this.pdfConvertAction = pdfConvertAction;
        this.markdownConvertAction = markdownConvertAction;
        this.aiSliceAction = aiSliceAction;
        this.failureAction = failureAction;
        this.taskInfoKey = taskInfoKey;
        this.waitTimeout = timeout;
    }


    public boolean processFile(FileProcessingState initState, FileProcessingEvent event, TaskInfo taskInfo)
            throws Exception
    {
        // 这里使用builder创建状态机，以便从指定的初始状态开始，以便从指定的初始状态开始，例如文件已经上传，从UPLOADED状态开始，发送事件PDF_CONVERT_START事件，开始PDF转换
        StateMachine<FileProcessingState, FileProcessingEvent> stateMachine = buildStateMachine(initState);
        CountDownLatch completionLatch = new CountDownLatch(1);

        StateMachineListenerAdapter<FileProcessingState, FileProcessingEvent> listener = new StateMachineListenerAdapter<>()
        {
            @Override
            public void stateChanged(State<FileProcessingState, FileProcessingEvent> from,
                                     State<FileProcessingState, FileProcessingEvent> to)
            {
                FileProcessingState from_state = null;
                FileProcessingState to_state = null;
                if (from != null) {
                    from_state = from.getId();
                }
                if (to != null) {
                    to_state = to.getId();
                }

                log.info("State change from {} to {}", from_state, to_state);

                // 检查是否到达最终状态
                if (to_state == FileProcessingState.COMPLETED
                        || to_state == FileProcessingState.FAILED
                        || to_state == FileProcessingState.UPLOADED
                        || to_state == FileProcessingState.PDF_CONVERTED
                        || to_state == FileProcessingState.MARKDOWN_CONVERTED
                ) {
                    completionLatch.countDown();
                }
            }
        };
        stateMachine.addStateListener(listener);
        Message<FileProcessingEvent> message = MessageBuilder.withPayload(event)
                .setHeader(taskInfoKey, taskInfo)
                .build();
        // 发送初始事件，开始文件处理流程
        stateMachine.sendEvent(Mono.just(message)).blockLast();

        // 等待处理完成
        return completionLatch.await(waitTimeout, TimeUnit.SECONDS);
    }

    private StateMachine<FileProcessingState, FileProcessingEvent> buildStateMachine(FileProcessingState initState)
            throws Exception
    {
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
                .action(markdownConvertAction)
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
                .withExternal()
                .source(FileProcessingState.MARKDOWN_CONVERT_SUBMITTED)
                .target(FileProcessingState.MARKDOWN_CONVERTED)
                .event(FileProcessingEvent.MD_CONVERT_SUCCESS)
                .and()
                // Markdown转换已提交 -> 失败
                .withExternal()
                .source(FileProcessingState.MARKDOWN_CONVERT_SUBMITTED)
                .target(FileProcessingState.FAILED)
                .event(FileProcessingEvent.MD_CONVERT_FAILURE)
                .action(failureAction)
                .and()
                // Markdown转换完成 -> AI切片已提交
                .withExternal()
                .source(FileProcessingState.MARKDOWN_CONVERTED)
                .target(FileProcessingState.AI_SLICE_SUBMITTED)
                .event(FileProcessingEvent.AI_SLICE_START)
                .action(aiSliceAction)
                .and()
                // Markdown转换完成 -> AI切片提交失败
                .withExternal()
                .source(FileProcessingState.MARKDOWN_CONVERTED)
                .target(FileProcessingState.FAILED)
                .event(FileProcessingEvent.AI_SLICE_FAILURE)
                .action(failureAction)
                .and()
                // AI切片已提交 -> AI处理中
                .withExternal()
                .source(FileProcessingState.AI_SLICE_SUBMITTED).target(FileProcessingState.AI_SLICING)
                .event(FileProcessingEvent.AI_SLICE_SUCCESS)
                .action(aiSliceAction)
                .and()
                // AI处理中 -> AI处理完成
                // AI处理中 -> 全部完成
                .withExternal()
                .source(FileProcessingState.AI_SLICING)
                .target(FileProcessingState.COMPLETED)
                .event(FileProcessingEvent.AI_SLICE_SUCCESS)
                .and()
                // AI处理中 -> 失败
                .withExternal()
                .source(FileProcessingState.AI_SLICING)
                .target(FileProcessingState.FAILED)
                .event(FileProcessingEvent.AI_SLICE_FAILURE)
                .action(failureAction);

        builder.configureConfiguration()
                .withConfiguration()
                .autoStartup(true);

        return builder.build();
    }
}