#!/bin/bash

echo "════════════════════════════════════════════════════════════"
echo "  Compilation Inskin v2.0.0 - APK Debug"
echo "════════════════════════════════════════════════════════════"
echo ""

# Vérifier Java
echo "→ Vérification Java..."
if ! command -v java &> /dev/null; then
    echo "❌ Java non trouvé. Installez JDK 17 ou plus."
    exit 1
fi
java -version
echo ""

# Donner permissions
echo "→ Configuration permissions Gradle..."
chmod +x gradlew
echo "✓ Permissions OK"
echo ""

# Nettoyer build précédent
echo "→ Nettoyage build précédent..."
./gradlew clean --no-daemon
echo ""

# Compiler APK
echo "→ Compilation APK Debug (peut prendre 5-10 minutes)..."
./gradlew assembleDebug --no-daemon --stacktrace

# Vérifier résultat
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo ""
    echo "════════════════════════════════════════════════════════════"
    echo "  ✅ COMPILATION RÉUSSIE !"
    echo "════════════════════════════════════════════════════════════"
    echo ""
    echo "📦 APK créé :"
    echo "   app/build/outputs/apk/debug/app-debug.apk"
    echo ""
    ls -lh app/build/outputs/apk/debug/app-debug.apk
    echo ""
    echo "📲 Pour installer :"
    echo "   adb install -r app/build/outputs/apk/debug/app-debug.apk"
    echo ""
else
    echo ""
    echo "════════════════════════════════════════════════════════════"
    echo "  ❌ ÉCHEC DE LA COMPILATION"
    echo "════════════════════════════════════════════════════════════"
    echo ""
    echo "Vérifiez les logs ci-dessus pour les erreurs."
    echo "Problèmes courants :"
    echo "  - Pas de connexion Internet (requis pour télécharger dépendances)"
    echo "  - Version Java incorrecte (nécessite JDK 17)"
    echo "  - Manque de mémoire (augmentez dans gradle.properties)"
    echo ""
    exit 1
fi
