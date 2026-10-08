# Carnet de notes géolocalisées

Application Android native (Kotlin, layouts XML) : un carnet de notes qui enregistre,
pour chaque note, un titre, un contenu, une photo, la date et le lieu où elle a été écrite.

## Fonctionnalités

- Ajouter une note (id, titre, contenu, photo, date, position de l'utilisateur)
- Lister les notes, avec recherche
- Voir le détail d'une note
- Carte avec les notes

## Stack technique

| Besoin | Choix |
|---|---|
| UI | Layouts XML, Material Components 3, ViewBinding |
| Navigation | Une activité par écran, reliées par des `Intent` |
| Carte | osmdroid (OpenStreetMap, sans clé API) |
| Images | Coil |
| Localisation | `LocationManager` (sans Google Play Services) |
| Stockage | Fichier JSON + photos dans le stockage interne |

## Architecture (MVVM)

```
 View                      ViewModel                    Repository              Stockage
 Activity + layout XML ──▶ état (StateFlow) + logique ──▶ NoteRepository ──▶ NoteStorage
   affiche l'état            survit aux rotations         notes en mémoire       notes.json
   relaie les actions        SavedStateHandle             (source de vérité)    + photos
```

- **View** : une `Activity` et son layout par écran. Elle observe l'état du ViewModel et lui
  transmet les actions ; elle ne garde que ce qu'Android impose (lanceurs d'appareil photo,
  de galerie, de permissions).
- **ViewModel** : un par écran, état exposé en `StateFlow`. Il reçoit ses dépendances
  (repository, préférences, localisation) par une fabrique `Factory` : il est testable seul.
- **Repository** : `NoteRepository`, unique point d'accès aux notes, partagé par tous les
  écrans via `App`. Il délègue les fichiers à `NoteStorage`.

## Structure

```
app/src/main/
├── java/com/ynov/helloworld/
│   ├── App.kt             Application : dépendances partagées (repository, préférences)
│   ├── data/              Note, NoteRepository, NoteStorage, AppPreferences
│   ├── location/          Permissions et récupération de la position
│   └── ui/
│       ├── list/          Liste : MainActivity, NoteListViewModel, NoteAdapter
│       ├── add/           Création : AddNoteActivity, AddNoteViewModel
│       ├── detail/        Détail : NoteDetailActivity, NoteDetailViewModel
│       ├── map/           Carte : MapActivity, MapViewModel
│       ├── onboarding/    Introduction : OnboardingActivity, OnboardingViewModel
│       └── NotesMap.kt    Carte osmdroid partagée (détail et carte)
└── res/
    ├── layout/            Un fichier XML par écran et par élément de liste
    ├── drawable/          Icônes vectorielles Material et formes
    └── values/            Couleurs (clair / sombre), thème, textes, dimensions
```

## Lancer le projet

Ouvrir le dossier dans Android Studio, puis **Run**. Sur émulateur, définir une position
dans *Extended controls → Location* avant de créer une note.

### Tester les performances

La variante `debug` est volontairement lente (code non optimisé, interprété).
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


## Tests

| Type | Emplacement | Outils | Lancement |
|---|---|---|---|
| Unitaires et écrans | `app/src/test` | JUnit 4, Robolectric, Espresso | `./gradlew testDebugUnitTest` |
| Bout en bout | `app/src/androidTest` | Espresso, AndroidX Test | `./gradlew connectedDebugAndroidTest` (appareil requis) |

Les tests unitaires tournent sur la JVM, sans émulateur, grâce à Robolectric. Ils suivent
les couches de l'architecture :

| Couche | Tests | Ce qui est vérifié |
|---|---|---|
| Stockage | `NoteStorageTest`, `AppPreferencesTest` | Aller-retour JSON, écriture atomique, redimensionnement des photos, préférences |
| Repository | `NoteRepositoryTest` | Tri, ajout, suppression avec la photo, ajout pendant le chargement |
| ViewModels | `*ViewModelTest` | Recherche, validation, position (simulée), photos, sélection, état restauré après rotation |
| Vues | `*ActivityTest` (Espresso) | Affichage de l'état, clics, dialogue de suppression, petit écran |
| Utilitaires | `NoteTest`, `FormatTest` | Modèle, dates et coordonnées |

Les ViewModels reçoivent leurs dépendances dans leur constructeur : les tests les créent
directement, avec par exemple une localisation simulée pour `AddNoteViewModel`.
`MainDispatcherRule` remplace le thread principal pour exécuter leurs coroutines immédiatement.

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
