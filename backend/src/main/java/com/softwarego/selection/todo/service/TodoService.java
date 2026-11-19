package com.softwarego.selection.todo.service;

import com.softwarego.selection.auth.SessionUser;
import com.softwarego.selection.lab.domain.LabInfo;
import com.softwarego.selection.lab.dto.AvailableTeacherItem;
import com.softwarego.selection.lab.service.LabInfoService;
import com.softwarego.selection.lab.service.QuotaService;
import com.softwarego.selection.offer.dto.AdmissionResultResponse;
import com.softwarego.selection.offer.dto.OfferItemResponse;
import com.softwarego.selection.offer.dto.OfferListResponse;
import com.softwarego.selection.offer.service.OfferService;
import com.softwarego.selection.stage.domain.StageCode;
import com.softwarego.selection.todo.domain.TodoType;
import com.softwarego.selection.todo.dto.TodoItemResponse;
import com.softwarego.selection.todo.dto.TodoListResponse;
import com.softwarego.selection.todo.mapper.TodoMapper;
import com.softwarego.selection.volunteer.domain.RoundState;
import com.softwarego.selection.volunteer.domain.VolunteerRound;
import com.softwarego.selection.volunteer.service.VolunteerRoundResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 个人待办实时推导：不落表、不缓存，只返回当前可办理事项。
 */
@Service
@RequiredArgsConstructor
public class TodoService {

    private static final String STATUS_TODO = "TODO";

    private final TodoMapper todoMapper;
    private final VolunteerRoundResolver roundResolver;
    private final OfferService offerService;
    private final LabInfoService labInfoService;
    private final QuotaService quotaService;

    public TodoListResponse getTodos(SessionUser user) {
        VolunteerRound round = roundResolver.resolve();
        List<TodoItemResponse> todos = switch (user.role()) {
            case STUDENT -> studentTodos(user.userId(), round);
            case TEACHER -> teacherTodos(user.userId(), round);
            case MANAGER -> managerTodos();
        };

        // deadline ASC（无截止最后）；同一截止时间按 type ASC，保证展示稳定。
        todos.sort(Comparator
                .comparing(TodoItemResponse::deadline,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(todo -> todo.type().name()));
        return new TodoListResponse(round.serverTime(), todos);
    }

    private List<TodoItemResponse> studentTodos(Long studentId, VolunteerRound round) {
        List<TodoItemResponse> todos = new ArrayList<>();
        AdmissionResultResponse admission = offerService.getAdmission(studentId);
        if (admission.admitted()) {
            // 已录取学生不再返回志愿/补录/确认待办。
            return todos;
        }

        // 待确认拟录取与阶段无关，只看本人未截止的 PENDING offer。
        OfferListResponse offers = offerService.getOffers(studentId);
        List<OfferItemResponse> actionable = offers.list().stream()
                .filter(OfferItemResponse::isCanConfirm)
                .toList();
        if (!actionable.isEmpty()) {
            OfferItemResponse earliest = actionable.stream()
                    .min(Comparator.comparing(OfferItemResponse::getConfirmDeadline))
                    .orElse(actionable.get(0));
            todos.add(todo(
                    TodoType.OFFER_CONFIRM,
                    "确认拟录取",
                    "有 " + actionable.size() + " 条待确认拟录取",
                    earliest.getRoundNo(),
                    earliest.getConfirmDeadline(),
                    "/student/offers",
                    "去确认"));
        }

        if (round.state() == RoundState.OPEN
                && todoMapper.countStudentApplication(studentId, round.roundNo()) == 0) {
            if (round.roundNo() == 1) {
                todos.add(todo(
                        TodoType.VOLUNTEER_SUBMIT,
                        "填报志愿",
                        "第一轮志愿填报进行中",
                        round.roundNo(),
                        round.roundEnd(),
                        "/student/volunteer",
                        "去填报"));
            } else {
                List<AvailableTeacherItem> available =
                        labInfoService.findAvailableTeachers(round.serverTime());
                if (!available.isEmpty()) {
                    todos.add(todo(
                            TodoType.VOLUNTEER_MAKEUP,
                            "补录填报",
                            "第" + round.roundNo() + "轮补录填报进行中",
                            round.roundNo(),
                            round.roundEnd(),
                            "/student/volunteer",
                            "去填报"));
                }
            }
        }
        return todos;
    }

    private List<TodoItemResponse> teacherTodos(Long teacherId, VolunteerRound round) {
        List<TodoItemResponse> todos = new ArrayList<>();
        LabInfo lab = labInfoService.findByTeacherId(teacherId);
        if (lab == null) {
            todos.add(todo(
                    TodoType.LAB_INFO,
                    "填写实训室信息",
                    "尚未填写实训室信息",
                    null,
                    null,
                    "/teacher/lab",
                    "去填写"));
        }

        if (round.state() == RoundState.OPEN) {
            long unscored = todoMapper.countUnscoredTargets(teacherId, round.roundNo());
            if (unscored > 0) {
                todos.add(todo(
                        TodoType.SCORE_ENTRY,
                        "录入面试分数",
                        "本轮有 " + unscored + " 份未评分申请",
                        round.roundNo(),
                        round.roundEnd(),
                        "/teacher/admissions",
                        "去评分"));
            }

            if (lab != null) {
                // 分数线在发放时由导师填写，待办只判断“已评分且尚未发放”+ 剩余名额。
                long grantable = todoMapper.countGrantableTargets(teacherId, round.roundNo());
                long occupied = quotaService.occupied(teacherId);
                long remaining = (long) lab.getPlannedQuota() - occupied;
                if (grantable > 0 && remaining > 0) {
                    todos.add(todo(
                            TodoType.GRANT_OFFER,
                            "发放拟录取",
                            "本轮有可发放的达标学生",
                            round.roundNo(),
                            round.roundEnd(),
                            "/teacher/admissions",
                            "去发放"));
                }
            }
        }
        return todos;
    }

    private List<TodoItemResponse> managerTodos() {
        List<TodoItemResponse> todos = new ArrayList<>();
        if (todoMapper.countUnconfiguredStages() > 0) {
            todos.add(todo(
                    TodoType.STAGE_CONFIG,
                    "配置阶段",
                    "阶段配置不完整，请及时配置",
                    null,
                    null,
                    "/manager/stages",
                    "去配置"));
        }
        return todos;
    }

    private TodoItemResponse todo(TodoType type,
                                  String title,
                                  String description,
                                  Integer roundNo,
                                  LocalDateTime deadline,
                                  String actionPath,
                                  String actionLabel) {
        String stageCode = null;
        String stageName = null;
        if (roundNo != null) {
            StageCode stage = StageCode.valueOf("ROUND_" + roundNo);
            stageCode = stage.name();
            stageName = stage.getStageName();
        }
        return new TodoItemResponse(
                type, title, description, stageCode, stageName,
                deadline, STATUS_TODO, actionPath, actionLabel);
    }
}
