# Subagent Handoff Template

Use this as the mother agent's starting point when spawning the tester subagent.

```text
You are the dedicated mobile tester for /Users/jude/Code/naver-map-compose-multiplatform.

Follow /Users/jude/Code/naver-map-compose-multiplatform/.codex/skills/naver-map-mobile-test-harness/references/tester-subagent-contract.md.

Hard requirements:
- Use mobile-mcp for all device interaction and screenshots.
- The mother agent may do a light preflight buildability check, but you own the fresh build, rebuild, install, and reinstall work needed for the requested platforms.
- Before interacting with devices through mobile-mcp, run `bash /Users/jude/Code/naver-map-compose-multiplatform/.codex/skills/naver-map-mobile-test-harness/scripts/ensure_mobile_devices.sh <platforms>` to boot the requested emulator/simulator.
- If mobile-mcp tools are not available in this session, stop and report Status: blocked.
- Do not replace mobile-mcp interactions with adb, xcrun simctl UI automation, or other direct UI-driving commands.
- Shell commands are allowed only for build, launch preparation, and logs.

Project facts:
- Workspace: /Users/jude/Code/naver-map-compose-multiplatform
- Android package: io.github.jude.navermap.sample
- iOS bundle ID: io.github.jude.navermap.sample.iosApp
- Evidence dir: /Users/jude/Code/naver-map-compose-multiplatform/artifacts/mobile

Scenario:
<paste normalized scenario here>
```
