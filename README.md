# Inskin

Application Android pour lecture, écriture, émulation et organisation de tags NFC/RFID et implants. Conçue en **Kotlin + Jetpack Compose**, avec une architecture modulaire et extensible (Flows, périphériques externes type Proxmark, etc.).

---

## Sommaire

* [Fonctionnalités](#fonctionnalités)
* [Architecture](#architecture)
* [Écrans](#écrans)
* [Prérequis](#prérequis)
* [Installation](#installation)
* [Compilation](#compilation)
* [Exécution](#exécution)
* [Compatibilité NFC](#compatibilité-nfc)
* [Roadmap](#roadmap)
* [Contribuer](#contribuer)
* [Licence](#licence)

---

## Fonctionnalités

* **Accueil avec 5 actions principales**

    * Scanner un tag
    * Historique des scans
    * Émulation de tags
    * Flows (système de nœuds automatisés)
    * Paramètres (mode sombre, options diverses)
    * Périphériques externes (Proxmark, etc. – en cours de développement)

* **Page Tag**

    * Chevron retour
    * Accès rapide à l’historique
    * Actions: Écriture, Émulation, Dump
    * Dump: enregistrer, copier, formater, lecture brute, commandes
    * Flows: rattacher un workflow au tag
    * Cercle principal: attribution d’une icône au tag via appui long
    * Icône cadenas: gestion des états de verrouillage/déverrouillage et mots de passe

* **Historique**

    * Liste complète des tags scannés, triés par date
    * UID, type de techno, contenu NDEF éventuel

* **Émulation**

    * Sélection d’un tag scanné pour l’émuler
    * Lancement et arrêt de l’émulation

* **Écriture**

    * Écriture d’NDEF (texte, URL, MIME, vCard, etc.)
    * Gestion des formats

* **Flows**

    * Système de nœuds visuel pour définir des comportements:

        * Exemple: « si tag UID=XXXX alors envoyer un message / ouvrir une app »

* **Périphériques**

    * Support prévu pour Proxmark3 et autres lecteurs externes (USB, BLE, TCP)
    * Interface dédiée par périphérique

---

## Architecture

* **Langage**: Kotlin
* **UI**: Jetpack Compose (Material3, thèmes clair/sombre)
* **Navigation**: Jetpack Navigation Compose
* **Persistance**: Room (historique, préférences, icônes, mots de passe)
* **Sécurité**: EncryptedSharedPreferences pour stockage de clés/mots de passe
* **Périphériques**: interfaces extensibles (USB/BLE/TCP)

---

## Écrans

### Accueil

* Scanner
* Historique
* Émulation
* Flows
* Paramètres
* Périphériques

### Page Tag

* Chevron retour
* Bouton Historique
* Actions: Écriture, Émulation, Dump
* Cercle principal = attribution d’icône
* Icône cadenas = gestion lock/password
* Flows rattachés

### Historique

* Liste chronologique des tags scannés

### Émulation

* Choix d’un tag enregistré
* Bouton démarrer/arrêter

### Écriture

* Formulaire NDEF
* Support multi-enregistrements

### Flows

* Éditeur nodal (déclencheurs, actions, conditions)

### Périphériques

* Liste des périphériques externes configurés
* Page spécifique par type

---

## Prérequis

* Android Studio Ladybug+
* JDK 17
* Téléphone Android avec NFC activé
* (optionnel) Proxmark3 ou périphérique externe pour tests avancés

---

## Installation

```bash
git clone https://github.com/douzils/Inskin.git
cd Inskin
```

Ouvrir dans Android Studio, puis **Sync Gradle**.

---

## Compilation

Depuis Android Studio:

* `Build > Make Project`
* Lancer sur un appareil physique NFC (l’émulateur Android **ne supporte pas** le NFC réel).

Depuis la ligne de commande:

```bash
./gradlew clean assembleDebug
```

---

## Exécution

* Activez le NFC sur votre téléphone
* Installez l’APK debug:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

* Approchez un tag: affichage UID + infos NDEF

---

## Compatibilité NFC

* **Technos supportées**:

    * NfcA, NfcB, NfcF, NfcV
    * IsoDep
    * Ndef / NdefFormatable
    * (optionnel) MifareClassic, si le chipset de l’appareil le permet

* **Écriture**: NDEF texte, URL, MIME, vCard

* **Émulation**: basique, limitée aux tags supportés par Android HCE

---

## Roadmap

* Écriture avancée multi-enregistrements
* Dump hex complet avec sauvegarde/export
* Historique persistant avec export/import
* Flows visuels (éditeur nodal complet)
* Support périphériques externes (Proxmark3)
* Publication sur Play Store (AAB signé)

---

## Contribuer

1. Fork
2. Créez une branche `feat/ma-feature`
3. Push et PR avec description claire
4. Ajoutez captures si c’est de l’UI

---
