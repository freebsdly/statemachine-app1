package com.trina.visiontask.repository.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "file_infos", indexes = {
        @Index(name = "file_infos_file_id_index", columnList = "file_id")
})
public class FileEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;
    private UUID fileId;
    private String fileName;
    private String fileType;
    private String mimeType;
    private String ossFileKey;
    private String ossPDFKey;
    private String ossMDKey;
    private String filePath;
    private String pdfPath;
    private String mdPath;
    private LocalDateTime createTime;
    private LocalDateTime modifyTime;
    private Long fileSize;

}
