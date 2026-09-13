#!/usr/bin/env bash
#
# A dump of the database, kept for a fortnight.
#
# Running Postgres yourself is what makes this setup free, and this is the part
# of it that a managed database would have been doing. The volume survives
# `docker compose down`, but not `docker compose down -v`, not a deleted
# instance, and not a mistaken DELETE.
#
# Hourly, via the crontab of the user that owns the compose project:
#   0 * * * * /home/ubuntu/BetSocial/deploy/backup.sh >> /home/ubuntu/backup.log 2>&1
#
# Restore:
#   gunzip -c betsocial-<timestamp>.sql.gz | \
#     docker compose exec -T db psql -U betsocial -d betsocial

set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEST="${BACKUP_DIR:-$HERE/backups}"
KEEP_DAYS="${BACKUP_KEEP_DAYS:-14}"

mkdir -p "$DEST"

STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
FILE="$DEST/betsocial-$STAMP.sql.gz"

# --clean --if-exists so the dump can be replayed over an existing database
# rather than only into an empty one, which is the state you are usually in when
# you need it.
docker compose -f "$HERE/docker-compose.yml" exec -T db \
	pg_dump -U betsocial -d betsocial --clean --if-exists \
	| gzip > "$FILE"

# A dump that failed halfway is still a file, and would rotate a good one out.
if [ ! -s "$FILE" ]; then
	echo "$(date -u +%FT%TZ) backup FAILED: $FILE is empty" >&2
	rm -f "$FILE"
	exit 1
fi

find "$DEST" -name 'betsocial-*.sql.gz' -mtime "+$KEEP_DAYS" -delete

echo "$(date -u +%FT%TZ) backup ok: $FILE ($(du -h "$FILE" | cut -f1))"
