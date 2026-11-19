package com.softwarego.selection.common.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.LocalDateTime;

/**
 * 健康检查响应。
 *
 * @param status     固定为 UP
 * @param serverTime 服务端当前时间（Asia/Shanghai，秒位精确）
 */
@JsonPropertyOrder({"status", "serverTime"})
public record HealthResponse(String status, LocalDateTime serverTime) {
}
