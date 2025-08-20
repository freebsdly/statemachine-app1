package com.trina.visiontask;

import com.trina.visiontask.biz.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.listener.StateMachineListener;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;

import java.util.EnumSet;

@Slf4j
@Configuration
@EnableStateMachineFactory
@AllArgsConstructor(onConstructor_ = @Autowired)
public class StateMachineConfig
        extends EnumStateMachineConfigurerAdapter<FileProcessingState, FileProcessingEvent>
{
    private final FileUploadAction fileUploadAction;

    private final PdfConvertAction pdfConvertAction;

    private final MarkdownConvertAction markdownConvertAction;

    private final AiSliceAction aiSliceAction;

    private final FailureAction failureAction;

    private final MessageProducer messageProducer;

    @Override
    public void configure(StateMachineConfigurationConfigurer<FileProcessingState, FileProcessingEvent> config)
            throws Exception
    {
        config
                .withConfiguration()
                .autoStartup(false)
                .listener(listener());
    }

    @Override
    public void configure(StateMachineStateConfigurer<FileProcessingState, FileProcessingEvent> states) throws Exception
    {
        states
                .withStates()
                .initial(FileProcessingState.INITIAL)
                .states(EnumSet.allOf(FileProcessingState.class))
                .end(FileProcessingState.COMPLETED)
                .end(FileProcessingState.FAILED);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<FileProcessingState, FileProcessingEvent> transitions)
            throws Exception
    {
        // 初始状态 -> 上传中
        transitions.withExternal()
                .source(FileProcessingState.INITIAL).target(FileProcessingState.UPLOADING)
                .event(FileProcessingEvent.UPLOAD_START)
                .action(fileUploadAction);

        // 上传中 -> 上传完成
        transitions.withExternal()
                .source(FileProcessingState.UPLOADING).target(FileProcessingState.UPLOADED)
                .event(FileProcessingEvent.UPLOAD_SUCCESS);

        // 上传中 -> 失败
        transitions.withExternal()
                .source(FileProcessingState.UPLOADING).target(FileProcessingState.FAILED)
                .event(FileProcessingEvent.UPLOAD_FAILURE)
                .action(failureAction);

        // 上传完成 -> PDF转换中
        transitions.withExternal()
                .source(FileProcessingState.UPLOADED).target(FileProcessingState.CONVERTING_TO_PDF)
                .event(FileProcessingEvent.PDF_CONVERT_START)
                .action(pdfConvertAction);

        // PDF转换中 -> PDF转换完成
        transitions.withExternal()
                .source(FileProcessingState.CONVERTING_TO_PDF).target(FileProcessingState.PDF_CONVERTED)
                .event(FileProcessingEvent.PDF_CONVERT_SUCCESS);

        // PDF转换中 -> 失败
        transitions.withExternal()
                .source(FileProcessingState.CONVERTING_TO_PDF).target(FileProcessingState.FAILED)
                .event(FileProcessingEvent.PDF_CONVERT_FAILURE)
                .action(failureAction);

        // PDF转换完成 -> Markdown转换中
        transitions.withExternal()
                .source(FileProcessingState.PDF_CONVERTED).target(FileProcessingState.CONVERTING_TO_MARKDOWN)
                .event(FileProcessingEvent.MD_CONVERT_START)
                .action(markdownConvertAction);

        // Markdown转换中 -> Markdown转换完成
        transitions.withExternal()
                .source(FileProcessingState.CONVERTING_TO_MARKDOWN).target(FileProcessingState.MARKDOWN_CONVERTED)
                .event(FileProcessingEvent.MD_CONVERT_SUCCESS);

        // Markdown转换中 -> 失败
        transitions.withExternal()
                .source(FileProcessingState.CONVERTING_TO_MARKDOWN).target(FileProcessingState.FAILED)
                .event(FileProcessingEvent.MD_CONVERT_FAILURE)
                .action(failureAction);

        // Markdown转换完成 -> AI处理中
        transitions.withExternal()
                .source(FileProcessingState.MARKDOWN_CONVERTED).target(FileProcessingState.AI_PROCESSING)
                .event(FileProcessingEvent.AI_SLICE_START)
                .action(aiSliceAction);

        // AI处理中 -> 全部完成
        transitions.withExternal()
                .source(FileProcessingState.AI_PROCESSING).target(FileProcessingState.COMPLETED)
                .event(FileProcessingEvent.AI_SLICE_SUCCESS);

        // AI处理中 -> 失败
        transitions.withExternal()
                .source(FileProcessingState.AI_PROCESSING).target(FileProcessingState.FAILED)
                .event(FileProcessingEvent.AI_SLICE_FAILURE)
                .action(failureAction);
    }

    @Bean
    public StateMachineListener<FileProcessingState, FileProcessingEvent> listener()
    {
        return new StateMachineListenerAdapter<>()
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
            }
        };
    }
}
