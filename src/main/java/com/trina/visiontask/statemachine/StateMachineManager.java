package com.trina.visiontask.statemachine;

import com.trina.visiontask.FileProcessingEvent;
import com.trina.visiontask.FileProcessingState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineBuilder;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class StateMachineManager {
    private static final Logger log = LoggerFactory.getLogger(StateMachineManager.class);
    private final Map<String, StateMachine<FileProcessingState, FileProcessingEvent>> machines = new HashMap<>();

    private final FailureAction failureAction;
    private final FileUploadAction fileUploadAction;
    private final MdConvertSubmitAction mdConvertSubmitAction;
    private final PdfConvertAction pdfConvertAction;
    private final AiSliceSubmitAction aiSliceSubmitAction;

    public StateMachineManager(FailureAction failureAction,
                               FileUploadAction fileUploadAction,
                               MdConvertSubmitAction mdConvertSubmitAction,
                               PdfConvertAction pdfConvertAction,
                               AiSliceSubmitAction aiSliceSubmitAction) {
        this.failureAction = failureAction;
        this.fileUploadAction = fileUploadAction;
        this.mdConvertSubmitAction = mdConvertSubmitAction;
        this.pdfConvertAction = pdfConvertAction;
        this.aiSliceSubmitAction = aiSliceSubmitAction;
    }

    public StateMachine<FileProcessingState, FileProcessingEvent> acquireStateMachine(
            FileProcessingState initState, String machineId
    ) throws Exception {
        StateMachine<FileProcessingState, FileProcessingEvent> stateMachine;
        // naive sync to handle concurrency with release
        synchronized (machines) {
            stateMachine = machines.get(machineId);
            if (stateMachine == null) {
                stateMachine = buildStateMachine(initState, machineId);
                machines.put(machineId, stateMachine);
            } else {
                throw new Exception("state machine already exists");
            }
        }

        return stateMachine;
    }

    public Optional<StateMachine<FileProcessingState, FileProcessingEvent>> getStateMachine(String machineId) {
        StateMachine<FileProcessingState, FileProcessingEvent> stateMachine;
        // naive sync to handle concurrency with release
        synchronized (machines) {
            stateMachine = machines.get(machineId);
        }

        return Optional.ofNullable(stateMachine);
    }

    public void releaseStateMachine(String machineId) {
        synchronized (machines) {
            machines.remove(machineId);
        }
    }


    private StateMachine<FileProcessingState, FileProcessingEvent> buildStateMachine(FileProcessingState initState, String id)
            throws Exception {
        var builder = StateMachineBuilder.<FileProcessingState, FileProcessingEvent>builder();
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
                // 这里由consumer从消息队列中获取消息
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
                // 这里由consumer从消息队列中获取消息
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
                .and()
                // Markdown转换已提交 -> 失败
                .withExternal()
                .source(FileProcessingState.MARKDOWN_CONVERT_SUBMITTED)
                .target(FileProcessingState.FAILED)
                .event(FileProcessingEvent.MD_CONVERT_FAILURE)
                .action(failureAction)
                .and()
                // 由consumer 从消息队列中获取消息
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
                .autoStartup(false);

        return builder.build();
    }
}
