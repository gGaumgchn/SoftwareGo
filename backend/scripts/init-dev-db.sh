#!/usr/bin/env bash
# 初始化 dev 库：建库 -> schema.sql -> data.sql -> 阶段时间重置。
#
# 仅用于空库；如果已存在 sys_user 表会直接退出，避免覆盖已有数据。
# 凭据来源：环境变量 DB_USERNAME/DB_PASSWORD，或 application-local.yml。
#
# 用法：
#   bash backend/scripts/init-dev-db.sh
#   DB_NAME=software_go_selection bash backend/scripts/init-dev-db.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./db-env.sh
source "$SCRIPT_DIR/db-env.sh"

DB_NAME="${DB_NAME:-software_go_selection}"

echo "目标数据库：${DB_NAME}（${DB_USERNAME}@${DB_HOST}:${DB_PORT}）"

create_database_if_needed "$DB_NAME"

existing_tables="$(mysql_cmd -N -B -e "
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = '${DB_NAME}'
      AND table_name IN ('sys_user', 'stage');
")"

if [[ "$existing_tables" != "0" ]]; then
    echo "数据库 ${DB_NAME} 已存在业务表，未执行初始化，避免覆盖数据。" >&2
    echo "如需重建，请先备份并手动删除该数据库。" >&2
    exit 1
fi

echo "执行 schema.sql ..."
mysql_cmd "$DB_NAME" < "$RESOURCES_DIR/schema.sql"

echo "执行 data.sql ..."
mysql_cmd "$DB_NAME" < "$RESOURCES_DIR/data.sql"

echo "生成种子简历占位 PDF ..."
bash "$SCRIPT_DIR/create-seed-resume-files.sh"

echo "重置阶段时间 ..."
mysql_cmd "$DB_NAME" < "$SCRIPT_DIR/reset-dev-db.sql" >/dev/null

echo "初始化完成：${DB_NAME}"
mysql_cmd -N -B "$DB_NAME" -e "
    SELECT 'sys_user', COUNT(*) FROM sys_user
    UNION ALL SELECT 'lab_info', COUNT(*) FROM lab_info
    UNION ALL SELECT 'volunteer_application', COUNT(*) FROM volunteer_application
    UNION ALL SELECT 'application_target', COUNT(*) FROM application_target
    UNION ALL SELECT 'offer', COUNT(*) FROM offer
    UNION ALL SELECT 'final_admission', COUNT(*) FROM final_admission;
"
