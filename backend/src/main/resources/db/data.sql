-- =========================================================
-- 实训室选拔系统 · dev 演示数据
-- 仅用于本地开发 / 答辩演示；生产环境禁止执行。
--
-- 前提：
--   1. 已执行 backend/src/main/resources/db/schema.sql（stage 已初始化 7 行）。
--   2. 目标库为空库；本脚本使用固定主键，重复执行会触发唯一键冲突。
--
-- 数据规模：
--   1 名负责人、4 名导师、10 名学生、4 个实训室（计划名额合计 8）。
--   第一轮 + 第二轮志愿 / 分数 / 拟录取样例，1 条统筹调剂最终录取样例。
--
-- 姓名、学工号、联系方式均为虚构，不对应任何真实人员。
-- =========================================================

SET NAMES utf8mb4;
SET time_zone = '+08:00';

START TRANSACTION;

-- ---------------------------------------------------------
-- 1. 系统用户（身份 = 学工号 + 角色）
-- ---------------------------------------------------------
INSERT INTO sys_user (id, staff_no, name, role) VALUES
(1,  '9000000001', '周明远', 'MANAGER'),
(2,  '1000000001', '陈启航', 'TEACHER'),
(3,  '1000000002', '林若溪', 'TEACHER'),
(4,  '1000000003', '赵子墨', 'TEACHER'),
(5,  '1000000004', '孙承宇', 'TEACHER'),
(6,  '2026001001', '王雨桐', 'STUDENT'),
(7,  '2026001002', '李昊然', 'STUDENT'),
(8,  '2026001003', '张可欣', 'STUDENT'),
(9,  '2026001004', '刘子涵', 'STUDENT'),
(10, '2026001005', '陈嘉懿', 'STUDENT'),
(11, '2026001006', '杨思远', 'STUDENT'),
(12, '2026001007', '黄诗雅', 'STUDENT'),
(13, '2026001008', '吴俊熙', 'STUDENT'),
(14, '2026001009', '徐若楠', 'STUDENT'),
(15, '2026001010', '郑一鸣', 'STUDENT');

-- ---------------------------------------------------------
-- 2. 简历文件元数据
--    仅登记元数据；如需真实下载走查，可在 uploads/resumes 下补占位 PDF。
-- ---------------------------------------------------------
INSERT INTO resume_file (id, owner_id, purpose, original_name, storage_path, content_type, file_size) VALUES
(1,  2,  'TEACHER_RESUME', '陈启航-导师简历.pdf', '2026/11/seed-teacher-01.pdf', 'application/pdf', 1166),
(2,  3,  'TEACHER_RESUME', '林若溪-导师简历.pdf', '2026/11/seed-teacher-02.pdf', 'application/pdf', 1166),
(3,  4,  'TEACHER_RESUME', '赵子墨-导师简历.pdf', '2026/11/seed-teacher-03.pdf', 'application/pdf', 1166),
(4,  5,  'TEACHER_RESUME', '孙承宇-导师简历.pdf', '2026/11/seed-teacher-04.pdf', 'application/pdf', 1166),
(5,  6,  'STUDENT_RESUME', '王雨桐-学生简历.pdf', '2026/11/seed-student-01.pdf', 'application/pdf', 1166),
(6,  7,  'STUDENT_RESUME', '李昊然-学生简历.pdf', '2026/11/seed-student-02.pdf', 'application/pdf', 1166),
(7,  8,  'STUDENT_RESUME', '张可欣-学生简历.pdf', '2026/11/seed-student-03.pdf', 'application/pdf', 1166),
(8,  9,  'STUDENT_RESUME', '刘子涵-学生简历.pdf', '2026/11/seed-student-04.pdf', 'application/pdf', 1166),
(9,  10, 'STUDENT_RESUME', '陈嘉懿-学生简历.pdf', '2026/11/seed-student-05.pdf', 'application/pdf', 1166),
(10, 11, 'STUDENT_RESUME', '杨思远-学生简历.pdf', '2026/11/seed-student-06.pdf', 'application/pdf', 1166),
(11, 12, 'STUDENT_RESUME', '黄诗雅-学生简历.pdf', '2026/11/seed-student-07.pdf', 'application/pdf', 1166),
(12, 13, 'STUDENT_RESUME', '吴俊熙-学生简历.pdf', '2026/11/seed-student-08.pdf', 'application/pdf', 1166),
(13, 14, 'STUDENT_RESUME', '徐若楠-学生简历.pdf', '2026/11/seed-student-09.pdf', 'application/pdf', 1166),
(14, 15, 'STUDENT_RESUME', '郑一鸣-学生简历.pdf', '2026/11/seed-student-10.pdf', 'application/pdf', 1166);

-- ---------------------------------------------------------
-- 3. 实训室信息（每导师一行，计划名额合计 8）
-- ---------------------------------------------------------
INSERT INTO lab_info (id, teacher_id, teacher_name, lab_name, location, planned_quota,
                      research_direction, contact, resume_file_id) VALUES
(1, 2, '陈启航', '智能软件实训室', '软件楼 A-301', 2, '智能软件与自动化测试', 'chen.qihang@example.edu', 1),
(2, 3, '林若溪', '数据工程实训室', '软件楼 A-302', 2, '大数据平台与数据治理', 'lin.ruoxi@example.edu', 2),
(3, 4, '赵子墨', '网络安全实训室', '软件楼 B-201', 2, '网络攻防与安全审计', 'zhao.zimo@example.edu', 3),
(4, 5, '孙承宇', '人机交互实训室', '软件楼 B-202', 2, '人机交互与可视分析', 'sun.chengyu@example.edu', 4);

-- ---------------------------------------------------------
-- 4. 志愿申请（每生每轮一行）
-- ---------------------------------------------------------
INSERT INTO volunteer_application (id, student_id, round_no, resume_file_id,
                                   intra_lab_adjust, college_adjust, submitted_at) VALUES
-- 第一轮：10 名学生
(1,  6,  1, 5,  1, 1, '2026-11-10 10:00:00'),
(2,  7,  1, 6,  1, 0, '2026-11-10 10:05:00'),
(3,  8,  1, 7,  1, 1, '2026-11-10 10:10:00'),
(4,  9,  1, 8,  1, 1, '2026-11-10 10:15:00'),
(5,  10, 1, 9,  0, 1, '2026-11-10 10:20:00'),
(6,  11, 1, 10, 1, 0, '2026-11-10 10:25:00'),
(7,  12, 1, 11, 1, 1, '2026-11-10 10:30:00'),
(8,  13, 1, 12, 1, 1, '2026-11-10 10:35:00'),
(9,  14, 1, 13, 1, 1, '2026-11-10 10:40:00'),
(10, 15, 1, 14, 1, 1, '2026-11-10 10:45:00'),
-- 第二轮：未录取学生补录
(11, 9,  2, 8,  1, 1, '2026-11-14 09:10:00'),
(12, 13, 2, 12, 1, 1, '2026-11-14 09:20:00'),
(13, 14, 2, 13, 1, 0, '2026-11-14 09:30:00');

-- ---------------------------------------------------------
-- 5. 志愿目标 / 面试分数
-- ---------------------------------------------------------
INSERT INTO application_target (id, application_id, teacher_id, preference_no,
                                score, tie_order, scored_at) VALUES
-- 第一轮
(1,  1,  2, 1, 92.00, NULL, '2026-11-11 09:00:00'),
(2,  1,  3, 2, 85.00, NULL, '2026-11-11 09:05:00'),
(3,  2,  2, 1, 87.00, NULL, '2026-11-11 09:10:00'),
(4,  2,  3, 2, 90.00, NULL, '2026-11-11 09:15:00'),
(5,  3,  3, 1, 83.00, NULL, '2026-11-11 09:20:00'),
(6,  3,  4, 2, 80.00, NULL, '2026-11-11 09:25:00'),
(7,  4,  2, 1, 70.00, NULL, '2026-11-11 09:30:00'),
(8,  4,  5, 2, 68.00, NULL, '2026-11-11 09:35:00'),
(9,  5,  3, 1, 95.00, NULL, '2026-11-11 09:40:00'),
(10, 6,  4, 1, 86.00, NULL, '2026-11-11 09:45:00'),
(11, 7,  5, 1, 88.00, NULL, '2026-11-11 09:50:00'),
(12, 8,  3, 1, 60.00, NULL, '2026-11-11 09:55:00'),
(13, 9,  4, 1, 84.00, NULL, '2026-11-11 10:00:00'),
(14, 10, 4, 1, 91.00, NULL, '2026-11-11 10:05:00'),
-- 第二轮
(15, 11, 2, 1, 90.00, NULL, '2026-11-15 09:00:00'),
(16, 12, 5, 1, 82.00, NULL, '2026-11-15 09:10:00'),
(17, 13, 3, 1, 79.00, NULL, '2026-11-15 09:20:00');

-- ---------------------------------------------------------
-- 6. 拟录取记录
--    第一轮：3 条 PENDING、2 条 CONFIRMED、1 条 ABANDONED；
--    第二轮：1 条 PENDING。
-- ---------------------------------------------------------
INSERT INTO offer (id, teacher_id, student_id, application_target_id, round_no, status,
                   confirm_deadline, granted_at, confirmed_at, abandoned_at, abandon_reason) VALUES
(1, 2, 6,  1,  1, 'CONFIRMED', DATE_ADD(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, NULL),
(2, 3, 6,  2,  1, 'ABANDONED', DATE_ADD(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), NULL, DATE_SUB(NOW(), INTERVAL 1 DAY), 'CONFIRMED_OTHER'),
(3, 3, 7,  4,  1, 'PENDING',   DATE_ADD(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), NULL, NULL, NULL),
(4, 4, 8,  6,  1, 'PENDING',   DATE_ADD(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), NULL, NULL, NULL),
(5, 3, 10, 9,  1, 'CONFIRMED', DATE_ADD(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, NULL),
(6, 4, 11, 10, 1, 'PENDING',   DATE_ADD(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), NULL, NULL, NULL),
(7, 5, 12, 11, 1, 'PENDING',   DATE_ADD(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), NULL, NULL, NULL),
(8, 2, 9,  15, 2, 'PENDING',   DATE_ADD(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, NULL, NULL);

-- ---------------------------------------------------------
-- 7. 最终录取
--    2 条志愿录取 + 1 条统筹调剂（调剂为线下结果，线上只读）
-- ---------------------------------------------------------
INSERT INTO final_admission (id, student_id, teacher_id, round_no, admission_type,
                             source_offer_id, admitted_at) VALUES
(1, 6,  2, 1,    'VOLUNTEER',       1,    DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, 10, 3, 1,    'VOLUNTEER',       5,    DATE_SUB(NOW(), INTERVAL 1 DAY)),
(3, 15, 5, NULL, 'ADJUST_COLLEGE',  NULL, DATE_SUB(NOW(), INTERVAL 1 HOUR));

COMMIT;
