#!/bin/zsh

# Installs a build on the connected device and opens it: the debug build, or with VARIANT=Release the store-speed one, installed
# through Gradle so its startup profile goes on too.

set -e

cd "$(dirname "$0")/.."

APP_ID=com.aquigs.sp21ace

./gradlew "install${VARIANT:-Debug}" -q

# -W blocks until the activity has launched, so the app is on screen when this returns
adb shell am start -W -n "$APP_ID/.MainActivity" > /dev/null

echo "Spanish 21 Ace is running"
