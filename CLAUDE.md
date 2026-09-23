# sp21-ace

Spanish 21 basic strategy trainer for Android ("Spanish 21 Ace"): set the table rules and the strategy chart adapts, drill hands with instant feedback, choose which hands get dealt, play full games, and keep a stats history. Native Kotlin + Jetpack Compose, built and tested entirely from the CLI.

## Stack

- Kotlin + Jetpack Compose (Material 3), single `app` module
- Android Gradle Plugin 9 with built-in Kotlin: do **not** apply `org.jetbrains.kotlin.android` to the module, only the Compose compiler plugin
- Every version number lives in `gradle/libs.versions.toml` or the Gradle wrapper; keep them on current stable releases
- Tests: JUnit 4 unit tests on the JVM, Compose UI tests on the emulator
- No Android Studio. Tooling (JDK 21, cmdline-tools, gradle, scrcpy) is installed by the machine setup, never by this repo

## Commands

```bash
./gradlew testDebugUnitTest          # JVM unit tests (pre-commit runs them too)
./gradlew assembleDebug              # → app/build/outputs/apk/debug/app-debug.apk
./gradlew lintDebug                  # Android lint → app/build/reports/lint-results-debug.html (pre-commit runs it too)
scripts/emulator.sh                  # this repo's AVD from the installed Play Store image, boot, wait (IMAGE_TAG=google_apis for adb root, HEADLESS=1 for no window)
./gradlew connectedDebugAndroidTest  # Compose UI + activity tests on the running emulator
scripts/run.sh                       # install the debug build and open it (VARIANT=Release: the minified, store-speed build, over the debug one)
scripts/screenshot.sh [name]         # adb screencap → screenshots/<name>.png (gitignored)
scripts/record.sh [name] [seconds]   # adb screenrecord → screenshots/<name>.mp4 (gitignored)
scripts/pr-media.sh <file> <caption>... # upload shots as GitHub attachments, print the PR body's media table
scripts/court-art/generate.sh        # regenerate the court card drawables from Fomin's CC0 SVGs (needs node)
scripts/answer-sounds/generate.sh    # regenerate the trainer's right and wrong answer sounds in res/raw (needs python3 and ffmpeg)
scrcpy                               # mirror the emulator interactively
```

Files headed `Shared script:`, `Shared workflow:` or `Shared config:` are copies of files in a separate tooling checkout. When its `sync-common` is on PATH, every build overwrites them, matched by name. Edit them at the source, never here, and do not name a repo-owned file after a shared one. To adopt another shared file, create it once under the same name and let the build fill it. Build config every app needs goes in the shared `scripts/android-app.gradle`.

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
- The reference app lives on a separate emulator that is signed in to Google Play. Work that does not need the reference app uses this repo's own AVD from `scripts/emulator.sh`. Only one emulator runs at a time.
- Correct basic strategy is the product. Every strategy decision comes from published, cited sources, cross-checked across independent sources for the selected rule set. Unit tests pin every chart cell. Never change a chart cell from intuition.
- Every change after the initial scaffold ships as a PR against `main`, using the PR template. Code changes get an adversarial-review pass and `/simplify` on the branch before handover; docs-only PRs skip those.
- User-visible changes carry screenshots (or a recording) in the PR's "Screenshots / recording" section:
  - Shoot every state the change touches, not one before and one after. Empty and filled, and before and after an answer, are separate states. Light and dark theme are separate states only when the change is about colour or theming; otherwise one theme is enough.
  - Take the before shots on `main` and the after shots on the branch.
  - Publish with `scripts/pr-media.sh` and paste its tags into one `| Before | After |` table per state, under a one-line bold caption, at most about four states. A screen that is new in the PR gets an After column only.
  - Media is uploaded as GitHub attachments, never committed. Shots must never show a signed-in Google account.
- The emulator is the test target. Gradle auto-downloads the platform and build-tools for `compileSdk` on first build; system images come from the machine setup (toggles in `~/.zsh_toggles`); `scripts/emulator.sh` only creates an AVD from the installed Play Store image and names the toggle to set if it is missing. Never run `sdkmanager` installs from this repo.
- Pure logic goes in `domain` with a unit test. UI behaviour gets a Compose test in `androidTest` that renders the composable with fake data. `MainActivityTest` is the one end-to-end smoke test against the real system.
- A passing test is not a passing feature: for UI changes, install on the emulator, screenshot, and look at the PNG before calling it done. That after shot is the one that goes in the PR.
- Pre-commit runs hygiene checks, markdownlint, lint and the unit tests. Install with `pre-commit install`.
- GitHub Actions run on every push: `android-ci` (build, lint, unit tests) and the pre-commit hooks. The emulator tests run locally only.

## Conventions

- Kotlin official code style, 4-space indent (`.editorconfig`). Terse over verbose.
- Comments explain *why*, never *what*. Self-evident code gets no comment.
- Commit messages describe the change and the reason. No `Co-Authored-By` trailers.
- PR template: check or uncheck items, never delete them.

## Don't

- Add Android Studio-only files or workflows (`.idea/`, run configurations).
- Put Android imports in `domain`.
- Change a strategy chart cell without a cited source and a test that pins it.
- Add libraries (DI, navigation, Hilt) before a feature needs them.
