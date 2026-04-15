---
name: release
description: Use when you need Codex to inspect recent changes, recommend the next release version with LLM judgment, and then cut a Maven Central release for this repository with a simple command that tags, pushes, and creates or updates the GitHub Release with gh CLI. This skill is intended to be invoked directly as `/release`.
---

# Release

Use this skill for release tasks in `/Users/jude/Code/naver-map-compose-multiplatform`.

## Workflow

1. Inspect the latest release tag, commit subjects, and changed files.
2. Use LLM judgment to recommend `major`, `minor`, or `patch`.
3. Propose the concrete release version and next snapshot version.
4. If the user wants to proceed, run:

```bash
./scripts/cut-release.sh <release-version> [next-snapshot-version]
```

5. After the release exists, write the GitHub Release notes in Korean and update the Release body with `gh release edit`.

## What The Script Does

- Creates a release commit with a non-snapshot `VERSION_NAME`
- Creates and pushes an annotated `vX.Y.Z` tag
- Creates or updates the GitHub Release with `gh`, including a generated description
- Bumps `VERSION_NAME` to the next snapshot and pushes `main`

## Version Recommendation

Do the version recommendation in the conversation, not in the script.

Suggested reasoning:

- Breaking change markers such as `feat!:` or `BREAKING CHANGE` usually mean `major`
- Shared API additions or behavior expansion under `naver-map-compose/src/commonMain` usually mean `minor`
- Fixes, tooling, docs, CI, and implementation-only changes usually mean `patch`
- If there is no previous release tag, use the current non-snapshot part of `VERSION_NAME` as the first release baseline

## Korean Release Notes

- Every release should end with Korean release notes, even if the helper script created an English placeholder body first.
- Summarize the release in natural Korean, not a literal commit dump.
- Prefer concise sections such as `Maven Central`, `주요 변경사항`, `수정 및 안정화`, and `기타`.
- Include the install coordinate for the released version.
- Group related commits into a user-facing summary instead of listing every internal release-management commit.
- If a compare link is helpful, keep it at the bottom.
- After drafting the Korean notes, apply them to GitHub with `gh release edit <tag> --notes-file <file>`.

## Guardrails

- `./scripts/cut-release.sh` must run on `main`
- `./scripts/cut-release.sh` refuses a dirty worktree
- `./scripts/cut-release.sh` refuses if the tag already exists
- `./scripts/cut-release.sh` expects `gh auth status` to succeed

## Notes

- This skill assumes the repository remote already points at GitHub.
- If the repo uses a remote other than `origin`, run with `REMOTE=<name> ./scripts/cut-release.sh ...`.
- Read [`docs/publishing.md`](../../../docs/publishing.md) when you need the release prerequisites or secret names.
