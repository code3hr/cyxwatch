# Android Tooling Baseline (For CyxWatch)

## Local environment check (June 13, 2026)

- JDK path used for Android builds:
  - `D:\ProgramFiles 64\jbr`
- Android SDK path used for validation:
  - `C:\Users\chick\AppData\Local\Android\Sdk`
- Reusable Android toolchain references from `D:\Dev\conspiracy\cyxchat\android_build\build_android.bat` were confirmed present:
  - CMake: `C:\Users\chick\AppData\Local\Android\Sdk\cmake\3.22.1\bin`
  - NDK: `C:\Users\chick\AppData\Local\Android\Sdk\ndk\27.0.12077973`
  - Toolchain template: `%NDK%\build\cmake\android.toolchain.cmake`

## Project state in `D:\Dev\cyxwatch`

- Gradle wrapper is now available at repository root:
  - `gradlew`
  - `gradlew.bat`
  - `gradle/wrapper/*`
- Baseline stack versions observed:
  - Android Gradle Plugin: `8.3.1`
  - Kotlin plugin: `1.9.25`
  - Compose compiler: `1.5.15`

## Baseline validation commands

Run from `D:\Dev\cyxwatch` (PowerShell), with:

```powershell
$env:JAVA_HOME='D:\ProgramFiles 64\jbr'
$env:ANDROID_HOME='C:\Users\chick\AppData\Local\Android\Sdk'
$env:GRADLE_USER_HOME='C:\Users\chick\AppData\Local\Temp\cyxwatch-gradle'

./gradlew.bat -v
./gradlew.bat test --no-daemon
./gradlew.bat lint --no-daemon
```

Known environment issues:
- `C:\Users\chick\.android\analytics.settings` may be unreadable/unwritable, causing warnings from Android metrics init.
- `build\reports\problems\problems-report.html` can be access-denied on task shutdown in this environment.
- `C:\Users\chick\.android\debug.keystore.lock` can block signing for `assembleDebug`.
