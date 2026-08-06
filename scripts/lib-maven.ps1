# =============================================================================
#  Dò mvn + JDK dùng chung cho run-flow.ps1 và chay.ps1.
#  Không hardcode phiên bản IDE: bản cũ bị gỡ sau mỗi lần IntelliJ tự cập nhật.
#  Dot-source: . "$PSScriptRoot\lib-maven.ps1"
# =============================================================================

function Get-MavenSearchPaths {
    $goc = @()
    $pf = if ($env:ProgramFiles) { $env:ProgramFiles } else { 'C:\Program Files' }
    $goc += (Join-Path $pf 'JetBrains')
    if ($env:LOCALAPPDATA) {
        $goc += (Join-Path $env:LOCALAPPDATA 'JetBrains\Toolbox\apps')
        $goc += (Join-Path $env:LOCALAPPDATA 'Programs')
    }
    return $goc
}

# Đường dẫn mvn dùng được, hoặc $null. Thứ tự: khóa run.mavenCmd → MAVEN_HOME/M2_HOME → PATH →
# Maven đi kèm IDE (lấy bản tên lớn nhất).
function Get-MavenCmd {
    param([string]$ConfigFile)

    if ($ConfigFile -and (Test-Path $ConfigFile)) {
        $line = Get-Content $ConfigFile -Encoding UTF8 |
            Where-Object { $_ -match '^\s*run\.mavenCmd\s*=' } |
            Select-Object -Last 1
        if ($line) {
            $khaiBao = ($line -split '=', 2)[1].Trim()
            if ($khaiBao -and (Test-Path $khaiBao)) { return $khaiBao }
        }
    }

    foreach ($bien in @($env:MAVEN_HOME, $env:M2_HOME)) {
        if ($bien) {
            foreach ($ten in @('mvn.cmd', 'mvn.bat')) {
                $ungVien = Join-Path $bien "bin\$ten"
                if (Test-Path $ungVien) { return $ungVien }
            }
        }
    }

    $tuPath = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if (-not $tuPath) { $tuPath = Get-Command mvn -ErrorAction SilentlyContinue }
    if ($tuPath) { return $tuPath.Source }

    foreach ($goc in (Get-MavenSearchPaths)) {
        if (-not (Test-Path $goc)) { continue }
        $found = Get-ChildItem $goc -Directory -ErrorAction SilentlyContinue |
            Sort-Object Name -Descending |
            ForEach-Object { Join-Path $_.FullName 'plugins\maven\lib\maven3\bin\mvn.cmd' } |
            Where-Object { Test-Path $_ } |
            Select-Object -First 1
        if ($found) { return $found }
    }
    return $null
}

# Đặt $env:JAVA_HOME nếu chưa có JDK hợp lệ. Trả về đường dẫn đang dùng, hoặc $null.
function Ensure-JavaHome {
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
        return $env:JAVA_HOME
    }
    $ungVien = @()
    $jdks = Join-Path $env:USERPROFILE '.jdks'
    if (Test-Path $jdks) {
        $ungVien += (Get-ChildItem $jdks -Directory -ErrorAction SilentlyContinue |
            Sort-Object Name -Descending | ForEach-Object { $_.FullName })
    }
    foreach ($goc in (Get-MavenSearchPaths)) {
        if (-not (Test-Path $goc)) { continue }
        $ungVien += (Get-ChildItem $goc -Directory -ErrorAction SilentlyContinue |
            Sort-Object Name -Descending | ForEach-Object { Join-Path $_.FullName 'jbr' })
    }
    foreach ($jdk in $ungVien) {
        if (Test-Path (Join-Path $jdk 'bin\java.exe')) {
            $env:JAVA_HOME = $jdk
            return $jdk
        }
    }
    return $null
}

function Write-MavenNotFound {
    Write-Host 'LỖI: Không tìm thấy mvn.' -ForegroundColor Red
    Write-Host '  Đã tìm ở: khóa run.mavenCmd trong src\test\resources\run-flow.properties;' -ForegroundColor DarkGray
    Write-Host '            MAVEN_HOME / M2_HOME; PATH;' -ForegroundColor DarkGray
    foreach ($goc in (Get-MavenSearchPaths)) {
        Write-Host ("            {0}\*\plugins\maven\lib\maven3" -f $goc) -ForegroundColor DarkGray
    }
    Write-Host '  Cách sửa: cài Maven, hoặc thêm dòng run.mavenCmd=<đường dẫn mvn.cmd>' -ForegroundColor Yellow
    Write-Host '            vào src\test\resources\run-flow.properties' -ForegroundColor Yellow
}
