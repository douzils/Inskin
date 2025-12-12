# Instructions de Compilation - Inskin v2.0.0

## 🛠️ Prérequis

- **Android Studio** Ladybug ou plus récent
- **JDK 17**
- **Connexion Internet** (pour la première compilation)
- **Appareil Android** avec NFC ou émulateur

## 📦 Compilation de l'APK

### Méthode 1: Via Android Studio (Recommandée)

1. **Ouvrir le projet**
   ```bash
   cd /home/user/Inskin
   ```
   - Ouvrir Android Studio
   - File → Open → Sélectionner le dossier Inskin

2. **Sync Gradle**
   - Attendre que Android Studio télécharge les dépendances
   - Sync Gradle Files (bouton en haut)

3. **Compiler l'APK Debug**
   - Build → Build Bundle(s) / APK(s) → Build APK(s)
   - Ou: `./gradlew assembleDebug`
   
4. **Localiser l'APK**
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

### Méthode 2: Ligne de Commande

```bash
cd /home/user/Inskin

# APK Debug (non signé)
./gradlew clean assembleDebug

# APK sera dans:
# app/build/outputs/apk/debug/app-debug.apk
```

### Méthode 3: APK Release Signé

1. **Créer un keystore** (une seule fois):
   ```bash
   keytool -genkey -v -keystore inskin-release.keystore \
     -alias inskin \
     -keyalg RSA \
     -keysize 2048 \
     -validity 10000
   ```

2. **Créer keystore.properties**:
   ```properties
   storeFile=inskin-release.keystore
   storePassword=VOTRE_MOT_DE_PASSE
   keyAlias=inskin
   keyPassword=VOTRE_MOT_DE_PASSE
   ```

3. **Modifier build.gradle.kts** (optionnel):
   Ajouter la configuration de signature si nécessaire

4. **Compiler**:
   ```bash
   ./gradlew assembleRelease
   
   # APK dans:
   # app/build/outputs/apk/release/app-release.apk
   ```

## 📲 Installation

### Sur Téléphone Physique

1. **Activer le mode développeur**:
   - Paramètres → À propos → Appuyer 7x sur "Numéro de build"

2. **Activer l'installation USB**:
   - Paramètres → Développeur → Installation via USB

3. **Installer via ADB**:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

### Depuis le Téléphone

1. **Copier l'APK** sur le téléphone
2. **Activer sources inconnues**:
   - Paramètres → Sécurité → Sources inconnues
3. **Ouvrir l'APK** et installer

## ⚠️ Problèmes Courants

### Erreur: Gradle non trouvé
```bash
# Télécharger Gradle wrapper
gradle wrapper --gradle-version 8.13
```

### Erreur: Connexion réseau
- Vérifier votre connexion Internet
- Configurer proxy si nécessaire
- Utiliser Android Studio qui gère mieux les dépendances

### Erreur: JDK non trouvé
```bash
# Vérifier Java
java -version

# Doit afficher Java 17 ou plus
```

### Erreur: SDK Android
```bash
# Définir ANDROID_HOME
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools
```

## 🎯 Taille APK Attendue

- **Debug APK**: ~15-20 MB
- **Release APK**: ~8-12 MB (avec minification)

## 🔍 Vérifier l'APK

```bash
# Info sur l'APK
aapt dump badging app/build/outputs/apk/debug/app-debug.apk

# Vérifier signature
jarsigner -verify -verbose app/build/outputs/apk/release/app-release.apk
```

## 📊 Contenu de l'APK v2.0.0

### Nouvelles Fonctionnalités Incluses

- ✅ Détection automatique de 9 implants DT
- ✅ Profils détaillés par implant
- ✅ Système de santé (compteur d'écritures)
- ✅ Interface ImplantInfoCard
- ✅ 40+ clés Mifare pré-chargées
- ✅ Mode Proxmark3 optimisé
- ✅ Auto-sélection de badges

### Dépendances

- Compose BOM 2024.10.01
- Room 2.6.1
- USB Serial 3.7.0
- Material3

## 🚀 Test Rapide

Après installation:

1. Ouvrir l'application
2. Activer NFC sur le téléphone
3. Approcher un tag NFC
4. Vérifier que la détection fonctionne
5. Si implant DT → carte d'info devrait s'afficher

## 📞 Support Compilation

Si problèmes persistent:
- Vérifier les logs: `./gradlew assembleDebug --stacktrace`
- Issues GitHub: https://github.com/douzils/Inskin/issues
- Inclure les logs d'erreur complets

---

**Bonne compilation ! 🛠️**
