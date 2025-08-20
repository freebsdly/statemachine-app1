package com.trina.visiontask.biz;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineBuilder;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.EnumSet;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class FileProcessingService
{

    // 注入各个步骤的处理器
    private final FileUploadAction fileUploadAction;

    private final PdfConvertAction pdfConvertAction;

    private final MarkdownConvertAction markdownConvertAction;

    private final AiSliceAction aiSliceAction;

    private final FailureAction failureAction;


    public void processFile(FileProcessingState initState, FileProcessingEvent event, FileInfo fileInfo)
            throws Exception
    {
        // 这里使用builder创建状态机，以便从指定的初始状态开始，例如文件已经上传，从UPLOADED状态开始，发送事件PDF_CONVERT_START事件，开始PDF转换
        StateMachine<FileProcessingState, FileProcessingEvent> stateMachine = buildStateMachine(initState);
        Message<FileProcessingEvent> message = MessageBuilder.withPayload(event)
                .setHeader("fileInfo", fileInfo)
                .build();
        // 发送初始事件，开始文件处理流程
        stateMachine.sendEvent(Mono.just(message)).blockLast();
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
        builder.configureTransitions()
                .withExternal()
                .source(FileProcessingState.INITIAL).target(FileProcessingState.UPLOADING)
                .event(FileProcessingEvent.UPLOAD_START)
                .action(fileUploadAction)
                .and()
                // 上传中 -> 上传完成
                .withExternal()
                .source(FileProcessingState.UPLOADING).target(FileProcessingState.UPLOADED)
                .event(FileProcessingEvent.UPLOAD_SUCCESS)
                .and()
                // 上传中 -> 失败
                .withExternal()
                .source(FileProcessingState.UPLOADING).target(FileProcessingState.FAILED)
                .event(FileProcessingEvent.UPLOAD_FAILURE)
                .action(failureAction)
                .and()
                // 上传完成 -> PDF转换中
                .withExternal()
                .source(FileProcessingState.UPLOADED).target(FileProcessingState.CONVERTING_TO_PDF)
                .event(FileProcessingEvent.PDF_CONVERT_START)
                .action(pdfConvertAction)
                .and()
                // PDF转换中 -> PDF转换完成
                .withExternal()
                .source(FileProcessingState.CONVERTING_TO_PDF).target(FileProcessingState.PDF_CONVERTED)
                .event(FileProcessingEvent.PDF_CONVERT_SUCCESS)
                .and()
                // PDF转换中 -> 失败
                .withExternal()
                .source(FileProcessingState.CONVERTING_TO_PDF).target(FileProcessingState.FAILED)
                .event(FileProcessingEvent.PDF_CONVERT_FAILURE)
                .action(failureAction)
                .and()
                // PDF转换完成 -> Markdown转换中
                .withExternal()
                .source(FileProcessingState.PDF_CONVERTED).target(FileProcessingState.CONVERTING_TO_MARKDOWN)
                .event(FileProcessingEvent.MD_CONVERT_START)
                .action(markdownConvertAction)
                .and()
                // Markdown转换中 -> Markdown转换完成
                .withExternal()
                .source(FileProcessingState.CONVERTING_TO_MARKDOWN).target(FileProcessingState.MARKDOWN_CONVERTED)
                .event(FileProcessingEvent.MD_CONVERT_SUCCESS)
                .and()
                // Markdown转换中 -> 失败
                .withExternal()
                .source(FileProcessingState.CONVERTING_TO_MARKDOWN).target(FileProcessingState.FAILED)
                .event(FileProcessingEvent.MD_CONVERT_FAILURE)
                .action(failureAction)
                .and()
                // Markdown转换完成 -> AI处理中
                .withExternal()
                .source(FileProcessingState.MARKDOWN_CONVERTED).target(FileProcessingState.AI_PROCESSING)
                .event(FileProcessingEvent.AI_SLICE_START)
                .action(aiSliceAction)
                .and()
                // AI处理中 -> 全部完成
                .withExternal()
                .source(FileProcessingState.AI_PROCESSING).target(FileProcessingState.COMPLETED)
                .event(FileProcessingEvent.AI_SLICE_SUCCESS)
                .and()
                // AI处理中 -> 失败
                .withExternal()
                .source(FileProcessingState.AI_PROCESSING).target(FileProcessingState.FAILED)
                .event(FileProcessingEvent.AI_SLICE_FAILURE)
                .action(failureAction);

        builder.configureConfiguration()
                .withConfiguration()
                .autoStartup(true)
                .listener(new StateMachineListenerAdapter<>()
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
                        //TODO: 在事件监听器中统一更新状态，注意失败时的状态处理
                    }
                });

        return builder.build();
    }
}