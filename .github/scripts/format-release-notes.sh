#!/usr/bin/env bash
set -euo pipefail

# Groups GitHub auto-generated release notes by issue type.
#
# Usage:
#   format-release-notes.sh <tag> [--update] [--repo owner/repo]
#
# Without --update: prints formatted notes to stdout
# With --update: updates the GitHub release in-place

REPO=""
TAG=""
UPDATE=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --update) UPDATE=true; shift ;;
    --repo) REPO="$2"; shift 2 ;;
    *) TAG="$1"; shift ;;
  esac
done

if [[ -z "$TAG" ]]; then
  echo "Usage: format-release-notes.sh <tag> [--update] [--repo owner/repo]" >&2
  exit 1
fi

if [[ -z "$REPO" ]]; then
  REPO=$(gh repo view --json nameWithOwner -q .nameWithOwner 2>/dev/null)
fi

TMPDIR=$(mktemp -d)
trap 'rm -rf "$TMPDIR"' EXIT

gh release view "$TAG" --repo "$REPO" --json body -q .body > "$TMPDIR/body.txt"

# Split into entry lines and trailing sections (New Contributors, Full Changelog)
awk '
  /^## New Contributors/ { found=1 }
  /^\*\*Full Changelog\*\*/ { found=1 }
  found  { print > "'"$TMPDIR"'/trailing.txt"; next }
  /^\* / { print > "'"$TMPDIR"'/entries.txt"; next }
' "$TMPDIR/body.txt"

touch "$TMPDIR/entries.txt" "$TMPDIR/trailing.txt"
touch "$TMPDIR/bugs.txt" "$TMPDIR/features.txt" "$TMPDIR/tasks.txt" "$TMPDIR/epics.txt"
touch "$TMPDIR/other.txt" "$TMPDIR/deps.txt" "$TMPDIR/issue_nums.txt"

# Classify each entry line
while IFS= read -r line; do
  if [[ "$line" =~ by[[:space:]]@dependabot ]]; then
    echo "$line" >> "$TMPDIR/deps.txt"
  elif [[ "$line" =~ ^\*[[:space:]]\[#([0-9]+)\] ]]; then
    num="${BASH_REMATCH[1]}"
    echo "$num" >> "$TMPDIR/issue_nums.txt"
    printf '%s\t%s\n' "$num" "$line" >> "$TMPDIR/issue_entries.txt"
  else
    echo "$line" >> "$TMPDIR/other.txt"
  fi
done < "$TMPDIR/entries.txt"

# Fetch issue types in parallel
if [[ -s "$TMPDIR/issue_nums.txt" ]]; then
  xargs -P 10 -I{} sh -c '
    type=$(gh api "repos/'"$REPO"'/issues/{}" --jq ".type.name // empty" 2>/dev/null || true)
    echo "{}	${type:-Unknown}"
  ' < "$TMPDIR/issue_nums.txt" > "$TMPDIR/types.txt"
fi

# Build type lookup and group entries
if [[ -s "$TMPDIR/issue_entries.txt" ]]; then
  while IFS=$'\t' read -r num line; do
    type="Unknown"
    if [[ -s "$TMPDIR/types.txt" ]]; then
      type=$(grep -m1 "^${num}	" "$TMPDIR/types.txt" | cut -f2 || echo "Unknown")
      [[ -z "$type" ]] && type="Unknown"
    fi
    case "$type" in
      Bug)     echo "$line" >> "$TMPDIR/bugs.txt" ;;
      Feature) echo "$line" >> "$TMPDIR/features.txt" ;;
      Task)    echo "$line" >> "$TMPDIR/tasks.txt" ;;
      Epic)    echo "$line" >> "$TMPDIR/epics.txt" ;;
      *)       echo "$line" >> "$TMPDIR/other.txt" ;;
    esac
  done < "$TMPDIR/issue_entries.txt"
fi

# Sort dependabot lines alphabetically
sort -f -o "$TMPDIR/deps.txt" "$TMPDIR/deps.txt"

# Build formatted output
{
  echo "## What's Changed"
  echo ""

  for section in "Epics:epics" "New Features:features" "Bug Fixes:bugs" "Tasks:tasks" "Other:other" "Dependencies:deps"; do
    title="${section%%:*}"
    file="$TMPDIR/${section##*:}.txt"
    if [[ -s "$file" ]]; then
      echo "### $title"
      cat "$file"
      echo ""
    fi
  done

  if [[ -s "$TMPDIR/trailing.txt" ]]; then
    cat "$TMPDIR/trailing.txt"
  fi
} > "$TMPDIR/formatted.txt"

if $UPDATE; then
  gh release edit "$TAG" --repo "$REPO" --notes-file "$TMPDIR/formatted.txt"
  echo "Release $TAG notes updated." >&2
else
  cat "$TMPDIR/formatted.txt"
fi
