package com.softwarego.selection.offer.controller;

import com.softwarego.selection.auth.CurrentUser;
import com.softwarego.selection.auth.SessionUser;
import com.softwarego.selection.common.Result;
import com.softwarego.selection.offer.dto.AdmissionResultResponse;
import com.softwarego.selection.offer.dto.ConfirmOfferResponse;
import com.softwarego.selection.offer.dto.OfferListResponse;
import com.softwarego.selection.offer.service.OfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/student")
@RequiredArgsConstructor
public class OfferController {

    private final OfferService offerService;

    @GetMapping("/offers")
    public Result<OfferListResponse> offers(@CurrentUser SessionUser sessionUser) {
        return Result.ok(offerService.getOffers(sessionUser.userId()));
    }

    @PostMapping("/offers/{id}/confirm")
    public Result<ConfirmOfferResponse> confirm(
            @PathVariable Long id,
            @CurrentUser SessionUser sessionUser) {
        return Result.ok(offerService.confirm(sessionUser.userId(), id));
    }

    @GetMapping("/admission")
    public Result<AdmissionResultResponse> admission(@CurrentUser SessionUser sessionUser) {
        return Result.ok(offerService.getAdmission(sessionUser.userId()));
    }
}
