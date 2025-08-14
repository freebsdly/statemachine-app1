package com.trina.visiontask;

import com.trina.visiontask.biz.FileInfo;
import com.trina.visiontask.biz.FileProcessingEvent;
import com.trina.visiontask.biz.FileProcessingService;
import com.trina.visiontask.biz.FileProcessingState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Api implements ApiDoc {

    @Autowired
    private FileProcessingService fileProcessingService;

    @PostMapping("/process-file")
    public String processFile(@RequestParam("state") FileProcessingState state, @RequestParam("event") FileProcessingEvent event) {
        try {
            // 创建文件信息对象
            FileInfo fileInfo = new FileInfo(
                    "aaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                    "txt",
                    "aaaaaaaaaaaaaaaaaaaaaaaaaaa",
                    1000
            );

//            FileProcessingEvent event = switch (eventName.toUpperCase()) {
//                case "UPLOAD_START" -> FileProcessingEvent.UPLOAD_START;
//                case "UPLOAD_SUCCESS" -> FileProcessingEvent.UPLOAD_SUCCESS;
//                case "UPLOAD_FAILURE" -> FileProcessingEvent.UPLOAD_FAILURE;
//                case "PDF_CONVERT_START" -> FileProcessingEvent.PDF_CONVERT_START;
//                case "PDF_CONVERT_SUCCESS" -> FileProcessingEvent.PDF_CONVERT_SUCCESS;
//                case "PDF_CONVERT_FAILURE" -> FileProcessingEvent.PDF_CONVERT_FAILURE;
//                case "MD_CONVERT_START" -> FileProcessingEvent.MD_CONVERT_START;
//                case "MD_CONVERT_SUCCESS" -> FileProcessingEvent.MD_CONVERT_SUCCESS;
//                case "MD_CONVERT_FAILURE" -> FileProcessingEvent.MD_CONVERT_FAILURE;
//                case "AI_PROCESS_START" -> FileProcessingEvent.AI_PROCESS_START;
//                case "AI_PROCESS_SUCCESS" -> FileProcessingEvent.AI_PROCESS_SUCCESS;
//                case "AI_PROCESS_FAILURE" -> FileProcessingEvent.AI_PROCESS_FAILURE;
//                default -> throw new IllegalArgumentException("无效的事件名称: " + eventName);
//            };
            // 启动文件处理流程
            fileProcessingService.processFile(state, event, fileInfo);
            return "文件处理流程已启动";
        } catch (Exception e) {
            return "处理失败: " + e.getMessage();
        }
    }
}
