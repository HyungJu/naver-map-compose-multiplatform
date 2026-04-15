#!/bin/bash
set -euo pipefail

platforms="${1:-both}"

case "$platforms" in
  android|ios|both)
    ;;
  *)
    echo "Usage: $0 [android|ios|both]" >&2
    exit 1
    ;;
esac

find_sdk_root() {
  if [ -n "${ANDROID_SDK_ROOT:-}" ] && [ -d "${ANDROID_SDK_ROOT}" ]; then
    printf '%s\n' "${ANDROID_SDK_ROOT}"
    return 0
  fi

  if [ -n "${ANDROID_HOME:-}" ] && [ -d "${ANDROID_HOME}" ]; then
    printf '%s\n' "${ANDROID_HOME}"
    return 0
  fi

  if [ -f "/Users/jude/Code/naver-map-compose-multiplatform/local.properties" ]; then
    local sdk_dir
    sdk_dir="$(grep '^sdk.dir=' /Users/jude/Code/naver-map-compose-multiplatform/local.properties | head -n1 | cut -d'=' -f2- || true)"
    if [ -n "${sdk_dir}" ] && [ -d "${sdk_dir}" ]; then
      printf '%s\n' "${sdk_dir}"
      return 0
    fi
  fi

  if [ -d "${HOME}/Library/Android/sdk" ]; then
    printf '%s\n' "${HOME}/Library/Android/sdk"
    return 0
  fi

  return 1
}

ensure_ios() {
  if ! command -v xcrun >/dev/null 2>&1; then
    echo "iOS bootstrap failed: xcrun was not found on PATH." >&2
    exit 1
  fi

  local booted_udid
  booted_udid="$(
    xcrun simctl list devices available | awk -F '[()]' '
      /iPhone/ && $4 ~ /Booted/ {
        gsub(/^[[:space:]]+|[[:space:]]+$/, "", $2)
        print $2
        exit
      }
    '
  )"

  if [ -z "${booted_udid}" ]; then
    local target_udid
    target_udid="$(
      xcrun simctl list devices available | awk -F '[()]' '
        /iPhone/ && $4 ~ /Shutdown/ {
          gsub(/^[[:space:]]+|[[:space:]]+$/, "", $2)
          print $2
          exit
        }
      '
    )"

    if [ -z "${target_udid}" ]; then
      echo "iOS bootstrap failed: no available iPhone simulator was found." >&2
      exit 1
    fi

    xcrun simctl boot "${target_udid}" >/dev/null
    xcrun simctl bootstatus "${target_udid}" -b
    open -a Simulator --args -CurrentDeviceUDID "${target_udid}" >/dev/null 2>&1 || true
    booted_udid="${target_udid}"
  fi

  echo "iOS simulator ready: ${booted_udid}"
}

wait_for_android_serial() {
  local adb_bin="$1"
  local serial=""

  for _ in $(seq 1 120); do
    serial="$("${adb_bin}" devices | awk 'NR > 1 && $2 == "device" && $1 ~ /^emulator-/ { print $1; exit }')"
    if [ -n "${serial}" ]; then
      printf '%s\n' "${serial}"
      return 0
    fi
    sleep 2
  done

  return 1
}

android_device_present() {
  local adb_bin="$1"
  local serial="$2"
  "${adb_bin}" devices | awk -v target="${serial}" 'NR > 1 && $1 == target && $2 == "device" { found = 1 } END { exit(found ? 0 : 1) }'
}

ensure_android() {
  local sdk_root
  sdk_root="$(find_sdk_root)" || {
    echo "Android bootstrap failed: Android SDK root was not found." >&2
    exit 1
  }

  local adb_bin="${sdk_root}/platform-tools/adb"
  local emulator_bin="${sdk_root}/emulator/emulator"

  if [ ! -x "${adb_bin}" ]; then
    echo "Android bootstrap failed: adb was not found at ${adb_bin}." >&2
    exit 1
  fi

  if [ ! -x "${emulator_bin}" ]; then
    echo "Android bootstrap failed: emulator binary was not found at ${emulator_bin}." >&2
    exit 1
  fi

  local booted_serial
  local emulator_pid=""
  booted_serial="$("${adb_bin}" devices | awk 'NR > 1 && $2 == "device" && $1 ~ /^emulator-/ { print $1; exit }')"

  if [ -z "${booted_serial}" ]; then
    local avd_name
    local log_path="/tmp/naver-map-android-emulator.log"
    avd_name="$("${emulator_bin}" -list-avds | head -n1)"
    if [ -z "${avd_name}" ]; then
      echo "Android bootstrap failed: no AVD was found." >&2
      exit 1
    fi

    emulator_pid="$(
      python3 - "${emulator_bin}" "${avd_name}" "${log_path}" <<'PY'
import subprocess
import sys

emulator_bin, avd_name, log_path = sys.argv[1:]
with open(log_path, "ab", buffering=0) as log_file:
    process = subprocess.Popen(
        [emulator_bin, "-avd", avd_name, "-netdelay", "none", "-netspeed", "full"],
        stdin=subprocess.DEVNULL,
        stdout=log_file,
        stderr=subprocess.STDOUT,
        start_new_session=True,
        close_fds=True,
    )
print(process.pid)
PY
    )"

    booted_serial="$(wait_for_android_serial "${adb_bin}")" || {
      echo "Android bootstrap failed: emulator ${avd_name} did not appear in adb." >&2
      exit 1
    }
  fi

  for _ in $(seq 1 120); do
    if [ "$("${adb_bin}" -s "${booted_serial}" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; then
      if [ -n "${emulator_pid}" ] && ! kill -0 "${emulator_pid}" >/dev/null 2>&1; then
        echo "Android bootstrap failed: emulator process exited after boot." >&2
        exit 1
      fi

      sleep 5

      if ! android_device_present "${adb_bin}" "${booted_serial}"; then
        echo "Android bootstrap failed: emulator ${booted_serial} disappeared from adb after boot." >&2
        exit 1
      fi

      if [ -n "${emulator_pid}" ] && ! kill -0 "${emulator_pid}" >/dev/null 2>&1; then
        echo "Android bootstrap failed: emulator process exited before it became stable." >&2
        exit 1
      fi

      echo "Android emulator ready: ${booted_serial}"
      return 0
    fi
    sleep 2
  done

  echo "Android bootstrap failed: emulator ${booted_serial} did not finish booting." >&2
  exit 1
}

if [ "${platforms}" = "android" ] || [ "${platforms}" = "both" ]; then
  ensure_android
fi

if [ "${platforms}" = "ios" ] || [ "${platforms}" = "both" ]; then
  ensure_ios
fi
