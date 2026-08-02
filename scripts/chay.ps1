# =============================================================================
#  Menu chạy test — ↑↓ chọn, Enter xác nhận (vẽ lại / cuộn, nhãn đầy đủ)
#  Cách dùng: .\scripts\chay.cmd
# =============================================================================

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
try { chcp 65001 | Out-Null } catch {}

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$cfgPath = Join-Path $root 'src\test\resources\run-flow.properties'
$masterPath = Join-Path $root 'src\main\resources\master-data.properties'
$runFlow = Join-Path $PSScriptRoot 'run-flow.ps1'

function Test-HasRealConsole {
    try {
        if ([Console]::IsOutputRedirected) { return $false }
        if ([Console]::IsInputRedirected) { return $false }
        $null = [Console]::CursorTop
        $null = $Host.UI.RawUI.BufferSize
        $null = $Host.UI.RawUI.KeyAvailable
        return $true
    } catch {
        return $false
    }
}

if ($env:TOAAN_MENU_CONSOLE -ne '1' -and -not (Test-HasRealConsole)) {
    Write-Host ''
    Write-Host 'Terminal hiện tại không hỗ trợ menu ↑↓.' -ForegroundColor Yellow
    Write-Host 'Đang mở cửa sổ CMD mới (dùng ↑↓ / Enter)...' -ForegroundColor Yellow
    $ps1 = $PSCommandPath
    $arg = "chcp 65001>nul & cd /d `"$root`" & set TOAAN_MENU_CONSOLE=1 & title ToaAn - Menu chay test & powershell.exe -NoProfile -ExecutionPolicy Bypass -File `"$ps1`""
    Start-Process -FilePath "$env:SystemRoot\System32\cmd.exe" -ArgumentList @('/k', $arg) -WorkingDirectory $root
    exit 0
}

function Set-Prop([string]$key, [string]$value) {
    if (-not (Test-Path $cfgPath)) { throw "Không thấy $cfgPath" }
    $lines = Get-Content $cfgPath -Encoding UTF8
    $found = $false
    $out = foreach ($line in $lines) {
        if ($line -match "^\s*$([regex]::Escape($key))\s*=") {
            $found = $true
            "$key=$value"
        } else { $line }
    }
    if (-not $found) { $out = @($out) + "$key=$value" }
    $enc = New-Object System.Text.UTF8Encoding $false
    [System.IO.File]::WriteAllLines($cfgPath, @($out), $enc)
}

function Get-MasterValue([string]$key) {
    if (-not (Test-Path $masterPath)) { return $null }
    $line = Get-Content $masterPath -Encoding UTF8 |
        Where-Object { $_ -match "^\s*$([regex]::Escape($key))\s*=" } |
        Select-Object -Last 1
    if (-not $line) { return $null }
    return (($line -split '=', 2)[1]).Trim()
}

function Get-LoaiDonList {
    $raw = Get-MasterValue 'loaiDon'
    $names = @()
    if ($raw) {
        $names = @($raw -split '\|' | ForEach-Object { $_.Trim() } | Where-Object { $_ })
    }
    if ($names.Count -eq 0) {
        $names = @(
            'Dân sự',
            'Hôn nhân và gia đình',
            'Lao động',
            'Kinh doanh, thương mại',
            'Hành chính',
            'Sở hữu trí tuệ',
            'Phá sản'
        )
    }
    $desc = @{
        'Dân sự'                 = 'tranh chấp dân sự (hợp đồng, đất đai, thừa kế...)'
        'Hôn nhân và gia đình'   = 'ly hôn, nuôi con, cấp dưỡng, chia tài sản...'
        'Lao động'               = 'sa thải, lương, BHXH, bồi thường lao động...'
        'Kinh doanh, thương mại' = 'hợp đồng KD-TM, góp vốn, nội bộ công ty...'
        'Hành chính'             = 'kiện quyết định / hành vi hành chính'
        'Sở hữu trí tuệ'         = 'nhãn hiệu, sáng chế, quyền tác giả...'
        'Phá sản'                = 'yêu cầu mở thủ tục phá sản'
    }
    $items = @()
    for ($i = 0; $i -lt $names.Count; $i++) {
        $n = $names[$i]
        $d = $desc[$n]
        if (-not $d) { $d = 'theo catalog hệ thống' }
        $items += @{
            Label = '{0}. {1}  —  {2}' -f ($i + 1), $n, $d
            Data  = $n
        }
    }
    return ,$items
}

function Get-LoaiViecList([string]$loaiDon) {
    if ([string]::IsNullOrWhiteSpace($loaiDon)) { return @() }
    $pairs = Get-MasterValue 'loaiDonViecPairs'
    $list = @()
    if ($pairs) {
        foreach ($p in ($pairs -split ';')) {
            $bits = $p -split '>', 2
            if ($bits.Count -ge 2 -and $bits[0].Trim() -eq $loaiDon.Trim()) {
                $list += $bits[1].Trim()
            }
        }
    }
    if ($list.Count -eq 0 -and $loaiDon -match 'Phá sản') {
        $list = @('Yêu cầu mở thủ tục phá sản')
    }
    $items = @()
    for ($i = 0; $i -lt $list.Count; $i++) {
        $items += @{
            Label = '{0}. {1}' -f ($i + 1), $list[$i]
            Data  = $list[$i]
        }
    }
    return ,$items
}

function Get-TuCachList {
    $raw = Get-MasterValue 'tuCachNopDonPhaSan'
    $list = @()
    if ($raw) {
        $list = @($raw -split '\|' | ForEach-Object { $_.Trim() } | Where-Object { $_ })
    }
    if ($list.Count -eq 0) {
        $list = @('Chủ nợ', 'Người lao động', 'DN / HTX tự nộp', 'Cổ đông – thành viên HTX')
    }
    $items = @()
    for ($i = 0; $i -lt $list.Count; $i++) {
        $items += @{
            Label = '{0}. {1}' -f ($i + 1), $list[$i]
            Data  = $list[$i]
        }
    }
    return ,$items
}

function ConvertTo-MenuEntries($Items) {
    $labelArr = New-Object System.Collections.Generic.List[string]
    $dataArr = New-Object System.Collections.Generic.List[string]
    foreach ($it in @($Items)) {
        if ($null -eq $it) { continue }
        if ($it -is [hashtable] -or $it -is [System.Collections.IDictionary] -or $it -is [pscustomobject]) {
            $lab = [string]$it.Label
            $dat = [string]$it.Data
            if ([string]::IsNullOrWhiteSpace($dat)) { $dat = [string]$it.Value }
            if ([string]::IsNullOrWhiteSpace($lab)) { $lab = $dat }
            if ([string]::IsNullOrWhiteSpace($lab) -and [string]::IsNullOrWhiteSpace($dat)) { continue }
            if ([string]::IsNullOrWhiteSpace($dat)) { $dat = $lab }
            $labelArr.Add($lab)
            $dataArr.Add($dat)
        } else {
            $s = [string]$it
            if ([string]::IsNullOrWhiteSpace($s)) { continue }
            $labelArr.Add($s)
            $dataArr.Add($s)
        }
    }
    return [pscustomobject]@{
        Labels = $labelArr.ToArray()
        Datas  = $dataArr.ToArray()
    }
}

function Show-SelectMenuNumber {
    param(
        [string]$Title,
        [string[]]$Labels,
        [string[]]$Datas,
        [int]$DefaultIndex = 0,
        [switch]$AllowCancel,
        [string]$Context = ''
    )
    Write-Host ''
    if ($Context) { Write-Host $Context -ForegroundColor Cyan }
    Write-Host $Title -ForegroundColor Yellow
    for ($i = 0; $i -lt $Labels.Length; $i++) {
        Write-Host ("  {0}" -f $Labels[$i])
    }
    $hint = "Nhập số trên nhãn (hoặc 1-$($Labels.Length)) rồi Enter"
    if ($AllowCancel) { $hint += ' (Enter trống = hủy)' }
    $ans = Read-Host $hint
    if ([string]::IsNullOrWhiteSpace($ans)) {
        if ($AllowCancel) { return $null }
        $ans = [string]($DefaultIndex + 1)
    }
    if ($ans -match '^\d+$') {
        $byData = [Array]::IndexOf($Datas, $ans)
        if ($byData -ge 0) {
            Write-Host (">>> Đã chọn: {0}" -f $Labels[$byData]) -ForegroundColor Green
            return $Datas[$byData]
        }
        $idx = [int]$ans - 1
        if ($idx -lt 0 -or $idx -ge $Labels.Length) {
            Write-Host 'Số không hợp lệ — lấy mặc định.' -ForegroundColor Red
            $idx = $DefaultIndex
        }
        Write-Host (">>> Đã chọn: {0}" -f $Labels[$idx]) -ForegroundColor Green
        return $Datas[$idx]
    }
    Write-Host 'Số không hợp lệ — lấy mặc định.' -ForegroundColor Red
    Write-Host (">>> Đã chọn: {0}" -f $Labels[$DefaultIndex]) -ForegroundColor Green
    return $Datas[$DefaultIndex]
}

# Vẽ lại toàn màn (cuộn/cls) — cho phép nhãn dài, không neo toạ độ
function Show-SelectMenuArrow {
    param(
        [string]$Title,
        [string[]]$Labels,
        [string[]]$Datas,
        [int]$DefaultIndex = 0,
        [switch]$AllowCancel,
        [string]$Context = ''
    )

    $selected = [Math]::Max(0, [Math]::Min($DefaultIndex, $Labels.Length - 1))
    $hint = '↑↓ di chuyển   Enter chọn   phím số = chọn nhanh'
    if ($AllowCancel) { $hint += '   Esc hủy' }

    while ($true) {
        Clear-Host
        Write-Host '========================================================' -ForegroundColor Cyan
        Write-Host '           TOÀ ÁN — CHỌN CÁCH CHẠY TEST' -ForegroundColor Cyan
        Write-Host '========================================================' -ForegroundColor Cyan
        if ($Context) {
            Write-Host $Context -ForegroundColor DarkCyan
            Write-Host ''
        }
        Write-Host $Title -ForegroundColor Yellow
        Write-Host ''
        for ($i = 0; $i -lt $Labels.Length; $i++) {
            $text = $Labels[$i]
            if ($i -eq $selected) {
                Write-Host ("> {0}" -f $text) -ForegroundColor Black -BackgroundColor Cyan
            } else {
                Write-Host ("  {0}" -f $text)
            }
        }
        Write-Host ''
        Write-Host $hint -ForegroundColor DarkGray
        Write-Host ("(đang chọn mục {0}/{1})" -f ($selected + 1), $Labels.Length) -ForegroundColor DarkGray

        $key = $Host.UI.RawUI.ReadKey('NoEcho,IncludeKeyDown')
        if ($key.KeyDown -eq $false) { continue }

        $vk = [int]$key.VirtualKeyCode
        $ch = $key.Character

        switch ($vk) {
            38 { $selected = if ($selected -le 0) { $Labels.Length - 1 } else { $selected - 1 } }
            40 { $selected = if ($selected -ge $Labels.Length - 1) { 0 } else { $selected + 1 } }
            13 {
                Clear-Host
                Write-Host (">>> Đã chọn: {0}" -f $Labels[$selected]) -ForegroundColor Green
                return $Datas[$selected]
            }
            27 {
                if ($AllowCancel) {
                    Clear-Host
                    return $null
                }
            }
            default {
                if ($ch -ge '0' -and $ch -le '9') {
                    $byData = [Array]::IndexOf($Datas, [string]$ch)
                    if ($byData -ge 0) {
                        Clear-Host
                        Write-Host (">>> Đã chọn: {0}" -f $Labels[$byData]) -ForegroundColor Green
                        return $Datas[$byData]
                    }
                    $idx = [int][string]$ch - 1
                    if ($idx -ge 0 -and $idx -lt $Labels.Length) {
                        Clear-Host
                        Write-Host (">>> Đã chọn: {0}" -f $Labels[$idx]) -ForegroundColor Green
                        return $Datas[$idx]
                    }
                }
            }
        }
    }
}

function Show-SelectMenu {
    param(
        [Parameter(Mandatory = $true)][string]$Title,
        [Parameter(Mandatory = $true)]$Items,
        [int]$DefaultIndex = 0,
        [switch]$AllowCancel,
        [string]$Context = ''
    )

    $parsed = ConvertTo-MenuEntries $Items
    if ($null -eq $parsed -or $parsed.Labels.Length -eq 0) {
        throw "Danh sách chọn trống: $Title"
    }

    $labels = [string[]]$parsed.Labels
    $datas = [string[]]$parsed.Datas
    $def = [Math]::Max(0, [Math]::Min($DefaultIndex, $labels.Length - 1))

    $result = $null
    if ($script:UseArrowMenu) {
        try {
            $result = Show-SelectMenuArrow -Title $Title -Labels $labels -Datas $datas `
                -DefaultIndex $def -AllowCancel:$AllowCancel -Context $Context
        } catch {
            Write-Host 'Menu ↑↓ lỗi — chuyển sang chọn bằng số.' -ForegroundColor Yellow
        }
    }
    if ($null -eq $result -and -not $AllowCancel) {
        $result = Show-SelectMenuNumber -Title $Title -Labels $labels -Datas $datas `
            -DefaultIndex $def -AllowCancel:$AllowCancel -Context $Context
    } elseif ($null -eq $result -and $AllowCancel) {
        $result = Show-SelectMenuNumber -Title $Title -Labels $labels -Datas $datas `
            -DefaultIndex $def -AllowCancel:$AllowCancel -Context $Context
    }

    if (-not $AllowCancel -and [string]::IsNullOrWhiteSpace([string]$result)) {
        throw "Không nhận được lựa chọn hợp lệ cho: $Title"
    }
    return [string]$result
}

$script:UseArrowMenu = Test-HasRealConsole

function Clear-CustomProps {
    Set-Prop 'run.cases' ''
    Set-Prop 'run.slots' ''
    # Các preset / wizard chạy theo cấu hình trong file — không để sheet chiếm quyền.
    Set-Prop 'run.caseSource' 'file'
}

function Get-CfgValue([string]$key, [string]$default) {
    if (-not (Test-Path $cfgPath)) { return $default }
    $line = Get-Content $cfgPath -Encoding UTF8 |
        Where-Object { $_ -match "^\s*$([regex]::Escape($key))\s*=" } |
        Select-Object -Last 1
    if (-not $line) { return $default }
    $val = ($line -split '=', 2)[1]
    if ($null -eq $val) { return $default }
    $val = $val.Trim()
    if ([string]::IsNullOrWhiteSpace($val)) { return $default }
    return $val
}

# Chạy danh sách case lấy từ Google Sheet (run.casesSheet).
function Apply-SheetSource {
    $url = Get-CfgValue 'run.casesSheet' ''
    if (-not $url) {
        Write-Host ''
        Write-Host 'Chưa có link Google Sheet trong run-flow.properties (khoá run.casesSheet).' -ForegroundColor Red
        Write-Host 'Mở file src\test\resources\run-flow.properties và dán link vào rồi thử lại.' -ForegroundColor Yellow
        Write-Host 'Nhấn Enter để quay lại menu...' -ForegroundColor DarkGray
        [void][Console]::ReadLine()
        return $false
    }

    $nChromeStr = Show-SelectMenu -Title 'Số cửa sổ Chrome mở cùng lúc:' -Items @(
        @{ Label = '1. 1 Chrome  —  chạy tuần tự từng case trong sheet'; Data = '1' }
        @{ Label = '2. 2 Chrome  —  tối đa 2 case song song'; Data = '2' }
        @{ Label = '3. 3 Chrome  —  tối đa 3 case song song'; Data = '3' }
    ) -Context 'Danh sách case lấy từ Google Sheet'
    $nChrome = [int]$nChromeStr

    Set-Prop 'run.cases' ''
    Set-Prop 'run.slots' ''
    Set-Prop 'run.caseSource' 'sheet'
    Set-Prop 'run.suite' 'master'
    Set-Prop 'run.browsers' "$nChrome"
    Set-Prop 'run.parallel' $(if ($nChrome -gt 1) { 'true' } else { 'false' })

    Write-Host ''
    Write-Host 'Đã ghi: nguồn case = Google Sheet' -ForegroundColor Cyan
    Write-Host ("  Link   : {0}" -f $url) -ForegroundColor DarkCyan
    Write-Host ("  Chrome : {0}" -f $nChrome) -ForegroundColor DarkCyan
    Write-Host '  Độ sâu / gửi đơn lấy theo từng dòng trong sheet (cột "Đến bước", "Gửi đơn").' -ForegroundColor DarkGray
    return $true
}

function Apply-Preset {
    param(
        [string]$Suite,
        [int]$Browsers,
        [string]$UntilStep = '6',
        [string]$Submit = 'false',
        [string]$Cases = ''
    )
    Clear-CustomProps
    $parallel = if ($Browsers -gt 1) { 'true' } else { 'false' }
    Set-Prop 'run.suite' $Suite
    Set-Prop 'run.browsers' "$Browsers"
    Set-Prop 'run.parallel' $parallel
    Set-Prop 'run.untilStep' $UntilStep
    Set-Prop 'run.submit' $Submit
    if ($Cases) { Set-Prop 'run.cases' $Cases }

    Write-Host ''
    if ($Cases) {
        $caseList = @($Cases -split '\|' | ForEach-Object { $_.Trim() } | Where-Object { $_ })
        Write-Host ("Đã ghi: {0} Chrome | {1} case" -f $Browsers, $caseList.Count) -ForegroundColor Cyan
        if ($caseList.Count -gt $Browsers) {
            Write-Host ("  → Case dư sẽ xếp hàng trên {0} Chrome (reuse session)." -f $Browsers) -ForegroundColor DarkCyan
        }
        for ($i = 0; $i -lt $caseList.Count; $i++) {
            Write-Host ("  {0}. {1}" -f ($i + 1), $caseList[$i]) -ForegroundColor Cyan
        }
    } else {
        Write-Host "Đã ghi: suite=$Suite | Chrome=$Browsers | untilStep=$UntilStep | submit=$Submit" -ForegroundColor Cyan
    }
}

function Ask-OneCaseToken {
    param([string]$ChromeLabel)

    $ctx = "======== $ChromeLabel ========  (Loại đơn → Việc → CN/TC → Dừng bước)"

    $loaiDonItems = Get-LoaiDonList
    $loaiDon = Show-SelectMenu -Title 'Bước A — Chọn LOẠI ĐƠN (7 loại):' -Items $loaiDonItems -Context $ctx
    if ([string]::IsNullOrWhiteSpace($loaiDon)) {
        throw 'Chưa chọn được loại đơn.'
    }
    Write-Host ("   [Tóm tắt] Loại đơn = {0}" -f $loaiDon) -ForegroundColor DarkCyan
    $ctx = "$ChromeLabel | Loại đơn: $loaiDon"

    $isPhaSan = ($loaiDon -match 'Phá sản')
    $loaiViec = 'Yêu cầu mở thủ tục phá sản'
    $tuCach = '-'
    if (-not $isPhaSan) {
        $viecItems = Get-LoaiViecList $loaiDon
        if (@($viecItems).Count -eq 0) {
            throw "Không có loại việc cho [$loaiDon] trong master-data.properties"
        }
        $loaiViec = Show-SelectMenu -Title ("Bước B — Chọn LOẠI VIỆC (thuộc {0}):" -f $loaiDon) `
            -Items $viecItems -Context $ctx
        Write-Host ("   [Tóm tắt] Loại việc = {0}" -f $loaiViec) -ForegroundColor DarkCyan
        $ctx = "$ChromeLabel | $loaiDon / $loaiViec"
    } else {
        $tuItems = Get-TuCachList
        $tuCach = Show-SelectMenu -Title 'Bước B — Chọn TƯ CÁCH nộp đơn (Phá sản):' `
            -Items $tuItems -Context $ctx
        Write-Host ("   [Tóm tắt] Tư cách = {0}" -f $tuCach) -ForegroundColor DarkCyan
        $ctx = "$ChromeLabel | Phá sản | Tư cách: $tuCach"
    }

    $chu = Show-SelectMenu -Title 'Bước C — Nguyên đơn là ai?' -Items @(
        @{ Label = '1. Cá nhân  —  người dân (điền CCCD, họ tên, địa chỉ...)'; Data = 'CN' }
        @{ Label = '2. Tổ chức / Doanh nghiệp  —  công ty, DN, HTX (điền MST, tên TC...)'; Data = 'TC' }
    ) -Context $ctx
    $chuLabel = if ($chu -eq 'TC') { 'Tổ chức / Doanh nghiệp' } else { 'Cá nhân' }
    Write-Host ("   [Tóm tắt] Chủ thể = {0}" -f $chuLabel) -ForegroundColor DarkCyan
    $ctx = "$ChromeLabel | $loaiDon | $chuLabel"

    $step = Show-SelectMenu -Title 'Bước D — Chạy đến đâu rồi DỪNG?' -Items @(
        @{ Label = '0. Chỉ đăng nhập  —  mở web + login, không mở form tạo đơn'; Data = '0' }
        @{ Label = '1. Loại đơn / Tòa án  —  dừng sau bước 1 (đã chọn loại đơn + tòa)'; Data = '1' }
        @{ Label = '2. Nguyên đơn  —  dừng sau điền thông tin nguyên đơn'; Data = '2' }
        @{ Label = '3. Bị đơn  —  dừng sau điền thông tin bị đơn'; Data = '3' }
        @{ Label = '4. Nội dung đơn  —  dừng sau điền nội dung / yêu cầu'; Data = '4' }
        @{ Label = '5. Tài liệu  —  dừng sau bước tải tài liệu'; Data = '5' }
        @{ Label = '6. Xem lại  —  điền đủ form đến màn Xem lại (chưa gửi đơn)'; Data = '6' }
    ) -DefaultIndex 6 -Context $ctx
    Write-Host ("   [Tóm tắt] Dừng ở bước = {0}" -f $step) -ForegroundColor DarkCyan

    $until = $step
    if ($step -eq '6') {
        $sub = Show-SelectMenu -Title 'Bước E — Có bấm GỬI ĐƠN không?' -Items @(
            @{ Label = '1. Không  —  dừng an toàn tại Xem lại (khuyến nghị khi test)'; Data = 'N' }
            @{ Label = '2. Có  —  bấm Gửi đơn thật lên hệ thống'; Data = 'Y' }
        ) -Context "$ctx | đến Xem lại"
        if ($sub -eq 'Y') { $until = '6:submit' }
        Write-Host ("   [Tóm tắt] Gửi đơn = {0}" -f $(if ($sub -eq 'Y') { 'Có' } else { 'Không' })) -ForegroundColor DarkCyan
    }

    Write-Host ''
    Write-Host '----- Case sẽ chạy -----' -ForegroundColor Green
    Write-Host ("  Loại đơn : {0}" -f $loaiDon)
    Write-Host ("  Loại việc: {0}" -f $loaiViec)
    Write-Host ("  Chủ thể  : {0}" -f $chuLabel)
    if ($tuCach -ne '-') { Write-Host ("  Tư cách  : {0}" -f $tuCach) }
    Write-Host ("  Độ sâu   : {0}" -f $until)
    Write-Host '------------------------' -ForegroundColor Green

    return "$loaiDon>$loaiViec>$chu>$tuCach>$until"
}

function Ask-CustomWizard {
    Write-Host ''
    Write-Host 'Cấu hình case tùy chọn — chọn xong sẽ chạy luôn.' -ForegroundColor Yellow
    Write-Host 'Có thể chọn ÍT Chrome hơn số case (vd. 2 Chrome chạy lần lượt 5 case).' -ForegroundColor DarkGray

    $nChromeStr = Show-SelectMenu -Title 'Bước 1 — Số cửa sổ Chrome mở cùng lúc:' -Items @(
        @{ Label = '1. 1 Chrome  —  chạy tuần tự từng case'; Data = '1' }
        @{ Label = '2. 2 Chrome  —  tối đa 2 case song song'; Data = '2' }
        @{ Label = '3. 3 Chrome  —  tối đa 3 case song song'; Data = '3' }
    ) -Context 'Cấu hình case tùy chọn'
    $nChrome = [int]$nChromeStr

    $caseCountItems = @()
    for ($c = 1; $c -le 8; $c++) {
        $hint = if ($c -le $nChrome) {
            'mỗi case 1 Chrome (hoặc ít case hơn Chrome)'
        } else {
            ("xếp hàng trên {0} Chrome" -f $nChrome)
        }
        $caseCountItems += @{ Label = ("{0}. {1} case  —  {2}" -f $c, $c, $hint); Data = "$c" }
    }
    $nCaseStr = Show-SelectMenu -Title 'Bước 2 — Số CASE muốn chạy:' -Items $caseCountItems `
        -Context ("Chrome song song: $nChrome")
    $nCase = [int]$nCaseStr

    $tokens = @()
    for ($i = 1; $i -le $nCase; $i++) {
        $tokens += (Ask-OneCaseToken -ChromeLabel "Case $i / $nCase  (Chrome tối đa $nChrome)")
    }
    $cases = [string]::Join('|', $tokens)
    Apply-Preset -Suite 'master' -Browsers $nChrome -UntilStep '6' -Submit 'false' -Cases $cases
}

# ---- Menu chính (lặp lại sau mỗi lần chạy) ----
$mainItems = @(
    @{ Label = '1. Chạy theo Google Sheet  —  danh sách case lấy từ link trong run-flow.properties'; Data = 'S' }
    @{ Label = '2. Cấu hình case rồi chạy  —  chọn loại đơn / CN-TC / bước dừng'; Data = '1' }
    @{ Label = '3. Smoke nhanh — 1 Chrome  —  vài case mẫu, có gửi đơn'; Data = '2' }
    @{ Label = '4. Smoke nhanh — 3 Chrome  —  chạy song song 3 cửa sổ'; Data = '3' }
    @{ Label = '5. Chỉ đăng nhập  —  kiểm tra login, không tạo đơn'; Data = '4' }
    @{ Label = '6. Mid regression — 3 Chrome  —  khoảng 40 case'; Data = '5' }
    @{ Label = '7. Full coverage — 3 Chrome  —  đủ ma trận (lâu)'; Data = '6' }
    @{ Label = '8. Xem cấu hình hiện tại  —  không chạy test'; Data = 'V' }
    @{ Label = '0. Thoát'; Data = '0' }
)

while ($true) {
    $hintMode = if ($script:UseArrowMenu) {
        '↑↓ + Enter để chọn. Chạy xong sẽ quay lại menu này (log xem ở test-output\last-run.log).'
    } else {
        'Nhập số rồi Enter. Chạy xong quay lại menu (log: test-output\last-run.log).'
    }

    $choice = Show-SelectMenu -Title 'Chọn cách chạy:' -Items $mainItems -Context $hintMode

    $shouldRun = $true
    switch ($choice) {
        'S' { if (-not (Apply-SheetSource)) { $shouldRun = $false } }
        '1' { Ask-CustomWizard }
        '2' { Apply-Preset -Suite 'smoke' -Browsers 1 }
        '3' { Apply-Preset -Suite 'smoke' -Browsers 3 }
        '4' { Apply-Preset -Suite 'login' -Browsers 1 -UntilStep '0' -Submit 'false' }
        '5' { Apply-Preset -Suite 'mid' -Browsers 3 }
        '6' { Apply-Preset -Suite 'full' -Browsers 3 }
        'V' {
            & $runFlow -DryRun
            $shouldRun = $false
            Write-Host ''
            Write-Host 'Nhấn Enter để quay lại menu...' -ForegroundColor DarkGray
            [void][Console]::ReadLine()
        }
        '0' {
            Write-Host 'Thoát.'
            exit 0
        }
        default {
            Write-Host "Lựa chọn không hợp lệ: [$choice]" -ForegroundColor Red
            $shouldRun = $false
        }
    }

    if (-not $shouldRun) { continue }

    Write-Host ''
    Write-Host 'Đang mở trình duyệt và chạy test...' -ForegroundColor Green
    Write-Host '(Chi tiết không in ra CMD — xem test-output\last-run.log)' -ForegroundColor DarkGray
    & $runFlow
    $code = $LASTEXITCODE

    Write-Host ''
    if ($code -eq 0) {
        Write-Host 'Lần chạy xong — THÀNH CÔNG. Quay lại menu cấu hình.' -ForegroundColor Green
    } else {
        Write-Host 'Lần chạy xong — THẤT BẠI. Quay lại menu cấu hình.' -ForegroundColor Yellow
    }
    Write-Host 'Nhấn Enter để tiếp tục chọn cấu hình...' -ForegroundColor DarkGray
    [void][Console]::ReadLine()
}