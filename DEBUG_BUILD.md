# 🔍 Guide de Débogage Build GitHub Actions

## Obtenir les Logs d'Erreur

### Étape 1 : Accéder aux Logs

1. Aller sur : https://github.com/douzils/Inskin/actions
2. Cliquer sur le workflow "Build APK" qui a ❌ failed
3. Cliquer sur "Build Debug APK" (job en rouge)

### Étape 2 : Trouver l'Erreur

Les logs sont organisés par étapes. Chercher :
- ❌ Étapes en rouge
- Lignes commençant par `error:`
- Lignes commençant par `FAILURE:`

### Étape 3 : Erreurs Courantes et Solutions

#### Erreur 1 : Unresolved reference

```
error: unresolved reference: ImplantInfo
```

**Solution** : Import manquant
```kotlin
import com.inskin.app.ImplantInfo
```

#### Erreur 2 : Type mismatch

```
error: type mismatch: inferred type is X but Y was expected  
```

**Solution** : Vérifier les types de données

#### Erreur 3 : Cannot access class

```
error: cannot access 'ClassName': it is internal in 'package'
```

**Solution** : Changer visibility ou déplacer classe

#### Erreur 4 : Duplicate class

```
error: duplicate class found
```

**Solution** : Supprimer déclaration en double

#### Erreur 5 : Gradle sync failed

```
Could not resolve all dependencies
```

**Solution** : Problème de dépendances dans build.gradle.kts

### Étape 4 : Partager les Logs

Copier les 20-30 lignes autour de l'erreur et partager.

## 🔧 Solutions Rapides

### Forcer un rebuild propre

Modifier `.github/workflows/build-apk.yml` :

```yaml
- name: Clean build
  run: ./gradlew clean
  
- name: Build Debug APK
  run: ./gradlew assembleDebug --stacktrace
```

### Augmenter la verbosité

```yaml
- name: Build with logs
  run: ./gradlew assembleDebug --stacktrace --debug
```

### Vérifier la syntaxe Kotlin

```bash
# Localement
./gradlew compileDebugKotlin
```

## 📊 Checklist de Vérification

- [ ] Tous les imports sont corrects
- [ ] Aucune référence cyclique
- [ ] Classes publiques où nécessaire
- [ ] Pas de noms de classes en conflit
- [ ] build.gradle.kts correct
- [ ] AndroidManifest.xml valide

## 🆘 Si Problème Persiste

1. Partager les logs complets
2. Indiquer quelle étape échoue
3. Mentionner les fichiers modifiés récemment

---

**Note** : Les erreurs de build sont normales pendant le développement. GitHub Actions permet de debugger facilement !
