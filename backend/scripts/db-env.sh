#!/usr/bin/env bash
# 数据库连接配置读取：优先环境变量，其次 application-local.yml。
# 本文件被其他脚本 source，不直接执行。
#
# 可用环境变量：
#   DB_HOST / DB_PORT / DB_PROTOCOL / DB_USERNAME / DB_PASSWORD
#   MYSQL_BIN（默认 mysql）

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
RESOURCES_DIR="$BACKEND_DIR/src/main/resources/db"
LOCAL_CONFIG="$BACKEND_DIR/src/main/resources/application-local.yml"

read_yaml_value() {
    local key="$1"
    [[ -f "$LOCAL_CONFIG" ]] || return 0
    awk -v key="$key" '
        $0 ~ "^[[:space:]]*" key ":[[:space:]]*" {
            sub("^[[:space:]]*" key ":[[:space:]]*", "");
            gsub(/^["'\'']|["'\'']$/, "");
            print;
            exit;
        }
    ' "$LOCAL_CONFIG"
}

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_PROTOCOL="${DB_PROTOCOL:-TCP}"
DB_USERNAME="${DB_USERNAME:-$(read_yaml_value username)}"
DB_PASSWORD="${DB_PASSWORD:-$(read_yaml_value password)}"
DB_USERNAME="${DB_USERNAME:-root}"
MYSQL_BIN="${MYSQL_BIN:-mysql}"

# 使用 MYSQL_PWD 避免密码出现在命令行参数/进程列表。
export MYSQL_PWD="${DB_PASSWORD:-}"

mysql_cmd() {
    "$MYSQL_BIN" \
        --protocol="$DB_PROTOCOL" \
        --host="$DB_HOST" \
        --port="$DB_PORT" \
        --user="$DB_USERNAME" \
        --default-character-set=utf8mb4 \
        --connect-timeout=5 \
        "$@"
}

database_exists() {
    local db_name="$1"
    local count
    count="$(mysql_cmd -N -B -e "
        SELECT COUNT(*)
        FROM information_schema.schemata
        WHERE schema_name = '${db_name}';
    ")" || return 1
    [[ "$count" != "0" ]]
}

create_database_if_needed() {
    local db_name="$1"
    if database_exists "$db_name"; then
        echo "数据库 ${db_name} 已存在，跳过创建。"
        return 0
    fi
    if ! mysql_cmd -e "CREATE DATABASE \`${db_name}\`
        DEFAULT CHARACTER SET utf8mb4
        DEFAULT COLLATE utf8mb4_0900_ai_ci;"; then
        echo "无法创建数据库 ${db_name}。" >&2
        echo "请让 DBA 先创建该库（utf8mb4 / utf8mb4_0900_ai_ci），或为 ${DB_USERNAME} 授予 CREATE 权限。" >&2
        echo "也可以直接使用已有数据库，例如 DB_NAME=software_go_selection。" >&2
        return 1
    fi
}
