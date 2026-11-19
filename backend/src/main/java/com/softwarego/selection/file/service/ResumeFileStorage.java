package com.softwarego.selection.file.service;

import java.io.IOException;
import java.nio.file.Path;

/**
 * 简历文件存储。生产实现写本地磁盘；测试可注入失败实现验证“失败不留数据库记录”。
 */
public interface ResumeFileStorage {

    void store(String storagePath, byte[] content) throws IOException;

    /**
     * 解析相对 storage_path；文件不存在或越出上传根目录时返回 null。
     */
    Path resolve(String storagePath);

    void delete(String storagePath);
}
