package com.softwarego.selection.stage;

import com.fasterxml.jackson.databind.JsonNode;
import com.softwarego.selection.stage.domain.StageCode;
import com.softwarego.selectiontest.support.FailingAnnouncementPublisher;
import com.softwarego.selectiontest.support.FailingAnnouncementPublisherConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 阶段保存、changeNo 幂等、公告发布/失败/重试、D3 已截止不可延长。
 * 不使用 @Transactional：公告发布使用 REQUIRES_NEW 独立事务，
 * 因此用显式清理（固定测试前缀 + 恢复阶段原时间）保证 dev 数据不被污染。
 */
@SpringBootTest(properties = "app.stage.allow-extend-ended=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(FailingAnnouncementPublisherConfig.class)
class StageSaveControllerTest extends StageTestSupport {

    @Autowired
    private FailingAnnouncementPublisher failingPublisher;

    private List<StageTimeRow> originalStages;

    @BeforeEach
    void setUp() {
        if (originalStages == null) {
            originalStages = loadStageTimes();
        }
        // 保存用例从“全部未配置”开始，避免种子数据里已截止阶段触发 D3；
        // 测试结束后由 tearDown 恢复 originalStages。
        jdbcTemplate.update("UPDATE stage SET start_at = NULL, end_at = NULL");
        deleteTestStageChanges();
        deleteTestAnnouncements();
        failingPublisher.reset();
    }

    @AfterEach
    void tearDown() {
        setStageTimes(originalStages);
        deleteTestStageChanges();
        deleteTestAnnouncements();
        failingPublisher.reset();
    }

    @Test
    void saveWithoutAnnouncementAndDuplicateIsIdempotent() throws Exception {
        MockHttpSession manager = managerSession();
        LocalDateTime base = now().plusDays(10);
        List<StageTimeRow> rows = sequentialRows(base);
        String changeNo = newChangeNo();
        String body = saveBody(changeNo, rows, false, null, null);
        String offerStatusBefore = jdbcTemplate.queryForObject(
                "SELECT status FROM offer WHERE id = 1", String.class);
        Integer finalCountBefore = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM final_admission WHERE student_id = 6", Integer.class);

        mockMvc.perform(put("/api/v1/manager/stages")
                        .session(manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.stageSaved").value(true))
                .andExpect(jsonPath("$.data.announcementStatus").value("NONE"))
                .andExpect(jsonPath("$.data.announcementId").value(nullValue()))
                .andExpect(jsonPath("$.data.message").value("保存成功"));

        assertThat(stageStart(StageCode.BRIEFING)).isEqualTo(rows.get(0).startAt());
        assertThat(stageEnd(StageCode.ENROLLMENT)).isEqualTo(rows.get(6).endAt());
        assertThat(stageChangeStatus(changeNo)).isEqualTo("NONE");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT publish_requested FROM stage_change WHERE change_no = ?",
                Integer.class, changeNo)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT operator_id FROM stage_change WHERE change_no = ?",
                Long.class, changeNo)).isEqualTo(1L);
        assertThat(countStageChangesByChangeNo(changeNo)).isEqualTo(1);

        List<StageTimeRow> otherRows = sequentialRows(base.plusDays(1));
        mockMvc.perform(put("/api/v1/manager/stages")
                        .session(manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveBody(changeNo, otherRows, false, null, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.announcementStatus").value("NONE"))
                .andExpect(jsonPath("$.data.message").value("保存成功"));

        // changeNo 重复：返回已有结果，不覆盖已保存时间，不新增记录。
        assertThat(stageStart(StageCode.BRIEFING)).isEqualTo(rows.get(0).startAt());
        assertThat(countStageChangesByChangeNo(changeNo)).isEqualTo(1);

        // 阶段调整只改 stage/stage_change，不回滚已确认/已放弃 offer 或最终录取。
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM offer WHERE id = 1", String.class))
                .isEqualTo(offerStatusBefore);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM final_admission WHERE student_id = 6", Integer.class))
                .isEqualTo(finalCountBefore);
    }

    @Test
    void saveWithAnnouncementPublishesAndRetryIsIdempotent() throws Exception {
        MockHttpSession manager = managerSession();
        MockHttpSession student = studentSession();
        LocalDateTime base = now().plusDays(20);
        List<StageTimeRow> rows = sequentialRows(base);
        String changeNo = newChangeNo();
        String title = TEST_TITLE_PREFIX + " 阶段调整通知";
        String content = "各阶段时间调整如下：\n\n第一轮录取：时间更新。";

        MvcResult result = mockMvc.perform(put("/api/v1/manager/stages")
                        .session(manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveBody(changeNo, rows, true, title, content)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stageSaved").value(true))
                .andExpect(jsonPath("$.data.announcementStatus").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.message").value("保存成功，公告已发布"))
                .andReturn();

        long announcementId = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("announcementId").asLong();
        assertThat(announcementId).isPositive();
        assertThat(stageChangeStatus(changeNo)).isEqualTo("PUBLISHED");
        assertThat(countAnnouncementsByTitle(title)).isEqualTo(1);

        Map<String, Object> announcement = jdbcTemplate.queryForMap(
                "SELECT title, content, publisher_id FROM announcement WHERE id = ?", announcementId);
        assertThat(announcement.get("title")).isEqualTo(title);
        assertThat(announcement.get("content")).isEqualTo(content);
        assertThat(((Number) announcement.get("publisher_id")).longValue()).isEqualTo(1L);

        mockMvc.perform(get("/api/v1/announcements/{id}", announcementId).session(student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value(title))
                .andExpect(jsonPath("$.data.content").value(content))
                .andExpect(jsonPath("$.data.publisherName").value("周明远"));

        MvcResult listResult = mockMvc.perform(get("/api/v1/announcements")
                        .session(student)
                        .param("page", "1")
                        .param("pageSize", "100"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode list = objectMapper.readTree(listResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("list");
        boolean found = false;
        for (JsonNode item : list) {
            if (item.path("id").asLong() == announcementId) {
                found = true;
                break;
            }
        }
        assertThat(found).isTrue();

        mockMvc.perform(post("/api/v1/manager/stages/announcement")
                        .session(manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("changeNo", changeNo))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.announcementStatus").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.announcementId").value(announcementId))
                .andExpect(jsonPath("$.data.message").value("公告已发布"));
        assertThat(countAnnouncementsByTitle(title)).isEqualTo(1);

        // 重复保存同一 changeNo：不重新发布、不覆盖阶段。
        mockMvc.perform(put("/api/v1/manager/stages")
                        .session(manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveBody(changeNo, sequentialRows(base.plusDays(1)), true, title, content)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.announcementStatus").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.announcementId").value(announcementId))
                .andExpect(jsonPath("$.data.message").value("保存成功，公告已发布"));

        assertThat(stageStart(StageCode.BRIEFING)).isEqualTo(rows.get(0).startAt());
        assertThat(countAnnouncementsByTitle(title)).isEqualTo(1);
    }

    @Test
    void announcementFailureAndRetry() throws Exception {
        MockHttpSession manager = managerSession();
        LocalDateTime base = now().plusDays(30);
        List<StageTimeRow> rows = sequentialRows(base);
        String changeNo = newChangeNo();
        String title = TEST_TITLE_PREFIX + " 失败重试通知";
        String content = "公告首次发布失败，随后重试。";

        failingPublisher.failNextPublish();

        mockMvc.perform(put("/api/v1/manager/stages")
                        .session(manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveBody(changeNo, rows, true, title, content)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stageSaved").value(true))
                .andExpect(jsonPath("$.data.announcementStatus").value("FAILED"))
                .andExpect(jsonPath("$.data.announcementId").value(nullValue()))
                .andExpect(jsonPath("$.data.message").value("阶段已保存，公告发布失败，请重试"));

        // 阶段已生效，公告未生成，stage_change 标记 FAILED。
        assertThat(stageStart(StageCode.BRIEFING)).isEqualTo(rows.get(0).startAt());
        assertThat(stageChangeStatus(changeNo)).isEqualTo("FAILED");
        assertThat(countAnnouncementsByTitle(title)).isZero();

        MvcResult changesResult = mockMvc.perform(get("/api/v1/manager/stage-changes")
                        .session(manager)
                        .param("status", "FAILED")
                        .param("page", "1")
                        .param("pageSize", "100"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode changes = objectMapper.readTree(changesResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("list");
        JsonNode target = null;
        for (JsonNode item : changes) {
            if (changeNo.equals(item.path("changeNo").asText())) {
                target = item;
                break;
            }
        }
        assertThat(target).isNotNull();
        assertThat(target.path("operatorId").asLong()).isEqualTo(1L);
        assertThat(target.path("operatorName").asText()).isEqualTo("周明远");
        assertThat(target.path("publishRequested").asBoolean()).isTrue();
        assertThat(target.path("announcementStatus").asText()).isEqualTo("FAILED");
        assertThat(target.path("draftTitle").asText()).isEqualTo(title);

        MvcResult retryResult = mockMvc.perform(post("/api/v1/manager/stages/announcement")
                        .session(manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("changeNo", changeNo))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.announcementStatus").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.message").value("公告已发布"))
                .andReturn();

        long announcementId = objectMapper.readTree(retryResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("announcementId").asLong();
        assertThat(announcementId).isPositive();
        assertThat(stageChangeStatus(changeNo)).isEqualTo("PUBLISHED");
        assertThat(countAnnouncementsByTitle(title)).isEqualTo(1);
        assertThat(stageStart(StageCode.BRIEFING)).isEqualTo(rows.get(0).startAt());

        mockMvc.perform(post("/api/v1/manager/stages/announcement")
                        .session(manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("changeNo", changeNo))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.announcementStatus").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.announcementId").value(announcementId))
                .andExpect(jsonPath("$.data.message").value("公告已发布"));
        assertThat(countAnnouncementsByTitle(title)).isEqualTo(1);
    }

    @Test
    void endedStageCannotBeExtended() throws Exception {
        LocalDateTime current = now();
        List<StageTimeRow> oldRows = briefingEndedRows(current);
        setStageTimes(oldRows);

        List<StageTimeRow> newRows = new ArrayList<>(oldRows);
        newRows.set(0, new StageTimeRow(
                StageCode.BRIEFING, current.minusHours(4), current.minusHours(1)));

        MockHttpSession manager = managerSession();
        String changeNo = newChangeNo();

        mockMvc.perform(put("/api/v1/manager/stages")
                        .session(manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveBody(changeNo, newRows, false, null, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40901))
                .andExpect(jsonPath("$.message").value("阶段已截止，不能延长"));

        assertThat(stageEnd(StageCode.BRIEFING)).isEqualTo(oldRows.get(0).endAt());
        assertThat(countStageChangesByChangeNo(changeNo)).isZero();
    }

    @Test
    void validationRejectsInvalidRequests() throws Exception {
        MockHttpSession manager = managerSession();
        LocalDateTime base = now().plusDays(40);
        List<StageTimeRow> rows = sequentialRows(base);
        LocalDateTime originalStart = stageStart(StageCode.BRIEFING);

        // 阶段数量不足
        mockMvc.perform(put("/api/v1/manager/stages")
                        .session(manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveBody(newChangeNo(), rows.subList(0, 6), false, null, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message").value("提交失败，请重试"));

        // 编码重复 / 缺失
        List<StageTimeRow> duplicateCodes = new ArrayList<>(rows);
        duplicateCodes.set(1, duplicateCodes.get(0));
        mockMvc.perform(put("/api/v1/manager/stages")
                        .session(manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveBody(newChangeNo(), duplicateCodes, false, null, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message").value("提交失败，请重试"));

        // 时间缺失
        List<Map<String, Object>> missingTimeStages = stageMaps(rows);
        missingTimeStages.get(0).remove("endAt");
        Map<String, Object> missingTimeBody = new LinkedHashMap<>();
        missingTimeBody.put("changeNo", newChangeNo());
        missingTimeBody.put("stages", missingTimeStages);
        missingTimeBody.put("publishAnnouncement", false);
        mockMvc.perform(put("/api/v1/manager/stages")
                        .session(manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(missingTimeBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message").value("请填写完整的阶段时间"));

        // 开始不早于结束
        List<StageTimeRow> reversed = new ArrayList<>(rows);
        StageTimeRow first = rows.get(0);
        reversed.set(0, new StageTimeRow(first.stageCode(), first.endAt(), first.startAt()));
        mockMvc.perform(put("/api/v1/manager/stages")
                        .session(manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveBody(newChangeNo(), reversed, false, null, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message").value("阶段结束时间必须晚于开始时间"));

        // 顺序重叠
        List<StageTimeRow> overlapped = new ArrayList<>(rows);
        StageTimeRow openDay = rows.get(1);
        overlapped.set(1, new StageTimeRow(
                openDay.stageCode(), first.startAt().plusMinutes(30), openDay.endAt()));
        mockMvc.perform(put("/api/v1/manager/stages")
                        .session(manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveBody(newChangeNo(), overlapped, false, null, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message").value("阶段时间必须按流程顺序设置且不能重叠"));

        // 公告内容为空：不保存阶段
        mockMvc.perform(put("/api/v1/manager/stages")
                        .session(manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveBody(newChangeNo(), rows, true, "", "正文")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message").value("公告内容不能为空"));
        assertThat(stageStart(StageCode.BRIEFING)).isEqualTo(originalStart);

        // 缺少 changeNo / publishAnnouncement
        mockMvc.perform(put("/api/v1/manager/stages")
                        .session(manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveBody(null, rows, false, null, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message").value("提交失败，请重试"));

        mockMvc.perform(put("/api/v1/manager/stages")
                        .session(manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveBody(newChangeNo(), rows, null, null, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message").value("提交失败，请重试"));
    }

    @Test
    void retryRejectsMissingOrNonPublishChange() throws Exception {
        MockHttpSession manager = managerSession();

        mockMvc.perform(post("/api/v1/manager/stages/announcement")
                        .session(manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("changeNo", newChangeNo()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));

        LocalDateTime base = now().plusDays(50);
        String changeNo = newChangeNo();
        mockMvc.perform(put("/api/v1/manager/stages")
                        .session(manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveBody(changeNo, sequentialRows(base), false, null, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.announcementStatus").value("NONE"));

        mockMvc.perform(post("/api/v1/manager/stages/announcement")
                        .session(manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("changeNo", changeNo))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40901))
                .andExpect(jsonPath("$.message").value("该次保存未请求发布公告"));
    }

}
