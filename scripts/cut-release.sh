#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GRADLE_PROPERTIES="$ROOT_DIR/gradle.properties"
REMOTE="${REMOTE:-origin}"
BRANCH="${BRANCH:-main}"

usage() {
  cat <<'EOF'
Usage:
  ./scripts/cut-release.sh <release-version> [next-snapshot-version]

Examples:
  ./scripts/cut-release.sh 0.1.0
  ./scripts/cut-release.sh 0.2.0 0.2.1-SNAPSHOT

Defaults:
  - next snapshot version defaults to the next patch snapshot
  - git remote defaults to origin
  - branch defaults to main
EOF
}

if [[ "${1:-}" == "" ]] || [[ "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

RELEASE_VERSION="$1"

compute_next_snapshot() {
  python3 - "$1" <<'PY'
import re
import sys

version = sys.argv[1]
match = re.fullmatch(r"(\d+)\.(\d+)\.(\d+)", version)
if not match:
    raise SystemExit(f"Unsupported release version: {version}")
major, minor, patch = map(int, match.groups())
print(f"{major}.{minor}.{patch + 1}-SNAPSHOT")
PY
}

NEXT_SNAPSHOT_VERSION="${2:-$(compute_next_snapshot "$RELEASE_VERSION")}"
TAG="v$RELEASE_VERSION"

require_clean_worktree() {
  if [[ -n "$(git -C "$ROOT_DIR" status --porcelain)" ]]; then
    echo "error: Worktree is dirty. Commit or stash changes before cutting a release." >&2
    exit 1
  fi
}

require_main_branch() {
  local current_branch
  current_branch="$(git -C "$ROOT_DIR" branch --show-current)"
  if [[ "$current_branch" != "$BRANCH" ]]; then
    echo "error: Release cut must run on $BRANCH, current branch is $current_branch." >&2
    exit 1
  fi
}

require_remote() {
  git -C "$ROOT_DIR" remote get-url "$REMOTE" >/dev/null 2>&1 || {
    echo "error: Git remote '$REMOTE' is not configured." >&2
    exit 1
  }
}

require_gh_auth() {
  gh auth status >/dev/null
}

ensure_tag_absent() {
  if git -C "$ROOT_DIR" rev-parse "$TAG" >/dev/null 2>&1; then
    echo "error: Local tag $TAG already exists." >&2
    exit 1
  fi
  if [[ -n "$(git -C "$ROOT_DIR" ls-remote --tags "$REMOTE" "$TAG")" ]]; then
    echo "error: Remote tag $TAG already exists on $REMOTE." >&2
    exit 1
  fi
}

previous_release_tag() {
  git -C "$ROOT_DIR" tag --list 'v*' --sort=-version:refname | grep -Fvx "$TAG" | head -n 1 || true
}

build_release_notes() {
  local notes_file="$1"
  local previous_tag
  local changes_range
  local changelog
  local repo_slug

  previous_tag="$(previous_release_tag)"
  repo_slug="$(gh repo view --json nameWithOwner -q .nameWithOwner)"
  if [[ -n "$previous_tag" ]]; then
    changes_range="$previous_tag..HEAD"
  else
    changes_range="HEAD"
  fi

  changelog="$(
    git -C "$ROOT_DIR" log --reverse --pretty='- %s (%h)' "$changes_range" \
      | grep -Ev '^- (Release |Prepare for )' || true
  )"
  if [[ -z "$changelog" ]]; then
    changelog="- No user-facing changes were detected for this release."
  fi

  cat >"$notes_file" <<EOF
## Maven Central

\`\`\`kotlin
implementation("io.github.hyungju.navermap:naver-map-compose:$RELEASE_VERSION")
\`\`\`

## Changes

$changelog
EOF

  if [[ -n "$previous_tag" ]]; then
    cat >>"$notes_file" <<EOF

## Full Changelog

https://github.com/$repo_slug/compare/$previous_tag...$TAG
EOF
  fi
}

write_version_name() {
  local target_version="$1"
  python3 - "$GRADLE_PROPERTIES" "$target_version" <<'PY'
from pathlib import Path
import sys

path = Path(sys.argv[1])
target = sys.argv[2]
lines = path.read_text().splitlines()
updated = []
replaced = False
for line in lines:
    if line.startswith("VERSION_NAME="):
        updated.append(f"VERSION_NAME={target}")
        replaced = True
    else:
        updated.append(line)
if not replaced:
    raise SystemExit("VERSION_NAME was not found in gradle.properties")
path.write_text("\n".join(updated) + "\n")
PY
}

commit_version_change() {
  local target_version="$1"
  local message="$2"
  write_version_name "$target_version"
  git -C "$ROOT_DIR" add gradle.properties
  git -C "$ROOT_DIR" commit -m "$message"
}

require_clean_worktree
require_main_branch
require_remote
require_gh_auth
ensure_tag_absent

echo "Cutting release $RELEASE_VERSION"
echo "Next snapshot will be $NEXT_SNAPSHOT_VERSION"

commit_version_change "$RELEASE_VERSION" "Release $RELEASE_VERSION"
git -C "$ROOT_DIR" tag -a "$TAG" -m "Release $TAG"
git -C "$ROOT_DIR" push "$REMOTE" "$TAG"

RELEASE_NOTES_FILE="$(mktemp)"
trap 'rm -f "$RELEASE_NOTES_FILE"' EXIT
build_release_notes "$RELEASE_NOTES_FILE"

if gh release view "$TAG" >/dev/null 2>&1; then
  gh release edit "$TAG" --title "$TAG" --notes-file "$RELEASE_NOTES_FILE" --latest --verify-tag
else
  gh release create "$TAG" --title "$TAG" --notes-file "$RELEASE_NOTES_FILE" --verify-tag --latest
fi

commit_version_change "$NEXT_SNAPSHOT_VERSION" "Prepare for $NEXT_SNAPSHOT_VERSION"
git -C "$ROOT_DIR" push "$REMOTE" "$BRANCH"

cat <<EOF
Done.
- release tag: $TAG
- next snapshot: $NEXT_SNAPSHOT_VERSION

If you want to watch the publish workflow:
  gh run list --workflow "Publish Maven Central" --limit 5
EOF
