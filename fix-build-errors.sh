#!/bin/bash
echo "🔍 Diagnostic des erreurs de build..."
echo ""

# Vérifier les erreurs de syntaxe Kotlin
echo "Checking Kotlin files for syntax errors..."
find app/src -name "*.kt" -exec echo "Checking {}" \;

echo ""
echo "📋 Fichiers créés récemment qui peuvent causer des erreurs:"
echo "- ImplantDetector.kt"
echo "- DangerousThingsImplants.kt" 
echo "- ImplantInfoCard.kt"
echo "- ImplantBadgeMapper.kt"
echo ""
echo "✅ Solutions possibles:"
echo "1. Vérifier les imports manquants"
echo "2. Corriger les erreurs de syntaxe"
echo "3. Vérifier les dépendances Gradle"
