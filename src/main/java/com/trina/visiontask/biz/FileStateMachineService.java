package com.trina.visiontask.biz;

import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class FileStateMachineService
{

    private final Map<String, StateMachine<FileProcessingState, FileProcessingEvent>> machines = new HashMap<>();
    private final FileUploadAction fileUploadAction;
    private final PdfConvertAction pdfConvertAction;
    private final MdConvertSubmitAction mdConvertSubmitAction;
    private final MdConvertCallbackAction mdConvertCallbackAction;
    private final AiSliceSubmitAction aiSliceSubmitAction;
    private final FailureAction failureAction;

    public FileStateMachineService(
            FileUploadAction fileUploadAction,
            PdfConvertAction pdfConvertAction,
            MdConvertSubmitAction mdConvertSubmitAction,
            MdConvertCallbackAction mdConvertCallbackAction,
            AiSliceSubmitAction aiSliceSubmitAction,
            FailureAction failureAction)
    {
        this.fileUploadAction = fileUploadAction;
        this.pdfConvertAction = pdfConvertAction;
        this.mdConvertSubmitAction = mdConvertSubmitAction;
        this.mdConvertCallbackAction = mdConvertCallbackAction;
        this.aiSliceSubmitAction = aiSliceSubmitAction;
        this.failureAction = failureAction;
    }

    public final void destroy() throws Exception
    {
        doStop();
    }

    public StateMachine<FileProcessingState, FileProcessingEvent> acquireStateMachine(
            String machineId,
            FileProcessingState initialState,
            boolean start) throws Exception
    {
        StateMachine<FileProcessingState, FileProcessingEvent> stateMachine;
        // naive sync to handle concurrency with release
        synchronized (machines) {
            stateMachine = machines.get(machineId);
            if (stateMachine == null) {
                stateMachine = buildStateMachine(initialState, machineId, start);
                machines.put(machineId, stateMachine);
            }
        }
        return stateMachine;
    }

    public void releaseStateMachine(String machineId)
    {
        synchronized (machines) {
            StateMachine<FileProcessingState, FileProcessingEvent> stateMachine = machines.remove(machineId);
            if (stateMachine != null) {
                stateMachine.stopReactively().block();
            }
        }
    }

    public boolean hasStateMachine(String machineId)
    {
        synchronized (machines) {
            return machines.containsKey(machineId);
        }
    }


    protected void doStop()
    {
        synchronized (machines) {
            ArrayList<String> machineIds = new ArrayList<>(machines.keySet());
            for (String machineId : machineIds) {
                releaseStateMachine(machineId);
            }
        }
    }

    private StateMachine<FileProcessingState, FileProcessingEvent> buildStateMachine(FileProcessingState initState,
                                                                                     String id, boolean autoStart)
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
                .autoStartup(autoStart);

        return builder.build();
    }

}

