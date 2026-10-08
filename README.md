# Carnet de notes géolocalisées

Application Android native (Kotlin, Jetpack Compose) : un carnet de notes qui enregistre,
pour chaque note, un titre, un contenu, une photo, la date et le lieu où elle a été écrite.

## Fonctionnalités

- Ajouter une note (id, titre, contenu, photo, date, position de l'utilisateur)
- Lister les notes, avec recherche
- Voir le détail d'une note
- Carte avec les notes

## Stack technique

| Besoin | Choix |
|---|---|
| UI | Jetpack Compose, Material 3 |
| Navigation | Navigation Compose |
| Carte | osmdroid (OpenStreetMap, sans clé API) |
| Images | Coil |
| Localisation | `LocationManager` (sans Google Play Services) |
| Stockage | Fichier JSON + photos dans le stockage interne |

## Structure

```
app/src/main/java/com/ynov/helloworld/
├── MainActivity.kt        Point d'entrée et navigation
├── NotesViewModel.kt      État de l'application
├── data/                  Modèle Note, persistance et préférences
├── location/              Permissions et récupération de la position
└── ui/                    Écrans, composants et thème
```

## Lancer le projet

Ouvrir le dossier dans Android Studio, puis **Run**. Sur émulateur, définir une position
dans *Extended controls → Location* avant de créer une note.

### Tester les performances

La variante `debug` est volontairement lente (Compose non optimisé, code interprété).
Pour juger de la fluidité réelle, utiliser la variante **`benchmark`** : mêmes optimisations
que la release (R8), mais signée avec la clé de debug pour s'installer directement.

*Build → Select Build Variant… → app : `benchmark`*, puis **Run**.

### Régler l'émulateur

Par défaut (*Graphics : Automatic*), l'émulateur peut se rabattre sur un rendu **logiciel**
(SwiftShader) : chaque image est alors calculée par le processeur et tout rame, la carte en
particulier. Pour vérifier :

```bash
adb shell dumpsys SurfaceFlinger | grep GLES
```

Si la ligne mentionne *SwiftShader*, dans *Device Manager → ✏️ Edit → Show Advanced Settings* :

| Réglage | Valeur conseillée |
|---|---|
| Graphics acceleration | **Hardware** (GPU de la machine) |
| RAM | 3 Go (2 Go est juste avec le Play Store) |
| Boot | *Cold boot* une fois après le changement |

Mesures sur un i5-1035G7 (Iris Plus), introduction balayée au doigt :

| Configuration | Démarrage à froid | Images saccadées |
|---|---|---|
| GPU, debug | ~9 s | 38 % |
| GPU, `benchmark` | ~2 s | 15 % |

## Tests

| Type | Emplacement | Outils | Lancement |
|---|---|---|---|
| Unitaires et écrans | `app/src/test` | JUnit 4, Robolectric, Compose UI Test | `./gradlew testDebugUnitTest` |
| Bout en bout | `app/src/androidTest` | Compose UI Test, AndroidX Test | `./gradlew connectedDebugAndroidTest` (appareil requis) |

Les tests unitaires tournent sur la JVM, sans émulateur, grâce à Robolectric :

- **Modèle et formatage** : localisation d'une note, dates, coordonnées (affichées et vocalisées).
- **Stockage** : aller-retour JSON, absence de fichier temporaire, redimensionnement des photos.
- **ViewModel** : chargement trié, ajout, suppression avec la photo, ajout pendant le chargement.
- **Écrans** : liste (vide, recherche, navigation), création (validation du titre),
  détail (confirmation de suppression), introduction (navigation, petit écran, texte agrandi).

Le rapport HTML est généré dans `app/build/reports/tests/testDebugUnitTest/index.html`.

---

## Versioning

### Numéro de version — Semantic Versioning

La version suit le format **MAJEUR.MINEUR.CORRECTIF** ([semver.org](https://semver.org/lang/fr/)) :

| Incrément | Quand | Exemple |
|---|---|---|
| MAJEUR | Changement incompatible (ex. format de stockage modifié) | 1.4.2 → 2.0.0 |
| MINEUR | Nouvelle fonctionnalité rétrocompatible | 1.4.2 → 1.5.0 |
| CORRECTIF | Correction de bug | 1.4.2 → 1.4.3 |

La source unique de vérité est [`version.properties`](version.properties).
Gradle en déduit automatiquement :

- `versionName` = `MAJEUR.MINEUR.CORRECTIF` (suffixé `-debug` en debug) ;
- `versionCode` = `MAJEUR × 10000 + MINEUR × 100 + CORRECTIF` (toujours croissant).

```bash
./gradlew printVersion   # affiche la version courante
./gradlew bumpPatch      # 1.0.0 -> 1.0.1
./gradlew bumpMinor      # 1.0.1 -> 1.1.0
./gradlew bumpMajor      # 1.1.0 -> 2.0.0
```

### Branches

| Branche | Rôle |
|---|---|
| `main` | Code stable ; chaque version publiée y est taguée (`v1.0.0`, `v1.1.0`…) |
| `develop` | Intégration des fonctionnalités en cours |
| `feature/<nom>` | Une fonctionnalité, créée depuis `develop` et fusionnée dans `develop` |
| `fix/<nom>` | Une correction de bug |

### Messages de commit — Conventional Commits

```
<type>(<portée>): <description>

feat(map): carrousel des notes sur la carte
fix(location): délai d'attente du GPS
docs: procédure de release
```

Types utilisés : `feat` (→ MINEUR), `fix` (→ CORRECTIF), `docs`, `style`, `refactor`,
`test`, `chore`. Un `!` après le type (`feat!:`) signale un changement incompatible (→ MAJEUR).

### Publier une version

1. Fusionner `develop` dans `main`.
2. Incrémenter la version : `./gradlew bumpMinor` (ou `bumpPatch` / `bumpMajor`).
3. Déplacer les entrées « Non publié » du [CHANGELOG](CHANGELOG.md) sous la nouvelle version.
4. Committer puis taguer :

```bash
git commit -am "chore(release): v1.1.0"
git tag -a v1.1.0 -m "Version 1.1.0"
git push origin main --tags
```
