package com.softwarego.selection.stage;

import com.softwarego.selection.stage.domain.StageCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 演示开关 app.stage.allow-extend-ended=true 时，允许延长已截止阶段。
 * 默认关闭时的严格校验见 {@link StageSaveControllerTest}。
 */
@SpringBootTest(properties = "app.stage.allow-extend-ended=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StageExtendEndedAllowedTest extends StageTestSupport {

    private List<StageTimeRow> originalStages;

    @BeforeEach
    void setUp() {
        originalStages = loadStageTimes();
        deleteTestStageChanges();
        deleteTestAnnouncements();
    }

    @AfterEach
    void tearDown() {
        setStageTimes(originalStages);
        deleteTestStageChanges();
        deleteTestAnnouncements();
    }

    @Test
    void endedStageCanBeExtendedWhenSwitchEnabled() throws Exception {
        LocalDateTime current = now();
        List<StageTimeRow> oldRows = briefingEndedRows(current);
        setStageTimes(oldRows);

        // 简报阶段已于 current-2h 结束，延长到 current-1h，与开放日开始时间相接，不重叠。
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
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.stageSaved").value(true));

        assertThat(stageEnd(StageCode.BRIEFING)).isEqualTo(newRows.get(0).endAt());
        assertThat(countStageChangesByChangeNo(changeNo)).isEqualTo(1);
    }
}
