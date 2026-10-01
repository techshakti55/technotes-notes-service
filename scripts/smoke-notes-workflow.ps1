# Uses only the isolated Docker smoke API on localhost:18081.
$ErrorActionPreference = 'Stop'
$base = 'http://localhost:18081'
$compose = Join-Path (Split-Path $PSScriptRoot -Parent) 'compose.notes-smoke.yml'
$token = $null
$authHeaders = $null

function Send-SmokeRequest {
    param([string]$Method, [string]$Path, [object]$Body = $null, [string]$ETag = '', [switch]$Anonymous)
    $headers = @{}
    if (-not $Anonymous) { $headers.Authorization = $authHeaders.Authorization }
    if ($ETag) { $headers['If-Match'] = $ETag }
    $options = @{
        Uri = "$base$Path"; Method = $Method; Headers = $headers
        UseBasicParsing = $true; TimeoutSec = 20; ErrorAction = 'Stop'
    }
    if ($null -ne $Body) {
        $options.ContentType = 'application/json'
        $options.Body = $Body | ConvertTo-Json -Depth 8 -Compress
    }
    $response = Invoke-WebRequest @options
    [pscustomobject]@{
        Status = [int]$response.StatusCode
        ETag = [string]$response.Headers['ETag']
        Data = ($response.Content | ConvertFrom-Json)
    }
}

function Require-Smoke {
    param([bool]$Condition, [string]$Message)
    if (-not $Condition) { throw $Message }
}

function Require-RejectedPatch {
    param([string]$Path, [string]$ETag, [int]$ExpectedStatus)
    try {
        $null = Send-SmokeRequest -Method PATCH -Path $Path -Body @{summary='Rejected update'} -ETag $ETag
    } catch {
        if ($null -eq $_.Exception.Response) { throw }
        $actual = [int]$_.Exception.Response.StatusCode
        if ($actual -ne $ExpectedStatus) { throw "Expected HTTP $ExpectedStatus, received HTTP $actual." }
        return
    }
    throw "PATCH unexpectedly succeeded; expected HTTP $ExpectedStatus."
}

try {
    [void](Read-Host 'Copy a fresh Admin access token in browser; return here and press Enter')
    $token = (Get-Clipboard -Raw).Trim()
    Require-Smoke ($token -match '^[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+$') 'Clipboard does not contain an access token.'
    $authHeaders = @{Authorization = "Bearer $token"}

    $null = Send-SmokeRequest -Method GET -Path '/api/v1/notes'
    Write-Host 'PASS: authenticated note list'

    $suffix = [guid]::NewGuid().ToString('N').Substring(0, 10)
    $category = Send-SmokeRequest -Method POST -Path '/api/v1/categories' -Body @{
        name = "Docker Smoke $suffix"; slug = "docker-smoke-$suffix"; sortOrder = 0
    }
    Require-Smoke ($category.Status -eq 201 -and [bool]$category.Data.id) 'Category creation failed.'
    Write-Host 'PASS: category created'

    $markdown = "# Docker smoke $suffix" + [Environment]::NewLine + [Environment]::NewLine + 'Container publish and restart verification.'
    $draft = Send-SmokeRequest -Method POST -Path '/api/v1/notes' -Body @{
        title = "Docker Smoke $suffix"; summary = 'Container integration check'
        contentMarkdown = $markdown; primaryCategoryId = $category.Data.id
        tags = @('docker-smoke'); visibility = 'PUBLIC'
    }
    Require-Smoke ($draft.Status -eq 201 -and $draft.Data.status -eq 'DRAFT' -and [bool]$draft.ETag) 'Draft creation or ETag failed.'
    $notePath = "/api/v1/notes/$($draft.Data.id)"
    Write-Host 'PASS: draft created with ETag'

    Require-RejectedPatch -Path $notePath -ETag '' -ExpectedStatus 428
    Write-Host 'PASS: missing If-Match rejected with 428'

    $edited = Send-SmokeRequest -Method PATCH -Path $notePath -ETag $draft.ETag -Body @{summary = 'Edited before submission'}
    Require-Smoke ($edited.Data.summary -eq 'Edited before submission' -and [bool]$edited.ETag) 'Draft edit failed.'
    Require-RejectedPatch -Path $notePath -ETag $draft.ETag -ExpectedStatus 412
    Write-Host 'PASS: edit succeeded; stale ETag rejected with 412'

    $submitted = Send-SmokeRequest -Method POST -Path "$notePath/submit" -ETag $edited.ETag
    Require-Smoke ($submitted.Data.status -eq 'IN_REVIEW' -and [bool]$submitted.ETag) 'Submission failed.'
    Write-Host 'PASS: IN_REVIEW'

    $published = Send-SmokeRequest -Method POST -Path "$notePath/publish" -ETag $submitted.ETag
    Require-Smoke ($published.Data.status -eq 'PUBLISHED' -and [bool]$published.Data.slug) 'Publish failed.'
    $publicPath = "/api/v1/public/notes/$($published.Data.slug)"
    $public = Send-SmokeRequest -Method GET -Path $publicPath -Anonymous
    Require-Smoke ($public.Data.contentMarkdown -eq $markdown) 'Anonymous published content did not match.'
    Write-Host 'PASS: published content readable without token'

    Write-Host 'Restarting only the Compose smoke MongoDB and Notes containers...'
    docker compose -f $compose restart mongodb notes
    if ($LASTEXITCODE -ne 0) { throw 'Smoke container restart failed.' }

    $ready = $false
    for ($attempt = 0; $attempt -lt 30; $attempt++) {
        try {
            $health = Invoke-RestMethod -Uri "$base/actuator/health" -TimeoutSec 2 -ErrorAction Stop
            if ($health.status -eq 'UP') { $ready = $true; break }
        } catch {}
        Start-Sleep -Seconds 2
    }
    Require-Smoke $ready 'Notes health did not return UP after restart.'
    $afterRestart = Send-SmokeRequest -Method GET -Path $publicPath -Anonymous
    Require-Smoke ($afterRestart.Data.contentMarkdown -eq $markdown) 'Published content did not persist after restart.'
    Write-Host 'PASS: published content persisted after MongoDB and Notes restart'
    Write-Host "SMOKE TEST PASSED. Public note: $base$publicPath"
    docker compose -f $compose ps -a
    $ids = @(docker compose -f $compose ps -q mongodb notes)
    if ($LASTEXITCODE -eq 0 -and $ids.Count -gt 0) { docker stats --no-stream @ids }
} finally {
    $token = $null
    $authHeaders = $null
}
