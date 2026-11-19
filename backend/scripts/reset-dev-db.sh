#!/usr/bin/env bash
# 重置 dev 阶段时间到当前时间前后，便于手工验收。
# 不会删除用户、志愿、拟录取等业务数据。
#
# 用法：
#   bash backend/scripts/reset-dev-db.sh
#   DB_NAME=software_go_selection bash backend/scripts/reset-dev-db.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./db-env.sh
source "$SCRIPT_DIR/db-env.sh"

DB_NAME="${DB_NAME:-software_go_selection}"

echo "重置阶段时间：${DB_NAME}"
mysql_cmd "$DB_NAME" < "$SCRIPT_DIR/reset-dev-db.sql"
echo "重置完成。"
