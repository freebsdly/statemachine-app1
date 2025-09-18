package com.trina.visiontask.repository.entity;

import com.trina.visiontask.FileProcessingState;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "file_infos", indexes = {
        @Index(name = "file_infos_file_id_index", columnList = "file_id"),
        @Index(name = "file_infos_tank_id_index", columnList = "tank_id"),
        @Index(name = "file_infos_file_name_index", columnList = "file_name"),
}, uniqueConstraints = {
        @UniqueConstraint(name = "file_infos_oss_file_key_unique", columnNames = "oss_file_key")
})
public class FileEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;
    private UUID fileId;
    @Column(length = 2000)
    private String fileName;
    private String fileType;
    private String mimeType;
    @Column(length = 2000)
    private String ossFileKey;
    @Column(length = 2000)
    private String ossPDFKey;
    @Column(length = 2000)
    private String ossMDKey;
    @Column(length = 2000)
    private String filePath;
    @Column(length = 2000)
    private String pdfPath;
    @Column(length = 2000)
    private String mdPath;
    private LocalDateTime createTime;
    private LocalDateTime modifyTime;
    private Long fileSize;
    private FileProcessingState status;
    @Column(length = 2000)
    private String parentPath;
    private String tankId;
    private UUID pdfFileId;
}
