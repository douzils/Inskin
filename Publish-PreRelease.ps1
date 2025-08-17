param(
  [Parameter(Mandatory=$true)] [string]$Version,
  [string]$Notes = "Pre-release auto",
  [Parameter(Mandatory=$true)] [string]$Repo,

  # --- Signature (debug par défaut) ---
  [string]$KeystorePath = "$env:USERPROFILE\.android\debug.keystore",
  [string]$KeystoreAlias = "androiddebugkey",
  [string]$KeystorePass = "android",
  [string]$KeyPass      = "android",

  # --- Build ---
  [switch]$UseWrapper = $true,   # utilise gradlew s'il est présent

  # --- Git (optionnel) ---
  [string]$Remote = "origin",
  [string]$Branch = "main",
  [switch]$ForceResetIfUnrelated,
  [switch]$AutoCommit,
  [string]$CommitMessage = "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') auto-commit",
  [switch]$SkipGitSync
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

function Fail($msg){ Write-Error $msg; exit 1 }

function Run {
  param(
    [Parameter(Mandatory=$true)][string]$File,
    [Parameter(Mandatory=$true)][string[]]$Args
  )
  $out="$env:TEMP\_out.txt"; $err="$env:TEMP\_err.txt"
  $p = Start-Process -FilePath $File -ArgumentList $Args -NoNewWindow -Wait -PassThru `
       -RedirectStandardOutput $out -RedirectStandardError $err
  if ($p.ExitCode -ne 0) {
    $e = (Get-Content $err -Raw -ErrorAction SilentlyContinue)
    if (-not $e) { $e = (Get-Content $out -Raw -ErrorAction SilentlyContinue) }
    Fail ("Cmd failed: `"$File $($Args -join ' ')`"`n$e")
  }
  Remove-Item $out,$err -ErrorAction SilentlyContinue
}

Write-Host "== Pré-requis: Java 17, Android SDK, gh CLI, Git ==" -ForegroundColor DarkGray

# Racine du dépôt
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $Root

# --- Git sync (optionnel) ---
if (-not $SkipGitSync) {
  if (-not (Get-Command git -ErrorAction SilentlyContinue)) { Fail "git introuvable dans le PATH." }
  $null = & git rev-parse --is-inside-work-tree 2>$null
  if ($LASTEXITCODE -ne 0) { Fail "Pas un dépôt git: $Root" }

  $null = & git rev-parse --verify $Branch 2>$null
  if ($LASTEXITCODE -ne 0) { Run "git" @("checkout","-b",$Branch) } else { Run "git" @("checkout",$Branch) }

  $tracking = (& git rev-parse --abbrev-ref --symbolic-full-name "HEAD@{upstream}" 2>$null)
  if (-not $tracking) { $null = & git branch "--set-upstream-to=$Remote/$Branch" $Branch 2>$null }

  $prevErr = $ErrorActionPreference; $ErrorActionPreference = 'Continue'
  $pullOut  = (& git pull $Remote $Branch 2>&1 | Out-String).Trim()
  $pullCode = $LASTEXITCODE
  $ErrorActionPreference = $prevErr

  if ($pullCode -ne 0) {
    if ($pullOut -match 'refusing to merge unrelated histories') {
      if (-not $ForceResetIfUnrelated) { Fail "Histoires non liées. Relance avec -ForceResetIfUnrelated." }
      $backup = "sauvegarde-local-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
      Write-Host "Unrelated histories. Sauvegarde: $backup puis reset dur."
      Run "git" @("branch",$backup)
      Run "git" @("fetch",$Remote)
      Run "git" @("reset","--hard","$Remote/$Branch")
      Run "git" @("clean","-fd")
    } elseif ($pullOut -match 'divergent' -or $pullOut -match 'rebase') {
      Run "git" @("pull","--rebase",$Remote,$Branch)
    } else {
      Fail "Echec du pull: $pullOut"
    }
  }

  if ($AutoCommit) {
    $st = (& git status --porcelain)
    if ($st) { Run "git" @("add","-A"); Run "git" @("commit","-m",$CommitMessage) }
  }

  $aheadLine = (& git rev-list --left-right --count "$Branch...$Remote/$Branch" 2>$null)
  if ($aheadLine) {
    $nums = $aheadLine -split "\s+"
    if ([int]$nums[0] -gt 0) { Run "git" @("push",$Remote,$Branch) }
  }
}

# --- Outils Android ---
$Sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } elseif ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } else { "$env:LOCALAPPDATA\Android\Sdk" }
$Bt  = (Get-ChildItem "$Sdk\build-tools" -Directory | Sort-Object Name -Descending | Select-Object -First 1 -ErrorAction Stop).FullName
$Zipalign  = Join-Path $Bt "zipalign.exe"
$ApkSigner = Join-Path $Bt "apksigner.bat"
if (!(Test-Path $Zipalign)) { Fail "zipalign introuvable: $Zipalign" }
if (!(Test-Path $ApkSigner)) { Fail "apksigner introuvable: $ApkSigner" }

# --- Build Release ---
$GradleCmd = if ($UseWrapper -and (Test-Path ".\gradlew.bat")) { ".\gradlew.bat" } else { "gradle" }
Run $GradleCmd @("clean",":app:assembleRelease")

$Unsigned = "app/build/outputs/apk/release/app-release-unsigned.apk"
$Aligned  = "app/build/outputs/apk/release/app-release-aligned.apk"
$Signed   = "app/build/outputs/apk/release/app-release-$Version-signed.apk"
if (!(Test-Path $Unsigned)) { Fail "APK unsigned introuvable: $Unsigned" }

# Align + Sign + Verify
Run $Zipalign @("-f","-p","4",$Unsigned,$Aligned)
Run $ApkSigner @(
  "sign","--ks",$KeystorePath,"--ks-key-alias",$KeystoreAlias,
  "--ks-pass","pass:$KeystorePass","--key-pass","pass:$KeyPass",
  "--out",$Signed,$Aligned
)
Run $ApkSigner @("verify","--verbose",$Signed)
Write-Host "APK signé: $Signed" -ForegroundColor Green

# --- Tag (si sync activée) ---
if (-not $SkipGitSync) {
  $tagExists = (& git rev-parse "refs/tags/$Version" 2>$null)
  if (-not $tagExists) {
    Write-Host "Tag $Version inexistant → création..." -ForegroundColor Yellow
    Run "git" @("tag","-a",$Version,"-m","Release $Version")
    Run "git" @("push",$Remote,$Version)
  } else {
    Write-Host "Tag $Version existe déjà, on continue." -ForegroundColor DarkGray
  }
}


# --- Create or update prerelease (via gh release create/upload) ---
$prevErr = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
$existing = (& gh release view $Version --repo $Repo 2>&1 | Out-String).Trim()
$code = $LASTEXITCODE
$ErrorActionPreference = $prevErr

if ($code -ne 0 -or -not $existing) {
  Run "gh" @(
    "release","create",$Version,
    "--repo",$Repo,
    "--title","Inskin $Version",
    "--notes",$Notes,
    "--prerelease",
    "--target",$Branch,
    $Signed
  )
  Write-Host "Pre-release $Version créée." -ForegroundColor Green
} else {
  Run "gh" @("release","upload",$Version,$Signed,"--repo",$Repo,"--clobber")
  Run "gh" @("release","edit",$Version,"--repo",$Repo,"--prerelease","--notes",$Notes)
  Write-Host "Pre-release $Version mise à jour." -ForegroundColor Green
}


Write-Host "Terminé." -ForegroundColor Cyan
