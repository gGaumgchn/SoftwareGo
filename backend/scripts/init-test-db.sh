#!/usr/bin/env bash
# 初始化独立测试库：只执行 schema.sql，不灌入演示数据。
#
# 本机没有 Docker，不使用 Testcontainers；测试通过 application-test.yml 的
# TEST_DB_URL 指向该库即可。
#
# 用法：
#   bash backend/scripts/init-test-db.sh
#   DB_NAME=software_go_selection_test bash backend/scripts/init-test-db.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./db-env.sh
source "$SCRIPT_DIR/db-env.sh"

DB_NAME="${DB_NAME:-software_go_selection_test}"

echo "目标测试库：${DB_NAME}（${DB_USERNAME}@${DB_HOST}:${DB_PORT}）"

create_database_if_needed "$DB_NAME"

existing_tables="$(mysql_cmd -N -B -e "
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = '${DB_NAME}'
      AND table_name = 'sys_user';
")"

if [[ "$existing_tables" != "0" ]]; then
    echo "测试库 ${DB_NAME} 已存在业务表，未执行初始化。" >&2
    echo "如需重建，请先备份并手动删除该库。" >&2
    exit 1
fi

echo "执行 schema.sql ..."
mysql_cmd "$DB_NAME" < "$RESOURCES_DIR/schema.sql"

echo "测试库初始化完成：${DB_NAME}"
echo "运行测试示例："
echo "  cd backend && TEST_DB_URL='jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useUnicode=true&characterEncoding=utf8&connectionTimeZone=Asia/Shanghai' mvn test"
