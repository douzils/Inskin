# Compilation Android sur Téléphone

## ⚠️ Avertissement

Compiler un projet Android sur téléphone est **possible** mais :
- ⏱️ **Très lent** (1-2 heures vs 5 minutes sur PC)
- 🔋 **Consomme beaucoup de batterie**
- 💾 **Nécessite 4-8 GB d'espace libre**
- 🔥 **Chauffe beaucoup le téléphone**
- ❌ **Peut échouer par manque de RAM**

## 📱 Option 1 : Termux (Recommandée si pas de PC)

### Prérequis
- Téléphone Android 7.0+
- 4 GB RAM minimum (8 GB recommandé)
- 8 GB espace libre minimum
- Batterie chargée à 80%+
- Mode économie d'énergie désactivé

### Installation

1. **Installer Termux**
   - Depuis F-Droid : https://f-droid.org/packages/com.termux/
   - ⚠️ NE PAS utiliser Play Store (version obsolète)

2. **Installer les dépendances**
   ```bash
   # Mise à jour des packages
   pkg update && pkg upgrade -y
   
   # Installer Git
   pkg install git -y
   
   # Installer OpenJDK 17
   pkg install openjdk-17 -y
   
   # Vérifier Java
   java -version
   # Devrait afficher "openjdk version 17..."
   ```

3. **Augmenter la limite de mémoire**
   ```bash
   # Créer fichier gradle.properties dans Termux home
   cd ~
   mkdir -p .gradle
   cat > .gradle/gradle.properties << 'EOG'
   org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m
   org.gradle.daemon=false
   org.gradle.parallel=false
   EOG
   ```

4. **Cloner le projet**
   ```bash
   cd ~/storage/shared  # Pour accéder au stockage partagé
   # ou
   cd ~
   
   git clone https://github.com/douzils/Inskin.git
   cd Inskin
   git checkout claude/android-nfc-implant-app-01LNUC55ZH73xDWSZmrcZBHp
   ```

5. **Compiler**
   ```bash
   # Donner permissions
   chmod +x gradlew
   
   # Compiler (TRÈS LONG - 1-2 heures)
   ./gradlew assembleDebug --no-daemon
   
   # Si erreur de mémoire, essayer :
   ./gradlew assembleDebug --no-daemon --max-workers=1
   ```

6. **Localiser l'APK**
   ```bash
   ls -lh app/build/outputs/apk/debug/app-debug.apk
   
   # Copier vers stockage partagé
   cp app/build/outputs/apk/debug/app-debug.apk \
      ~/storage/shared/Download/inskin-v2.0.0.apk
   ```

### Problèmes Courants

**Erreur : Out of Memory**
```bash
# Réduire encore plus
export GRADLE_OPTS="-Xmx1536m"
./gradlew assembleDebug --no-daemon --max-workers=1
```

**Erreur : Permission denied**
```bash
chmod +x gradlew
termux-setup-storage  # Autoriser accès stockage
```

**Téléphone trop chaud**
- Arrêter la compilation
- Laisser refroidir 30 minutes
- Réessayer avec écran éteint

## 📱 Option 2 : AIDE (Android IDE)

**Application** : AIDE - Android IDE
**Disponibilité** : Play Store (version payante pour builds complets)

⚠️ Limitations :
- Version gratuite limitée
- Ne supporte pas tous les plugins Gradle
- Peut ne pas compiler Inskin (projet complexe)

## 📱 Option 3 : Spck Editor

**Application** : Spck Editor
**Disponibilité** : Play Store

⚠️ Limitations :
- Éditeur uniquement
- Pas de compilation native Android
- OK pour éditer le code seulement

## ☁️ Option 4 : GitHub Actions (RECOMMANDÉE)

**La meilleure solution** : Compiler dans le cloud

### Configuration

1. **Créer workflow GitHub Actions**

Créez `.github/workflows/build.yml` :

```yaml
name: Build APK

on:
  push:
    branches: [ claude/android-nfc-implant-app-* ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
        
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
      
    - name: Build Debug APK
      run: ./gradlew assembleDebug
      
    - name: Upload APK
      uses: actions/upload-artifact@v3
      with:
        name: inskin-v2.0.0-debug
        path: app/build/outputs/apk/debug/app-debug.apk
```

2. **Utilisation**
   - Push le code sur GitHub
   - Aller sur "Actions" dans le repo
   - Lancer le workflow
   - Télécharger l'APK depuis "Artifacts"

**Avantages** :
- ✅ Gratuit pour repos publics
- ✅ Rapide (5-10 minutes)
- ✅ Pas de consommation de ressources locales
- ✅ Toujours le même environnement

## 🌐 Option 5 : Services Cloud Build

### Appetize.io
- Build dans le cloud
- Test en ligne
- Payant après essai gratuit

### Codemagic
- CI/CD pour Flutter/Android
- Gratuit pour projets open source
- Configuration nécessaire

### Bitrise
- CI/CD mobile
- Plan gratuit limité
- Configuration nécessaire

## 📊 Comparaison

| Méthode | Temps | Difficulté | Coût | Recommandé |
|---------|-------|------------|------|------------|
| PC/Mac | 5-10 min | Facile | Gratuit | ⭐⭐⭐⭐⭐ |
| GitHub Actions | 5-10 min | Facile | Gratuit | ⭐⭐⭐⭐⭐ |
| Termux | 1-2h | Difficile | Gratuit | ⭐⭐ |
| AIDE | 30-60 min | Moyen | Payant | ⭐⭐ |
| Cloud Build | 5-15 min | Moyen | Payant | ⭐⭐⭐ |

## 🎯 Recommandation

**Si vous n'avez PAS de PC :**
1. ⭐ **GitHub Actions** (meilleure solution)
2. Termux (si pas d'autre choix)

**Si vous avez un PC :**
- Utilisez Android Studio (beaucoup plus simple)

## 💡 Astuce Rapide

Pour **tester sans compiler** :
1. Fork le repo sur GitHub
2. Activez GitHub Actions
3. L'APK sera compilé automatiquement
4. Téléchargez depuis Artifacts

---

**Note** : Pour Inskin v2.0.0 spécifiquement, GitHub Actions est la meilleure option si vous n'avez pas de PC.
