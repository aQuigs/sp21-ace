# sp21-ace

Spanish 21 basic strategy trainer for Android ("Spanish 21 Ace"): set the table rules and the strategy chart adapts, drill hands with instant feedback, choose which hands get dealt, play full games, and keep a stats history. Native Kotlin + Jetpack Compose, built and tested entirely from the CLI.

## Stack

- Kotlin + Jetpack Compose (Material 3), single `app` module
- Every version number lives in `gradle/libs.versions.toml` or the Gradle wrapper; keep them on current stable releases
- Tests: JUnit 4 unit tests on the JVM, Compose UI tests on the emulator

## Commands

```bash
./gradlew testDebugUnitTest          # JVM unit tests (pre-commit runs them too)
./gradlew assembleDebug              # → app/build/outputs/apk/debug/app-debug.apk
./gradlew lintDebug                  # Android lint → app/build/reports/lint-results-debug.html (pre-commit runs it too)
scripts/emulator-lock.sh <command>   # device work (the commands below that touch the emulator): boots the emulator under the shared lock, runs <command>, stops it
scripts/emulator-lock.sh ./gradlew connectedDebugAndroidTest  # Compose UI + activity tests on the emulator
scripts/emulator.sh [stop]           # boot (or stop) this repo's AVD from the installed Play Store image by hand (IMAGE_TAG=google_apis for adb root, WINDOW=1 to show the emulator window, headless otherwise)
scripts/run.sh                       # install the debug build and open it (VARIANT=Release: the minified, store-speed build, over the debug one)
scripts/screenshot.sh [name]         # adb screencap → screenshots/<name>.png (gitignored)
scripts/record.sh [name] [seconds]   # adb screenrecord → screenshots/<name>.mp4 (gitignored)
scripts/pr-media.sh <file> <caption>... # upload shots as GitHub attachments, print the PR body's media table
scripts/court-art/generate.sh        # regenerate the court card drawables from Fomin's CC0 SVGs (needs node)
scripts/answer-sounds/generate.sh    # regenerate the trainer's right and wrong answer sounds in res/raw (needs python3 and ffmpeg)
scrcpy                               # mirror the emulator interactively
```

## Layout

```text
app/src/main/kotlin/com/aquigs/sp21ace/
├── MainActivity.kt      # composition root: wires domain state into the UI
├── data/                # storage adapters (SharedPreferences, files)
├── domain/              # pure Kotlin: cards, rules, strategy, practice history; no Android imports
└── ui/                  # Compose: screens, components, theme
app/src/test/            # JVM unit tests (domain, and data's line formats)
app/src/androidTest/     # Compose UI tests and the activity smoke test (emulator)
scripts/                 # emulator, run, screenshot helpers (zsh), and generators for the court card art and the app's sounds
```

Dependencies flow down only: `ui → domain` and `data → domain`, and `MainActivity` is the only place that wires them together, so screens never read storage. `domain` never imports `android.*`, so every rule and strategy decision is testable on the JVM.

## How we work

- Blackjack Ace (`com.blackjack_ace.blackjackace`) is the behaviour reference. Where it has a feature, mimic how it behaves and how it is laid out, in our own colours, for Spanish 21. Unsure how it does something? Open it on the emulator that has it installed and look, do not guess. Where it has no such feature, use your judgement or ask.
- The reference app lives on a separate emulator that is signed in to Google Play. Work that does not need the reference app uses this repo's own AVD through `scripts/emulator-lock.sh`. Only one emulator runs at a time.
- Correct basic strategy is the product. Every strategy decision comes from published, cited sources, cross-checked across independent sources for the selected rule set. Unit tests pin every chart cell. Never change a chart cell from intuition.
- A PR that only refreshes shared files through `sync-common` is done when merged, not when opened: Claude merges it once its checks pass, even if the task only says to open it. Any other change in it leaves the merge to the user.
- Pure logic goes in `domain` with a unit test. `MainActivityTest` is the one end-to-end smoke test against the real system.
- For the UI check, build first, then install and screenshot in one lock: `scripts/emulator-lock.sh zsh -c 'scripts/run.sh && scripts/screenshot.sh <name>'`. That after shot is the one that goes in the PR.

## Don't

- Put Android imports in `domain`.
- Change a strategy chart cell without a cited source and a test that pins it.
