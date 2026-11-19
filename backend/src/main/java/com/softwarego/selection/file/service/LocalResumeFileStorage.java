package com.softwarego.selection.file.service;

import com.softwarego.selection.config.UploadProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * 本地磁盘存储：root 来自 app.upload.dir，storage_path 只能是相对路径。
 */
@Component
@RequiredArgsConstructor
public class LocalResumeFileStorage implements ResumeFileStorage {

    private final UploadProperties uploadProperties;

    @Override
    public void store(String storagePath, byte[] content) throws IOException {
        Path root = root();
        Path target = safeResolve(root, storagePath);
        Files.createDirectories(target.getParent());
        Files.write(target, content, StandardOpenOption.CREATE_NEW);
    }

    @Override
    public Path resolve(String storagePath) {
        Path root = root();
        try {
            Path target = safeResolve(root, storagePath);
            if (!Files.isRegularFile(target)) {
                return null;
            }
            return target;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    @Override
    public void delete(String storagePath) {
        try {
            Path target = safeResolve(root(), storagePath);
            Files.deleteIfExists(target);
        } catch (Exception ignored) {
            // 清理失败不覆盖上传主流程的错误。
        }
    }

    private Path root() {
        return Path.of(uploadProperties.getDir()).toAbsolutePath().normalize();
    }

    private Path safeResolve(Path root, String storagePath) {
        Path target = root.resolve(storagePath).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("invalid storage path");
        }
        return target;
    }
}
