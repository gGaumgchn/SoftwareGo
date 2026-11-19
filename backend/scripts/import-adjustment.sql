-- 统筹调剂结果导入（线下确定，线上只读）
--
-- 用法：
--   1. 修改下面三个参数；
--   2. 用 mysql 客户端执行整个文件：
--        mysql ... software_go_selection < backend/scripts/import-adjustment.sql
--      或进入 mysql 后 source 本文件。
--
-- 说明：
--   - 只写 final_admission，不提供 HTTP 调剂接口。
--   - admission_type 只能是 ADJUST_INTRA_LAB / ADJUST_COLLEGE；
--   - round_no、source_offer_id 固定为 NULL；
--   - 0 行插入表示校验未通过（已有最终录取 / 未处理 PENDING/CONFIRMED offer /
--     导师名额不足 / 实验室不存在 / 类型非法），可用文末诊断 SQL 定位；
--   - uk_fa_student 唯一索引兜底重复/并发导入。
--
-- 本脚本的校验口径与 OfferService.importAdjustment 一致；JUnit
-- AdjustmentImportTest 覆盖成功、重复、已有录取、遗留 offer、名额不足等场景。

-- 必填：替换下面的 NULL；未填写时脚本不会插入任何数据。
-- 示例：SET @student_id = 13; SET @teacher_id = 4;
--       SET @admission_type = 'ADJUST_COLLEGE';
SET @student_id = NULL;
SET @teacher_id = NULL;
SET @admission_type = NULL;
SET @planned_quota = NULL;
SET @now = NOW();

START TRANSACTION;

-- 锁定导师名额行，避免并发导入突破 planned_quota。
SELECT planned_quota INTO @planned_quota
FROM lab_info
WHERE teacher_id = @teacher_id
FOR UPDATE;

-- 该生已过截止的 PENDING 先按超时处理；未截止 PENDING/CONFIRMED 仍会阻止导入。
UPDATE offer
SET status = 'ABANDONED',
    abandoned_at = @now,
    abandon_reason = 'TIMEOUT'
WHERE student_id = @student_id
  AND status = 'PENDING'
  AND confirm_deadline <= @now;

INSERT INTO final_admission (
    student_id, teacher_id, round_no, admission_type, source_offer_id, admitted_at
)
SELECT @student_id, @teacher_id, NULL, @admission_type, NULL, @now
FROM DUAL
WHERE @admission_type IN ('ADJUST_INTRA_LAB', 'ADJUST_COLLEGE')
  AND @planned_quota IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM final_admission WHERE student_id = @student_id
  )
  AND NOT EXISTS (
      SELECT 1 FROM offer
      WHERE student_id = @student_id
        AND status IN ('PENDING', 'CONFIRMED')
  )
  AND (
      (SELECT COUNT(*) FROM final_admission WHERE teacher_id = @teacher_id)
      + (SELECT COUNT(*) FROM offer
         WHERE teacher_id = @teacher_id
           AND status = 'PENDING'
           AND confirm_deadline > @now)
  ) < @planned_quota;

SELECT ROW_COUNT() AS inserted_rows;

COMMIT;

-- 若 inserted_rows = 0，依次执行以下诊断 SQL 定位原因：
-- SELECT COUNT(*) AS existing_final FROM final_admission WHERE student_id = @student_id;
-- SELECT id, status, confirm_deadline FROM offer
--  WHERE student_id = @student_id AND status IN ('PENDING', 'CONFIRMED');
-- SELECT planned_quota FROM lab_info WHERE teacher_id = @teacher_id;
-- SELECT COUNT(*) AS occupied_final FROM final_admission WHERE teacher_id = @teacher_id;
-- SELECT COUNT(*) AS occupied_pending FROM offer
--  WHERE teacher_id = @teacher_id AND status = 'PENDING' AND confirm_deadline > @now;
