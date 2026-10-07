# Changelog

Toutes les évolutions notables du projet sont consignées ici.

Le format suit [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/)
et le projet respecte le [Semantic Versioning](https://semver.org/lang/fr/).

## [Non publié]

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
