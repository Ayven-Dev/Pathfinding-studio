package com.pathfinding.algorithms;

import com.pathfinding.algorithms.internal.IndexedMinHeap;
import com.pathfinding.api.GridLike;
import com.pathfinding.api.PathRequest;
import com.pathfinding.api.PathResult;
import com.pathfinding.api.Pathfinder;
import com.pathfinding.api.Vec2;
import com.pathfinding.api.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A* sur grille 4-connexe (déplacements haut/bas/gauche/droite), heuristique
 * de Manhattan.
 *
 * <p>A* explore les cases par coût croissant {@code f = g + h}, où {@code g}
 * est le coût réel depuis le départ et {@code h} une estimation optimiste du
 * coût restant. Tant que {@code h} ne surestime jamais, le premier chemin
 * atteignant l'arrivée est optimal.</p>
 *
 * <p>Cette classe sert aussi de base aux autres variantes de grille : elle
 * fixe deux conventions réutilisées partout.
 * <ul>
 *   <li><b>Encodage des cases.</b> Une case {@code (x, y)} devient un entier
 *       {@code idx = x * H + y}. On peut alors indexer des tableaux plats
 *       plutôt qu'une Map, d'où des accès O(1) sans boxing.</li>
 *   <li><b>Reconstruction.</b> On ne garde pas d'objets « nœud » : un seul
 *       tableau {@code parent[idx]} suffit à remonter le chemin
 *       (voir {@link #reconstruct}).</li>
 * </ul>
 */
public final class AStar4 implements Pathfinder {

    // Décalages des 4 voisins : Est, Ouest, Sud, Nord. Indexés ensemble.
    private static final int[] DX = {1, -1, 0, 0};
    private static final int[] DY = {0, 0, 1, -1};

    @Override public String name() { return "A* (4-dir, Manhattan)"; }

    @Override
    public PathResult find(PathRequest req) {
        long t0 = System.nanoTime();
        PathResult.Builder rb = PathResult.builder().algorithmName(name());

        // Cet algorithme raisonne en cases : on exige une vue grille.
        World w = req.world();
        if (!(w instanceof GridLike grid)) {
            return rb.success(false).failureReason("A*4 nécessite une grille discrète")
                    .elapsedNanos(System.nanoTime() - t0).build();
        }
        if (grid.startCellX() < 0 || grid.goalCellX() < 0) {
            return rb.success(false).failureReason("Départ ou arrivée non défini")
                    .elapsedNanos(System.nanoTime() - t0).build();
        }
        int sx = grid.startCellX(), sy = grid.startCellY();
        int gx = grid.goalCellX(),  gy = grid.goalCellY();
        int W = grid.gridWidth(), H = grid.gridHeight();
        int N = W * H;

        // bestG[idx]  : meilleur coût connu pour atteindre la case (∞ = jamais atteinte).
        // parent[idx] : case précédente sur le meilleur chemin connu (-1 = aucune).
        // Tableaux plats indexés par idx = x*H + y (voir en-tête).
        double[] bestG  = new double[N];
        int[]    parent = new int[N];
        Arrays.fill(bestG, Double.POSITIVE_INFINITY);
        Arrays.fill(parent, -1);

        // File de priorité indexée : une case n'y figure qu'une fois, et on
        // peut réviser sa priorité à la baisse (decreaseKey) au lieu d'empiler
        // des doublons à filtrer ensuite.
        IndexedMinHeap open = new IndexedMinHeap(N);
        List<Vec2> explored = new ArrayList<>();   // trace pour la visualisation
        int generated = 0, expanded = 0, maxOpen = 0;

        int startIdx = sx * H + sy;
        int goalIdx  = gx * H + gy;
        bestG[startIdx] = 0;
        open.insert(startIdx, manhattan(sx, sy, gx, gy));
        generated++;

        while (!open.isEmpty()) {
            maxOpen = Math.max(maxOpen, open.size());
            int curIdx = open.extractMin();   // case de plus petit f restante
            int cx = curIdx / H, cy = curIdx % H;
            expanded++;
            explored.add(Vec2.ofCellCenter(cx, cy));

            // Sortie de la case d'arrivée = chemin optimal trouvé.
            if (curIdx == goalIdx) {
                List<Vec2> path = reconstruct(parent, goalIdx, H);
                rb.success(true).path(path).explored(explored)
                        .nodesExpanded(expanded).nodesGenerated(generated)
                        .maxOpenSize(maxOpen).elapsedNanos(System.nanoTime() - t0);
                PathResult.fillGeometry(rb, path);
                return rb.build();
            }

            double curG = bestG[curIdx];
            for (int i = 0; i < 4; i++) {
                int nx = cx + DX[i], ny = cy + DY[i];
                if (!grid.isFreeCell(nx, ny)) continue;     // hors limites ou obstacle
                int nIdx = nx * H + ny;
                double ng = curG + 1.0;                     // 1 pas = 1 unité de coût
                // « Relâchement » : on a trouvé un chemin plus court vers le voisin.
                if (ng < bestG[nIdx]) {
                    bestG[nIdx] = ng;
                    parent[nIdx] = curIdx;
                    double f = ng + manhattan(nx, ny, gx, gy);
                    // Premier passage → on l'ajoute ; sinon on remonte sa priorité.
                    if (!open.contains(nIdx)) {
                        open.insert(nIdx, f);
                        generated++;
                    } else {
                        open.decreaseKey(nIdx, f);
                    }
                }
            }
        }

        // File vidée sans atteindre l'arrivée : aucune route n'existe.
        return rb.success(false).failureReason("Aucun chemin").explored(explored)
                .nodesExpanded(expanded).nodesGenerated(generated).maxOpenSize(maxOpen)
                .elapsedNanos(System.nanoTime() - t0).build();
    }

    /** Distance de Manhattan entre deux cases — heuristique admissible en 4 directions. */
    static double manhattan(int x0, int y0, int x1, int y1) {
        return Math.abs(x0 - x1) + Math.abs(y0 - y1);
    }

    /**
     * Reconstruit le chemin départ → arrivée en suivant {@code parent[]} à
     * rebours puis en inversant la liste. Convention d'encodage partagée :
     * {@code idx = x * height + y}.
     */
    static List<Vec2> reconstruct(int[] parent, int goalIdx, int height) {
        List<Vec2> out = new ArrayList<>();
        for (int i = goalIdx; i >= 0; i = parent[i]) {
            out.add(Vec2.ofCellCenter(i / height, i % height));
        }
        Collections.reverse(out);
        return out;
    }
}
