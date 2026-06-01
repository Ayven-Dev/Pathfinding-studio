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
import java.util.List;

/**
 * A* sur grille 8-connexe : ajoute les déplacements diagonaux (coût √2) aux
 * quatre directions cardinales (coût 1), avec l'heuristique octile adaptée.
 *
 * <p>Différence notable avec {@link AStar4} : on interdit le « corner
 * cutting ». Couper en diagonale au coin d'un obstacle ferait passer le chemin
 * par une arête bloquée ; ici un mouvement diagonal n'est permis que si les
 * deux cases cardinales qui l'encadrent sont libres.</p>
 *
 * <p>Mêmes structures que {@link AStar4} (tableaux plats + file indexée) et
 * mêmes conventions d'encodage/reconstruction.</p>
 */
public final class AStar8 implements Pathfinder {

    // 8 voisins : 4 cardinaux puis 4 diagonaux. L'ordre n'a pas d'importance.
    private static final int[] DX = { 1, -1, 0,  0,  1,  1, -1, -1};
    private static final int[] DY = { 0,  0, 1, -1,  1, -1,  1, -1};
    private static final double SQRT2 = Math.sqrt(2);

    @Override public String name() { return "A* (8-dir, Octile)"; }

    @Override
    public PathResult find(PathRequest req) {
        long t0 = System.nanoTime();
        PathResult.Builder rb = PathResult.builder().algorithmName(name());

        World w = req.world();
        if (!(w instanceof GridLike grid)) {
            return rb.success(false).failureReason("A*8 nécessite une grille discrète")
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

        double[] bestG  = new double[N];
        int[]    parent = new int[N];
        Arrays.fill(bestG, Double.POSITIVE_INFINITY);
        Arrays.fill(parent, -1);

        IndexedMinHeap open = new IndexedMinHeap(N);
        List<Vec2> explored = new ArrayList<>();
        int generated = 0, expanded = 0, maxOpen = 0;

        int startIdx = sx * H + sy;
        int goalIdx  = gx * H + gy;
        bestG[startIdx] = 0;
        open.insert(startIdx, octile(sx, sy, gx, gy));
        generated++;

        while (!open.isEmpty()) {
            maxOpen = Math.max(maxOpen, open.size());
            int curIdx = open.extractMin();
            int cx = curIdx / H, cy = curIdx % H;
            expanded++;
            explored.add(Vec2.ofCellCenter(cx, cy));

            if (curIdx == goalIdx) {
                List<Vec2> path = AStar4.reconstruct(parent, goalIdx, H);
                rb.success(true).path(path).explored(explored)
                        .nodesExpanded(expanded).nodesGenerated(generated)
                        .maxOpenSize(maxOpen).elapsedNanos(System.nanoTime() - t0);
                PathResult.fillGeometry(rb, path);
                return rb.build();
            }

            double curG = bestG[curIdx];
            for (int i = 0; i < 8; i++) {
                int nx = cx + DX[i], ny = cy + DY[i];
                if (!grid.isFreeCell(nx, ny)) continue;

                boolean diagonal = DX[i] != 0 && DY[i] != 0;
                // Anti corner-cutting : pour une diagonale, refuser le passage
                // si l'une des deux cases cardinales adjacentes est bloquée.
                if (diagonal && (!grid.isFreeCell(cx + DX[i], cy) || !grid.isFreeCell(cx, cy + DY[i]))) {
                    continue;
                }

                int nIdx = nx * H + ny;
                double ng = curG + (diagonal ? SQRT2 : 1.0);
                if (ng < bestG[nIdx]) {
                    bestG[nIdx] = ng;
                    parent[nIdx] = curIdx;
                    double f = ng + octile(nx, ny, gx, gy);
                    if (!open.contains(nIdx)) {
                        open.insert(nIdx, f);
                        generated++;
                    } else {
                        open.decreaseKey(nIdx, f);
                    }
                }
            }
        }

        return rb.success(false).failureReason("Aucun chemin").explored(explored)
                .nodesExpanded(expanded).nodesGenerated(generated).maxOpenSize(maxOpen)
                .elapsedNanos(System.nanoTime() - t0).build();
    }

    /**
     * Distance octile : longueur du trajet optimal sur grille 8-connexe en
     * terrain dégagé (autant de diagonales que possible, le reste en droit).
     * Réutilisée par {@link JumpPointSearch}, d'où sa visibilité package.
     */
    static double octile(int x0, int y0, int x1, int y1) {
        int dx = Math.abs(x0 - x1);
        int dy = Math.abs(y0 - y1);
        return (dx + dy) + (SQRT2 - 2) * Math.min(dx, dy);
    }
}
