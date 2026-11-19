package com.softwarego.selection.lab.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

/**
 * GET /labs 响应：分页数据 + 全量研究方向选项。
 */
@JsonPropertyOrder({"list", "total", "page", "pageSize", "researchDirections"})
public record LabListResponse(
        List<LabListItem> list,
        long total,
        int page,
        int pageSize,
        List<String> researchDirections
) {
}
