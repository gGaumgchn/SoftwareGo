package com.softwarego.selection.todo.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.softwarego.selection.todo.domain.TodoType;

import java.time.LocalDateTime;

/**
 * 单条个人待办。
 */
@JsonPropertyOrder({"type", "title", "description", "stageCode", "stageName",
        "deadline", "status", "actionPath", "actionLabel"})
public record TodoItemResponse(
        TodoType type,
        String title,
        String description,
        String stageCode,
        String stageName,
        LocalDateTime deadline,
        String status,
        String actionPath,
        String actionLabel
) {
}
