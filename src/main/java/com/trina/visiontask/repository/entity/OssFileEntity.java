package com.trina.visiontask.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "oss_file")
public class OssFileEntity {
    @Id
    @Size(max = 64)
    @Column(name = "id", nullable = false, length = 64)
    private String id;

    @Size(max = 64)
    @NotNull
    @Column(name = "tank_id", nullable = false, length = 64)
    private String tankId;

    @Size(max = 64)
    @Column(name = "empno", length = 64)
    private String empno;

    @Size(max = 64)
    @Column(name = "file_suffix", length = 64)
    private String fileSuffix;

    @Size(max = 256)
    @NotNull
    @Column(name = "url", nullable = false, length = 256)
    private String url;

    @Size(max = 256)
    @Column(name = "file_name", length = 256)
    private String fileName;

    @Column(name = "upload_time")
    private LocalDateTime uploadTime;

    @Column(name = "status")
    private Long status;

    @Size(max = 128)
    @Column(name = "message", length = 128)
    private String message;

    @Size(max = 64)
    @Column(name = "session_id", length = 64)
    private String sessionId;

    @Size(max = 255)
    @Column(name = "session_name")
    private String sessionName;

    @Column(name = "text", length = Integer.MAX_VALUE)
    private String text;

    @Size(max = 255)
    @Column(name = "original_id")
    private String originalId;

    @Size(max = 255)
    @Column(name = "pdf_status")
    private String pdfStatus;

    @Column(name = "convert_time")
    private LocalDateTime convertTime;

    @Size(max = 64)
    @Column(name = "error_code", length = 64)
    private String errorCode;

    @Column(name = "size")
    private Long size;

}