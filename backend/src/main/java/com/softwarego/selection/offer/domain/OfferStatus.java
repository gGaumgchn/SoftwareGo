package com.softwarego.selection.offer.domain;

/**
 * 拟录取状态，只允许 PENDING -> CONFIRMED / ABANDONED。
 */
public enum OfferStatus {
    PENDING,
    CONFIRMED,
    ABANDONED
}
