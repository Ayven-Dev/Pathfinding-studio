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
 * Theta* : A* « any-angle ». Il explore la grille comme {@link AStar8}, mais
 * autorise les segments de chemin à prendre n'importe quel angle au lieu de se
 * limiter aux 8 directions.
 *
 * <p>L'idée tient en une variante du relâchement. Quand on atteint un voisin
 * via la case courante, on tente d'abord de le relier en ligne droite au
 * <i>parent</i> de la case courante. Si rien ne bloque cette ligne
 * ({@link World#lineOfSight}), ce raccourci est au moins aussi court que le
 * détour par la case courante : on l'adopte. Le chemin final est donc fait de
 * longues lignes droites entre coins d'obstacles plutôt que d'un escalier.</p>
 *
 * <p>Conséquence pour le code : {@code parent[case]} ne pointe plus forcément
 * vers une case voisine, mais vers n'importe quelle case visible en amont.
 * Le surcoût se concentre dans les appels à {@code lineOfSight}.</p>
 */
public final class ThetaStar implements Pathfinder {

    private static final int[] DX = { 1, -1, 0,  0,  1,  1, -1, -1};
    private static final int[] DY = { 0,  0, 1, -1,  1, -1,  1, -1};

    @Override public String name() { return "Theta* (any-angle)"; }

    @Override
    public PathResult find(PathRequest req) {
        long t0 = System.nanoTime();
        PathResult.Builder rb = PathResult.builder().algorithmName(name());

        World w = req.world();
        if (!(w instanceof GridLike grid)) {
            return rb.success(false).failureReason("Theta* nécessite une grille discrète")
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
        Vec2 goalV = Vec2.ofCellCenter(gx, gy);
        bestG[startIdx] = 0;
        // Le départ se déclare son propre parent. Ainsi « tenter le parent »
        // depuis le départ revient à pointer sur lui-même, et la reconstruction
        // a une borne d'arrêt nette.
        parent[startIdx] = startIdx;
        open.insert(startIdx, Vec2.ofCellCenter(sx, sy).distance(goalV));
        generated++;

        while (!open.isEmpty()) {
            maxOpen = Math.max(maxOpen, open.size());
            int curIdx = open.extractMin();
            int cx = curIdx / H, cy = curIdx % H;
            expanded++;
            Vec2 curV = Vec2.ofCellCenter(cx, cy);
            explored.add(curV);

            if (curIdx == goalIdx) {
                List<Vec2> path = reconstruct(parent, startIdx, goalIdx, H);
                rb.success(true).path(path).explored(explored)
                        .nodesExpanded(expanded).nodesGenerated(generated)
                        .maxOpenSize(maxOpen).elapsedNanos(System.nanoTime() - t0);
                PathResult.fillGeometry(rb, path);
                return rb.build();
            }

            // Le parent de la case courante : candidat au raccourci en ligne droite.
            int parIdx = parent[curIdx];
            Vec2 parV = Vec2.ofCellCenter(parIdx / H, parIdx % H);

            for (int i = 0; i < 8; i++) {
                int nx = cx + DX[i], ny = cy + DY[i];
                if (!grid.isFreeCell(nx, ny)) continue;
                if (DX[i] != 0 && DY[i] != 0
                        && (!grid.isFreeCell(cx + DX[i], cy) || !grid.isFreeCell(cx, cy + DY[i]))) {
                    continue;   // anti corner-cutting, comme en 8-connexe
                }
                int nIdx = nx * H + ny;
                Vec2 nV = Vec2.ofCellCenter(nx, ny);

                // Deux façons d'atteindre le voisin ; on choisit la moins chère.
                //  - Raccourci : relier directement le parent au voisin, si la
                //    ligne est dégagée (c'est ce qui donne l'angle libre).
                //  - Sinon : passer par la case courante, comme un A* normal.
                int candParent;
                double candG;
                if (parIdx != curIdx && w.lineOfSight(parV, nV)) {
                    candParent = parIdx;
                    candG = bestG[parIdx] + parV.distance(nV);
                } else {
                    candParent = curIdx;
                    candG = bestG[curIdx] + curV.distance(nV);
                }

                if (candG < bestG[nIdx]) {
                    bestG[nIdx] = candG;
                    parent[nIdx] = candParent;
                    double f = candG + nV.distance(goalV);
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
     * Variante de reconstruction propre à Theta* : on remonte {@code parent[]}
     * jusqu'au départ (qui se référence lui-même). On ne peut pas réutiliser
     * {@link AStar4#reconstruct} qui s'arrête sur {@code -1}, puisqu'ici le
     * départ pointe sur lui-même.
     */
    private static List<Vec2> reconstruct(int[] parent, int startIdx, int goalIdx, int H) {
        List<Vec2> out = new ArrayList<>();
        int idx = goalIdx;
        while (true) {
            out.add(Vec2.ofCellCenter(idx / H, idx % H));
            if (idx == startIdx) break;
            idx = parent[idx];
        }
        Collections.reverse(out);
        return out;
    }
}
