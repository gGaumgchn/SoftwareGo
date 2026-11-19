package com.softwarego.selectiontest.support;

import com.softwarego.selection.file.service.LocalResumeFileStorage;
import com.softwarego.selection.file.service.ResumeFileStorage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 测试专用存储：可让下一次 store 直接失败，验证“上传失败不留数据库记录”。
 * 成功路径仍委托真实本地磁盘存储。
 */
public class FailingResumeFileStorage implements ResumeFileStorage {

    private final LocalResumeFileStorage delegate;
    private final AtomicBoolean failNext = new AtomicBoolean(false);

    public FailingResumeFileStorage(LocalResumeFileStorage delegate) {
        this.delegate = delegate;
    }

    @Override
    public void store(String storagePath, byte[] content) throws IOException {
        if (failNext.compareAndSet(true, false)) {
            throw new IllegalStateException("simulated resume storage failure");
        }
        delegate.store(storagePath, content);
    }

    @Override
    public Path resolve(String storagePath) {
        return delegate.resolve(storagePath);
    }

    @Override
    public void delete(String storagePath) {
        delegate.delete(storagePath);
    }

    public void failNextStore() {
        failNext.set(true);
    }

    public void reset() {
        failNext.set(false);
    }
}
