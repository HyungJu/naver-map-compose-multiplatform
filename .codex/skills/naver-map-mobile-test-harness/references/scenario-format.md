# Scenario Format

Turn the user's request into this compact shape before delegating to the tester subagent:

```text
Scenario name: <short label>
Platforms: android | ios | both
Goal: <what the user wants to validate>
Preconditions:
- <state or setup>
Steps:
1. <action>
2. <action>
Assertions:
- <expected visible text or behavior>
- <expected navigation, gesture result, or state>
Evidence:
- <required screenshot names or notes>
```

## Normalization rules

- If the user says "both", use Android and iOS.
- If the user gives no platform, default to both.
- If the user gives no evidence request, require at least one screenshot per platform.
- If the scenario is vague, keep it as a smoke scenario and avoid inventing complex flows.
- Prefer visible assertions that a tester can confirm from screen content.

## Example

```text
Scenario name: Launch placeholder smoke
Platforms: both
Goal: Verify the sample app launches and shows the shared placeholder screen.
Preconditions:
- Boot one available Android emulator and one available iOS simulator.
Steps:
1. Install and launch the sample app.
2. Wait for the first stable screen.
Assertions:
- "Compose Multiplatform Sample" is visible.
- "Next step: replace this placeholder" text is visible.
Evidence:
- Save one home screenshot for Android.
- Save one home screenshot for iOS.
```
