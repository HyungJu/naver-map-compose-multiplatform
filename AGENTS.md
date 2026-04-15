# AGENTS.md

## Scope

These instructions apply to the entire workspace at `/Users/jude/Code/naver-map-compose-multiplatform`.

## Required Mobile Validation

- For this project, use the `$naver-map-mobile-test-harness` skill at `/Users/jude/Code/naver-map-compose-multiplatform/.codex/skills/naver-map-mobile-test-harness/SKILL.md` whenever the user asks for mobile testing, asks to verify a user-facing change on device, or when a task needs real Android and iOS validation before being treated as finished.
- If the user asks for testing, do not substitute a simulator-free explanation, unit-only validation, or a direct `adb` / `xcrun simctl` UI-driving fallback for the required harness workflow.
- Before declaring a feature finished, `passed`, complete, ready, or equivalent, run the harness workflow and obtain a `passed` result when the change affects the shared app behavior that should be verified on device.
- If the harness run is blocked or fails, do not present the feature as passed. Report the blocker or failure clearly and treat the task as not yet complete.

## Planning Requirement

- When creating a plan for work in this repository, explicitly include a mobile validation step that uses `$naver-map-mobile-test-harness` whenever the task includes UI changes, user-requested testing, or any path where the final outcome depends on real device confirmation.
- The plan should make it clear that implementation is not the final step; a passing harness result is part of the completion criteria for qualifying tasks.

## Execution Notes

- Follow the skill's required workflow, including the preflight buildability check by the mother agent, device bootstrapping, and delegated scenario-based validation through `mobile-mcp`.
- Use the structured pass / fail / blocked outcome from the harness in the final report, including evidence paths when available.
