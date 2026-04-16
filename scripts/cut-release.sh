#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GRADLE_PROPERTIES="$ROOT_DIR/gradle.properties"
REMOTE="${REMOTE:-origin}"
BRANCH="${BRANCH:-main}"
PRE_RELEASE_CHECK_CMD="${PRE_RELEASE_CHECK_CMD:-./gradlew :naver-map-compose:publishToMavenLocal}"
SKIP_PRE_RELEASE_CHECK="${SKIP_PRE_RELEASE_CHECK:-0}"
SKIP_GH_RELEASE="${SKIP_GH_RELEASE:-0}"
DRY_RUN=0

usage() {
  cat <<'EOF'
Usage:
  ./scripts/cut-release.sh [--dry-run] [--skip-preflight] [--skip-gh-release] <release-version> [next-snapshot-version]

Examples:
  ./scripts/cut-release.sh 0.1.4
  ./scripts/cut-release.sh 0.2.0 0.3.0-SNAPSHOT
  ./scripts/cut-release.sh --dry-run 0.1.4

Defaults:
  - next snapshot version defaults to the next patch snapshot
  - git remote defaults to origin
  - branch defaults to main
  - preflight validation defaults to: ./gradlew :naver-map-compose:publishToMavenLocal

Environment overrides:
  REMOTE=<name>
  BRANCH=<name>
  PRE_RELEASE_CHECK_CMD="<command>"
  SKIP_PRE_RELEASE_CHECK=1
  SKIP_GH_RELEASE=1
EOF
}

die() {
  echo "error: $*" >&2
  exit 1
}

run() {
  if [[ "$DRY_RUN" == "1" ]]; then
    printf '[dry-run] '
    printf '%q ' "$@"
    printf '\n'
    return 0
  fi

  "$@"
}

parse_args() {
  local positional=()

  while [[ $# -gt 0 ]]; do
    case "$1" in
      --dry-run)
        DRY_RUN=1
        ;;
      --skip-preflight)
        SKIP_PRE_RELEASE_CHECK=1
        ;;
      --skip-gh-release)
        SKIP_GH_RELEASE=1
        ;;
      --help|-h)
        usage
        exit 0
        ;;
      --)
        shift
        while [[ $# -gt 0 ]]; do
          positional+=("$1")
          shift
        done
        break
        ;;
      -*)
        die "Unknown option: $1"
        ;;
      *)
        positional+=("$1")
        ;;
    esac
    shift
  done

  if [[ "${#positional[@]}" -lt 1 || "${#positional[@]}" -gt 2 ]]; then
    usage >&2
    exit 1
  fi

  RELEASE_VERSION="${positional[0]}"
  NEXT_SNAPSHOT_VERSION="${positional[1]:-$(compute_next_snapshot "$RELEASE_VERSION")}"
  TAG="v$RELEASE_VERSION"
}

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

validate_versions() {
  python3 - "$RELEASE_VERSION" "$NEXT_SNAPSHOT_VERSION" <<'PY'
import re
import sys

release_version = sys.argv[1]
next_snapshot = sys.argv[2]

release_match = re.fullmatch(r"(\d+)\.(\d+)\.(\d+)", release_version)
snapshot_match = re.fullmatch(r"(\d+)\.(\d+)\.(\d+)-SNAPSHOT", next_snapshot)

if not release_match:
    raise SystemExit(f"Unsupported release version: {release_version}")
if not snapshot_match:
    raise SystemExit(f"Unsupported next snapshot version: {next_snapshot}")

release_tuple = tuple(map(int, release_match.groups()))
snapshot_tuple = tuple(map(int, snapshot_match.groups()))

if snapshot_tuple <= release_tuple:
    raise SystemExit(
        f"Next snapshot version must be greater than release version: {next_snapshot} <= {release_version}"
    )
PY
}

read_version_name() {
  python3 - "$GRADLE_PROPERTIES" <<'PY'
from pathlib import Path
import sys

path = Path(sys.argv[1])
for line in path.read_text().splitlines():
    if line.startswith("VERSION_NAME="):
        print(line.split("=", 1)[1].strip())
        break
else:
    raise SystemExit("VERSION_NAME was not found in gradle.properties")
PY
}

require_snapshot_working_version() {
  CURRENT_VERSION="$(read_version_name)"
  if [[ "$CURRENT_VERSION" != *-SNAPSHOT ]]; then
    die "VERSION_NAME must be a -SNAPSHOT before cutting a release. Current value: $CURRENT_VERSION"
  fi
}

require_clean_worktree() {
  if [[ -n "$(git -C "$ROOT_DIR" status --porcelain)" ]]; then
    die "Worktree is dirty. Commit or stash changes before cutting a release."
  fi
}

require_main_branch() {
  local current_branch
  current_branch="$(git -C "$ROOT_DIR" branch --show-current)"
  if [[ "$current_branch" != "$BRANCH" ]]; then
    die "Release cut must run on $BRANCH, current branch is $current_branch."
  fi
}

require_remote() {
  git -C "$ROOT_DIR" remote get-url "$REMOTE" >/dev/null 2>&1 || die "Git remote '$REMOTE' is not configured."
}

fetch_remote_state() {
  git -C "$ROOT_DIR" fetch "$REMOTE" "$BRANCH" --tags >/dev/null 2>&1 || die "Failed to fetch $REMOTE/$BRANCH."
}

require_not_behind_remote() {
  local local_sha remote_sha merge_base
  local_sha="$(git -C "$ROOT_DIR" rev-parse HEAD)"
  remote_sha="$(git -C "$ROOT_DIR" rev-parse "$REMOTE/$BRANCH")"
  merge_base="$(git -C "$ROOT_DIR" merge-base HEAD "$REMOTE/$BRANCH")"

  if [[ "$local_sha" == "$remote_sha" ]]; then
    return 0
  fi

  if [[ "$merge_base" == "$local_sha" ]]; then
    die "Local $BRANCH is behind $REMOTE/$BRANCH. Pull or rebase before releasing."
  fi

  if [[ "$merge_base" != "$remote_sha" ]]; then
    die "Local $BRANCH has diverged from $REMOTE/$BRANCH. Reconcile the branch before releasing."
  fi
}

require_gh_auth() {
  if [[ "$SKIP_GH_RELEASE" == "1" ]]; then
    return 0
  fi

  gh auth status >/dev/null || die "gh auth status failed."
}

ensure_tag_absent() {
  if git -C "$ROOT_DIR" rev-parse "$TAG" >/dev/null 2>&1; then
    die "Local tag $TAG already exists."
  fi
  if [[ -n "$(git -C "$ROOT_DIR" ls-remote --tags "$REMOTE" "$TAG")" ]]; then
    die "Remote tag $TAG already exists on $REMOTE."
  fi
}

run_preflight_check() {
  if [[ "$SKIP_PRE_RELEASE_CHECK" == "1" ]]; then
    echo "Skipping preflight validation."
    return 0
  fi

  echo "Running preflight validation: $PRE_RELEASE_CHECK_CMD"
  if [[ "$DRY_RUN" == "1" ]]; then
    echo "[dry-run] bash -lc $(printf '%q' "$PRE_RELEASE_CHECK_CMD")"
    return 0
  fi

  (
    cd "$ROOT_DIR"
    bash -lc "$PRE_RELEASE_CHECK_CMD"
  )
}

previous_release_tag() {
  git -C "$ROOT_DIR" tag --list 'v*' --sort=-version:refname | grep -Fvx "$TAG" | head -n 1 || true
}

repo_slug_from_remote() {
  local remote_url slug
  remote_url="$(git -C "$ROOT_DIR" remote get-url "$REMOTE")"
  slug="$(printf '%s' "$remote_url" | sed -E 's#(git@github.com:|https://github.com/)##; s#\.git$##')"
  if [[ -z "$slug" || "$slug" == "$remote_url" ]]; then
    die "Could not infer GitHub repository slug from remote URL: $remote_url"
  fi
  printf '%s\n' "$slug"
}

build_release_notes() {
  local notes_file="$1"
  local previous_tag
  local changes_range
  local changelog
  local repo_slug

  previous_tag="$(previous_release_tag)"
  repo_slug="$(repo_slug_from_remote)"

  if [[ -n "$previous_tag" ]]; then
    changes_range="$previous_tag..HEAD"
  else
    changes_range="HEAD"
  fi

  changelog="$(
    git -C "$ROOT_DIR" log --reverse --pretty='- %s (%h)' "$changes_range" \
      | grep -Ev '^- (Release |Prepare for |chore\(release\): )' || true
  )"
  if [[ -z "$changelog" ]]; then
    changelog="- No user-facing changes were detected for this release."
  fi

  cat >"$notes_file" <<EOF
## Maven Central

\`\`\`kotlin
implementation("io.github.hyungju.navermap:naver-map-compose:$RELEASE_VERSION")
\`\`\`

## Included Changes

$changelog
EOF

  if [[ -n "$previous_tag" ]]; then
    cat >>"$notes_file" <<EOF

## Compare

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

  if [[ "$DRY_RUN" == "1" ]]; then
    echo "[dry-run] VERSION_NAME=$target_version"
    printf '[dry-run] %q ' git -C "$ROOT_DIR" add gradle.properties
    printf '\n'
    printf '[dry-run] %q ' git -C "$ROOT_DIR" commit -m "$message"
    printf '\n'
    return 0
  fi

  write_version_name "$target_version"
  run git -C "$ROOT_DIR" add gradle.properties
  run git -C "$ROOT_DIR" commit -m "$message"
}

create_or_update_github_release() {
  local notes_file="$1"

  if [[ "$SKIP_GH_RELEASE" == "1" ]]; then
    echo "Skipping GitHub Release creation."
    return 0
  fi

  if gh release view "$TAG" >/dev/null 2>&1; then
    run gh release edit "$TAG" --title "$TAG" --notes-file "$notes_file" --latest --verify-tag
  else
    run gh release create "$TAG" --title "$TAG" --notes-file "$notes_file" --verify-tag --latest
  fi
}

main() {
  parse_args "$@"
  validate_versions
  require_snapshot_working_version
  require_clean_worktree
  require_main_branch
  require_remote
  fetch_remote_state
  require_not_behind_remote
  require_gh_auth
  ensure_tag_absent
  run_preflight_check

  echo "Cutting release $RELEASE_VERSION"
  echo "Current version: $CURRENT_VERSION"
  echo "Next snapshot: $NEXT_SNAPSHOT_VERSION"

  commit_version_change "$RELEASE_VERSION" "chore(release): $RELEASE_VERSION"
  run git -C "$ROOT_DIR" tag -a "$TAG" -m "chore(release): $TAG"
  run git -C "$ROOT_DIR" push "$REMOTE" "$BRANCH" "$TAG"

  RELEASE_NOTES_FILE="$(mktemp)"
  trap 'rm -f "$RELEASE_NOTES_FILE"' EXIT
  build_release_notes "$RELEASE_NOTES_FILE"
  create_or_update_github_release "$RELEASE_NOTES_FILE"

  commit_version_change "$NEXT_SNAPSHOT_VERSION" "chore(release): 다음 개발 버전 $NEXT_SNAPSHOT_VERSION"
  run git -C "$ROOT_DIR" push "$REMOTE" "$BRANCH"

  cat <<EOF
Done.
- release tag: $TAG
- next snapshot: $NEXT_SNAPSHOT_VERSION

If you want to watch the publish workflow:
  gh run list --workflow "Publish Maven Central" --limit 5
EOF
}

main "$@"
