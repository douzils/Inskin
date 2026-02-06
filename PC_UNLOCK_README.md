# 🔓 Inskin PC Unlock - Déverrouillage PC via NFC

Système de déverrouillage automatique de votre PC Windows par scan NFC de votre implant.

## 📋 Prérequis

### Côté PC (Windows)
- Windows 10/11
- Bluetooth activé
- Python 3.7+ installé
- Droits administrateur (recommandé)

### Côté Android
- Application Inskin installée
- Bluetooth activé
- Implant NFC Dangerous Things

## 🚀 Installation PC

### 1. Installer Python
Téléchargez et installez Python depuis [python.org](https://www.python.org/downloads/)

### 2. Installer les dépendances
```bash
pip install pybluez pywin32
```

**Note**: Si `pybluez` échoue, essayez:
```bash
pip install pybluez-win10
```

### 3. Apparier le téléphone
1. Ouvrir **Paramètres Windows** > **Bluetooth et appareils**
2. Activer le Bluetooth
3. Sur votre téléphone Android, activer le Bluetooth
4. Cliquer sur **Ajouter un appareil** sur Windows
5. Sélectionner votre téléphone dans la liste
6. Confirmer le code d'appairage sur les deux appareils

### 4. Lancer le serveur
```bash
python pc_unlock_server.py
```

**Astuce**: Lancer en tant qu'administrateur pour un meilleur fonctionnement:
```bash
# PowerShell en mode admin
python pc_unlock_server.py
```

### 5. Créer un raccourci de démarrage automatique (optionnel)
Pour lancer automatiquement au démarrage de Windows:

1. Créer un fichier `start_unlock_server.bat`:
```batch
@echo off
cd C:\chemin\vers\Inskin
python pc_unlock_server.py
```

2. Copier ce fichier dans le dossier de démarrage:
```
%APPDATA%\Microsoft\Windows\Start Menu\Programs\Startup
```

## 📱 Configuration Android

### 1. Ajouter un PC
1. Ouvrir l'application Inskin
2. Aller dans **Paramètres** > **Déverrouillage PC**
3. Appuyer sur **+ Ajouter un PC**
4. Sélectionner votre PC dans la liste des appareils Bluetooth appairés
5. Donner un nom au PC (ex: "PC Bureau")
6. **(Optionnel)** Entrer votre mot de passe Windows
7. Activer **Nécessite scan NFC** pour sécuriser
8. Appuyer sur **Ajouter**

### 2. Activer le déverrouillage automatique
1. Dans l'écran **Déverrouillage PC**
2. Activer le toggle **Déverrouillage Auto NFC**

### 3. Connecter au PC
1. Appuyer sur **Connecter** sur la carte de votre PC
2. Attendre la confirmation de connexion (point vert)

## 🎯 Utilisation

### Déverrouillage manuel
1. Connecter au PC via l'application
2. Appuyer sur **Déverrouiller**

### Déverrouillage automatique
1. Activer **Déverrouillage Auto NFC**
2. Connecter au PC
3. Scanner votre implant NFC
4. Le PC se déverrouille automatiquement ! 🎉

## 🔒 Sécurité

### Stockage du mot de passe
- Le mot de passe est stocké **localement** sur le téléphone uniquement
- **Jamais** sauvegardé sur le cloud
- Transmis via Bluetooth chiffré uniquement lors du déverrouillage

### Appareils autorisés
Pour restreindre les connexions à des appareils spécifiques, modifier `pc_unlock_server.py`:

```python
# Liste des adresses MAC autorisées
AUTHORIZED_DEVICES = [
    "AA:BB:CC:DD:EE:FF",  # Votre téléphone
    "11:22:33:44:55:66"   # Autre appareil
]
```

### Désactiver le scan NFC requis
Si vous voulez déverrouiller sans scanner l'implant:
1. Éditer la configuration du PC dans l'app
2. Désactiver **Nécessite scan NFC**

⚠️ **Attention**: Moins sécurisé, le téléphone peut déverrouiller sans confirmation physique

## 🛠️ Dépannage

### Le serveur ne démarre pas
- Vérifier que Python est installé: `python --version`
- Vérifier les dépendances: `pip list | grep pybluez`
- Lancer en tant qu'administrateur

### Le téléphone ne se connecte pas
- Vérifier l'appairage Bluetooth Windows
- Redémarrer le serveur Python
- Vérifier que le Bluetooth est activé sur les deux appareils

### Le mot de passe ne fonctionne pas
- Vérifier que le serveur a les droits administrateur
- Tester avec la commande `TYPE:` au lieu de `UNLOCK:`
- Vérifier que le clavier est bien en disposition française/US

### Erreur "PyWin32 non disponible"
```bash
pip install --upgrade pywin32
python -m pywin32_postinstall -install
```

## 📊 Statistiques

L'application garde un historique:
- Nombre de déverrouillages par PC
- Date du dernier déverrouillage
- Nombre total d'utilisations

## 🔧 Commandes avancées

Le protocole supporte plusieurs commandes:

### Via l'application
- **Déverrouiller**: Envoie le mot de passe et appuie sur Entrée
- **Réveiller**: Réveille l'écran sans déverrouiller
- **Verrouiller**: Verrouille Windows à distance

### Via Bluetooth direct (développeurs)
```
UNLOCK:<password>   # Déverrouille avec mot de passe
TYPE:<password>     # Tape le mot de passe (sans Entrée)
WAKE                # Réveille l'écran
LOCK                # Verrouille Windows
CUSTOM:<cmd>        # Commande personnalisée
```

## 🌐 Compatibilité

### Systèmes testés
- ✅ Windows 10 (1903+)
- ✅ Windows 11
- ❌ Linux (non supporté actuellement)
- ❌ macOS (non supporté actuellement)

### Implants testés
- ✅ xNT (NFC Type 2)
- ✅ NExT (NFC + RFID)
- ✅ FlexNT
- ✅ VivoKey Spark

## 📝 Changelog

### Version 1.0 (2025-02-06)
- Déverrouillage automatique par NFC
- Support Bluetooth
- Interface de configuration
- Gestion multi-PC
- Statistiques d'utilisation

## 🤝 Support

Pour toute question ou problème:
1. Vérifier cette documentation
2. Consulter les logs du serveur Python
3. Ouvrir une issue sur GitHub

## 📜 Licence

Voir LICENSE dans le projet principal Inskin.

---

**Fait avec ❤️ pour la communauté Dangerous Things**
