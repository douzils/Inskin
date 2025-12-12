# Guide de Release v2.0.0

Ce guide vous explique comment créer une release propre de l'application Inskin v2.0.0.

## ✅ Étapes Complétées

- [x] Version bumped à 2.0.0 dans `app/build.gradle.kts`
- [x] CHANGELOG.md créé avec historique complet
- [x] RELEASE_NOTES.md créé pour GitHub
- [x] Commit créé et poussé vers la branche
- [x] Tag v2.0.0 créé localement

## 📦 Étapes de Release

### 1. Compiler l'APK de Release

#### Option A: Sans signature (Debug)
```bash
./gradlew assembleDebug
```
L'APK sera dans: `app/build/outputs/apk/debug/app-debug.apk`

#### Option B: Avec signature (Production)

**Créer un keystore** (première fois seulement):
```bash
keytool -genkey -v -keystore inskin-release.keystore \
  -alias inskin -keyalg RSA -keysize 2048 -validity 10000
```

**Configurer la signature** dans `app/build.gradle.kts`:
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../inskin-release.keystore")
            storePassword = "YOUR_STORE_PASSWORD"
            keyAlias = "inskin"
            keyPassword = "YOUR_KEY_PASSWORD"
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            // ... reste de la config
        }
    }
}
```

**Compiler**:
```bash
./gradlew assembleRelease
```
L'APK sera dans: `app/build/outputs/apk/release/app-release.apk`

### 2. Créer une Pull Request

1. Aller sur GitHub: https://github.com/douzils/Inskin
2. Créer une PR depuis la branche: `claude/android-nfc-implant-app-01LNUC55ZH73xDWSZmrcZBHp`
3. Titre: "Release v2.0.0 - Système Complet Implants Dangerous Things"
4. Description: Copier le contenu de `RELEASE_NOTES.md`
5. Merger la PR dans la branche principale

### 3. Créer le Tag sur GitHub

**Option A: Via Interface Web**

1. Aller sur: https://github.com/douzils/Inskin/releases/new
2. Tag version: `v2.0.0`
3. Target: Sélectionner la branche principale (après merge de la PR)
4. Release title: `v2.0.0 - Système Complet Implants Dangerous Things`
5. Description: Copier le contenu de `RELEASE_NOTES.md`
6. Attacher l'APK: `app-release.apk` ou `app-debug.apk`
7. Cocher "Set as the latest release"
8. Publier la release

**Option B: Via Ligne de Commande** (si permissions disponibles)

```bash
# Pousser le tag (si pas déjà fait)
git push origin v2.0.0

# Créer la release avec gh CLI
gh release create v2.0.0 \
  --title "v2.0.0 - Système Complet Implants Dangerous Things" \
  --notes-file RELEASE_NOTES.md \
  app/build/outputs/apk/release/app-release.apk
```

### 4. Vérifications Post-Release

- [ ] Tag v2.0.0 visible sur GitHub
- [ ] Release publiée avec notes complètes
- [ ] APK téléchargeable
- [ ] CHANGELOG.md à jour dans le repo
- [ ] README.md mis à jour avec lien vers release

### 5. Communication

Annoncer la release sur:
- [ ] README.md (badge de version)
- [ ] Forum Dangerous Things
- [ ] Réseaux sociaux (si applicable)

## 📝 Fichiers de Release Créés

```
📁 Inskin/
├── CHANGELOG.md           # Historique des versions
├── RELEASE_NOTES.md       # Notes de la release v2.0.0
├── IMPROVEMENTS.md        # Documentation complète (400+ lignes)
├── RELEASE_GUIDE.md       # Ce fichier
└── app/build.gradle.kts   # Version: 2.0.0 (versionCode: 2)
```

## 🎯 Checklist Finale

### Avant Publication
- [ ] Tests manuels sur appareil physique
- [ ] Vérification détection implants (si possible)
- [ ] Test lecture NFC standard
- [ ] Test historique et persistance
- [ ] Test mode Proxmark3 (si disponible)
- [ ] Vérification crash logs

### Documentation
- [ ] CHANGELOG.md complet
- [ ] RELEASE_NOTES.md complet
- [ ] IMPROVEMENTS.md à jour
- [ ] README.md mis à jour

### Git & GitHub
- [ ] Branche mergée
- [ ] Tag créé
- [ ] Release publiée
- [ ] APK disponible

## 🔒 Sécurité Keystore

⚠️ **IMPORTANT**: 
- **NE JAMAIS** commiter le fichier `.keystore` dans Git
- **NE JAMAIS** commiter les mots de passe
- Ajouter à `.gitignore`:
  ```
  *.keystore
  *.jks
  keystore.properties
  ```
- Sauvegarder le keystore dans un endroit sûr
- Documenter les mots de passe de façon sécurisée

## 📊 Métriques de la Release

- **Version**: 0.1.0 → 2.0.0
- **VersionCode**: 1 → 2
- **Lignes ajoutées**: 1,715
- **Fichiers créés**: 6
- **Fichiers modifiés**: 12
- **Implants supportés**: 9
- **Clés Mifare**: 40+

## 🎉 Après la Release

1. Créer une branche `develop` pour futures fonctionnalités
2. Planifier v2.1.0 avec roadmap
3. Monitorer les issues GitHub
4. Collecter feedback utilisateurs
5. Planifier prochaines améliorations

## 📞 Support

Questions ou problèmes:
- GitHub Issues: https://github.com/douzils/Inskin/issues
- Forum DT: https://forum.dangerousthings.com

---

**Bonne release ! 🚀**
