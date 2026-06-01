# Pathfinding Studio

Prototype Java 21 de recherche de chemin 2D. Un éditeur graphique permet de
poser un départ, une arrivée et des obstacles, puis de comparer **six
algorithmes** de planification avec benchmark et profilage intégrés.
L'application sépare nettement une **librairie d'algorithmes** (sans dépendance
à l'UI) d'une **interface Swing** au design inspiré d'Apple.

> **Contraintes cinétiques** — l'objectif à terme est de planifier sous
> contrainte d'angle de virage (utile pour des mobiles non holonomes : avion,
> voiture, robot…). Le module cinétique est en cours de refonte et n'est pas
> inclus dans cette version (l'approche initiale, à base de virages forcés à
> 45°/90° sur grille, n'était pas satisfaisante).

## Fonctionnalités

- **Deux environnements**, choisis depuis le menu d'accueil :
  - **Mode grille** — quadrillage de cases, obstacles « à main levée ».
  - **Mode carte continue** — coordonnées réelles, obstacles polygonaux de
    forme quelconque.
- **Six algorithmes**, proposés selon leur compatibilité avec le monde :
  | Algorithme | Grille | Continu | En bref |
  | --- | :---: | :---: | --- |
  | A* (4-dir) | ✓ | | Référence, déplacements cardinaux. |
  | A* (8-dir) | ✓ | | Diagonales, sans corner-cutting. |
  | Theta* | ✓ | | A* « any-angle » : trajets en lignes droites. |
  | Jump Point Search | ✓ | | A* qui saute les cases symétriques. |
  | RRT* | ✓ | ✓ | Échantillonnage, optimal asymptotiquement. |
  | Theta*-RRT* | ✓ | ✓ | RRT* aux liaisons redressées. |
- **Visualisation** : départ en bleu, arrivée en vert, trajet final en noir.
  Une case à cocher affiche en rouge les cases/​nœuds parcourus qui ne font pas
  partie du trajet.
- **Benchmark & profilage** : exécute chaque algorithme N fois (premier run
  écarté pour l'échauffement JIT) et rapporte temps (moy/min/max/σ), nœuds
  expansés/générés, taille max de l'open set, longueur et virages cumulés.

## Architecture

La frontière librairie / application est explicite :

```
src/main/java/com/pathfinding/
├── api/            Contrat de la librairie : World, GridLike, Vec2, Bounds,
│                   Obstacle, PathRequest, PathResult, Pathfinder
│                   (avec ses capacités supportsGrid / supportsContinuous)
├── world/          Implémentations de World : GridWorld, ContinuousWorld
├── algorithms/     Les 6 Pathfinder + internal/ (IndexedMinHeap, KdTree)
├── benchmark/      Banc d'essai indépendant de l'UI
└── ui/             Swing : menu, éditeurs, thème, composants
```

Un algorithme ne connaît que l'interface `World` (tests `isFree` /
`lineOfSight`) : il fonctionne donc indifféremment sur grille ou carte
continue selon les capacités qu'il déclare. Ajouter un algorithme = créer une
classe `Pathfinder` et l'enregistrer dans `AlgorithmRegistry` ; ses capacités
sont lues automatiquement et l'UI le propose dans les bons modes.

### Optimisations

- **Famille A\*** (`AStar4/8`, `ThetaStar`, `JumpPointSearch`) : file de
  priorité **indexée avec `decreaseKey`** (`IndexedMinHeap`) et tableaux
  primitifs (`double[] bestG`, `int[] parent`) au lieu de Map et d'objets
  chaînés — pas d'auto-boxing, aucune allocation par expansion.
- **RRT\* / Theta\*-RRT\*** : recherches de voisinage en **`KdTree`** (≈ O(log n))
  au lieu d'un balayage linéaire de l'arbre.

## Prérequis

- JDK 21 ou plus récent (`java`/`javac` dans le `PATH`).
- Maven 3.9+ optionnel (compilation à la main possible, voir plus bas).

## Compiler et lancer

Avec Maven :

```powershell
mvn -q package
java -jar target/pathfinding-studio.jar
```

Sans Maven, via les scripts fournis :

```powershell
.\run.ps1      # Windows / PowerShell
```

```bash
./run.sh       # Linux / macOS
```

## Utilisation

1. Depuis le menu, ouvrir **Mode grille** ou **Mode carte continue**.
2. Poser le départ, l'arrivée, puis dessiner des obstacles
   (clic-glissé en grille ; clics successifs + double-clic en continu).
3. Choisir un algorithme puis **Lancer**. Cocher « Afficher les cases
   parcourues » pour voir l'exploration.
4. **Benchmark tous** exécute tous les algorithmes du mode courant et affiche
   le tableau comparatif.

## Repères de choix d'algorithme

- **Plus court chemin géométrique** : `Theta*` (grille) ou `Theta*-RRT*`
  (carte continue).
- **Exploration la plus rapide sur grille dégagée** : `Jump Point Search`.
- **Carte continue à obstacles polygonaux** : `RRT*` / `Theta*-RRT*`.
