package com.pathfinding.api;

/**
 * Vue "grille discrète" qu'un {@link World} peut optionnellement exposer.
 * Les algorithmes spécifiquement conçus pour une grille (A*4, A*8, Theta*,
 * JPS) testent {@code world instanceof GridLike} et échouent proprement si ce
 * n'est pas le cas. Les algorithmes par échantillonnage (RRT*, Theta*-RRT*)
 * ne dépendent que de {@link World}.
 */
public interface GridLike {

    int gridWidth();

    int gridHeight();

    /** Vrai si la case (cx, cy) est dans les limites et non bloquée. */
    boolean isFreeCell(int cx, int cy);

    /** Coordonnées de case du départ. */
    int startCellX();
    int startCellY();

    /** Coordonnées de case de l'arrivée. */
    int goalCellX();
    int goalCellY();
}
