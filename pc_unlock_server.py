#!/usr/bin/env python3
"""
Inskin PC Unlock Server
Écoute les commandes Bluetooth depuis l'application Android Inskin
et déverrouille le PC Windows automatiquement.

Installation:
1. pip install pybluez pywin32
2. Appairer le téléphone Android avec le PC via Bluetooth
3. Lancer ce script en tant qu'administrateur

Usage:
python pc_unlock_server.py

Commandes supportées:
- UNLOCK:<password>   - Déverrouille Windows avec le mot de passe
- TYPE:<password>     - Simule la frappe du mot de passe
- WAKE               - Réveille l'écran
- LOCK               - Verrouille Windows
"""

import bluetooth
import sys
import time
import logging
from threading import Thread

# Windows-specific imports
try:
    import win32api
    import win32con
    import win32security
    import ctypes
    from ctypes import wintypes
    WINDOWS_AVAILABLE = True
except ImportError:
    WINDOWS_AVAILABLE = False
    print("⚠️ PyWin32 non disponible. Les fonctions Windows ne fonctionneront pas.")
    print("   Installez avec: pip install pywin32")

# Configuration
SERVER_NAME = "Inskin PC Unlock"
UUID = "00001101-0000-1000-8000-00805F9B34FB"  # SPP UUID
PORT = 1
AUTHORIZED_DEVICES = []  # Liste des adresses MAC autorisées (vide = tous)

# Setup logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


class WindowsUnlocker:
    """Gère le déverrouillage et le contrôle de Windows"""

    @staticmethod
    def unlock_with_password(password):
        """Déverrouille Windows avec le mot de passe"""
        if not WINDOWS_AVAILABLE:
            logger.error("PyWin32 non disponible")
            return False

        try:
            # Réveiller l'écran d'abord
            WindowsUnlocker.wake_screen()
            time.sleep(0.5)

            # Simuler la frappe du mot de passe
            WindowsUnlocker.type_password(password)
            time.sleep(0.2)

            # Appuyer sur Entrée
            WindowsUnlocker.press_key(win32con.VK_RETURN)

            logger.info("✓ Mot de passe saisi, déverrouillage en cours...")
            return True

        except Exception as e:
            logger.error(f"Erreur lors du déverrouillage: {e}")
            return False

    @staticmethod
    def type_password(password):
        """Simule la frappe du mot de passe"""
        if not WINDOWS_AVAILABLE:
            return False

        for char in password:
            # Conversion caractère -> code clavier
            vk_code = win32api.VkKeyScan(char)

            # Appuyer et relâcher
            win32api.keybd_event(vk_code, 0, 0, 0)  # Key down
            time.sleep(0.05)
            win32api.keybd_event(vk_code, 0, win32con.KEYEVENTF_KEYUP, 0)  # Key up
            time.sleep(0.05)

        return True

    @staticmethod
    def press_key(vk_code):
        """Appuie sur une touche virtuelle"""
        if not WINDOWS_AVAILABLE:
            return

        win32api.keybd_event(vk_code, 0, 0, 0)
        time.sleep(0.05)
        win32api.keybd_event(vk_code, 0, win32con.KEYEVENTF_KEYUP, 0)

    @staticmethod
    def wake_screen():
        """Réveille l'écran du PC"""
        if not WINDOWS_AVAILABLE:
            return False

        try:
            # Empêche la mise en veille
            ctypes.windll.kernel32.SetThreadExecutionState(
                0x80000000 | 0x00000001  # ES_CONTINUOUS | ES_SYSTEM_REQUIRED
            )

            # Bouge la souris légèrement pour réveiller
            import win32gui
            x, y = win32gui.GetCursorPos()
            win32api.SetCursorPos((x + 1, y + 1))
            time.sleep(0.1)
            win32api.SetCursorPos((x, y))

            logger.info("✓ Écran réveillé")
            return True

        except Exception as e:
            logger.error(f"Erreur wake_screen: {e}")
            return False

    @staticmethod
    def lock_windows():
        """Verrouille Windows"""
        if not WINDOWS_AVAILABLE:
            return False

        try:
            ctypes.windll.user32.LockWorkStation()
            logger.info("✓ Windows verrouillé")
            return True
        except Exception as e:
            logger.error(f"Erreur lock_windows: {e}")
            return False

    @staticmethod
    def is_locked():
        """Vérifie si Windows est verrouillé"""
        if not WINDOWS_AVAILABLE:
            return False

        try:
            # Vérifie si le screensaver est actif
            OpenDesktop = ctypes.windll.user32.OpenDesktopW
            CloseDesktop = ctypes.windll.user32.CloseDesktop

            desktop = OpenDesktop("Default", 0, False, 0x0100)
            if desktop:
                CloseDesktop(desktop)
                return False
            return True
        except:
            return False


class BluetoothServer:
    """Serveur Bluetooth pour recevoir les commandes de déverrouillage"""

    def __init__(self):
        self.server_sock = None
        self.running = False

    def start(self):
        """Démarre le serveur Bluetooth"""
        try:
            # Créer le socket Bluetooth
            self.server_sock = bluetooth.BluetoothSocket(bluetooth.RFCOMM)
            self.server_sock.bind(("", PORT))
            self.server_sock.listen(1)

            # Annoncer le service
            bluetooth.advertise_service(
                self.server_sock,
                SERVER_NAME,
                service_id=UUID,
                service_classes=[UUID, bluetooth.SERIAL_PORT_CLASS],
                profiles=[bluetooth.SERIAL_PORT_PROFILE]
            )

            logger.info(f"🔵 Serveur Bluetooth démarré")
            logger.info(f"   Nom: {SERVER_NAME}")
            logger.info(f"   UUID: {UUID}")
            logger.info(f"   En attente de connexions...")

            self.running = True
            self._accept_connections()

        except Exception as e:
            logger.error(f"Erreur démarrage serveur: {e}")
            self.stop()

    def _accept_connections(self):
        """Accepte les connexions entrantes"""
        while self.running:
            try:
                logger.info("⏳ En attente de connexion...")
                client_sock, client_info = self.server_sock.accept()

                logger.info(f"✓ Connexion acceptée de {client_info}")

                # Vérifier si l'appareil est autorisé
                if AUTHORIZED_DEVICES and client_info[0] not in AUTHORIZED_DEVICES:
                    logger.warning(f"⚠️ Appareil non autorisé: {client_info[0]}")
                    client_sock.close()
                    continue

                # Gérer la connexion dans un thread séparé
                thread = Thread(target=self._handle_client, args=(client_sock, client_info))
                thread.daemon = True
                thread.start()

            except Exception as e:
                if self.running:
                    logger.error(f"Erreur acceptation connexion: {e}")

    def _handle_client(self, client_sock, client_info):
        """Gère les commandes d'un client"""
        try:
            logger.info(f"📱 Client connecté: {client_info[0]}")
            client_sock.send(b"OK: Connected to Inskin PC Unlock Server\n")

            while self.running:
                try:
                    # Recevoir les données
                    data = client_sock.recv(1024)
                    if not data:
                        break

                    # Décoder la commande
                    command = data.decode('utf-8').strip()
                    logger.info(f"📥 Commande reçue: {command[:20]}...")  # Masquer le mot de passe

                    # Exécuter la commande
                    response = self._execute_command(command)
                    client_sock.send(response.encode('utf-8') + b"\n")

                except Exception as e:
                    logger.error(f"Erreur traitement commande: {e}")
                    break

        except Exception as e:
            logger.error(f"Erreur client: {e}")
        finally:
            client_sock.close()
            logger.info(f"❌ Client déconnecté: {client_info[0]}")

    def _execute_command(self, command):
        """Exécute une commande reçue"""
        try:
            if command.startswith("UNLOCK:"):
                password = command[7:]
                success = WindowsUnlocker.unlock_with_password(password)
                return "OK: UNLOCKED" if success else "ERROR: Failed to unlock"

            elif command.startswith("TYPE:"):
                password = command[5:]
                success = WindowsUnlocker.type_password(password)
                return "OK: Password typed" if success else "ERROR: Failed to type"

            elif command == "WAKE":
                success = WindowsUnlocker.wake_screen()
                return "OK: Screen awake" if success else "ERROR: Failed to wake"

            elif command == "LOCK":
                success = WindowsUnlocker.lock_windows()
                return "OK: Windows locked" if success else "ERROR: Failed to lock"

            elif command.startswith("CUSTOM:"):
                custom_cmd = command[7:]
                logger.info(f"Commande personnalisée: {custom_cmd}")
                return "OK: Custom command received"

            else:
                logger.warning(f"Commande inconnue: {command}")
                return "ERROR: Unknown command"

        except Exception as e:
            logger.error(f"Erreur exécution commande: {e}")
            return f"ERROR: {str(e)}"

    def stop(self):
        """Arrête le serveur"""
        self.running = False
        if self.server_sock:
            try:
                self.server_sock.close()
            except:
                pass
        logger.info("🔴 Serveur arrêté")


def main():
    """Point d'entrée principal"""
    print("""
╔══════════════════════════════════════════════════════╗
║     Inskin PC Unlock Server                          ║
║     Déverrouillage PC via NFC + Bluetooth            ║
╚══════════════════════════════════════════════════════╝
    """)

    # Vérifications
    if not WINDOWS_AVAILABLE:
        print("⚠️ PyWin32 non disponible. Installation requise:")
        print("   pip install pywin32")
        print("")
        print("Continuer en mode écoute uniquement? (o/n)")
        if input().lower() != 'o':
            sys.exit(1)

    # Vérifier les droits admin (recommandé pour Windows)
    try:
        is_admin = ctypes.windll.shell32.IsUserAnAdmin()
        if not is_admin:
            logger.warning("⚠️ Droits administrateur recommandés pour un fonctionnement optimal")
    except:
        pass

    # Démarrer le serveur
    server = BluetoothServer()

    try:
        server.start()
    except KeyboardInterrupt:
        logger.info("\n⏸️  Arrêt demandé par l'utilisateur")
    except Exception as e:
        logger.error(f"Erreur fatale: {e}")
    finally:
        server.stop()


if __name__ == "__main__":
    main()
