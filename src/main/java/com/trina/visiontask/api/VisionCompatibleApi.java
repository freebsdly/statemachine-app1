package com.trina.visiontask.api;

import cn.dev33.satoken.stp.StpUtil;
import com.trina.core.entity.vo.AuthUserVO;
import com.trina.visiontask.service.ReparseDTO;
import com.trina.visiontask.service.TaskDTO;
import com.trina.visiontask.service.TaskService;
import com.trina.visiontask.service.UploadDTO;
import com.trina.visiontask.statemachine.FileProcessingService;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class VisionCompatibleApi implements VisionCompatibleApiDoc {
    private static final Logger log = LoggerFactory.getLogger(VisionCompatibleApi.class);

    private final FileProcessingService fileProcessingService;
    private final ApiMapper apiMapper;
    private final TaskService taskService;

    public VisionCompatibleApi(
            FileProcessingService fileProcessingService,
            ApiMapper apiMapper,
            TaskService taskService) {
        this.fileProcessingService = fileProcessingService;
        this.apiMapper = apiMapper;
        this.taskService = taskService;
    }

    @Timed(value = "api.parseFile", description = "parse file")
    @GetMapping("/document/fileParse")
    @Override
    public CompatibleApiBody<Boolean> parseFile(@RequestParam("ossId") UUID fileId) throws Exception {
        AuthUserVO user = (AuthUserVO) StpUtil.getTokenSession().get("user");
        try {
            ReparseDTO dto = new ReparseDTO(fileId, user.getRealName());
            TaskDTO taskDTO = taskService.reParsePdfFile(dto);
            taskService.checkOssFileRecord(taskDTO);
            return CompatibleApiBody.success(true);
        } catch (Exception e) {
            return CompatibleApiBody.failure(e.getMessage());
        }
    }

    @Timed(value = "api.uploadDocument", description = "upload document")
    @PostMapping(value = "/document/uploadDocument", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Override
    public CompatibleApiBody<UploadDocumentResult> uploadDocument(
            @ModelAttribute UploadDocumentDTO dto) throws Exception {

        AuthUserVO user = null;
        if ("0".equals(dto.getTankId())) {
            user = (AuthUserVO) StpUtil.getTokenSession().get("user");
            if (user.getStaffNumber() != null && !user.getStaffNumber().isEmpty()) {
                dto.setTankId(user.getStaffNumber());
            }
        }
        try {
            UploadDTO uploadDTO = apiMapper.to(dto);
            if (user != null) {
                uploadDTO.setOperator(user.getRealName());
            }
            TaskDTO taskDTO = taskService.uploadFile(uploadDTO);
            taskService.checkOssFileRecord(taskDTO);
            return new CompatibleApiBody<>(200, "操作成功",
                    new UploadDocumentResult(taskDTO.getFileInfo().getFileId(), dto.getIsConflict()));
        } catch (Exception e) {
            return CompatibleApiBody.failure(e.getMessage());
        }
    }

    @Timed(value = "api.saveFileStatus", description = "save file status")
    @PostMapping(value = "/document/saveFileStatus")
    @Override
    public CompatibleApiBody<String> saveFileStatus(@RequestBody CallbackDTO dto) throws Exception {
        log.debug("save file status callback: {}", dto);
        fileProcessingService.processCallback(apiMapper.to(dto));
        return CompatibleApiBody.success();
    }
}
