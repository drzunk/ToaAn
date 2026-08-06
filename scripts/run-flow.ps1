# =============================================================================
#  Chạy theo: src/test/resources/run-flow.properties
#  Menu: .\scripts\chay.cmd
# =============================================================================

param([switch]$DryRun)

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
try { chcp 65001 | Out-Null } catch {}

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

# Ép JVM in log UTF-8 (tránh lỗi font tiếng Việt trong console)
$utfFlags = '-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -Dconsole.encoding=UTF-8'
if ($env:JAVA_TOOL_OPTIONS) {
    if ($env:JAVA_TOOL_OPTIONS -notmatch 'file\.encoding') {
        $env:JAVA_TOOL_OPTIONS = ($env:JAVA_TOOL_OPTIONS.Trim() + ' ' + $utfFlags).Trim()
    }
} else {
    $env:JAVA_TOOL_OPTIONS = $utfFlags
}
if (-not $env:MAVEN_OPTS) { $env:MAVEN_OPTS = $utfFlags }
elseif ($env:MAVEN_OPTS -notmatch 'file\.encoding') {
    $env:MAVEN_OPTS = ($env:MAVEN_OPTS.Trim() + ' ' + $utfFlags).Trim()
}

$cfgPath = Join-Path $root 'src\test\resources\run-flow.properties'
if (-not (Test-Path $cfgPath)) {
    Write-Host "LỖI: Không thấy file cấu hình: $cfgPath" -ForegroundColor Red
    Write-Host '   Chạy menu: .\scripts\chay.cmd' -ForegroundColor Yellow
    exit 1
}

function Get-PropValue([string]$file, [string]$key, [string]$default) {
    if (-not (Test-Path $file)) { return $default }
    $line = Get-Content $file -Encoding UTF8 |
        Where-Object { $_ -match "^\s*$([regex]::Escape($key))\s*=" } |
        Select-Object -Last 1
    if (-not $line) { return $default }
    $val = ($line -split '=', 2)[1]
    if ($null -eq $val) { return $default }
    $val = ($val -split '#', 2)[0].Trim()
    if ([string]::IsNullOrWhiteSpace($val)) { return $default }
    return $val
}

function Get-RunFlowValue([string]$key, [string]$default) {
    return Get-PropValue $cfgPath $key $default
}

$suite = (Get-RunFlowValue 'run.suite' 'smoke').ToLowerInvariant()
$parallel = (Get-RunFlowValue 'run.parallel' 'false').ToLowerInvariant()
$browsers = Get-RunFlowValue 'run.browsers' '3'
$winW = Get-RunFlowValue 'run.window.width' '520'
$winH = Get-RunFlowValue 'run.window.height' '580'
$winScale = Get-RunFlowValue 'run.window.scale' '0.55'
$requireSubmit = Get-RunFlowValue 'run.requireSubmit' 'false'
$openReport = Get-RunFlowValue 'run.openReport' 'true'
$untilStep = Get-RunFlowValue 'run.untilStep' '6'
$submitDon = Get-RunFlowValue 'run.submit' 'false'
$slots = Get-RunFlowValue 'run.slots' ''
$cases = Get-RunFlowValue 'run.cases' ''
$casesSheet = Get-RunFlowValue 'run.casesSheet' ''
$caseSource = (Get-RunFlowValue 'run.caseSource' 'sheet').ToLowerInvariant()
$useSheet = ($casesSheet -and $caseSource -ne 'file' -and $caseSource -ne 'properties')

if ($useSheet) {
    # Số case nằm trên sheet — Java đọc và tự kẹp run.browsers <= số case.
    $suite = 'master'
    $b = 1
    try { $b = [int]$browsers } catch { $b = 1 }
    if ($b -lt 1) { $b = 1 }
    if ($b -gt 8) { $b = 8 }
    $browsers = "$b"
    $parallel = if ($b -gt 1) { 'true' } else { 'false' }
} elseif ($cases) {
    $caseCount = @($cases -split '\|' | Where-Object { $_.Trim() -ne '' }).Count
    if ($caseCount -ge 1) {
        $suite = 'master'
        # Giữ run.browsers từ file — không ép = số case. Clamp: không mở nhiều Chrome hơn case.
        $b = 1
        try { $b = [int]$browsers } catch { $b = 1 }
        if ($b -lt 1) { $b = 1 }
        if ($b -gt 8) { $b = 8 }
        if ($b -gt $caseCount) { $b = $caseCount }
        $browsers = "$b"
        $parallel = if ($b -gt 1) { 'true' } else { 'false' }
    }
} elseif ($slots) {
    $slotCount = @($slots -split '\|' | Where-Object { $_.Trim() -ne '' }).Count
    if ($slotCount -gt 1) {
        $browsers = "$slotCount"
        $parallel = 'true'
    } elseif ($slotCount -eq 1) {
        $browsers = '1'
        $parallel = 'false'
    }
}

$isParallel = ($parallel -eq 'true' -or $parallel -eq 'yes' -or $parallel -eq '1')
if ($isParallel -and [int]$browsers -le 1) { $isParallel = $false }

# Hiển thị cases: >2 case → STT + mỗi case một dòng (dễ nhìn trên CMD / last-run.log).
function Get-CaseDisplayLines {
    param([string]$CasesRaw, [string]$Prefix = '  cases     : ')
    $list = @($CasesRaw -split '\|' | ForEach-Object { $_.Trim() } | Where-Object { $_ })
    if ($list.Count -eq 0) {
        return @(($Prefix + '(không)'))
    }
    if ($list.Count -le 2) {
        return @(($Prefix + ($list -join '|')))
    }
    $lines = @(($Prefix + "$($list.Count) case"))
    $pad = ' ' * [Math]::Max(0, ($Prefix.Length - 2))
    for ($i = 0; $i -lt $list.Count; $i++) {
        $lines += ("{0}  {1}. {2}" -f $pad, ($i + 1), $list[$i])
    }
    return $lines
}

$profile = switch ($suite) {
    'smoke'  { if ($isParallel) { 'parallel-smoke' } else { 'smoke' } }
    'mid'    { if ($isParallel) { 'parallel-mid' } else { 'mid' } }
    'full'   { if ($isParallel) { 'parallel-full' } else { 'full' } }
    'buoc23' { if ($isParallel) { 'parallel-buoc23' } else { 'full' } }
    'login'  { 'login' }
    'unit'   { 'unit' }
    'master' { 'master' }
    default  {
        Write-Host "LỖI: run.suite='$suite' không hợp lệ." -ForegroundColor Red
        exit 1
    }
}

$extraSuiteXml = $null
if ($suite -eq 'buoc23' -and -not $isParallel) {
    $extraSuiteXml = 'src/test/resources/suites/testng-buoc23.xml'
    $profile = $null
}

Write-Host ''
Write-Host '==============================================================' -ForegroundColor Cyan
Write-Host '             CHẠY THEO run-flow.properties' -ForegroundColor Cyan
Write-Host '==============================================================' -ForegroundColor Cyan
Write-Host ("  Gói       : {0}" -f $suite)
Write-Host ("  Song song : {0}" -f ($(if ($isParallel) { "BẬT ($browsers Chrome)" } else { 'TẮT (1 Chrome)' })))
if ($useSheet) {
    Write-Host '  Nguồn case: Google Sheet' -ForegroundColor Cyan
    Write-Host ("              {0}" -f $casesSheet) -ForegroundColor DarkGray
    Write-Host '              (Java tải sheet lúc chạy — số case in trong log)' -ForegroundColor DarkGray
} elseif ($cases) {
    foreach ($line in (Get-CaseDisplayLines -CasesRaw $cases -Prefix '  cases     : ')) {
        Write-Host $line
    }
} elseif ($slots) {
    Write-Host ("  slots     : {0}" -f $slots)
} else {
    Write-Host ("  untilStep : {0}  |  submit={1}" -f $untilStep, $submitDon)
}
Write-Host ("  Mở báo cáo: {0}" -f $openReport)
if ($profile) { Write-Host ("  Maven -P  : {0}" -f $profile) }
else { Write-Host ("  Suite XML : {0}" -f $extraSuiteXml) }
Write-Host '  Menu      : .\scripts\chay.cmd' -ForegroundColor Cyan
Write-Host '==============================================================' -ForegroundColor Cyan
Write-Host ''

if ($DryRun) {
    Write-Host 'DryRun: chỉ xem cấu hình, không chạy Maven.' -ForegroundColor Yellow
    exit 0
}

. "$PSScriptRoot\lib-maven.ps1"
[void](Ensure-JavaHome)
$mvnCmd = Get-MavenCmd -ConfigFile $cfgPath
if (-not $mvnCmd) {
    Write-MavenNotFound
    exit 1
}

$sysProps = @(
    "-Dtaodon.suite=$suite"
    "-Dtaodon.parallel=$isParallel"
    "-Dtaodon.threads=$browsers"
    "-Dtaodon.requireSubmit=$requireSubmit"
    "-Dtaodon.window.width=$winW"
    "-Dtaodon.window.height=$winH"
    "-Dtaodon.window.scale=$winScale"
    "-Dtaodon.untilStep=$untilStep"
    "-Dtaodon.submit=$submitDon"
    "-Drun.suite=$suite"
    "-Drun.parallel=$isParallel"
    "-Drun.browsers=$browsers"
    "-Drun.window.width=$winW"
    "-Drun.window.height=$winH"
    "-Drun.window.scale=$winScale"
    "-Drun.requireSubmit=$requireSubmit"
    "-Drun.openReport=$openReport"
    "-Drun.untilStep=$untilStep"
    "-Drun.submit=$submitDon"
    "-Dfile.encoding=UTF-8"
    "-Dsun.stdout.encoding=UTF-8"
    "-Dsun.stderr.encoding=UTF-8"
)
if ($slots) {
    $sysProps += "-Drun.slots=$slots"
    $sysProps += "-Dtaodon.slots=$slots"
}
# Knob tinh chỉnh tốc độ, truyền qua biến môi trường để thử mà không phải sửa script/build lại.
# Ví dụ:  $env:TAODON_EXTRA_PROPS = '-Dtaodon.wait.scale=0.6 -Dtaodon.longTextChars=15'
# Danh sách knob: taodon.wait.scale | taodon.probeMs | taodon.pageLoad | taodon.longTextChars
#                 taodon.countCalls | taodon.profile | taodon.screenshot | taodon.submit.timeoutSec
if ($env:TAODON_EXTRA_PROPS) {
    foreach ($p in ($env:TAODON_EXTRA_PROPS -split '\s+')) {
        if ($p) { $sysProps += $p }
    }
    Write-Host ("  Knob thêm : {0}" -f $env:TAODON_EXTRA_PROPS) -ForegroundColor Yellow
}
if ($useSheet) {
    # URL an toàn khi truyền -D (đã nằm trong 1 phần tử mảng, không bị shell tách).
    $sysProps += "-Drun.caseSource=sheet"
    $sysProps += "-Drun.casesSheet=$casesSheet"
    $sheetGid = Get-RunFlowValue 'run.casesSheetGid' ''
    if ($sheetGid) { $sysProps += "-Drun.casesSheetGid=$sheetGid" }
} elseif ($cases) {
    # Không truyền -Drun.cases (ký tự ">" làm vỡ shell). Java đọc từ run-flow.properties.
    $sysProps += "-Drun.caseSource=file"
    Write-Host '  (cases lấy từ run-flow.properties — không truyền -D)' -ForegroundColor DarkGray
}

Write-Host 'Đang khởi động Maven / mở Chrome...' -ForegroundColor Green
Write-Host 'Log chi tiết không in ra CMD — chỉ ghi file (giữ lại từng lượt chạy).' -ForegroundColor DarkGray

$logDir = Join-Path $root 'test-output'
if (-not (Test-Path $logDir)) {
    New-Item -ItemType Directory -Path $logDir -Force | Out-Null
}
# Mỗi lượt một file riêng: bảng phân tích thời gian của UiProfiler chỉ nằm ở stdout, mà bản cũ
# ghi đè last-run.log mỗi lần chạy nên số liệu lượt trước mất sạch — không so sánh trước/sau được.
$runStamp = Get-Date -Format 'yyyyMMdd_HHmmss'
$runLogDir = Join-Path $logDir 'runs'
if (-not (Test-Path $runLogDir)) {
    New-Item -ItemType Directory -Path $runLogDir -Force | Out-Null
}
$logFile = Join-Path $runLogDir ("run_{0}_{1}.log" -f $suite, $runStamp)
$latestLogFile = Join-Path $logDir 'last-run.log'
$header = [System.Collections.Generic.List[string]]::new()
[void]$header.Add("==== ToaAn run log ====")
[void]$header.Add(("Thời điểm : {0}" -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss')))
[void]$header.Add(("Gói       : {0}" -f $suite))
[void]$header.Add(("Song song : {0}" -f ($(if ($isParallel) { "BẬT ($browsers Chrome)" } else { 'TẮT (1 Chrome)' }))))
if ($useSheet) {
    [void]$header.Add(("cases     : Google Sheet -> {0}" -f $casesSheet))
} elseif ($cases) {
    foreach ($line in (Get-CaseDisplayLines -CasesRaw $cases -Prefix 'cases     : ')) {
        [void]$header.Add($line)
    }
} else {
    [void]$header.Add('cases     : (không)')
}
[void]$header.Add(("untilStep : {0} | submit={1}" -f $untilStep, $submitDon))
[void]$header.Add(("Maven -P  : {0}" -f ($(if ($profile) { $profile } else { $extraSuiteXml }))))
[void]$header.Add('================================')
[void]$header.Add('')
$utf8Log = New-Object System.Text.UTF8Encoding $true
[System.IO.File]::WriteAllLines($logFile, $header, $utf8Log)
Copy-Item -LiteralPath $logFile -Destination $latestLogFile -Force
Write-Host ("File log : {0}" -f $logFile) -ForegroundColor Cyan
Write-Host ("           (bản mới nhất cũng ở {0})" -f $latestLogFile) -ForegroundColor DarkGray
Write-Host 'Đang chạy... (chờ Chrome / Maven)' -ForegroundColor Yellow

$prevEap = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
$output = $null
try {
    if ($extraSuiteXml) {
        $output = & $mvnCmd test "-DsuiteXmlFile=$extraSuiteXml" @sysProps 2>&1
    } else {
        $output = & $mvnCmd "-P$profile" test @sysProps 2>&1
    }
} finally {
    $ErrorActionPreference = $prevEap
}
$exitCode = $LASTEXITCODE
if ($null -eq $exitCode) { $exitCode = 0 }

$body = @()
if ($null -ne $output) {
    $body = @($output | ForEach-Object { "$_" })
}
$footer = @(
    ''
    ("==== Kết thúc | exit={0} | {1} ====" -f $exitCode, (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'))
)
[System.IO.File]::WriteAllLines($logFile, ($header + $body + $footer), $utf8Log)
Copy-Item -LiteralPath $logFile -Destination $latestLogFile -Force

$logText = ($body -join "`n")
Write-Host ''
Write-Host '-------------- KẾT QUẢ --------------' -ForegroundColor Cyan
if ($exitCode -eq 0) {
    Write-Host '  Trạng thái : THÀNH CÔNG' -ForegroundColor Green
} else {
    Write-Host '  Trạng thái : THẤT BẠI' -ForegroundColor Red
}
if ($logText -match 'Tests run:\s*(\d+),\s*Failures:\s*(\d+),\s*Errors:\s*(\d+),\s*Skipped:\s*(\d+)') {
    Write-Host ("  Tests      : run={0} | fail={1} | error={2} | skip={3}" -f $Matches[1], $Matches[2], $Matches[3], $Matches[4])
} elseif ($logText -match 'Tests run:\s*(\d+)') {
    Write-Host ("  Tests      : {0}" -f $Matches[0])
}
Write-Host ("  Log đầy đủ : {0}" -f $logFile) -ForegroundColor DarkGray
Write-Host '------------------------------------' -ForegroundColor Cyan

exit $exitCode