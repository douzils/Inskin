# Publish-Release.ps1 (avec logs + barre de progression)
param(
  [Parameter(Mandatory=$true)] [string]$Version,
  [Parameter(Mandatory=$true)] [string]$Repo,
  [string]$Notes = "Release auto",

  # Keystore (remplace par ton keystore release si besoin)
  [string]$KeystorePath = "$env:USERPROFILE\.android\debug.keystore",
  [string]$KeystoreAlias = "androiddebugkey",
  [string]$KeystorePass = "android",
  [string]$KeyPass      = "android",

  # Build
  [switch]$UseWrapper = $true,

  # Git
  [string]$Remote = "origin",
  [string]$Branch = "main",
  [switch]$ForceResetIfUnrelated,
  [switch]$AutoCommit,
  [string]$CommitMessage = "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') auto-commit",
  [switch]$SkipGitSync,

  # Release
  [switch]$MarkLatest = $true
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$script:StartTime = Get-Date

function Now { (Get-Date).ToString("yyyy-MM-dd HH:mm:ss") }
function Log  { param([string]$Msg) Write-Host ("[{0}] {1}" -f (Now), $Msg) }
function Fail { param([string]$Msg) Write-Error ("[{0}] {1}" -f (Now), $Msg); exit 1 }

# Affiche et exécute une commande, capture sortie/erreur, stoppe si code != 0
function Run {
  param(
    [Parameter(Mandatory=$true)][string]$File,
    [Parameter(Mandatory=$true)][string[]]$Args
  )
  $Args = @($Args | Where-Object { $_ -ne $null -and $_ -ne "" })
  $cmdLine = "$File " + ($Args | ForEach-Object { if ($_ -match '\s') { '"' + $_ + '"' } else { $_ } }) -join ' '
  Log "RUN: $cmdLine"
  $out="$env:TEMP\_out.txt"; $err="$env:TEMP\_err.txt"
  $p = Start-Process -FilePath $File -ArgumentList $Args -NoNewWindow -Wait -PassThru `
       -RedirectStandardOutput $out -RedirectStandardError $err
  $stdout = (Get-Content $out -Raw -ErrorAction SilentlyContinue)
  $stderr = (Get-Content $err -Raw -ErrorAction SilentlyContinue)
  if ($stdout) { Log "OUT: $($stdout.Trim())" }
  if ($stderr) { Log "ERR: $($stderr.Trim())" }
  if ($p.ExitCode -ne 0) { Fail ("Cmd failed ($($p.ExitCode)): `n$cmdLine`n$stderr`n$stdout") }
  Remove-Item $out,$err -ErrorAction SilentlyContinue
}

# Barre de progression
# Steps: Init, (Git), SDK, Build, Align, Sign, Verify, (Tag), Release, Checks, Done
$TotalSteps = (9 + ($(if($SkipGitSync){0}else{2})))  # 11 si Git, 9 sinon
$Step = 0

function StepProgress { param([string]$Activity,[int]$Increment=1)
  $script:Step += $Increment
  $pct = [int](($script:Step / $TotalSteps) * 100)
  if ($pct -gt 100) { $pct = 100 }
  if ($pct -lt 0)   { $pct = 0 }
  Write-Progress -Activity $Activity -Status ("{0}% ({1}/{2})" -f $pct,$script:Step,$TotalSteps) -PercentComplete $pct
  Log $Activity
}


# Début
StepProgress "Initialisation"
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $Root
Log "Repo root: $Root"
if (-not (Get-Command gh -ErrorAction SilentlyContinue)) { Fail "gh introuvable. Installe GitHub CLI et fais 'gh auth login'." }
if (-not (Get-Command git -ErrorAction SilentlyContinue)) { Fail "git introuvable dans le PATH." }
Run "gh" @("auth","status")

# --- Git sync ---
if (-not $SkipGitSync) {
  StepProgress "Git sync sur branche $Branch"
  $null = & git rev-parse --is-inside-work-tree 2>$null
  if ($LASTEXITCODE -ne 0) { Fail "Pas un dépôt git: $Root" }

  # Branche et remote
  $hasBranch = (& git rev-parse --verify $Branch 2>$null); $code=$LASTEXITCODE
  if ($code -ne 0) { Run "git" @("checkout","-b",$Branch) } else { Run "git" @("checkout",$Branch) }
  $remoteUrl = (& git remote get-url $Remote 2>$null)
  Log "Remote '$Remote' -> $remoteUrl"

  # Upstream
  $tracking = (& git rev-parse --abbrev-ref --symbolic-full-name "HEAD@{upstream}" 2>$null)
  if (-not $tracking) { Log "Upstream manquant -> set upstream"; $null = & git branch "--set-upstream-to=$Remote/$Branch" $Branch 2>$null }

  # Pull avec gestion des cas
  $prev=$ErrorActionPreference; $ErrorActionPreference='Continue'
  $pullOut = (& git pull $Remote $Branch 2>&1 | Out-String).Trim()
  $pullCode=$LASTEXITCODE; $ErrorActionPreference=$prev
  if ($pullCode -ne 0) {
    if ($pullOut -match 'refusing to merge unrelated histories') {
      if (-not $ForceResetIfUnrelated) { Fail "Histoires non liées. Relance avec -ForceResetIfUnrelated." }
      $backup="sauvegarde-local-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
      Log "Unrelated histories. Backup: $backup puis reset dur."
      Run "git" @("branch",$backup)
      Run "git" @("fetch",$Remote)
      Run "git" @("reset","--hard","$Remote/$Branch")
      Run "git" @("clean","-fd")
    } elseif ($pullOut -match 'divergent' -or $pullOut -match 'rebase') {
      Run "git" @("pull","--rebase",$Remote,$Branch)
    } else {
      Fail "Echec pull: $pullOut"
    }
  }

  if ($AutoCommit) {
    $st = (& git status --porcelain)
    if ($st) { Run "git" @("add","-A"); Run "git" @("commit","-m",$CommitMessage) }
  }

  $ahead = (& git rev-list --left-right --count "$Branch...$Remote/$Branch" 2>$null)
  if ($ahead) {
    $n = $ahead -split "\s+"
    if ([int]$n[0] -gt 0) { Run "git" @("push",$Remote,$Branch) }
  }
}

# --- Android SDK / build-tools ---
StepProgress "Localisation SDK Android"
$Sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } elseif ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } else { "$env:LOCALAPPDATA\Android\Sdk" }
$Bt  = (Get-ChildItem "$Sdk\build-tools" -Directory | Sort-Object Name -Descending | Select-Object -First 1 -ErrorAction Stop).FullName
$Zipalign  = Join-Path $Bt "zipalign.exe"
$ApkSigner = Join-Path $Bt "apksigner.bat"
Log "SDK: $Sdk"
Log "Build-tools: $Bt"
if (!(Test-Path $Zipalign)) { Fail "zipalign introuvable: $Zipalign" }
if (!(Test-Path $ApkSigner)) { Fail "apksigner introuvable: $ApkSigner" }

# --- Build release ---
StepProgress "Compilation Gradle assembleRelease"
$GradleCmd = if ($UseWrapper -and (Test-Path ".\gradlew.bat")) { ".\gradlew.bat" } else { "gradle" }
Run $GradleCmd @("clean",":app:assembleRelease")

$Unsigned = "app/build/outputs/apk/release/app-release-unsigned.apk"
$Aligned  = "app/build/outputs/apk/release/app-release-aligned.apk"
$Signed   = "app/build/outputs/apk/release/app-release-$Version-signed.apk"
if (!(Test-Path $Unsigned)) { Fail "APK unsigned introuvable: $Unsigned" }

# --- Align / Sign / Verify ---
StepProgress "Alignement APK (zipalign)"
Run $Zipalign @("-f","-p","4",$Unsigned,$Aligned)

StepProgress "Signature APK (apksigner)"
Run $ApkSigner @(
  "sign","--ks",$KeystorePath,"--ks-key-alias",$KeystoreAlias,
  "--ks-pass","pass:$KeystorePass","--key-pass","pass:$KeyPass",
  "--out",$Signed,$Aligned
)

StepProgress "Vérification APK signé"
Run $ApkSigner @("verify","--verbose",$Signed)
Log "APK signé: $Signed"

# --- Tag Git ---
if (-not $SkipGitSync) {
  StepProgress "Tag Git $Version"
  & git rev-parse --verify HEAD 2>$null
  if ($LASTEXITCODE -ne 0) { Fail "Aucun commit. Fais un commit initial." }

  & git show-ref --tags --verify --quiet "refs/tags/$Version"
  if ($LASTEXITCODE -ne 0) {
    $tmpTagMsg = Join-Path $env:TEMP ("tagmsg_{0}.txt" -f ($Version -replace '[^\w\.-]','_'))
    "Release $Version" | Out-File -FilePath $tmpTagMsg -Encoding UTF8 -Force
    Run "git" @("tag","-a",$Version,"-F",$tmpTagMsg,"HEAD")
    Remove-Item $tmpTagMsg -ErrorAction SilentlyContinue
    Run "git" @("push",$Remote,$Version)
  } else {
    Log "Tag $Version déjà présent."
  }
}

# --- Release GitHub ---
StepProgress "Création/maj GitHub Release"

# 1) vérifier le tag sur le remote
$tagRemote = (& git ls-remote --tags $Remote "refs/tags/$Version" 2>$null)
if (-not $tagRemote -or -not $tagRemote.Trim()) {
  Log "Tag $Version absent sur $Remote → push"
  Run "git" @("push",$Remote,"refs/tags/$Version")
} else {
  Log "Tag $Version présent sur $Remote"
}

# 2) notes via fichier pour éviter les soucis de quoting
$tmpNotes = Join-Path $env:TEMP ("notes_{0}.txt" -f ($Version -replace '[^\w\.-]','_'))
$Notes | Out-File -FilePath $tmpNotes -Encoding UTF8 -Force

# 3) créer la release (sans --target si le tag existe côté remote)
$createArgs = @("release","create",$Version,"-R",$Repo,"-t","Inskin $Version","-F",$tmpNotes)
if (-not $tagRemote -or -not $tagRemote.Trim()) { $createArgs += @("--target",$Branch) }
if ($MarkLatest) { $createArgs += "--latest" }
$createArgs += $Signed

# 4) tester existence
$prev=$ErrorActionPreference; $ErrorActionPreference='Continue'
$existingText = (& gh release view $Version -R $Repo 2>&1 | Out-String).Trim()
$ghViewCode = $LASTEXITCODE; $ErrorActionPreference=$prev
if ($existingText) { Log "gh release view → $existingText" }

if ($ghViewCode -ne 0 -or -not $existingText -or ($existingText -match "release not found")) {
  Run "gh" $createArgs
  Log "Release $Version créée."
} else {
  # si déjà créée, upload/clobber + edit notes
  Run "gh" @("release","upload",$Version,$Signed,"-R",$Repo,"--clobber")
  $editArgs = @("release","edit",$Version,"-R",$Repo,"-F",$tmpNotes)
  if ($MarkLatest) { $editArgs += "--latest" }
  Run "gh" $editArgs
  Log "Release $Version mise à jour."
}

Remove-Item $tmpNotes -ErrorAction SilentlyContinue

# --- Vérifications finales ---
StepProgress "Vérifications finales"
Run "git" @("show-ref","--tags","-d")
Run "gh"   @("release","view",$Version,"--repo",$Repo)
if (Test-Path $Signed) {
  $size = (Get-Item $Signed).Length
  Log ("APK final: {0} ({1:N0} bytes)" -f $Signed, $size)
}

StepProgress "Terminé"
$elapsed = (Get-Date) - $script:StartTime
Log ("Durée totale: {0:mm\:ss}" -f $elapsed)
Write-Progress -Activity "Terminé" -Completed
