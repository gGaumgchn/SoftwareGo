package com.softwarego.selection.offer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 拟录取超时放弃定时任务。读取/确认接口仍会做惰性校正。
 */
@Component
@RequiredArgsConstructor
public class OfferTimeoutTask {

    private final OfferService offerService;

    @Scheduled(fixedDelayString = "${app.offer.timeout-scan-ms:60000}")
    public void expirePendingOffers() {
        offerService.expireAllPendingOffers();
    }
}
