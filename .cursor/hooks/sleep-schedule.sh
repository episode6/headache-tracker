#!/usr/bin/env bash
# After agent stop: request doc sync when code or project docs changed.
set -euo pipefail

cd "$(git rev-parse --show-toplevel 2>/dev/null)" || exit 0

collect_changed_files() {
  {
    git diff --name-only HEAD 2>/dev/null || true
    git diff --cached --name-only 2>/dev/null || true
    git ls-files --others --exclude-standard 2>/dev/null || true
  } | sort -u | grep -v '^$' || true
}

is_relevant() {
  local file=$1
  case "$file" in
    app/*|gradle/*|gradlew|gradlew.bat|gradle.properties|settings.gradle.kts|build.gradle.kts|README.md|AGENTS.md|.cursor/skills/*|.cursor/hooks/*|.cursor/hooks.json)
      return 0
      ;;
  esac
  return 1
}

changed=$(collect_changed_files)
if [ -z "$changed" ]; then
  exit 0
fi

relevant=0
while IFS= read -r file; do
  [ -z "$file" ] && continue
  if is_relevant "$file"; then
    relevant=1
    break
  fi
done <<< "$changed"

if [ "$relevant" -eq 0 ]; then
  exit 0
fi

# Only doc/skill/hook edits: still sync, but skip if the sole change is this hook script running mid-fix.
only_meta=1
while IFS= read -r file; do
  [ -z "$file" ] && continue
  case "$file" in
    README.md|AGENTS.md|.cursor/skills/*|.cursor/hooks/*|.cursor/hooks.json)
      ;;
    *)
      only_meta=0
      break
      ;;
  esac
done <<< "$changed"

cat <<'EOF'
{
  "followup_message": "Run the sleep-schedule skill: read .cursor/skills/sleep-schedule/SKILL.md and follow it completely. Verify README.md, AGENTS.md, and all repo-local skills under .cursor/skills/ against the codebase; fix inconsistencies, errors, and undocumented features. If everything is already in sync, reply briefly that sleep schedule passed."
}
EOF

exit 0
