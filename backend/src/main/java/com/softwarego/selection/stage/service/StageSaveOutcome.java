package com.softwarego.selection.stage.service;

import com.softwarego.selection.stage.domain.StageChange;

/**
 * 阶段保存事务结果。newlySaved=false 表示 changeNo 已存在，直接复用已有记录。
 */
public record StageSaveOutcome(StageChange change, boolean newlySaved) {
}
