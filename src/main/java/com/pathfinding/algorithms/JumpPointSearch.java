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
 * Jump Point Search : un A* 8-connexe qui saute les cases « sans intérêt ».
 *
 * <p>Sur une grille uniforme, A* perd un temps fou à explorer des cases
 * équivalentes (toutes les façons de zigzaguer dans une zone dégagée coûtent
 * pareil). JPS exploite cette symétrie : depuis une case, il file tout droit
 * dans une direction et ne s'arrête (« point de saut ») que si l'arrivée est
 * atteinte ou si un obstacle proche oblige le chemin à passer précisément par
 * là (un « voisin forcé »). Seuls ces points de saut entrent dans la file de
 * priorité ; le chemin trouvé reste optimal, mais avec bien moins
 * d'expansions qu'A*-8.</p>
 *
 * <p>Réutilise l'ossature et l'encodage d'{@link AStar4}/{@link AStar8} (file
 * indexée, tableaux plats, {@code idx = x*H + y}). Le travail spécifique est
 * dans {@link #jump}/{@link #jumpAxial}, qui font avancer le « rayon » de saut.</p>
 */
public final class JumpPointSearch implements Pathfinder {

    // Contexte de la recherche en cours. Stocké en champs (plutôt que passé en
    // argument) pour alléger la signature des sauts, appelés très souvent.
    private GridLike grid;
    private int H;
    private int gx, gy;
    /** Toutes les cases franchies par les sauts — uniquement pour la visualisation. */
    private List<Vec2> jumpExplored;

    @Override public String name() { return "Jump Point Search"; }

    @Override
    public PathResult find(PathRequest req) {
        long t0 = System.nanoTime();
        PathResult.Builder rb = PathResult.builder().algorithmName(name());

        World w = req.world();
        if (!(w instanceof GridLike g)) {
            return rb.success(false).failureReason("JPS nécessite une grille discrète")
                    .elapsedNanos(System.nanoTime() - t0).build();
        }
        if (g.startCellX() < 0 || g.goalCellX() < 0) {
            return rb.success(false).failureReason("Départ ou arrivée non défini")
                    .elapsedNanos(System.nanoTime() - t0).build();
        }
        this.grid = g;
        this.H = g.gridHeight();
        this.gx = g.goalCellX();
        this.gy = g.goalCellY();
        this.jumpExplored = new ArrayList<>();

        int sx = g.startCellX(), sy = g.startCellY();
        int W = g.gridWidth();
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
        open.insert(startIdx, AStar8.octile(sx, sy, gx, gy));
        generated++;

        while (!open.isEmpty()) {
            maxOpen = Math.max(maxOpen, open.size());
            int curIdx = open.extractMin();
            int cx = curIdx / H, cy = curIdx % H;
            expanded++;
            explored.add(Vec2.ofCellCenter(cx, cy));

            if (curIdx == goalIdx) {
                // parent[] ne relie que des points de saut : on re-remplit les
                // cases intermédiaires pour obtenir un tracé continu à l'écran.
                List<Vec2> dense = densify(AStar4.reconstruct(parent, goalIdx, H));
                rb.success(true).path(dense).explored(mergeExplored(explored))
                        .nodesExpanded(expanded).nodesGenerated(generated)
                        .maxOpenSize(maxOpen).elapsedNanos(System.nanoTime() - t0);
                PathResult.fillGeometry(rb, dense);
                return rb.build();
            }

            // Pour chaque direction utile, on saute jusqu'au prochain point de saut.
            for (long dir : pruneDirections(parent, curIdx, cx, cy)) {
                int dx = (int) (dir >> 32);
                int dy = (int) dir;
                int jpIdx = jump(cx, cy, dx, dy);
                if (jpIdx < 0) continue;          // direction sans issue
                int jx = jpIdx / H, jy = jpIdx % H;
                double ng = bestG[curIdx] + AStar8.octile(cx, cy, jx, jy);
                if (ng < bestG[jpIdx]) {          // relâchement, comme dans A*
                    bestG[jpIdx] = ng;
                    parent[jpIdx] = curIdx;
                    double f = ng + AStar8.octile(jx, jy, gx, gy);
                    if (!open.contains(jpIdx)) {
                        open.insert(jpIdx, f);
                        generated++;
                    } else {
                        open.decreaseKey(jpIdx, f);
                    }
                }
            }
        }

        return rb.success(false).failureReason("Aucun chemin").explored(mergeExplored(explored))
                .nodesExpanded(expanded).nodesGenerated(generated).maxOpenSize(maxOpen)
                .elapsedNanos(System.nanoTime() - t0).build();
    }

    /**
     * Quelles directions valent la peine d'être sautées depuis la case
     * courante ? On part de la direction d'où l'on vient (inutile de revenir en
     * arrière), plus les directions « forcées » par un obstacle voisin.
     *
     * <p>Chaque direction est empaquetée dans un {@code long} :
     * {@code (dx << 32) | (dy & 0xFFFFFFFF)}, pour renvoyer un tableau de
     * primitives plutôt qu'une liste d'objets {@code int[2]}.</p>
     */
    private long[] pruneDirections(int[] parent, int curIdx, int cx, int cy) {
        int parIdx = parent[curIdx];
        if (parIdx < 0) {
            // Case de départ : aucune direction privilégiée, on tente les 8.
            long[] all = new long[8];
            int k = 0;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0) continue;
                    all[k++] = pack(dx, dy);
                }
            }
            return all;
        }
        // Direction d'arrivée sur la case (signe du déplacement depuis le parent).
        int pdx = Integer.signum(cx - parIdx / H);
        int pdy = Integer.signum(cy - parIdx % H);
        long[] dirs = new long[5];   // au plus : direction principale + 2 axes + 2 forcées
        int k = 0;
        if (pdx != 0 && pdy != 0) {
            // Arrivée en diagonale : on continue tout droit + les deux axes.
            dirs[k++] = pack(pdx, 0);
            dirs[k++] = pack(0, pdy);
            dirs[k++] = pack(pdx, pdy);
            // Voisins forcés : un obstacle « dans le dos » oblige à contourner.
            if (!grid.isFreeCell(cx - pdx, cy) && grid.isFreeCell(cx - pdx, cy + pdy))
                dirs[k++] = pack(-pdx, pdy);
            if (!grid.isFreeCell(cx, cy - pdy) && grid.isFreeCell(cx + pdx, cy - pdy))
                dirs[k++] = pack(pdx, -pdy);
        } else if (pdx != 0) {
            // Arrivée à l'horizontale : on continue, sauf voisin forcé au-dessus/dessous.
            dirs[k++] = pack(pdx, 0);
            if (!grid.isFreeCell(cx, cy + 1) && grid.isFreeCell(cx + pdx, cy + 1)) dirs[k++] = pack(pdx, 1);
            if (!grid.isFreeCell(cx, cy - 1) && grid.isFreeCell(cx + pdx, cy - 1)) dirs[k++] = pack(pdx, -1);
        } else {
            // Arrivée à la verticale : symétrique du cas précédent.
            dirs[k++] = pack(0, pdy);
            if (!grid.isFreeCell(cx + 1, cy) && grid.isFreeCell(cx + 1, cy + pdy)) dirs[k++] = pack(1, pdy);
            if (!grid.isFreeCell(cx - 1, cy) && grid.isFreeCell(cx - 1, cy + pdy)) dirs[k++] = pack(-1, pdy);
        }
        return Arrays.copyOf(dirs, k);
    }

    /**
     * Avance depuis (x, y) dans la direction (dx, dy) jusqu'au prochain point
     * de saut. Renvoie son index {@code x*H + y}, ou {@code -1} si la direction
     * bute sur un mur ou le bord sans rien révéler d'intéressant.
     */
    private int jump(int x, int y, int dx, int dy) {
        // Une direction purement horizontale ou verticale : tout est dans jumpAxial.
        if (dx == 0 || dy == 0) return jumpAxial(x, y, dx, dy);

        // Marche diagonale, pas à pas.
        while (true) {
            int nx = x + dx, ny = y + dy;
            if (!grid.isFreeCell(nx, ny)) return -1;
            // Diagonale interdite si elle frôle le coin de deux obstacles.
            if (!grid.isFreeCell(x + dx, y) && !grid.isFreeCell(x, y + dy)) return -1;
            jumpExplored.add(Vec2.ofCellCenter(nx, ny));

            if (nx == gx && ny == gy) return nx * H + ny;
            // Un voisin forcé ici rend cette case incontournable → point de saut.
            if ((!grid.isFreeCell(nx - dx, ny) && grid.isFreeCell(nx - dx, ny + dy))
                    || (!grid.isFreeCell(nx, ny - dy) && grid.isFreeCell(nx + dx, ny - dy))) {
                return nx * H + ny;
            }
            // Avant de continuer la diagonale, on sonde les deux axes depuis ici.
            // Si l'un d'eux trouve un point de saut, cette case en devient un aussi.
            if (jumpAxial(nx, ny, dx, 0) >= 0) return nx * H + ny;
            if (jumpAxial(nx, ny, 0, dy) >= 0) return nx * H + ny;

            x = nx; y = ny;
        }
    }

    /** Variante horizontale/verticale de {@link #jump} (un seul de dx/dy est non nul). */
    private int jumpAxial(int x, int y, int dx, int dy) {
        while (true) {
            int nx = x + dx, ny = y + dy;
            if (!grid.isFreeCell(nx, ny)) return -1;
            jumpExplored.add(Vec2.ofCellCenter(nx, ny));

            if (nx == gx && ny == gy) return nx * H + ny;
            if (dx != 0) {   // déplacement horizontal : obstacles au-dessus/dessous ?
                if ((!grid.isFreeCell(nx, ny + 1) && grid.isFreeCell(nx + dx, ny + 1))
                        || (!grid.isFreeCell(nx, ny - 1) && grid.isFreeCell(nx + dx, ny - 1))) {
                    return nx * H + ny;
                }
            } else {         // déplacement vertical : obstacles à gauche/droite ?
                if ((!grid.isFreeCell(nx + 1, ny) && grid.isFreeCell(nx + 1, ny + dy))
                        || (!grid.isFreeCell(nx - 1, ny) && grid.isFreeCell(nx - 1, ny + dy))) {
                    return nx * H + ny;
                }
            }
            x = nx; y = ny;
        }
    }

    private static long pack(int dx, int dy) {
        return ((long) dx << 32) | (dy & 0xFFFFFFFFL);
    }

    /** Concatène les cases expansées et les cases franchies par les sauts. */
    private List<Vec2> mergeExplored(List<Vec2> expanded) {
        List<Vec2> all = new ArrayList<>(expanded.size() + jumpExplored.size());
        all.addAll(expanded);
        all.addAll(jumpExplored);
        return all;
    }

    /**
     * Le chemin ne contient que des points de saut, parfois très espacés. On
     * réinsère les cases intermédiaires (toujours alignées) pour que le tracé
     * affiché suive bien la grille.
     */
    private static List<Vec2> densify(List<Vec2> jumpPoints) {
        if (jumpPoints.size() < 2) return jumpPoints;
        List<Vec2> dense = new ArrayList<>();
        dense.add(jumpPoints.get(0));
        for (int i = 1; i < jumpPoints.size(); i++) {
            Vec2 a = jumpPoints.get(i - 1);
            Vec2 b = jumpPoints.get(i);
            int ax = (int) Math.floor(a.x()), ay = (int) Math.floor(a.y());
            int bx = (int) Math.floor(b.x()), by = (int) Math.floor(b.y());
            int sx = Integer.signum(bx - ax), sy = Integer.signum(by - ay);
            int x = ax, y = ay;
            while (x != bx || y != by) {
                x += sx; y += sy;
                dense.add(Vec2.ofCellCenter(x, y));
            }
        }
        return dense;
    }
}
