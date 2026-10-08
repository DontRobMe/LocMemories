# Changelog

Toutes les évolutions notables du projet sont consignées ici.

Le format suit [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/)
et le projet respecte le [Semantic Versioning](https://semver.org/lang/fr/).

## [Non publié]

### Corrigé
- Ajout de note : une photo illisible ou supprimée ne fait plus planter l'application.
- Suppression des opérateurs `!!` : notes sans position gérées sans risque de plantage.

## [1.1.0] - 2026-10-08

### Ajouté
- Introduction en carrousel au premier lancement (4 pages, « Passer », indicateur accessible).
- Bouton « Revoir l'introduction » dans la barre de la liste.
- Tests unitaires, de ViewModels et d'écrans (JUnit, Robolectric, Espresso) : 60 tests exécutés sans émulateur.
- Tests de bout en bout instrumentés : premier lancement, création et persistance d'une note.

### Modifié
- Projet renommé **LocMemories** : nom de l'application, package et `applicationId` `com.ynov.locmemories`.
- Interface réécrite en layouts XML (Material Components 3, ViewBinding) : une activité par écran
  au lieu de Jetpack Compose.
- Architecture MVVM : une Activity (vue) et un ViewModel par écran, `NoteRepository` comme source
  de vérité unique, `NoteStorage` pour les fichiers ; dépendances injectées par des fabriques.
- Carte : marqueur agrandi et caméra animée pour la note sélectionnée dans le carrousel.
- Mini-carte du détail figée (gestes désactivés) pour ne pas gêner le défilement.

### Corrigé
- Carte : mention obligatoire « © OpenStreetMap contributors » désormais affichée.
- Release : plantage au démarrage (IllegalAccessError) causé par l'option R8 `packageScope`
  du modèle de projet AGP 9 ; retour à l'optimisation R8 complète.
- Introduction : titre coupé sur petit écran ou avec un texte agrandi (illustration adaptative, page défilante).

### Performances
- Cache des tuiles OpenStreetMap dans le stockage privé de l'application (elles étaient retéléchargées à chaque affichage depuis Android 10).
- Photos redimensionnées à 2048 px et orientées (EXIF) à l'enregistrement : environ 10 fois plus légères.
- Lecture, écriture et traitement des photos hors du thread principal ; sauvegarde atomique du JSON.
- Formateurs de date mis en cache.
- Variante `benchmark` : build optimisé (R8) installable pour mesurer les performances réelles.
- Icônes : 21 drawables vectoriels au lieu de la bibliothèque `material-icons-extended`
  (builds R8 plus rapides, APK plus léger).
- Gradle : compilation parallèle, cache de build et mémoire du démon augmentée.

## [1.0.0] - 2026-10-07

### Ajouté
- Création d'une note : titre, contenu, photo (appareil photo ou galerie), date et position GPS.
- Liste des notes avec recherche instantanée sur le titre et le contenu.
- Détail d'une note : photo, date, coordonnées, mini-carte, partage et suppression.
- Carte plein écran (OpenStreetMap) de toutes les notes géolocalisées, avec carrousel.
- Persistance locale des notes (JSON) et des photos (stockage interne).
- Thème Material 3 teal, clair et sombre.
- Accessibilité : descriptions TalkBack, titres de section, annonces dynamiques,
  messages d'erreur explicites, mise en page compatible avec le texte agrandi.
- Versioning : version pilotée par `version.properties` et tâches Gradle `bump*`.

[Non publié]: ../../compare/v1.1.0...HEAD
[1.1.0]: ../../compare/v1.0.0...v1.1.0
[1.0.0]: ../../releases/tag/v1.0.0
