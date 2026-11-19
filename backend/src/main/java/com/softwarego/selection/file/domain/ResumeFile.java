package com.softwarego.selection.file.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * resume_file 实体。文件本体在 uploads 目录，数据库只保存元数据。
 */
@Getter
@Setter
@NoArgsConstructor
public class ResumeFile {

    private Long id;
    private Long ownerId;
    private ResumePurpose purpose;
    private String originalName;
    private String storagePath;
    private String contentType;
    private Long fileSize;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
