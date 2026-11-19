-- =========================================================
-- 实训室选拔系统 · 数据库 DDL（唯一事实源）
-- MySQL 8.0+ / InnoDB / utf8mb4 / utf8mb4_0900_ai_ci
--
-- 与 docs/数据库设计/数据库设计.md §10 保持一致；
-- 修改表结构时必须同步更新该设计文档，不要只改本地数据库。
--
-- 说明：
--   1. 本脚本用于空库初始化；重复执行会因表已存在而报错。
--   2. 不建物理外键，关联完整性由 Service 层保证（逻辑外键见文件末尾注释）。
--   3. 本文件不创建数据库、不切换数据库；由 scripts/init-dev-db.sh / init-test-db.sh 指定目标库。
--   4. 应用启动不会自动执行本文件（spring.sql.init.mode=never）。
-- =========================================================

SET NAMES utf8mb4;
SET time_zone = '+08:00';

-- ---------------------------------------------------------
-- 1. 系统用户
-- ---------------------------------------------------------
CREATE TABLE sys_user (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    staff_no    VARCHAR(10)     NOT NULL                COMMENT '学工号（10位数字）',
    name        VARCHAR(64)     NULL                    COMMENT '姓名，可为空；为空时显示学工号',
    role        VARCHAR(32)     NOT NULL                COMMENT '身份：STUDENT/TEACHER/MANAGER',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_staff_no_role (staff_no, role),
    KEY idx_role (role),
    CONSTRAINT ck_sys_user_role CHECK (role IN ('STUDENT', 'TEACHER', 'MANAGER')),
    CONSTRAINT ck_sys_user_staff_no CHECK (staff_no REGEXP '^[0-9]{10}$')
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '系统用户（身份 = 学工号 + 角色）';

-- ---------------------------------------------------------
-- 2. 阶段配置（固定 7 行）
-- ---------------------------------------------------------
CREATE TABLE stage (
    id          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '主键',
    stage_code  VARCHAR(32)      NOT NULL                COMMENT '固定阶段编码',
    stage_name  VARCHAR(64)      NOT NULL                COMMENT '固定阶段名称',
    sort_no     TINYINT UNSIGNED NOT NULL                COMMENT '流程顺序 1-7',
    start_at    DATETIME         NULL                    COMMENT '开始时间；未配置为 NULL',
    end_at      DATETIME         NULL                    COMMENT '结束时间；未配置为 NULL',
    created_at  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_stage_code (stage_code),
    UNIQUE KEY uk_stage_sort_no (sort_no),
    CONSTRAINT ck_stage_time_pair CHECK (
        (start_at IS NULL AND end_at IS NULL)
        OR (start_at IS NOT NULL AND end_at IS NOT NULL AND start_at < end_at)
    )
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '阶段配置（固定 7 行，仅更新起止时间）';

-- ---------------------------------------------------------
-- 3. 阶段保存与公告发布记录
-- ---------------------------------------------------------
CREATE TABLE stage_change (
    id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    change_no            VARCHAR(64)     NOT NULL                COMMENT '保存请求幂等键',
    operator_id          BIGINT UNSIGNED NOT NULL                COMMENT '操作负责人 sys_user.id',
    publish_requested    TINYINT(1)      NOT NULL DEFAULT 0      COMMENT '是否请求发布公告',
    announcement_status  VARCHAR(32)     NOT NULL                COMMENT 'NONE/PENDING/PUBLISHED/FAILED',
    draft_title          VARCHAR(200)    NULL                    COMMENT '公告标题草稿',
    draft_content        MEDIUMTEXT      NULL                    COMMENT '公告正文草稿',
    announcement_id      BIGINT UNSIGNED NULL                    COMMENT '发布成功后的公告 id',
    created_at           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_stage_change_change_no (change_no),
    UNIQUE KEY uk_stage_change_announcement (announcement_id),
    KEY idx_stage_change_status_created (announcement_status, created_at),
    CONSTRAINT ck_stage_change_publish_requested CHECK (publish_requested IN (0, 1)),
    CONSTRAINT ck_stage_change_status CHECK (announcement_status IN ('NONE', 'PENDING', 'PUBLISHED', 'FAILED')),
    CONSTRAINT ck_stage_change_publish CHECK (
        (publish_requested = 0 AND announcement_status = 'NONE')
        OR (publish_requested = 1 AND announcement_status IN ('PENDING', 'PUBLISHED', 'FAILED'))
    ),
    CONSTRAINT ck_stage_change_draft CHECK (
        (publish_requested = 0 AND draft_title IS NULL AND draft_content IS NULL)
        OR (publish_requested = 1 AND draft_title IS NOT NULL AND draft_content IS NOT NULL)
    ),
    CONSTRAINT ck_stage_change_announcement_state CHECK (
        (announcement_status = 'PUBLISHED' AND announcement_id IS NOT NULL)
        OR (announcement_status <> 'PUBLISHED' AND announcement_id IS NULL)
    )
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '阶段保存与公告发布记录（幂等 / 重试）';

-- ---------------------------------------------------------
-- 4. 公告
-- ---------------------------------------------------------
CREATE TABLE announcement (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    title         VARCHAR(200)    NOT NULL                COMMENT '公告标题',
    content       MEDIUMTEXT      NOT NULL                COMMENT '公告正文（纯文本）',
    publisher_id  BIGINT UNSIGNED NOT NULL                COMMENT '发布负责人 sys_user.id',
    published_at  DATETIME        NOT NULL                COMMENT '发布时间（服务端时间）',
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_announcement_published_at (published_at, id),
    KEY idx_announcement_publisher (publisher_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '公告（只增不改不删，仅阶段保存时生成）';

-- ---------------------------------------------------------
-- 5. 简历文件
-- ---------------------------------------------------------
CREATE TABLE resume_file (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    owner_id       BIGINT UNSIGNED NOT NULL                COMMENT '所有者 sys_user.id',
    purpose        VARCHAR(32)     NOT NULL                COMMENT 'STUDENT_RESUME/TEACHER_RESUME',
    original_name  VARCHAR(255)    NOT NULL                COMMENT '原始文件名',
    storage_path   VARCHAR(255)    NOT NULL                COMMENT '相对上传根目录的路径',
    content_type   VARCHAR(100)    NOT NULL                COMMENT 'MIME 类型',
    file_size      BIGINT UNSIGNED NOT NULL                COMMENT '文件大小（字节）',
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_resume_storage_path (storage_path),
    KEY idx_resume_owner_purpose (owner_id, purpose),
    CONSTRAINT ck_resume_purpose CHECK (purpose IN ('STUDENT_RESUME', 'TEACHER_RESUME')),
    CONSTRAINT ck_resume_size CHECK (file_size > 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '简历文件元数据（PDF，鉴权下载）';

-- ---------------------------------------------------------
-- 6. 导师实训室信息（每个导师一行）
-- ---------------------------------------------------------
CREATE TABLE lab_info (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    teacher_id          BIGINT UNSIGNED NOT NULL                COMMENT '导师 sys_user.id',
    teacher_name        VARCHAR(64)     NOT NULL                COMMENT '导师姓名（空白规范化后）',
    lab_name            VARCHAR(100)    NOT NULL                COMMENT '实训室名称',
    location            VARCHAR(200)    NOT NULL                COMMENT '实训室位置',
    planned_quota       INT UNSIGNED    NOT NULL                COMMENT '计划招收人数（跨轮次总数）',
    research_direction  VARCHAR(200)    NOT NULL                COMMENT '研究方向',
    contact             VARCHAR(100)    NOT NULL                COMMENT '联系方式',
    resume_file_id      BIGINT UNSIGNED NOT NULL                COMMENT '导师简历 resume_file.id',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_lab_teacher (teacher_id),
    KEY idx_lab_teacher_name (teacher_name),
    KEY idx_lab_name (lab_name),
    KEY idx_lab_research_direction (research_direction),
    CONSTRAINT ck_lab_planned_quota CHECK (planned_quota > 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '导师实训室信息（每导师一行）';

-- ---------------------------------------------------------
-- 7. 志愿申请（每生每轮一行）
-- ---------------------------------------------------------
CREATE TABLE volunteer_application (
    id                BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '主键',
    student_id        BIGINT UNSIGNED  NOT NULL                COMMENT '学生 sys_user.id',
    round_no          TINYINT UNSIGNED NOT NULL                COMMENT '轮次 1/2/3',
    resume_file_id    BIGINT UNSIGNED  NOT NULL                COMMENT '学生简历 resume_file.id',
    intra_lab_adjust  TINYINT(1)       NOT NULL                COMMENT '是否同意实训室内调剂',
    college_adjust    TINYINT(1)       NOT NULL                COMMENT '是否同意学院内调剂',
    submitted_at      DATETIME         NOT NULL                COMMENT '最后一次成功提交时间',
    created_at        DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at        DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_va_student_round (student_id, round_no),
    KEY idx_va_round (round_no),
    KEY idx_va_student_submitted (student_id, submitted_at),
    CONSTRAINT ck_va_round CHECK (round_no BETWEEN 1 AND 3),
    CONSTRAINT ck_va_adjust CHECK (intra_lab_adjust IN (0, 1) AND college_adjust IN (0, 1))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '志愿申请（每生每轮一行）';

-- ---------------------------------------------------------
-- 8. 志愿目标 / 面试结果
-- ---------------------------------------------------------
CREATE TABLE application_target (
    id              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '主键',
    application_id  BIGINT UNSIGNED  NOT NULL                COMMENT 'volunteer_application.id',
    teacher_id      BIGINT UNSIGNED  NOT NULL                COMMENT '导师 sys_user.id',
    preference_no   TINYINT UNSIGNED NOT NULL                COMMENT '输入框序号 1-3（平行志愿）',
    score           DECIMAL(5,2)     NULL                    COMMENT '面试分数 0-100；NULL 未评分',
    tie_order       INT UNSIGNED     NULL                    COMMENT '同分手动顺序，越小越靠前',
    scored_at       DATETIME         NULL                    COMMENT '最近评分时间',
    created_at      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_at_application_teacher (application_id, teacher_id),
    UNIQUE KEY uk_at_application_pref (application_id, preference_no),
    KEY idx_at_teacher (teacher_id),
    KEY idx_at_teacher_score (teacher_id, score),
    CONSTRAINT ck_at_preference CHECK (preference_no BETWEEN 1 AND 3),
    CONSTRAINT ck_at_score CHECK (score IS NULL OR (score >= 0 AND score <= 100))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '志愿目标（含面试分数与同分顺序）';

-- ---------------------------------------------------------
-- 9. 拟录取记录
-- ---------------------------------------------------------
CREATE TABLE offer (
    id                    BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '主键',
    teacher_id            BIGINT UNSIGNED  NOT NULL                COMMENT '发放导师 sys_user.id',
    student_id            BIGINT UNSIGNED  NOT NULL                COMMENT '学生 sys_user.id',
    application_target_id BIGINT UNSIGNED  NOT NULL                COMMENT '来源 application_target.id',
    round_no              TINYINT UNSIGNED NOT NULL                COMMENT '轮次 1/2/3',
    status                VARCHAR(32)      NOT NULL                COMMENT 'PENDING/CONFIRMED/ABANDONED',
    confirm_deadline      DATETIME         NOT NULL                COMMENT '确认截止时间（发放时快照）',
    granted_at            DATETIME         NOT NULL                COMMENT '发放时间',
    confirmed_at          DATETIME         NULL                    COMMENT '确认时间',
    abandoned_at          DATETIME         NULL                    COMMENT '放弃时间',
    abandon_reason        VARCHAR(32)      NULL                    COMMENT 'TIMEOUT/CONFIRMED_OTHER',
    created_at            DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at            DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_offer_teacher_round_student (teacher_id, round_no, student_id),
    UNIQUE KEY uk_offer_application_target (application_target_id),
    KEY idx_offer_student_round (student_id, round_no),
    KEY idx_offer_student_status (student_id, status),
    KEY idx_offer_teacher_round_status (teacher_id, round_no, status),
    KEY idx_offer_teacher_status (teacher_id, status),
    KEY idx_offer_status_deadline (status, confirm_deadline),
    CONSTRAINT ck_offer_round CHECK (round_no BETWEEN 1 AND 3),
    CONSTRAINT ck_offer_status CHECK (status IN ('PENDING', 'CONFIRMED', 'ABANDONED')),
    CONSTRAINT ck_offer_abandon_reason CHECK (abandon_reason IS NULL OR abandon_reason IN ('TIMEOUT', 'CONFIRMED_OTHER')),
    CONSTRAINT ck_offer_state_consistency CHECK (
        (status = 'PENDING' AND confirmed_at IS NULL AND abandoned_at IS NULL AND abandon_reason IS NULL)
        OR (status = 'CONFIRMED' AND confirmed_at IS NOT NULL AND abandoned_at IS NULL AND abandon_reason IS NULL)
        OR (status = 'ABANDONED' AND confirmed_at IS NULL AND abandoned_at IS NOT NULL AND abandon_reason IS NOT NULL)
    )
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '拟录取记录（待确认/已确认/已放弃）';

-- ---------------------------------------------------------
-- 10. 最终录取
-- ---------------------------------------------------------
CREATE TABLE final_admission (
    id               BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '主键',
    student_id       BIGINT UNSIGNED  NOT NULL                COMMENT '学生 sys_user.id',
    teacher_id       BIGINT UNSIGNED  NOT NULL                COMMENT '录取导师 sys_user.id',
    round_no         TINYINT UNSIGNED NULL                    COMMENT '志愿录取轮次；调剂为 NULL',
    admission_type   VARCHAR(32)      NOT NULL                COMMENT 'VOLUNTEER/ADJUST_INTRA_LAB/ADJUST_COLLEGE',
    source_offer_id  BIGINT UNSIGNED  NULL                    COMMENT '志愿录取来源 offer.id；调剂为 NULL',
    admitted_at      DATETIME         NOT NULL                COMMENT '录取形成时间',
    created_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_fa_student (student_id),
    UNIQUE KEY uk_fa_source_offer (source_offer_id),
    KEY idx_fa_teacher (teacher_id),
    KEY idx_fa_type (admission_type),
    CONSTRAINT ck_fa_type CHECK (admission_type IN ('VOLUNTEER', 'ADJUST_INTRA_LAB', 'ADJUST_COLLEGE')),
    CONSTRAINT ck_fa_round_source CHECK (
        (admission_type = 'VOLUNTEER'
            AND round_no IS NOT NULL
            AND round_no BETWEEN 1 AND 3
            AND source_offer_id IS NOT NULL)
        OR
        (admission_type IN ('ADJUST_INTRA_LAB', 'ADJUST_COLLEGE')
            AND round_no IS NULL
            AND source_offer_id IS NULL)
    )
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '最终录取（每生唯一去向）';

-- ---------------------------------------------------------
-- 初始化阶段（固定 7 行）
-- ---------------------------------------------------------
START TRANSACTION;

INSERT INTO stage (stage_code, stage_name, sort_no, start_at, end_at) VALUES
('BRIEFING',   '集中宣讲',     1, NULL, NULL),
('OPEN_DAY',   '实训室开放日', 2, NULL, NULL),
('ROUND_1',    '第一轮录取',   3, NULL, NULL),
('ROUND_2',    '第二轮录取',   4, NULL, NULL),
('ROUND_3',    '第三轮录取',   5, NULL, NULL),
('ADJUSTMENT', '统筹调剂',     6, NULL, NULL),
('ENROLLMENT', '正式入组',     7, NULL, NULL);

COMMIT;

-- =========================================================
-- 逻辑外键清单（按设计文档 §2.3，不建物理外键）
-- =========================================================
-- lab_info.teacher_id                 -> sys_user.id
-- lab_info.resume_file_id             -> resume_file.id
-- resume_file.owner_id                -> sys_user.id
-- volunteer_application.student_id    -> sys_user.id
-- volunteer_application.resume_file_id-> resume_file.id
-- application_target.application_id   -> volunteer_application.id
-- application_target.teacher_id       -> sys_user.id
-- offer.teacher_id                    -> sys_user.id
-- offer.student_id                    -> sys_user.id
-- offer.application_target_id         -> application_target.id
-- final_admission.student_id          -> sys_user.id
-- final_admission.teacher_id          -> sys_user.id
-- final_admission.source_offer_id     -> offer.id
-- announcement.publisher_id           -> sys_user.id
-- stage_change.operator_id            -> sys_user.id
-- stage_change.announcement_id        -> announcement.id
