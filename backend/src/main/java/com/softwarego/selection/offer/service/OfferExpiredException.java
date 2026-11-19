package com.softwarego.selection.offer.service;

import com.softwarego.selection.common.BusinessException;
import com.softwarego.selection.common.ErrorCode;

/**
 * 确认时发现已过截止时间。确认事务对该异常不回滚，以便把 offer 惰性标记为 TIMEOUT。
 */
public class OfferExpiredException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public OfferExpiredException() {
        super(ErrorCode.CONFLICT, "确认时间已截止");
    }
}
