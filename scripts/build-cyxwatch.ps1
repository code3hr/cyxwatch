param(
    [ValidateSet('debug', 'release', 'test', 'lint', 'ci')]
    [string]$Mode = 'debug',
    [switch]$NoDaemon = $true,
    [switch]$CleanBuildState,
    [switch]$StopJava,
    [string]$BuildDir = 'C:\Users\chick\AppData\Local\Temp\cyxwatch-rootbuild'
)

$ErrorActionPreference = 'Stop'

if ($StopJava) {
    Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
}

$env:JAVA_HOME = 'D:\ProgramFiles 64\jbr'
$env:ANDROID_HOME = 'C:\Users\chick\AppData\Local\Android\Sdk'
$env:GRADLE_USER_HOME = 'C:\Users\chick\AppData\Local\Temp\cyxwatch-gradle'
$env:ANDROID_SDK_HOME = 'C:\Users\chick\AppData\Local\Temp\cyxwatch-android-home'

$cacheRoot = Join-Path $env:GRADLE_USER_HOME ''
$androidHome = $env:ANDROID_SDK_HOME
if ($CleanBuildState) {
    Write-Host "Cleaning local Gradle and SDK cache state for cyxwatch"
    @(
        $env:GRADLE_USER_HOME,
        $BuildDir,
        $env:ANDROID_SDK_HOME
    ) | ForEach-Object {
        if (Test-Path $_) {
            try {
                Remove-Item -Recurse -Force $_ -ErrorAction Stop
            } catch {
                Write-Warning "Unable to remove $_ : $($_.Exception.Message)"
            }
        }
    }
}

New-Item -ItemType Directory -Path $env:GRADLE_USER_HOME, $BuildDir, $androidHome -Force | Out-Null

$baseArgs = @(
    '--no-daemon',
    "-PbuildDir=$BuildDir"
)
if (-not $NoDaemon) {
    $baseArgs = $baseArgs | Where-Object { $_ -ne '--no-daemon' }
}

$projectRoot = Resolve-Path (Join-Path $PSScriptRoot '..')
$buildDirDebugApk = Join-Path $BuildDir 'outputs\apk\debug\app-debug.apk'
$buildDirReleaseApk = Join-Path $BuildDir 'outputs\apk\release\app-release.apk'
$buildDirUnsignedReleaseApk = Join-Path $BuildDir 'outputs\apk\release\app-release-unsigned.apk'

$canonicalDebugApk = Join-Path $projectRoot 'app\build\outputs\apk\debug\app-debug.apk'
$canonicalReleaseApk = Join-Path $projectRoot 'app\build\outputs\apk\release\app-release.apk'
$canonicalUnsignedReleaseApk = Join-Path $projectRoot 'app\build\outputs\apk\release\app-release-unsigned.apk'

function Copy-ApkArtifact {
    param(
        [string]$Source,
        [string]$Destination
    )

    if (Test-Path $Source) {
        New-Item -ItemType Directory -Path (Split-Path $Destination -Parent) -Force | Out-Null
        Copy-Item -Path $Source -Destination $Destination -Force
    }
}

function Write-ApkDiagnostics {
    param([string]$Path)
    if (-not (Test-Path $Path)) {
        return
    }
    $file = Get-Item $Path
    Write-Host "APK: $($file.FullName)"
    Write-Host "Size: $($file.Length) bytes"
    Write-Host "Updated: $($file.LastWriteTime)"
}

function Sync-DebugApkArtifact {
    Copy-ApkArtifact -Source $buildDirDebugApk -Destination $canonicalDebugApk
    if (Test-Path $canonicalDebugApk) {
        Write-ApkDiagnostics -Path $canonicalDebugApk
    } elseif (Test-Path $buildDirDebugApk) {
        Write-ApkDiagnostics -Path $buildDirDebugApk
        Write-Warning "Could not sync debug APK into app/build outputs path."
    } else {
        Write-Warning "No debug APK found in temp buildDir."
    }
}

function Sync-ReleaseApkArtifact {
    if (Test-Path $buildDirReleaseApk) {
        Copy-ApkArtifact -Source $buildDirReleaseApk -Destination $canonicalReleaseApk
        if (Test-Path $canonicalUnsignedReleaseApk) {
            try {
                Remove-Item -Force $canonicalUnsignedReleaseApk -ErrorAction Stop
            } catch {
                Write-Warning "Could not remove stale unsigned release artifact: $canonicalUnsignedReleaseApk"
            }
        }
        Write-Host "Release artifact signed from: $buildDirReleaseApk"
        Write-ApkDiagnostics -Path $canonicalReleaseApk
        return
    }

    if (Test-Path $buildDirUnsignedReleaseApk) {
        Copy-ApkArtifact -Source $buildDirUnsignedReleaseApk -Destination $canonicalUnsignedReleaseApk
        if (Test-Path $canonicalReleaseApk) {
            try {
                Remove-Item -Force $canonicalReleaseApk -ErrorAction Stop
            } catch {
                Write-Warning "Could not remove stale signed release artifact: $canonicalReleaseApk"
            }
        }
        Write-Host "Release artifact unsigned from: $buildDirUnsignedReleaseApk"
        Write-ApkDiagnostics -Path $canonicalUnsignedReleaseApk
        return
    }

    Write-Warning "No release APK found in temp buildDir."
}

Push-Location $PSScriptRoot\..

switch ($Mode) {
    'debug' {
        & .\gradlew.bat assembleDebug @baseArgs
        Sync-DebugApkArtifact
    }
    'release' {
        & .\gradlew.bat assembleRelease @baseArgs
        Sync-ReleaseApkArtifact
    }
    'test' {
        & .\gradlew.bat test @baseArgs
    }
    'lint' {
        & .\gradlew.bat lintDebug @baseArgs
    }
    'ci' {
        & .\gradlew.bat test @baseArgs
        & .\gradlew.bat lintDebug @baseArgs
        & .\gradlew.bat :app:compileDebugAndroidTestKotlin @baseArgs
        & .\gradlew.bat assembleDebug @baseArgs
        & .\gradlew.bat assembleRelease @baseArgs
        Sync-DebugApkArtifact
        Sync-ReleaseApkArtifact
    }
}

Pop-Location
