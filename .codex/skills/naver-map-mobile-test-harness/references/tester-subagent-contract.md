# Tester Subagent Contract

The dedicated tester subagent receives:

- the normalized scenario
- the platform scope
- the workspace path
- the artifact directory
- the Android package and iOS bundle ID

## Required behavior

1. Treat the mother agent's build step as a preflight only.
2. Perform the fresh build or rebuild needed for each requested platform yourself before validating the app on device.
3. Run `bash /Users/jude/Code/naver-map-compose-multiplatform/.codex/skills/naver-map-mobile-test-harness/scripts/ensure_mobile_devices.sh <platforms>` before using `mobile-mcp`.
4. Confirm whether `mobile-mcp` tools are actually available in the current session.
5. Use `mobile-mcp` for device listing, launch, element inspection, gestures, and screenshots.
6. Use shell commands only for build, install prerequisites, device bootstrapping, and log collection.
7. If `mobile-mcp` tools are unavailable, stop and return `blocked`.
8. If the bootstrap script cannot provide the requested platform device, stop and return `blocked`.
9. Save evidence under `/Users/jude/Code/naver-map-compose-multiplatform/artifacts/mobile`.

## Required response format

Return a concise report with these sections in order:

```text
Status: passed | failed | blocked
Used mobile-mcp: yes | no
Scenario: <name>
Platforms:
- <platform>: <result>
Checks:
- <check>: pass | fail | blocked
Artifacts:
- <absolute path>
Issues:
- <issue or none>
Next action:
- <one concrete recommendation>
```

## Failure policy

- `passed`: all requested checks passed and required evidence exists
- `failed`: the app ran but one or more requested checks failed
- `blocked`: required setup or required tools prevented execution

Treat missing AVDs, missing iPhone simulators, failed device boots, or fresh rebuild/install failures as setup blockers.

## Evidence policy

- Use absolute file paths.
- Prefer one screenshot per stable checkpoint.
- Mention missing evidence explicitly if a step failed early.
