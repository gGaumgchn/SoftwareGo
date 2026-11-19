package com.softwarego.selection.admission.domain;

import java.time.LocalDateTime;

/**
 * 导师录取页面解析出的轮次信息。
 */
public record AdmissionRound(
        LocalDateTime serverTime,
        Integer roundNo,
        AdmissionRoundState state,
        LocalDateTime roundStart,
        LocalDateTime roundEnd
) {
}
