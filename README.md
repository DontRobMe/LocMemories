# LocMemories

Application Android native en Kotlin : un carnet de notes qui enregistre, pour chaque note,
un titre, un contenu, une photo, la date et le lieu où elle a été écrite.

- [Fonctionnalités](#fonctionnalités)
- [Démarrage](#démarrage)
- [Architecture](#architecture)
- [Tests](#tests)
- [Performances](#performances)
- [Versioning](#versioning)
- [Crédits](#crédits)

## Fonctionnalités

- **Ajouter une note** : titre, contenu, photo (appareil photo ou galerie), date et position.
- **Lister les notes**, avec une recherche sur le titre et le contenu.
- **Voir le détail** d'une note : photo, lieu sur une mini-carte, partage, suppression.
- **Carte** de toutes les notes géolocalisées, avec un carrousel pour passer de l'une à l'autre.
- **Introduction** en quatre pages au premier lancement.

L'interface suit Material 3 (thème clair et sombre) et prend en charge TalkBack, les grandes
tailles de texte et les petits écrans.

## Démarrage

**Prérequis** : Android Studio récent (le JDK fourni suffit), Android 7.0 (API 24) minimum
sur l'appareil.

1. Ouvrir le dossier dans Android Studio et laisser la synchronisation Gradle se terminer.
2. Lancer l'application avec **Run**.
3. Sur émulateur, définir une position dans *Extended controls → Location* avant de créer
   une note.

L'application demande la **localisation** à la création de la première note. L'appareil photo
est ouvert par l'application système, sans autorisation supplémentaire. Les notes restent sur
l'appareil ; seule la carte utilise internet, pour télécharger les fonds OpenStreetMap.

## Architecture

L'application suit le modèle **MVVM**, avec une activité par écran.

```
 Vue                         ViewModel                     Repository            Stockage
 Activity + layout XML  ──▶  état (StateFlow) + logique ──▶ NoteRepository  ──▶  NoteStorage
   affiche l'état              survit aux rotations          notes en mémoire      notes.json
   relaie les actions          SavedStateHandle              source de vérité      + photos
```

| Couche | Rôle |
|---|---|
| **Vue** | Une `Activity` et son layout XML par écran. Elle affiche l'état du ViewModel et lui transmet les actions. Elle ne garde que ce qu'Android impose : lanceurs d'appareil photo, de galerie et de permissions. |
| **ViewModel** | Un par écran. Il porte l'état et la logique, et reçoit ses dépendances (repository, préférences, localisation) par sa `Factory` : il se teste sans écran. |
| **Repository** | `NoteRepository`, unique accès aux notes, partagé par tous les écrans via `App`. Il confie la lecture et l'écriture des fichiers à `NoteStorage`. |

### Arborescence

```
app/src/main/
├── java/com/ynov/locmemories/
│   ├── App.kt              Dépendances partagées (repository, préférences)
│   ├── data/               Note, NoteRepository, NoteStorage, AppPreferences
│   ├── location/           Permissions et position courante
│   └── ui/
│       ├── list/           Liste des notes (écran d'accueil)
│       ├── add/            Création d'une note
│       ├── detail/         Détail d'une note
│       ├── map/            Carte des notes
│       ├── onboarding/     Introduction
│       ├── NotesMap.kt     Carte osmdroid, partagée par le détail et la carte
│       └── Format.kt       Dates et coordonnées
└── res/
    ├── layout/             Un fichier par écran et par élément de liste
    ├── drawable/           Icônes vectorielles et formes
    └── values*/            Couleurs, thème, textes, dimensions (petits et grands écrans)
```

### Bibliothèques

| Besoin | Choix |
|---|---|
| Interface | Layouts XML, Material Components 3, ViewBinding |
| Carte | osmdroid (OpenStreetMap, sans clé API) |
| Images | Coil |
| Localisation | `LocationManager` d'Android, sans Google Play Services |
| Stockage | Fichier JSON et photos dans le stockage privé de l'application |

## Tests

Les tests unitaires tournent sur l'ordinateur, sans émulateur, grâce à Robolectric :

```bash
./gradlew testDebugUnitTest
```

Le rapport détaillé est généré dans `app/build/reports/tests/testDebugUnitTest/index.html`.
Dans Android Studio : clic droit sur `app/src/test/java` → **Run Tests** (variante `debug`).

| Couche | Tests | Ce qui est vérifié |
|---|---|---|
| Stockage | `NoteStorageTest`, `AppPreferencesTest` | JSON, écriture atomique, redimensionnement des photos |
| Repository | `NoteRepositoryTest` | Tri, ajout, suppression, ajout pendant le chargement |
| ViewModels | `*ViewModelTest` | Recherche, validation, position simulée, photos, état après rotation |
| Vues | `*ActivityTest` (Espresso) | Affichage, clics, confirmation de suppression, petit écran |
| Utilitaires | `NoteTest`, `FormatTest` | Modèle, dates, coordonnées |

Le test de bout en bout (premier lancement, puis création d'une note) demande un émulateur
ou un téléphone connecté :

```bash
./gradlew connectedDebugAndroidTest
```

## Performances

La variante `debug` n'est pas optimisée. Pour juger de la fluidité réelle, choisir la
variante **`benchmark`** (*Build → Select Build Variant*) : elle est optimisée par R8 comme
la release, mais signée avec la clé de debug pour s'installer directement.

Sur émulateur, vérifier que le rendu passe par la carte graphique de la machine :

```bash
adb shell dumpsys SurfaceFlinger | grep GLES
```

Si la ligne mentionne *SwiftShader*, le rendu est logiciel et tout rame. Dans
*Device Manager → Edit → Show Advanced Settings*, régler **Graphics acceleration** sur
*Hardware* (3 Go de RAM conseillés), puis faire un *Cold boot*.

## Versioning

**Numéro de version.** La version suit le [Semantic Versioning](https://semver.org/lang/fr/)
(MAJEUR.MINEUR.CORRECTIF) et n'est écrite que dans [`version.properties`](version.properties).
Gradle en déduit `versionName` et `versionCode` (`MAJEUR × 10000 + MINEUR × 100 + CORRECTIF`).

```bash
./gradlew printVersion   # version courante
./gradlew bumpPatch      # 1.0.0 -> 1.0.1 : correction de bug
./gradlew bumpMinor      # 1.0.1 -> 1.1.0 : nouvelle fonctionnalité
./gradlew bumpMajor      # 1.1.0 -> 2.0.0 : changement incompatible
```

**Branches.**

| Branche | Rôle |
|---|---|
| `main` | Versions publiées, taguées `v1.0.0`, `v1.1.0`… |
| `develop` | Intégration des fonctionnalités terminées |
| `feature/…`, `fix/…`, `docs/…` | Un sujet par branche, créée depuis `develop` et fusionnée dans `develop` |

**Commits.** Format [Conventional Commits](https://www.conventionalcommits.org/fr/) :
`feat(map): carrousel des notes`, `fix: …`, `docs: …`, `test: …`, `refactor: …`, `chore: …`.

**Publier une version.**

1. Fusionner `develop` dans `main`.
2. Incrémenter la version (`./gradlew bumpMinor`, par exemple).
3. Déplacer les entrées « Non publié » du [CHANGELOG](CHANGELOG.md) sous la nouvelle version.
4. Committer, taguer et pousser :

```bash
git commit -am "chore(release): v1.1.0" && git tag -a v1.1.0 -m "Version 1.1.0" && git push origin main --tags
```

## Crédits

- Fonds de carte © [contributeurs OpenStreetMap](https://www.openstreetmap.org/copyright),
  affichés avec [osmdroid](https://github.com/osmdroid/osmdroid).
- Icônes [Material Symbols](https://fonts.google.com/icons) (licence Apache 2.0).
