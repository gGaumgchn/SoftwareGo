package com.softwarego.selection.offer;

import com.fasterxml.jackson.databind.JsonNode;
import com.softwarego.selection.offer.service.OfferService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 拟录取列表/确认/最终录取/超时惰性校正/定时校正最小测试。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OfferControllerTest extends OfferTestSupport {

    @Autowired
    private OfferService offerService;

    @Test
    void studentViewsOwnOffersOnly() throws Exception {
        StudentOffers data = createStudentWithOffers("2026999990");

        mockMvc.perform(get("/api/v1/student/offers").session(data.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.serverTime").value(notNullValue()))
                .andExpect(jsonPath("$.data.list", hasSize(2)))
                .andExpect(jsonPath("$.data.list[*].teacherName",
                        containsInAnyOrder("陈启航", "林若溪")))
                .andExpect(jsonPath("$.data.list[*].status", everyItem(is("PENDING"))))
                .andExpect(jsonPath("$.data.list[*].canConfirm", everyItem(is(true))))
                .andExpect(jsonPath("$.data.list[0].labName").value(notNullValue()));

        MockHttpSession other = login("STUDENT", "2026999988");
        mockMvc.perform(get("/api/v1/student/offers").session(other))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list", hasSize(0)));

        mockMvc.perform(get("/api/v1/student/admission").session(data.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.admitted").value(false))
                .andExpect(jsonPath("$.data.admission").value(nullValue()));
    }

    @Test
    void confirmOneAbandonsOthersAndCreatesFinalAdmission() throws Exception {
        StudentOffers data = createStudentWithOffers("2026999990");
        Long confirmedOfferId = data.offerIds().get(0);
        Long abandonedOfferId = data.offerIds().get(1);

        mockMvc.perform(post("/api/v1/student/offers/{id}/confirm", confirmedOfferId)
                        .session(data.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.offerId").value(confirmedOfferId.intValue()))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.confirmedAt").value(notNullValue()))
                .andExpect(jsonPath("$.data.finalAdmission").value(notNullValue()))
                .andExpect(jsonPath("$.data.finalAdmission.admissionType").value("VOLUNTEER"))
                .andExpect(jsonPath("$.data.finalAdmission.roundNo").value(1))
                .andExpect(jsonPath("$.data.abandonedOfferIds", hasSize(1)))
                .andExpect(jsonPath("$.data.abandonedOfferIds[0]")
                        .value(abandonedOfferId.intValue()));

        Map<String, Object> confirmedRow = offerRow(confirmedOfferId);
        assertThat(confirmedRow.get("status")).isEqualTo("CONFIRMED");
        Map<String, Object> abandonedRow = offerRow(abandonedOfferId);
        assertThat(abandonedRow.get("status")).isEqualTo("ABANDONED");
        assertThat(abandonedRow.get("abandon_reason")).isEqualTo("CONFIRMED_OTHER");

        Map<String, Object> finalRow = finalAdmissionRow(data.studentId());
        assertThat(finalRow.get("admission_type")).isEqualTo("VOLUNTEER");
        assertThat(((Number) finalRow.get("source_offer_id")).longValue())
                .isEqualTo(confirmedOfferId);

        mockMvc.perform(get("/api/v1/student/admission").session(data.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.admitted").value(true))
                .andExpect(jsonPath("$.data.admission.admissionType").value("VOLUNTEER"))
                .andExpect(jsonPath("$.data.admission.roundNo").value(1))
                .andExpect(jsonPath("$.data.admission.teacherName").value("陈启航"))
                .andExpect(jsonPath("$.data.admission.labName").value("智能软件实训室"));

        mockMvc.perform(get("/api/v1/student/offers").session(data.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list[*].canConfirm", everyItem(is(false))));

        // 重复确认已确认记录
        mockMvc.perform(post("/api/v1/student/offers/{id}/confirm", confirmedOfferId)
                        .session(data.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40901))
                .andExpect(jsonPath("$.message").value("已确认录取，不可更改"));

        assertThat(finalAdmissionCount(data.studentId())).isEqualTo(1);
    }

    @Test
    void alreadyConfirmedOfferIsRejected() throws Exception {
        StudentOffers data = createStudentWithOffers("2026999990");
        Long offerId = data.offerIds().get(0);
        markOfferConfirmed(offerId);

        mockMvc.perform(post("/api/v1/student/offers/{id}/confirm", offerId)
                        .session(data.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40901))
                .andExpect(jsonPath("$.message").value("已确认录取，不可更改"));
    }

    @Test
    void abandonedOfferIsRejected() throws Exception {
        StudentOffers data = createStudentWithOffers("2026999990");
        Long offerId = data.offerIds().get(0);
        markOfferAbandoned(offerId);

        mockMvc.perform(post("/api/v1/student/offers/{id}/confirm", offerId)
                        .session(data.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40901))
                .andExpect(jsonPath("$.message").value("确认失败，请刷新查看结果后重试"));
    }

    @Test
    void expiredConfirmMarksTimeout() throws Exception {
        StudentOffers data = createStudentWithOffers("2026999990");
        Long offerId = data.offerIds().get(0);
        setOfferDeadline(offerId, now().minusHours(1));

        mockMvc.perform(post("/api/v1/student/offers/{id}/confirm", offerId)
                        .session(data.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40901))
                .andExpect(jsonPath("$.message").value("确认时间已截止"));

        Map<String, Object> row = offerRow(offerId);
        assertThat(row.get("status")).isEqualTo("ABANDONED");
        assertThat(row.get("abandon_reason")).isEqualTo("TIMEOUT");
    }

    @Test
    void getOffersLazilyExpiresPastDeadline() throws Exception {
        StudentOffers data = createStudentWithOffers("2026999990");
        Long expiredOfferId = data.offerIds().get(0);
        Long pendingOfferId = data.offerIds().get(1);
        setOfferDeadline(expiredOfferId, now().minusHours(1));

        MvcResult result = mockMvc.perform(get("/api/v1/student/offers").session(data.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list", hasSize(2)))
                .andReturn();

        JsonNode list = json(result).path("data").path("list");
        for (JsonNode item : list) {
            if (item.path("offerId").asLong() == expiredOfferId) {
                assertThat(item.path("status").asText()).isEqualTo("ABANDONED");
                assertThat(item.path("abandonReason").asText()).isEqualTo("TIMEOUT");
                assertThat(item.path("canConfirm").asBoolean()).isFalse();
            }
            if (item.path("offerId").asLong() == pendingOfferId) {
                assertThat(item.path("status").asText()).isEqualTo("PENDING");
                assertThat(item.path("canConfirm").asBoolean()).isTrue();
            }
        }
    }

    @Test
    void scheduledExpiryOnlyTouchesPending() throws Exception {
        StudentOffers data = createStudentWithOffers("2026999990");
        Long expiredOfferId = data.offerIds().get(0);
        Long confirmedOfferId = data.offerIds().get(1);
        setOfferDeadline(expiredOfferId, now().minusHours(1));
        markOfferConfirmed(confirmedOfferId);
        setOfferDeadline(confirmedOfferId, now().minusHours(1));

        offerService.expireAllPendingOffers();

        assertThat(offerRow(expiredOfferId).get("status")).isEqualTo("ABANDONED");
        assertThat(offerRow(expiredOfferId).get("abandon_reason")).isEqualTo("TIMEOUT");
        assertThat(offerRow(confirmedOfferId).get("status")).isEqualTo("CONFIRMED");
    }

    @Test
    void existingFinalAdmissionBlocksConfirm() throws Exception {
        StudentOffers data = createStudentWithOffers("2026999990");
        Long offerId = data.offerIds().get(0);
        insertAdjustmentFinal(data.studentId(), 2L);

        mockMvc.perform(post("/api/v1/student/offers/{id}/confirm", offerId)
                        .session(data.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40901))
                .andExpect(jsonPath("$.message").value("已确认录取，不可更改"));

        assertThat(offerRow(offerId).get("status")).isEqualTo("PENDING");
        assertThat(finalAdmissionCount(data.studentId())).isEqualTo(1);
    }

    @Test
    void nonOwnerAndMissingOfferRejected() throws Exception {
        StudentOffers data = createStudentWithOffers("2026999990");
        MockHttpSession other = login("STUDENT", "2026999988");

        mockMvc.perform(post("/api/v1/student/offers/{id}/confirm", data.offerIds().get(0))
                        .session(other))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301))
                .andExpect(jsonPath("$.message").value("无权访问该数据"));

        mockMvc.perform(post("/api/v1/student/offers/{id}/confirm", 999999L)
                        .session(data.session()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    @Test
    void admissionVolunteerAdjustmentAndNone() throws Exception {
        mockMvc.perform(get("/api/v1/student/admission").session(studentSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.admitted").value(true))
                .andExpect(jsonPath("$.data.admission.admissionType").value("VOLUNTEER"))
                .andExpect(jsonPath("$.data.admission.roundNo").value(1))
                .andExpect(jsonPath("$.data.admission.teacherName").value("陈启航"))
                .andExpect(jsonPath("$.data.admission.labName").value("智能软件实训室"))
                .andExpect(jsonPath("$.data.admission.location").value(notNullValue()));

        MockHttpSession adjustmentStudent = login("STUDENT", "2026001010");
        mockMvc.perform(get("/api/v1/student/admission").session(adjustmentStudent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.admitted").value(true))
                .andExpect(jsonPath("$.data.admission.admissionType").value("ADJUST_COLLEGE"))
                .andExpect(jsonPath("$.data.admission.roundNo").value(nullValue()))
                .andExpect(jsonPath("$.data.admission.teacherName").value("孙承宇"));

        MockHttpSession unadmittedStudent = login("STUDENT", "2026001008");
        mockMvc.perform(get("/api/v1/student/admission").session(unadmittedStudent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.admitted").value(false))
                .andExpect(jsonPath("$.data.admission").value(nullValue()));
    }

    @Test
    void nonStudentRejected() throws Exception {
        mockMvc.perform(get("/api/v1/student/offers").session(teacherSession()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));

        mockMvc.perform(get("/api/v1/student/admission").session(teacherSession()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));

        mockMvc.perform(post("/api/v1/student/offers/{id}/confirm", 1L)
                        .session(teacherSession()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));

        mockMvc.perform(get("/api/v1/student/offers"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
    }
}
