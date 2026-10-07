# Changelog

Toutes les évolutions notables du projet sont consignées ici.

Le format suit [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/)
et le projet respecte le [Semantic Versioning](https://semver.org/lang/fr/).

## [Non publié]

### Ajouté
- Introduction en carrousel au premier lancement (4 pages, « Passer », indicateur accessible).
- Bouton « Revoir l'introduction » dans la barre de la liste.
- Tests unitaires et d'écrans (JUnit, Robolectric, Compose UI Test) : 49 tests exécutés sans émulateur.
- Tests de bout en bout instrumentés : premier lancement, création et persistance d'une note.

### Modifié
- Carte : passage d'osmdroid à MapLibre Native (rendu vectoriel fluide, rotation et inclinaison).
- Fonds de carte OpenFreeMap « Positron » (clair) et « Dark » (sombre), sans clé API.
- Titre des notes affiché sous chaque marqueur ; marqueur agrandi pour la note sélectionnée.

### Corrigé
- Introduction : titre coupé sur petit écran ou avec un texte agrandi (illustration adaptative, page défilante).

### Performances
- Mini-carte du détail rendue en image statique (`MapSnapshotter`) au lieu d'un moteur de carte complet.
- Photos redimensionnées à 2048 px et orientées (EXIF) à l'enregistrement : environ 10 fois plus légères.
- Lecture, écriture et traitement des photos hors du thread principal ; sauvegarde atomique du JSON.
- Carrousel d'introduction animé sans recomposition pendant le balayage.
- Formateurs de date mis en cache, type de contenu déclaré pour la liste.
- Variante `benchmark` : build optimisé (R8) installable pour mesurer les performances réelles.

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

[Non publié]: ../../compare/v1.0.0...HEAD
[1.0.0]: ../../releases/tag/v1.0.0
