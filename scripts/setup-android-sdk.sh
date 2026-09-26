#!/bin/bash
# 클라우드 세션 시작 시 Android SDK가 없으면 설치한다. (dl.google.com 네트워크 허용 필요)
set -euo pipefail
[ "${CLAUDE_CODE_REMOTE:-}" = "true" ] || exit 0

SDK=/opt/android-sdk
PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$(cd "$(dirname "$0")/.." && pwd)}"
echo "sdk.dir=$SDK" > "$PROJECT_DIR/local.properties"
[ -n "${CLAUDE_ENV_FILE:-}" ] && echo "export ANDROID_HOME=$SDK" >> "$CLAUDE_ENV_FILE"

if [ -d "$SDK/platforms/android-36" ] && [ -d "$SDK/build-tools/35.0.0" ]; then
  exit 0
fi

mkdir -p "$SDK/cmdline-tools"
TMP=$(mktemp -d)
curl -sSL -o "$TMP/clt.zip" https://dl.google.com/android/repository/commandlinetools-linux-16111833_latest.zip
unzip -q "$TMP/clt.zip" -d "$TMP"
rm -rf "$SDK/cmdline-tools/latest"
mv "$TMP/cmdline-tools" "$SDK/cmdline-tools/latest"
rm -rf "$TMP"
yes | "$SDK/cmdline-tools/latest/bin/sdkmanager" --licenses > /dev/null 2>&1 || true
"$SDK/cmdline-tools/latest/bin/sdkmanager" "platform-tools" "platforms;android-36" "build-tools;35.0.0" > /dev/null
echo "Android SDK installed at $SDK"
