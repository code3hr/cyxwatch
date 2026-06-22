param(
    [Parameter(HelpMessage = "Skip assembleDebug and reuse existing debug APK.")]
    [switch]$NoBuild,

    [Parameter(HelpMessage = "Skip APK install step.")]
    [switch]$NoInstall,

    [Parameter(HelpMessage = "Skip launch step.")]
    [switch]$NoLaunch,

    [Parameter(HelpMessage = "Optionally target a specific device by serial.")]
    [string]$DeviceId,

    [Parameter(HelpMessage = "How many startup log lines to capture after launch.")]
    [int]$LogTailLines = 200,

    [Parameter(HelpMessage = "Seconds to wait after launch before checking logs.")]
    [int]$PostLaunchWaitSeconds = 5
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Fail([string]$message) {
    Write-Host $message -ForegroundColor Red
    exit 1
}

function Resolve-Tool([string]$name) {
    if (-not (Get-Command $name -ErrorAction SilentlyContinue)) {
        Fail "Required tool '$name' was not found in PATH."
    }
}

Resolve-Tool "adb"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = (Get-Item $projectRoot).Parent.FullName
Set-Location $projectRoot

Write-Host "Checking connected device..." -ForegroundColor Cyan
$adbDevices = adb devices
$connected = @(
    $adbDevices | Select-String "^\s*([^\s]+)\s+device$" | ForEach-Object { $_.Matches[0].Groups[1].Value }
)
$authorized = @(
    $adbDevices | Select-String "^\s*([^\s]+)\s+unauthorized$" | ForEach-Object { $_.Matches[0].Groups[1].Value }
)
$offline = @(
    $adbDevices | Select-String "^\s*([^\s]+)\s+offline$" | ForEach-Object { $_.Matches[0].Groups[1].Value }
)

if (-not $connected) {
    if ($authorized) { Fail "ADB sees unauthorized device(s): $($authorized -join ', '). Accept the RSA debug prompt on the phone." }
    if ($offline) { Fail "ADB sees offline device(s): $($offline -join ', '). Unplug/replug USB and retry." }
    Fail "No connected Android devices found."
}

Write-Host "Connected device(s): $($connected -join ', ')" -ForegroundColor Green

$activeDevice = $connected | Select-Object -First 1
if ($DeviceId) {
    if ($connected -notcontains $DeviceId) {
        Fail "Device '$DeviceId' is not connected. Pick one of: $($connected -join ', ')"
    }
    $activeDevice = $DeviceId
}
elseif ($connected.Count -gt 1) {
    Fail "Multiple devices connected. Re-run with -DeviceId <serial>."
}

$deviceArg = @("-s", $activeDevice)

if (-not $NoBuild) {
    Write-Host "Building debug APK..." -ForegroundColor Cyan
    & .\gradlew.bat :app:assembleDebug
    if ($LASTEXITCODE -ne 0) { Fail "assembleDebug failed." }
}

$apkPath = Join-Path $projectRoot "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $apkPath)) {
    Fail "Debug APK not found at: $apkPath. Build first or pass -NoBuild only if APK exists."
}

Write-Host "Checking whether package exists on device..." -ForegroundColor Cyan
$pathCheck = adb @deviceArg shell pm path com.cyxwatch.app
if ($pathCheck -and -not $NoInstall) {
    Write-Host "Existing app found on device." -ForegroundColor Green
}

if (-not $NoInstall) {
    Write-Host "Installing APK: $apkPath" -ForegroundColor Cyan
    adb @deviceArg install -r $apkPath
    if ($LASTEXITCODE -ne 0) { Fail "APK install failed." }
}

if (-not $NoLaunch) {
    Write-Host "Launching com.cyxwatch.app/.MainActivity..." -ForegroundColor Cyan
    adb @deviceArg shell am start -W -n "com.cyxwatch.app/.MainActivity"
    if ($LASTEXITCODE -ne 0) { Fail "App launch command failed." }
}

Start-Sleep -Seconds $PostLaunchWaitSeconds

Write-Host "Checking app process..." -ForegroundColor Cyan
$appPid = (adb @deviceArg shell pidof com.cyxwatch.app).Trim()
if ([string]::IsNullOrWhiteSpace($appPid)) {
    Fail "Could not find running process for com.cyxwatch.app."
}
Write-Host "App PID: $appPid" -ForegroundColor Green

Write-Host "Current focus window (should include com.cyxwatch.app)..." -ForegroundColor Cyan
$focus = adb @deviceArg shell dumpsys window windows
if ($focus -match "com\.cyxwatch\.app") {
    Write-Host "Focus check passed." -ForegroundColor Green
} else {
    Write-Host "Focus check did not find com.cyxwatch.app. It may be running in background." -ForegroundColor Yellow
}

Write-Host "Collecting startup logs..." -ForegroundColor Cyan
$logLines = adb @deviceArg logcat -d -v time
$filtered = $logLines | Select-String -Pattern 'AndroidRuntime|FATAL EXCEPTION|Process com\.cyxwatch\.app|com\.cyxwatch\.app|cyxwatch|runtime' | Select-Object -Last $LogTailLines

if ($filtered) {
    Write-Host "---- App-Related Log Lines (tail $LogTailLines) ----" -ForegroundColor Yellow
    $filtered
} else {
    Write-Host "No app-tagged lines matched in the selected log window." -ForegroundColor Yellow
}

if ($filtered | Select-String -Pattern "FATAL EXCEPTION|ANR in com\.cyxwatch\.app|Process: com\.cyxwatch\.app|Caused by:") {
    Fail "Crash signature detected in startup logs."
}

Write-Host "Smoke test passed: com.cyxwatch.app started and is running." -ForegroundColor Green
