param(
  [Parameter(Mandatory=$true)] [string]$Version,               # ex: v0.0.2
  [string]$Notes = "Pre-release auto",
  [Parameter(Mandatory=$true)] [string]$Repo,                  # ex: douzils/Inskin
  [string]$KeystorePath = "$env:USERPROFILE\.android\debug.keystore",  # par défaut: debug keystore
  [string]$KeystoreAlias = "androiddebugkey",
  [string]$KeystorePass = "android",
  [string]$KeyPass      = "android",
  [switch]$UseWrapper = $false                                 # true => .\gradlew ; false => gradle
)

$ErrorActionPreference = "Stop"

function Fail($msg){ Write-Error $msg; exit 1 }

Write-Host "== Pré-requis: Java 17, Android SDK, gh CLI, Git ==" -ForegroundColor DarkGray

# 0) Dossiers & chemins
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $Root

# 1) Résoudre Android SDK build-tools (zipalign & apksigner)
$Sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } elseif ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } else { "$env:LOCALAPPDATA\Android\Sdk" }
$Bt  = (Get-ChildItem "$Sdk\build-tools" -Directory | Sort-Object Name -Descending | Select-Object -First 1 -ErrorAction Stop).FullName
$Zipalign  = Join-Path $Bt "zipalign.exe"
$ApkSigner = Join-Path $Bt "apksigner.bat"
if (!(Test-Path $Zipalign)) { Fail "zipalign introuvable: $Zipalign" }
if (!(Test-Path $ApkSigner)) { Fail "apksigner introuvable: $ApkSigner" }

# 2) Build (release)
$GradleCmd = if ($UseWrapper -and (Test-Path ".\gradlew.bat")) { ".\gradlew.bat" } else { "gradle" }
& $GradleCmd clean :app:assembleRelease
if ($LASTEXITCODE -ne 0) { Fail "Echec build Gradle" }

# 3) Fichiers APK
$Unsigned = "app/build/outputs/apk/release/app-release-unsigned.apk"
$Aligned  = "app/build/outputs/apk/release/app-release-aligned.apk"
$Signed   = "app/build/outputs/apk/release/app-$Version-signed.apk"
if (!(Test-Path $Unsigned)) { Fail "APK unsigned introuvable: $Unsigned" }

# 4) zipalign + sign + verify
& $Zipalign -f -p 4 $Unsigned $Aligned
& $ApkSigner sign `
  --ks "$KeystorePath" `
  --ks-key-alias "$KeystoreAlias" `
  --ks-pass "pass:$KeystorePass" `
  --key-pass "pass:$KeyPass" `
  --out "$Signed" "$Aligned"
if ($LASTEXITCODE -ne 0) { Fail "Signature échouée" }

& $ApkSigner verify --verbose "$Signed"
if ($LASTEXITCODE -ne 0) { Fail "Vérification signature échouée" }

Write-Host "APK signé: $Signed" -ForegroundColor Green

# 5) Créer/mettre à jour la PRE-RELEASE GitHub
#    - si la release existe, on met à jour l’asset (--clobber)
#    - sinon, on crée la release pré-release
# Crée la release si absente
$existing = (gh release view $Version --repo $Repo 2>$null)
if (-not $existing) {
  gh release create $Version `
    --repo $Repo `
    --title "Inskin $Version" `
    --notes "$Notes" `
    --prerelease `
    "$Signed" | Out-Null
  if ($LASTEXITCODE -ne 0) { Fail "Création pre-release échouée" }
  Write-Host "Pre-release $Version créée." -ForegroundColor Green
} else {
  # Met à jour l’asset
  gh release upload $Version "$Signed" --repo $Repo --clobber | Out-Null
  if ($LASTEXITCODE -ne 0) { Fail "Upload asset échoué" }
  # Assure le flag prerelease + notes
  gh release edit $Version --repo $Repo --prerelease --notes "$Notes" | Out-Null
  Write-Host "Pre-release $Version mise à jour." -ForegroundColor Green
}

Write-Host "✅ Terminé." -ForegroundColor Cyan
