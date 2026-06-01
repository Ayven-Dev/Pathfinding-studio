package com.pathfinding.api;

/**
 * Point d'entrée unique d'un algorithme de planification : on lui passe un
 * {@link PathRequest} (le monde à traverser), il rend un {@link PathResult}
 * (le chemin + les compteurs de profilage).
 *
 * <p>Une implémentation déclare aussi ses capacités via {@link #supportsGrid()}
 * et {@link #supportsContinuous()}. L'IU s'appuie dessus pour ne proposer un
 * algorithme que dans les modes où il sait fonctionner — d'où des valeurs par
 * défaut qui décrivent le cas le plus courant (un planificateur de grille
 * classique).</p>
 */
public interface Pathfinder {

    /** Libellé affiché dans les menus et les tableaux de benchmark. */
    String name();

    /** Lance la recherche. L'implémentation ne doit jamais renvoyer {@code null}. */
    PathResult find(PathRequest request);

    /**
     * Capacité à travailler sur une grille discrète ({@link GridLike}).
     * Vrai par défaut : c'est le cas de tous les algorithmes de ce projet,
     * y compris les planificateurs par échantillonnage (qui voient la grille
     * comme un {@link World} continu via {@code isFree}/{@code lineOfSight}).
     */
    default boolean supportsGrid() { return true; }

    /**
     * Capacité à travailler sur une carte continue (obstacles polygonaux,
     * sans découpage en cases). Faux par défaut : seuls les planificateurs qui
     * raisonnent en coordonnées réelles (RRT*, Theta*-RRT*) l'activent.
     */
    default boolean supportsContinuous() { return false; }
}
