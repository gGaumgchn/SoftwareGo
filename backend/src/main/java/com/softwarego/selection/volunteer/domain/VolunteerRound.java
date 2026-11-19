package com.softwarego.selection.volunteer.domain;

import java.time.LocalDateTime;

/**
 * 服务端根据 stage 表解析出的当前/下一可填报轮次。
 */
public record VolunteerRound(
        LocalDateTime serverTime,
        Integer roundNo,
        RoundState state,
        LocalDateTime roundStart,
        LocalDateTime roundEnd
) {
}
