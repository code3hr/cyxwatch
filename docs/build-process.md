# Android Build Runbook

## Goal

Keep Android build and validation reproducible without rediscovering tool paths or cleanup steps.

## Baseline environment (current session)

- Workspace: `D:\Dev\cyxwatch`
- Gradle wrapper: `D:\Dev\cyxwatch\gradlew.bat` (available and works)
- Local `gradle` executable in `D:\ProgramFiles 64\bin`: not found
- Java: `D:\ProgramFiles 64\jbr`
- SDK: `C:\Users\chick\AppData\Local\Android\Sdk`
- Cache dir: `C:\Users\chick\AppData\Local\Temp\cyxwatch-gradle`
- Alternate SDK home (for `.android` lock issues): `C:\Users\chick\AppData\Local\Temp\cyxwatch-android-home`
- Toolchain references from sibling build project:
  - CMake: `C:\Users\chick\AppData\Local\Android\Sdk\cmake\3.22.1\bin`
  - NDK: `C:\Users\chick\AppData\Local\Android\Sdk\ndk\27.0.12077973`

## Runbook (PowerShell)

```powershell
$env:JAVA_HOME='D:\ProgramFiles 64\jbr'
$env:ANDROID_HOME='C:\Users\chick\AppData\Local\Android\Sdk'
$env:GRADLE_USER_HOME='C:\Users\chick\AppData\Local\Temp\cyxwatch-gradle'
$env:ANDROID_SDK_HOME='C:\Users\chick\AppData\Local\Temp\cyxwatch-android-home'
$env:PATH="D:\\ProgramFiles 64\\bin;$env:PATH"

Set-Location D:\Dev\cyxwatch
./gradlew.bat :app:testDebugUnitTest --tests "com.cyxwatch.app.domain.PrivacyScoreCalculatorTest" --no-daemon -PbuildDir='C:\Users\chick\AppData\Local\Temp\cyxwatch-rootbuild'

# Optional full validation in a temp build directory (avoids repo ACL lock failures):
./gradlew.bat -v
./gradlew.bat assembleDebug --no-daemon
./gradlew.bat test --no-daemon
./gradlew.bat lint --no-daemon
```

Preferred one-command local CI-equivalent run:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\build-cyxwatch.ps1 -Mode ci
```

### Artifact behavior for helper script

`scripts\build-cyxwatch.ps1` uses a writable temp `-PbuildDir` and then syncs outputs to the standard project paths:

- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release.apk` (signed)
- `app/build/outputs/apk/release/app-release-unsigned.apk` (unsigned fallback)

If a synced APK is 0 bytes, use the temp build artifact from:

- Debug: `C:\Users\chick\AppData\Local\Temp\cyxwatch-rootbuild\outputs\apk\debug\app-debug.apk`
- Release: `C:\Users\chick\AppData\Local\Temp\cyxwatch-rootbuild\outputs\apk\release\app-release.apk` or `app-release-unsigned.apk`.

## Commands used by docs

- `./gradlew.bat -v` (wrapper and plugin version baseline)
- `./gradlew.bat :app:testDebugUnitTest --tests "com.cyxwatch.app.domain.PrivacyScoreCalculatorTest" --no-daemon -PbuildDir='C:\Users\chick\AppData\Local\Temp\cyxwatch-rootbuild'`
- `./gradlew.bat assembleDebug --no-daemon`
- `./gradlew.bat test --no-daemon`
- `./gradlew.bat lint --no-daemon`
- `./gradlew.bat :app:assembleDebug --no-daemon`

If you hit `.android/debug.keystore.lock` `Access is denied`, run with `ANDROID_SDK_HOME` set to a writable directory so Android signing writes locks and analytics to that location.

Use `-PbuildDir='C:\Users\chick\AppData\Local\Temp\cyxwatch-rootbuild'` whenever you hit repository write/access-denied errors under `D:\Dev\cyxwatch\app\build` or `D:\Dev\cyxwatch\build`.

If you use a custom `-PbuildDir`, build outputs live in that temp directory first and `app/build/...` may remain stale. Use `scripts\build-cyxwatch.ps1 -Mode ci|release|debug` to synchronize canonical APK files back into `app/build/outputs/...`.

## Release APK installability

- `app-release-unsigned.apk` is only produced when no signing key is available.
- If release signing credentials are configured, `assembleRelease` outputs a signed APK that is installable.
- If no release credentials exist, Gradle now reuses the Android debug signing config (`signingConfigs.debug`) for release fallback, so debug and release use the same local certificate.
- If debug keystore is missing, create one with Android tooling (`keytool`) or provide explicit `CYXWATCH_RELEASE_*` credentials before release.

Release signing inputs (optional):

- `CYXWATCH_RELEASE_KEYSTORE_PATH` / `CYXWATCH_KEYSTORE_PATH` (or `cyxwatch.release.keystore.path`)
- `CYXWATCH_RELEASE_KEYSTORE_PASSWORD` / `CYXWATCH_KEYSTORE_PASSWORD` (or `cyxwatch.release.keystore.password`)
- `CYXWATCH_RELEASE_KEY_ALIAS` / `CYXWATCH_KEY_ALIAS` (or `cyxwatch.release.keystore.alias`)
- `CYXWATCH_RELEASE_KEY_PASSWORD` / `CYXWATCH_KEY_PASSWORD` (or `cyxwatch.release.key.password`)

Example (PowerShell, debug keystore):
```powershell
$env:JAVA_HOME='D:\ProgramFiles 64\jbr'
$out='D:\Dev\cyxwatch\artifacts\cyxwatch-0.0.4-release-debugsigned.apk'
$input='C:\Users\chick\AppData\Local\Temp\cyxwatch-release-build\outputs\apk\release\app-release.apk'

./gradlew.bat assembleRelease --no-daemon -PbuildDir='C:\Users\chick\AppData\Local\Temp\cyxwatch-release-build'

& 'C:\Users\chick\AppData\Local\Android\Sdk\build-tools\36.1.0\apksigner.bat' sign `
  --ks "$env:USERPROFILE\.android\debug.keystore" `
  --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android `
  --out $out $input
```

If `adb` is in PATH and you are upgrading from an installed debug build, uninstall first only if you need a different certificate.

If `adb` is in PATH:
```powershell
adb install -r -d -g D:\Dev\cyxwatch\artifacts\cyxwatch-0.0.4-release-debugsigned.apk
```

## Current observed outcomes

Latest run on 2026-06-19:

- `./gradlew.bat -v` succeeds.
- `./gradlew.bat testDebugUnitTest --no-daemon` succeeds when run with local Gradle/SDK cache variables.
- `./gradlew.bat assembleDebug --no-daemon` succeeds when using writable Gradle and Android SDK home overrides.
- `./gradlew.bat assembleRelease --no-daemon` succeeds when the build cache is clean and keytool keystore locations are writable.
- Earlier failures in this workspace were filesystem permission/lock issues, not application regressions:
  - `D:\Dev\cyxwatch\app\build\...`
  - `D:\Dev\cyxwatch\build\reports\problems\problems-report.html`
  - `C:\Users\chick\.android\debug.keystore.lock`
- `./gradlew.bat :app:testDebugUnitTest --tests "com.cyxwatch.app.domain.PrivacyScoreCalculatorTest" --no-daemon -PbuildDir='C:\Users\chick\AppData\Local\Temp\cyxwatch-rootbuild'` currently passes in this environment.

Recovery command that clears lock-prone state:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\build-cyxwatch.ps1 -Mode ci -StopJava -CleanBuildState
```

## CI/CD release flow

- Current tag target: `0.0.4`.
- GitHub Actions workflow: `.github/workflows/ci-cd.yml`.
- On every push to `main` and pull request:
  - `./gradlew test`
  - `./gradlew lintDebug`
  - `./gradlew :app:compileDebugAndroidTestKotlin`
  - `./gradlew assembleDebug`
  - `./gradlew assembleRelease`
  - Upload `app-debug.apk` and the release output (signed if configured, otherwise unsigned) as workflow artifacts.
  - If release signing is available on CI, `assembleRelease` produces signed `app-release.apk`.
- On any tag push (for example `0.0.4` or `v0.0.4`) **or manual workflow dispatch with `run_release=true`**:
  - rebuilds release artifact,
  - creates/updates GitHub release,
  - uploads `cyxwatch-<tag>-release.apk` when signed, otherwise `cyxwatch-<tag>-release-unsigned.apk`.

## Cleanup sequence

Use this after repeated lock failures:

```powershell
Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force -ErrorAction SilentlyContinue 'C:\Users\chick\AppData\Local\Temp\cyxwatch-rootbuild'
Remove-Item -Recurse -Force -ErrorAction SilentlyContinue 'C:\Users\chick\AppData\Local\Temp\cyxwatch-gradle'
New-Item -ItemType Directory -Force -Path 'C:\Users\chick\AppData\Local\Temp\cyxwatch-rootbuild' | Out-Null
New-Item -ItemType Directory -Force -Path 'C:\Users\chick\AppData\Local\Temp\cyxwatch-gradle' | Out-Null
```

If `.android` is still locked, rerun from a user profile with full write access to `C:\Users\chick\.android`.

## Troubleshooting map

- `SDK processing. This version only understands SDK XML...`
  - warning only unless build fails; typically safe to continue.
- `JAVA_HOME is not set`
  - set `JAVA_HOME` and rerun.
- `SDK location not found`
  - set `ANDROID_HOME` (or `local.properties` with `sdk.dir=...`).
- `lint` or runtime checks report usage-access API level problems
  - keep `UsageAccessManager` API-guarded paths for pre-`Q`.

## Why builds looked "stuck" and how to avoid it

The common stall/long-wait pattern is usually environment setup, not code:

- `JAVA_HOME` is missing (most direct cause when invoking `gradlew.bat` without the helper script).
- Stale Gradle wrapper lock or temp cache under a restricted path.
- Antivirus/OS lock contention on `.gradle` or `.android` state files (`.lck`, `debug.keystore.lock`).

Run this sequence when stuck is observed:

1. Verify Java/toolchain env values are set (same as section above).
2. Restart helper script with a fresh temp build dir (`-BuildDir` in the temp location).
3. Kill stale Java/Gradle processes if needed.

Recent failures were not app regressions. They were environment/toolchain setup failures:

- `org.gradle.wrapper.GradleWrapperMain` repeatedly opening/removing
  `...\\.gradle\\wrapper\\dists\\gradle-8.14-all\\...\\.lck`:
  - a stale wrapper lock from prior interrupted runs can hold a partial lock state.
  - if that cache directory is under the repo with restricted ACLs, wrapper retries can look like a hang.
- `Could not initialize native services: Failed to load native-platform.dll`:
  - usually a partial Gradle cache/bootstrap state or unsupported temp environment after a partial download/unpack.
- `gradlew.bat` seeming to pause forever before network activity:
  - lock contention or blocked antivirus scan on `%USERPROFILE%\.gradle` directories can delay wrapper bootstrap by minutes.

Keep this command order for every local run:

1. Clean prior wrapper/build state from writable temp folders only:
   - `C:\Users\chick\AppData\Local\Temp\cyxwatch-gradle`
   - `C:\Users\chick\AppData\Local\Temp\cyxwatch-rootbuild`
2. Set full toolchain + cache variables before any gradle task.
3. Use `-PbuildDir='C:\Users\chick\AppData\Local\Temp\cyxwatch-rootbuild'` on first attempts.
4. Use `--no-daemon` for Android tasks when a filesystem ACL issue is suspected.

Recommended startup block:

```powershell
$env:JAVA_HOME='D:\ProgramFiles 64\jbr'
$env:ANDROID_HOME='C:\Users\chick\AppData\Local\Android\Sdk'
$env:ANDROID_SDK_HOME='C:\Users\chick\AppData\Local\Temp\cyxwatch-android-home'
$env:GRADLE_USER_HOME='C:\Users\chick\AppData\Local\Temp\cyxwatch-gradle'

New-Item -ItemType Directory -Path $env:GRADLE_USER_HOME,$env:ANDROID_SDK_HOME,'C:\Users\chick\AppData\Local\Temp\cyxwatch-rootbuild' -Force | Out-Null

./gradlew.bat -v
./gradlew.bat assembleDebug --no-daemon -PbuildDir='C:\Users\chick\AppData\Local\Temp\cyxwatch-rootbuild'
```

If locks keep coming back, stop stale Java/Gradle processes first:

```powershell
Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force
```

Then rerun the same command sequence.
