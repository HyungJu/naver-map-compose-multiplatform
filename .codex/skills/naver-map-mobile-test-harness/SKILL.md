---
name: naver-map-mobile-test-harness
description: Use when you need scenario-driven Android and iOS app testing for /Users/jude/Code/naver-map-compose-multiplatform through a dedicated Codex subagent, with mobile-mcp required for device interaction, screenshots, and evidence collection.
---

# Naver Map Mobile Test Harness

Use this skill only for `/Users/jude/Code/naver-map-compose-multiplatform`.

This skill is for the shared `composeApp` sample when the user wants:

- a real run on Android, iOS, or both
- scenario-based testing instead of a fixed smoke script
- screenshots or other evidence
- the work delegated to a dedicated tester subagent

## Hard rules

1. Device interaction must go through `mobile-mcp`.
2. If `mobile-mcp` is missing from Codex config, install it first with `scripts/ensure_mobile_mcp.sh`.
3. Before testing, boot the requested Android emulator and iOS simulator with `scripts/ensure_mobile_devices.sh`.
4. If `mobile-mcp` is configured but not available to the current session tools, stop and report `blocked`.
5. Do not silently fall back to `adb`, `xcrun simctl`, or other direct UI-driving commands for taps, swipes, screenshots, or element inspection.
6. Shell commands are allowed for build and environment preparation only.

## Project facts

- Workspace root: `/Users/jude/Code/naver-map-compose-multiplatform`
- Shared sample app module: `composeApp`
- Android package: `io.github.jude.navermap.sample`
- iOS bundle ID: `io.github.jude.navermap.sample.iosApp`
- Default evidence directory: `/Users/jude/Code/naver-map-compose-multiplatform/artifacts/mobile`

## Required workflow

1. Read the user scenario.
If the user did not provide one, use the baseline in `references/default-smoke-scenario.md`.

2. Normalize the scenario.
Read `references/scenario-format.md` and turn the request into concrete checks, target platforms, and expected evidence.

3. Prove the app is buildable before delegating.
The mother agent should run one light buildability check for the current change and stop early if the app is already broken.
This is a preflight only.
Fresh platform builds, rebuilds, and reinstalls still belong to the tester subagent.

4. Ensure `mobile-mcp` exists.
Run `bash /Users/jude/Code/naver-map-compose-multiplatform/.codex/skills/naver-map-mobile-test-harness/scripts/ensure_mobile_mcp.sh`.

5. Ensure requested devices are booted.
Run `bash /Users/jude/Code/naver-map-compose-multiplatform/.codex/skills/naver-map-mobile-test-harness/scripts/ensure_mobile_devices.sh <platforms>`.
Use `android`, `ios`, or `both` based on the normalized scenario.

6. Spawn the tester subagent.
Use `spawn_agent` with a dedicated tester prompt based on `references/subagent-handoff-template.md`.
Pass only the scenario, platform scope, artifact directory, the project facts above, and the fact that the mother agent already completed a preflight buildability check.

7. Require a structured tester response.
The tester subagent must follow `references/tester-subagent-contract.md`.

8. Report back from the mother agent.
Summarize pass, fail, or blocked state.
Include evidence paths and the main issues.
If blocked, explain whether the blocker was:
- missing `mobile-mcp`
- `mobile-mcp` not loaded in the session
- no compatible Android emulator or iOS simulator was available to boot
- device boot failed before `mobile-mcp` interaction
- fresh platform rebuild or reinstall failed in the tester subagent
- build failure before device interaction

## Tester subagent behavior

The tester subagent owns:

- fresh platform builds, rebuilds, and reinstalls for the requested targets
- booting requested Android/iOS devices if needed
- selecting available devices
- launching the app
- running the scenario
- capturing screenshots and evidence
- producing a concise report for the mother agent

The tester subagent must not:

- edit product code unless the mother agent explicitly asked for a fix
- assume the mother agent's preflight build replaces a fresh target-platform build
- claim `mobile-mcp` usage unless it actually used those tools
- replace a blocked MCP run with direct UI automation commands

## Files to read on demand

- Scenario format: `references/scenario-format.md`
- Default baseline: `references/default-smoke-scenario.md`
- Tester contract: `references/tester-subagent-contract.md`
- Handoff prompt starter: `references/subagent-handoff-template.md`
- Device bootstrap script: `scripts/ensure_mobile_devices.sh`

## Output expectation

The mother agent should return:

- overall status: `passed`, `failed`, or `blocked`
- which scenario ran
- which platforms were attempted
- key assertions and their results
- screenshot or evidence paths
- the top next action if anything failed or blocked
