package com.softwarego.selection.volunteer.domain;

/**
 * 当前不可提交的原因。
 */
public enum BlockedReason {
    ADMITTED,
    NOT_OPEN,
    ENDED,
    NO_ROUND,
    NO_QUOTA,
    PROCESSED
}
