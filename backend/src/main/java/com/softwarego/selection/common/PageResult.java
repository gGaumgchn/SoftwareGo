package com.softwarego.selection.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

/**
 * 统一分页响应体，放在统一响应的 {@code data} 中。
 *
 * @param list     当前页数据
 * @param total    总记录数
 * @param page     页码，从 1 开始
 * @param pageSize 每页条数
 * @param <T>      列表元素类型
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({"list", "total", "page", "pageSize"})
public record PageResult<T>(List<T> list, long total, int page, int pageSize) {

    public static <T> PageResult<T> of(List<T> list, long total, int page, int pageSize) {
        return new PageResult<>(list, total, page, pageSize);
    }

    public static <T> PageResult<T> empty(int page, int pageSize) {
        return new PageResult<>(List.of(), 0L, page, pageSize);
    }
}
