package com.softwarego.selection.todo.domain;

/**
 * 个人待办类型。待办不落表，按当前阶段 + 用户状态实时推导。
 */
public enum TodoType {
    VOLUNTEER_SUBMIT,
    VOLUNTEER_MAKEUP,
    OFFER_CONFIRM,
    LAB_INFO,
    SCORE_ENTRY,
    GRANT_OFFER,
    STAGE_CONFIG
}
