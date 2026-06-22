Param(
    [Parameter(HelpMessage = "Skip assembleDebug and use existing debug APK.")]
    [switch]$NoBuild,

    [Parameter(HelpMessage = "Skip APK install step.")]
    [switch]$NoInstall,

    [Parameter(HelpMessage = "Skip launching MainActivity after install.")]
    [switch]$NoLaunch,

    [Parameter(HelpMessage = "Filter tags for logcat output (defaults to CyxWatch).")]
    [string]$LogTag = "CyxWatch",

    [Parameter(HelpMessage = "Capture this many recent logcat lines. 0 means stream until Ctrl+C.")]
    [int]$LogLineCount = 0,

    [Parameter(HelpMessage = "Optionally target a specific device by serial.")]
    [string]$DeviceId
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
    if ($authorized) { Fail "ADB sees an unauthorized device. Accept the RSA debug prompt on the phone." }
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
} elseif ($connected.Count -gt 1) {
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

if (-not $NoInstall) {
    Write-Host "Installing APK: $apkPath" -ForegroundColor Cyan
    adb @deviceArg install -r $apkPath
    if ($LASTEXITCODE -ne 0) { Fail "APK install failed." }
}

if (-not $NoLaunch) {
    Write-Host "Launching CyxWatch..." -ForegroundColor Cyan
    adb @deviceArg shell am start -n "com.cyxwatch.app/.MainActivity"
}

Write-Host "Starting logcat (tag: $LogTag)." -ForegroundColor Cyan
if ($LogLineCount -gt 0) {
    Write-Host "Capturing last $LogLineCount line(s). Press Ctrl+C to stop early."
    adb @deviceArg logcat -s "$LogTag" -d -t $LogLineCount
} else {
    Write-Host "Streaming logs (Ctrl+C to stop)."
    adb @deviceArg logcat -s "$LogTag"
}
