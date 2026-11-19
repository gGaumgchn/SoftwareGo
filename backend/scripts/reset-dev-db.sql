-- =========================================================
-- 把 dev 阶段时间重置到当前时间前后，便于手工验收。
--
-- 时间轴（以执行时刻为基准）：
--   集中宣讲      -8 天 ~ -7 天
--   实训室开放日  -7 天 ~ -6 天
--   第一轮录取    -1 天 ~ +2 天   ← 当前正在进行
--   第二轮录取    +3 天 ~ +5 天
--   第三轮录取    +6 天 ~ +8 天
--   统筹调剂      +9 天 ~ +10 天
--   正式入组      +11 天 ~ +13 天
--
-- 同时把 PENDING 拟录取的确认截止时间对齐到对应轮次结束时间，
-- 避免加载种子数据后待确认记录立刻超时。
-- =========================================================

SET NAMES utf8mb4;
SET time_zone = '+08:00';

SET @now = DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:00');

UPDATE stage
SET start_at = DATE_SUB(@now, INTERVAL 8 DAY),
    end_at   = DATE_SUB(@now, INTERVAL 7 DAY)
WHERE stage_code = 'BRIEFING';

UPDATE stage
SET start_at = DATE_SUB(@now, INTERVAL 7 DAY),
    end_at   = DATE_SUB(@now, INTERVAL 6 DAY)
WHERE stage_code = 'OPEN_DAY';

UPDATE stage
SET start_at = DATE_SUB(@now, INTERVAL 1 DAY),
    end_at   = DATE_ADD(@now, INTERVAL 2 DAY)
WHERE stage_code = 'ROUND_1';

UPDATE stage
SET start_at = DATE_ADD(@now, INTERVAL 3 DAY),
    end_at   = DATE_ADD(@now, INTERVAL 5 DAY)
WHERE stage_code = 'ROUND_2';

UPDATE stage
SET start_at = DATE_ADD(@now, INTERVAL 6 DAY),
    end_at   = DATE_ADD(@now, INTERVAL 8 DAY)
WHERE stage_code = 'ROUND_3';

UPDATE stage
SET start_at = DATE_ADD(@now, INTERVAL 9 DAY),
    end_at   = DATE_ADD(@now, INTERVAL 10 DAY)
WHERE stage_code = 'ADJUSTMENT';

UPDATE stage
SET start_at = DATE_ADD(@now, INTERVAL 11 DAY),
    end_at   = DATE_ADD(@now, INTERVAL 13 DAY)
WHERE stage_code = 'ENROLLMENT';

UPDATE offer o
    JOIN stage s ON s.stage_code = CONCAT('ROUND_', o.round_no)
SET o.confirm_deadline = s.end_at
WHERE o.status = 'PENDING';

SELECT stage_code, stage_name, sort_no, start_at, end_at
FROM stage
ORDER BY sort_no;

SELECT id, teacher_id, student_id, round_no, status, confirm_deadline
FROM offer
ORDER BY round_no, id;
