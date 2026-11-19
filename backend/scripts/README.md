# 数据库脚本说明

## 唯一事实源

DDL 唯一事实源是：

```text
backend/src/main/resources/db/schema.sql
```

`backend/sql/schema.sql` 已改为指向该文件的说明，不再维护第二份 DDL。

## 凭据

脚本按以下顺序读取数据库连接信息：

1. 环境变量：`DB_HOST`、`DB_PORT`、`DB_PROTOCOL`、`DB_USERNAME`、`DB_PASSWORD`
2. `backend/src/main/resources/application-local.yml` 中的 `username` / `password`

`application-local.yml` 已被 Git 忽略，禁止提交真实密码。脚本使用 `MYSQL_PWD`
传递密码，不会把密码放到命令行参数里。

## 初始化 dev 库

```bash
cp backend/src/main/resources/application-local.yml.example \
   backend/src/main/resources/application-local.yml
# 编辑 application-local.yml，填写本地 MySQL 账号密码

bash backend/scripts/init-dev-db.sh
```

脚本会：

1. 创建 `software_go_selection`（如果不存在）；
2. 执行 `schema.sql`（10 张表 + 7 行阶段）；
3. 执行 `data.sql`（1 负责人、4 导师、10 学生、4 实训室、两轮志愿/分数/拟录取、1 条调剂）；
4. 执行 `reset-dev-db.sql`，把阶段时间重置到当前时间前后。

如果目标库已存在 `sys_user` / `stage` 表，脚本会退出，避免覆盖已有数据。

> 如果数据库账号没有 `CREATE DATABASE` 权限，请让 DBA 先创建
> `software_go_selection`（utf8mb4 / utf8mb4_0900_ai_ci）。库已存在时，
> 脚本会自动跳过创建，只执行建表/种子数据。

## 重置阶段时间

```bash
bash backend/scripts/reset-dev-db.sh
```

只更新 `stage` 的起止时间，并把 `PENDING` 拟录取的 `confirm_deadline`
对齐到对应轮次结束时间；不会删除用户、志愿、拟录取数据。

## 独立测试库（可选）

本机没有 Docker，不使用 Testcontainers。需要隔离测试库时：

```bash
bash backend/scripts/init-test-db.sh
cd backend
TEST_DB_URL='jdbc:mysql://localhost:3306/software_go_selection_test?useUnicode=true&characterEncoding=utf8&connectionTimeZone=Asia/Shanghai' \
  mvn test
```

`application-test.yml` 默认使用 `software_go_selection`（当前项目确认 B0 直接
在 dev 库验证）；设置 `TEST_DB_URL` 即可切换到独立测试库。

## 统筹调剂结果导入（P7）

统筹调剂线下确定，线上只读展示，不提供学生确认/导师接收接口。调剂结果通过
`final_admission` 导入：

```bash
# 1. 修改脚本顶部的参数（默认 NULL，未填写不会插入）
#    SET @student_id = 13;
#    SET @teacher_id = 4;
#    SET @admission_type = 'ADJUST_COLLEGE';
# 2. 执行
mysql ... software_go_selection < backend/scripts/import-adjustment.sql
```

脚本 `import-adjustment.sql`：

- 只写 `final_admission`，`admission_type` 为
  `ADJUST_INTRA_LAB` / `ADJUST_COLLEGE`，`round_no`、`source_offer_id` 为 `NULL`；
- 导入前校验：学生无最终录取、无未截止 `PENDING`/`CONFIRMED` offer
  （已过截止的 `PENDING` 先按 `TIMEOUT` 处理）、导师名额充足且不突破
  `planned_quota`；
- 锁定 `lab_info` 名额行，事务内完成；`uk_fa_student` 唯一索引兜底重复/并发；
- `inserted_rows=0` 表示校验未通过，按脚本末尾诊断 SQL 定位；
- 与 `OfferService.importAdjustment` 的校验口径一致，后者由
  `AdjustmentImportTest` 覆盖，不作为 HTTP 接口暴露。

学生通过 `GET /student/admission` 查看调剂结果，`roundNo=null`，
`admissionType=ADJUST_*`，没有确认按钮/确认接口。

## 种子数据约定

- 姓名、学工号、联系方式均为虚构，不对应真实人员。
- `data.sql` 使用固定主键，只用于空库；重复执行会触发唯一键冲突。
- 生产环境禁止执行 `data.sql`；应用启动时 `spring.sql.init.mode=never`，
  不会自动灌入演示数据。
- `resume_file` 只登记元数据；如需下载走查，执行：
  ```bash
  bash backend/scripts/create-seed-resume-files.sh
  ```
  脚本会在 `backend/uploads/resumes/2026/11/` 下生成对应占位 PDF，
  不修改数据库；`uploads/` 已被 Git 忽略。
