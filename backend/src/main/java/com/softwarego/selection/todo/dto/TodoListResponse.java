package com.softwarego.selection.todo.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * GET /todos 响应。
 */
@JsonPropertyOrder({"serverTime", "list"})
public record TodoListResponse(
        LocalDateTime serverTime,
        List<TodoItemResponse> list
) {
}
