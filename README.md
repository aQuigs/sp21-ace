# sp21-ace

Spanish 21 Ace, a Spanish 21 basic strategy trainer for Android. The goal: set the table rules and the strategy chart adapts, drill hands with instant feedback, choose which hands get dealt, play full games, and track your stats over time. Native Kotlin + Jetpack Compose, CLI-only workflow.

## Local dev

Requires JDK 21, Gradle, the Android command-line tools with `platform-tools` and `emulator`, and an arm64 `google_apis_playstore` system image. `scripts/emulator.sh` never installs anything and says what is missing.

```bash
./gradlew assembleDebug lintDebug testDebugUnitTest  # build, lint, unit tests
scripts/emulator.sh                                  # boot the emulator (creates the AVD on first run)
./gradlew connectedDebugAndroidTest                  # UI tests on the emulator
scripts/run.sh                                       # install and open the app
```
