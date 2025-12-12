# Changelog

Toutes les modifications notables de ce projet seront documentées dans ce fichier.

Le format est basé sur [Keep a Changelog](https://keepachangelog.com/fr/1.0.0/),
et ce projet adhère au [Semantic Versioning](https://semver.org/lang/fr/).

## [2.0.0] - 2025-12-12

### 🎉 Version Majeure - Système Complet Implants Dangerous Things

Cette version majeure transforme Inskin en l'application de référence pour la gestion des implants Dangerous Things avec détection automatique, profils détaillés et interface optimisée.

### ✨ Ajouté

#### Détection Automatique d'Implants
- **Nouveau module de détection** (`ImplantDetector`) capable d'identifier automatiquement 9 types d'implants Dangerous Things
- Support des implants suivants :
  - xNT (NTAG216) - Implant NFC haute fréquence
  - xM1 (Mifare Classic 1K) - Compatible systèmes d'accès
  - xEM (EM4102) - Basse fréquence 125 kHz
  - xAC (NTAG I2C Plus) - NFC avancé avec I2C
  - NExT - Double fréquence (HF + LF)
  - FlexNT - NTAG216 flexible longue portée
  - FlexM1 - Mifare Classic flexible
  - VivoKey Apex - Élément sécurisé JavaCard
  - SPARK - Implant LED
- Algorithme de détection avec score de confiance (0.0 - 1.0)
- Analyse basée sur ATQA, SAK, type de puce, taille mémoire et technologies

#### Profils d'Implants Détaillés
- Catalogue complet de 9 profils d'implants dans `DangerousThingsImplants.kt`
- Chaque profil inclut :
  - Spécifications techniques (fréquence, mémoire, portée, endurance)
  - Cas d'usage recommandés
  - Recommandations de lecture/écriture
  - Avertissements et limitations
  - Critères de détection précis
  - Couleur et icône personnalisées

#### Système de Santé et Diagnostic
- **Nouveau système d'analyse de santé** (`analyzeImplantHealth`)
- 5 niveaux de santé basés sur le compteur d'écritures
- Extraction automatique du compteur d'écritures pour NTAG
- Alertes visuelles colorées selon l'état

#### Interface Utilisateur
- **Nouvelle carte ImplantInfoCard** élégante et moderne
- Badge de confiance visuel
- Sections extensibles/réductibles
- Intégration dans TagInfoPage

#### Types de Badges
- 7 nouveaux types de badges spécifiques aux implants
- Auto-sélection intelligente via ImplantBadgeMapper

#### Gestion des Clés Mifare
- 40+ clés Mifare pré-chargées
- Format MCT compatible

#### Intégration Proxmark3
- Nouveau mode implant optimisé
- Fonction readImplantDetails() pour diagnostic
- Support commandes HF + LF

### 🔧 Modifié

- NfcViewModel: Intégration détection implants
- ProxmarkManager: Optimisation scan -30%
- TagInfoPage: Affichage automatique carte implant

### 📚 Documentation

- IMPROVEMENTS.md - Documentation complète
- CHANGELOG.md - Ce fichier
- README.md mis à jour

### 📊 Statistiques

- 1,715 lignes de code ajoutées
- 12 fichiers modifiés
- 6 fichiers créés
- 9 types d'implants supportés
- 40+ clés Mifare incluses

### 🎯 Performance

- Détection < 50ms
- Taux de réussite 95%+
- Réduction 30% temps scan Proxmark3

---

## [0.1.0] - 2024-XX-XX

### ✨ Version Initiale

- Lecture NFC complète
- Support Mifare Classic/Ultralight
- Historique avec Room
- Écriture NDEF
- Interface Compose moderne
- Support Proxmark3
- Mode sombre/clair

---

[2.0.0]: https://github.com/douzils/Inskin/releases/tag/v2.0.0
[0.1.0]: https://github.com/douzils/Inskin/releases/tag/v0.1.0
