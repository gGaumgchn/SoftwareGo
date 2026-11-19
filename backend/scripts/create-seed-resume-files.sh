#!/usr/bin/env bash
# 为 dev 种子 resume_file 生成占位 PDF，使 /files/{id} 下载可用于手工验收。
# 只写 backend/uploads（已被 Git 忽略），不修改数据库。
#
# 用法：
#   bash backend/scripts/create-seed-resume-files.sh
#   bash backend/scripts/create-seed-resume-files.sh /path/to/uploads/resumes/2026/11

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
TARGET_DIR="${1:-$BACKEND_DIR/uploads/resumes/2026/11}"

mkdir -p "$TARGET_DIR"

write_pdf() {
    local path="$1"
    if [[ -f "$path" ]]; then
        return 0
    fi
    {
        printf '%%PDF-1.4\n1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n'
        printf '2 0 obj\n<< /Type /Pages /Kids [] /Count 0 >>\nendobj\n'
        printf 'trailer\n<< /Root 1 0 R >>\n%%%%EOF\n'
        head -c 1024 /dev/zero | tr '\0' ' '
    } > "$path"
}

for index in 01 02 03 04; do
    write_pdf "$TARGET_DIR/seed-teacher-$index.pdf"
done

for index in 01 02 03 04 05 06 07 08 09 10; do
    write_pdf "$TARGET_DIR/seed-student-$index.pdf"
done

echo "种子简历占位 PDF 已就绪：$TARGET_DIR"
