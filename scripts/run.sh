#!/bin/zsh

# Installs the debug build on the connected device and opens it.

set -e

cd "$(dirname "$0")/.."

APP_ID=com.aquigs.sp21ace

./gradlew installDebug -q

# -W blocks until the activity has launched, so the app is on screen when this returns
adb shell am start -W -n "$APP_ID/.MainActivity" > /dev/null

echo "Spanish 21 Ace is running"
