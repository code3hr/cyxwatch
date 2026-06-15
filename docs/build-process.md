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

## Commands used by docs

- `./gradlew.bat -v` (wrapper and plugin version baseline)
- `./gradlew.bat :app:testDebugUnitTest --tests "com.cyxwatch.app.domain.PrivacyScoreCalculatorTest" --no-daemon -PbuildDir='C:\Users\chick\AppData\Local\Temp\cyxwatch-rootbuild'`
- `./gradlew.bat assembleDebug --no-daemon`
- `./gradlew.bat test --no-daemon`
- `./gradlew.bat lint --no-daemon`
- `./gradlew.bat :app:assembleDebug --no-daemon`

If you hit `.android/debug.keystore.lock` `Access is denied`, run with `ANDROID_SDK_HOME` set to a writable directory so Android signing writes locks and analytics to that location.

Use `-PbuildDir='C:\Users\chick\AppData\Local\Temp\cyxwatch-rootbuild'` whenever you hit repository write/access-denied errors under `D:\Dev\cyxwatch\app\build` or `D:\Dev\cyxwatch\build`.

## Current observed outcomes

Latest run on 2026-06-14:

- `./gradlew.bat -v` succeeds.
- `./gradlew.bat testDebugUnitTest --no-daemon` succeeds when run with normal access to Android build output directories.
- `./gradlew.bat assembleDebug --no-daemon` succeeds when run with normal access to Android build output directories and the Android debug keystore path.
- Earlier failures in this workspace were filesystem permission/lock issues, not application regressions:
  - `D:\Dev\cyxwatch\app\build\...`
  - `D:\Dev\cyxwatch\build\reports\problems\problems-report.html`
  - `C:\Users\chick\.android\debug.keystore.lock`
- `./gradlew.bat :app:testDebugUnitTest --tests "com.cyxwatch.app.domain.PrivacyScoreCalculatorTest" --no-daemon -PbuildDir='C:\Users\chick\AppData\Local\Temp\cyxwatch-rootbuild'` currently passes in this environment.

## CI/CD release flow

- Tagging strategy (first release target): `0.0.1`.
- GitHub Actions workflow: `.github/workflows/ci-cd.yml`.
- On every push to `main` and pull request:
  - `./gradlew test`
  - `./gradlew lintDebug`
  - `./gradlew :app:compileDebugAndroidTestKotlin`
  - `./gradlew assembleDebug`
  - `./gradlew assembleRelease`
  - Upload `app-debug.apk` and `app-release-unsigned.apk` as workflow artifacts.
- On any tag push (for example `0.0.1` or `v0.0.1`):
  - rebuilds release artifact,
  - creates/updates GitHub release,
  - attaches `cyxwatch-<tag>-release-unsigned.apk`.

Note:

- `app-release-unsigned.apk` is intentionally unsigned for GitHub attachment and should not be installed directly.
- For local side-load testing, build the signed debug variant (`assembleDebug`) or sign the release APK with a local keystore.

Example local ad-hoc signing (debug keystore):

```powershell
$env:JAVA_HOME='D:\ProgramFiles 64\jbr'
$env:PATH='C:\Users\chick\AppData\Local\Android\Sdk\build-tools\36.1.0;C:\ProgramFiles 64\bin;' + $env:PATH
$env:ANDROID_HOME='C:\Users\chick\AppData\Local\Android\Sdk'

& "$env:ANDROID_HOME\\build-tools\\36.1.0\\apksigner.bat" sign `
  --ks "$env:USERPROFILE\\.android\\debug.keystore" `
  --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android `
  --out 'D:\Dev\cyxwatch\artifacts\cyxwatch-0.0.1-release-debugsigned.apk' `
  'D:\Dev\cyxwatch\artifacts\cyxwatch-0.0.1-release-unsigned.apk'
```

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
