package com.pathfinding.api;

/**
 * Paramètres d'entrée d'un appel au planificateur.
 *
 * <p>On regroupe ici ce qui ne dépend pas de l'algorithme choisi : le monde à
 * traverser et une graine aléatoire (utile aux algorithmes par
 * échantillonnage ; ignorée par les autres). Un algorithme peut donc ignorer
 * les champs qui ne le concernent pas.</p>
 */
public record PathRequest(World world, long randomSeed) {

    /** Requête avec une graine dérivée de l'horloge (runs non reproductibles). */
    public static PathRequest of(World world) {
        return new PathRequest(world, System.nanoTime());
    }
}
