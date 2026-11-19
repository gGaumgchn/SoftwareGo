package com.softwarego.selection.todo.controller;

import com.softwarego.selection.auth.CurrentUser;
import com.softwarego.selection.auth.SessionUser;
import com.softwarego.selection.common.Result;
import com.softwarego.selection.todo.dto.TodoListResponse;
import com.softwarego.selection.todo.service.TodoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;

    @GetMapping("/todos")
    public Result<TodoListResponse> todos(@CurrentUser SessionUser sessionUser) {
        return Result.ok(todoService.getTodos(sessionUser));
    }
}
