package com.pathfinding.ui.shared;

import com.pathfinding.algorithms.AStar4;
import com.pathfinding.algorithms.AStar8;
import com.pathfinding.algorithms.JumpPointSearch;
import com.pathfinding.algorithms.RRTStar;
import com.pathfinding.algorithms.ThetaStar;
import com.pathfinding.algorithms.ThetaStarRRT;
import com.pathfinding.api.Pathfinder;

import java.util.List;
import java.util.function.Supplier;

/**
 * Liste, en un seul endroit, les algorithmes proposés par l'IU. Ajouter un
 * algorithme = ajouter une ligne dans {@link #ENTRIES} ; ses capacités
 * (grille / continu) sont lues automatiquement sur l'algorithme lui-même, donc
 * rien d'autre à mettre à jour.
 *
 * <p>Les algorithmes sont sans état entre deux recherches : on garde un
 * {@link Supplier} et on fabrique une instance neuve à chaque exécution ou
 * run de benchmark.</p>
 */
public final class AlgorithmRegistry {

    /**
     * Une entrée du menu. Les drapeaux {@code grid}/{@code continuous} sont
     * calculés une fois, en interrogeant une instance fabriquée par la factory,
     * pour rester synchronisés avec l'algorithme sans duplication.
     */
    public static final class Entry {
        private final String name;
        private final Supplier<Pathfinder> factory;
        private final boolean grid;
        private final boolean continuous;

        Entry(String name, Supplier<Pathfinder> factory) {
            this.name = name;
            this.factory = factory;
            Pathfinder probe = factory.get();
            this.grid = probe.supportsGrid();
            this.continuous = probe.supportsContinuous();
        }

        public String name() { return name; }
        public boolean grid() { return grid; }
        public boolean continuous() { return continuous; }
        public Pathfinder build() { return factory.get(); }
    }

    private static final List<Entry> ENTRIES = List.of(
            new Entry("A* (4-dir)",        AStar4::new),
            new Entry("A* (8-dir)",        AStar8::new),
            new Entry("Theta*",            ThetaStar::new),
            new Entry("Jump Point Search", JumpPointSearch::new),
            new Entry("RRT*",              RRTStar::new),
            new Entry("Theta*-RRT*",       ThetaStarRRT::new)
    );

    private AlgorithmRegistry() {}

    public static List<Entry> all() { return ENTRIES; }

    /** Algorithmes proposés en mode grille (tous ceux qui savent traiter une grille). */
    public static List<Entry> forGrid() {
        return ENTRIES.stream().filter(Entry::grid).toList();
    }

    /** Algorithmes proposés en mode carte continue (uniquement ceux qui la gèrent nativement). */
    public static List<Entry> forContinuous() {
        return ENTRIES.stream().filter(Entry::continuous).toList();
    }
}
