# Default Smoke Scenario

Use this when the user did not provide a custom scenario.

```text
Scenario name: Launch placeholder smoke
Platforms: both
Goal: Verify the Compose Multiplatform sample app boots on Android and iOS and shows the shared placeholder content.
Preconditions:
- Use one available Android emulator and one available iOS simulator.
Steps:
1. Build the sample app for the target platform.
2. Install and launch the app.
3. Wait for the first stable screen.
Assertions:
- "Compose Multiplatform Sample" is visible.
- The placeholder card from the library is visible.
- "Next step: replace this placeholder" is visible.
Evidence:
- Save one screenshot per platform under /Users/jude/Code/naver-map-compose-multiplatform/artifacts/mobile.
```
