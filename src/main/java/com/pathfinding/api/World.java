package com.pathfinding.api;

/**
 * Abstraction "monde" sur laquelle opèrent les algorithmes de pathfinding.
 * C'est l'interface centrale de la librairie : les algorithmes ne connaissent
 * que cette interface, ce qui leur permet de fonctionner indifféremment sur
 * une grille discrète ou sur une carte continue à obstacles polygonaux.
 *
 * <p>Tous les algorithmes consomment au minimum :
 * <ul>
 *   <li>les positions de départ et d'arrivée,</li>
 *   <li>un test de validité d'un point ({@link #isFree(Vec2)}),</li>
 *   <li>un test de visibilité directe ({@link #lineOfSight(Vec2, Vec2)}).</li>
 * </ul>
 *
 * <p>Les algorithmes de grille (A*4, A*8, Theta*, JPS) ont besoin en plus
 * d'une vue grille ; ils vérifient à l'exécution que l'instance reçue
 * implémente {@link GridLike} (et échouent proprement sinon).</p>
 */
public interface World {

    /** Point de départ du planificateur. */
    Vec2 start();

    /** Point d'arrivée. */
    Vec2 goal();

    /** Boîte englobante du monde — utile pour l'échantillonnage uniforme. */
    Bounds bounds();

    /** Vrai si le point {@code p} n'est ni hors-limites ni dans un obstacle. */
    boolean isFree(Vec2 p);

    /**
     * Vrai si le segment [a,b] est libre d'obstacle. C'est le test clef
     * partagé par Theta*, RRT*, Theta*-RRT* et tout planificateur "any-angle".
     */
    boolean lineOfSight(Vec2 a, Vec2 b);
}
