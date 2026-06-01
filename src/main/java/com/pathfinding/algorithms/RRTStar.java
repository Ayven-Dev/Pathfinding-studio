package com.pathfinding.algorithms;

import com.pathfinding.algorithms.internal.KdTree;
import com.pathfinding.api.Bounds;
import com.pathfinding.api.PathRequest;
import com.pathfinding.api.PathResult;
import com.pathfinding.api.Pathfinder;
import com.pathfinding.api.Vec2;
import com.pathfinding.api.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * RRT* — planificateur par échantillonnage aléatoire, à coût asymptotiquement
 * optimal.
 *
 * <p>Plutôt que d'explorer des cases, on fait pousser un arbre dans l'espace
 * libre : on tire des points au hasard, on relie chaque nouveau point à
 * l'arbre par un court segment vérifié sans collision, et on réorganise les
 * liaisons proches pour réduire les coûts au fil de l'eau. Plus on tire de
 * points, plus le chemin se rapproche de l'optimum. Comme tout passe par
 * {@code isFree}/{@code lineOfSight}, l'algorithme fonctionne aussi bien sur
 * une carte continue que sur une grille — d'où {@link #supportsContinuous()}.</p>
 *
 * <p>Détail d'implémentation important : les voisins se cherchent via un
 * {@link KdTree} (≈ O(log n)) et non par balayage de tout l'arbre, sinon le
 * coût deviendrait quadratique en le nombre d'itérations.</p>
 *
 * <p>Paramètres réglables au constructeur : nombre d'itérations, longueur de
 * pas, biais vers le but, rayon de reconnexion, tolérance d'arrivée.</p>
 */
public final class RRTStar implements Pathfinder {

    private final int maxIterations;
    private final double stepSize;
    private final double goalBias;       // probabilité de viser directement le but
    private final double rewireRadius;   // rayon de recherche des voisins
    private final double goalThreshold;  // distance sous laquelle on tente de relier le but

    public RRTStar() {
        this(3000, 1.5, 0.10, 3.0, 1.0);
    }

    public RRTStar(int maxIterations, double stepSize, double goalBias,
                   double rewireRadius, double goalThreshold) {
        this.maxIterations = maxIterations;
        this.stepSize = stepSize;
        this.goalBias = goalBias;
        this.rewireRadius = rewireRadius;
        this.goalThreshold = goalThreshold;
    }

    @Override public String name() { return "RRT*"; }

    @Override public boolean supportsContinuous() { return true; }

    @Override
    public PathResult find(PathRequest req) {
        long t0 = System.nanoTime();
        PathResult.Builder rb = PathResult.builder().algorithmName(name());

        World w = req.world();
        Vec2 start = w.start();
        Vec2 goal  = w.goal();
        if (start == null || goal == null) {
            return rb.success(false).failureReason("Départ ou arrivée non défini")
                    .elapsedNanos(System.nanoTime() - t0).build();
        }

        Random rng = new Random(req.randomSeed());
        Bounds B = w.bounds();

        // L'arbre : un nœud par sommet (position, parent, coût depuis le départ).
        // Le KdTree indexe les mêmes positions pour les recherches de voisinage.
        List<TreeNode> tree = new ArrayList<>(maxIterations + 1);
        KdTree spatial = new KdTree();
        tree.add(new TreeNode(start, -1, 0));
        spatial.insert(start, 0);

        // Meilleure connexion au but trouvée jusqu'ici (on garde la moins chère).
        int bestGoalParent = -1;
        double bestGoalCost = Double.POSITIVE_INFINITY;

        int expanded = 0, generated = 1;
        List<Integer> nearBuf = new ArrayList<>();  // tampon de voisins, réutilisé chaque tour

        for (int iter = 0; iter < maxIterations; iter++) {
            // (1) Tirer une cible : de temps en temps le but lui-même, pour
            //     accélérer la convergence ; sinon un point libre au hasard.
            Vec2 qRand = (rng.nextDouble() < goalBias) ? goal : sampleFree(w, B, rng);
            if (qRand == null) continue;

            // (2) Trouver le nœud le plus proche, puis n'avancer que d'un pas
            //     vers la cible. On rejette si ce petit segment heurte un obstacle.
            int nearestIdx = spatial.nearest(qRand);
            TreeNode nearest = tree.get(nearestIdx);
            Vec2 qNew = steer(nearest.position, qRand, stepSize);
            if (!w.isFree(qNew) || !w.lineOfSight(nearest.position, qNew)) continue;
            expanded++;

            // (3) Choisir le meilleur parent : parmi les nœuds proches de qNew,
            //     celui qui minimise le coût total tout en restant en vue directe.
            nearBuf.clear();
            spatial.range(qNew, rewireRadius, nearBuf::add);
            int bestParent = nearestIdx;
            double bestCost = nearest.cost + nearest.position.distance(qNew);
            for (int i = 0, n = nearBuf.size(); i < n; i++) {
                int idx = nearBuf.get(i);
                if (idx == nearestIdx) continue;
                TreeNode cand = tree.get(idx);
                double c = cand.cost + cand.position.distance(qNew);
                if (c < bestCost && w.lineOfSight(cand.position, qNew)) {
                    bestParent = idx;
                    bestCost = c;
                }
            }

            int newIdx = tree.size();
            tree.add(new TreeNode(qNew, bestParent, bestCost));
            spatial.insert(qNew, newIdx);
            generated++;

            // (4) Reconnexion : maintenant que qNew existe, certains voisins
            //     gagneraient à passer par lui. On les rebranche si c'est moins cher.
            for (int i = 0, n = nearBuf.size(); i < n; i++) {
                int idx = nearBuf.get(i);
                if (idx == bestParent) continue;
                TreeNode cand = tree.get(idx);
                double c = bestCost + qNew.distance(cand.position);
                if (c < cand.cost && w.lineOfSight(qNew, cand.position)) {
                    cand.parent = newIdx;
                    cand.cost = c;
                }
            }

            // (5) Si qNew voit le but et en est assez proche, mémoriser cette
            //     connexion candidate (on continue malgré tout : un tour ultérieur
            //     peut produire un raccordement meilleur marché).
            if (qNew.distance(goal) <= goalThreshold && w.lineOfSight(qNew, goal)) {
                double total = bestCost + qNew.distance(goal);
                if (total < bestGoalCost) {
                    bestGoalCost = total;
                    bestGoalParent = newIdx;
                }
            }
        }

        // Trace de visualisation : l'ensemble des sommets de l'arbre.
        List<Vec2> explored = new ArrayList<>(tree.size());
        for (TreeNode n : tree) explored.add(n.position);

        if (bestGoalParent < 0) {
            return rb.success(false).failureReason("Pas de chemin trouvé en " + maxIterations + " itérations")
                    .explored(explored)
                    .nodesExpanded(expanded).nodesGenerated(generated)
                    .elapsedNanos(System.nanoTime() - t0).build();
        }

        List<Vec2> path = reconstruct(tree, bestGoalParent, goal);
        rb.success(true).path(path).explored(explored)
                .nodesExpanded(expanded).nodesGenerated(generated)
                .maxOpenSize(generated).elapsedNanos(System.nanoTime() - t0);
        PathResult.fillGeometry(rb, path);
        return rb.build();
    }

    /**
     * Tire un point libre dans les limites du monde. Quelques essais suffisent
     * en général ; au-delà on renonce pour ce tour (l'espace est peut-être très
     * encombré) plutôt que de boucler indéfiniment.
     */
    static Vec2 sampleFree(World w, Bounds B, Random rng) {
        for (int i = 0; i < 30; i++) {
            double x = B.minX() + rng.nextDouble() * B.width();
            double y = B.minY() + rng.nextDouble() * B.height();
            Vec2 v = new Vec2(x, y);
            if (w.isFree(v)) return v;
        }
        return null;
    }

    /** Ramène {@code to} à au plus {@code max} de distance de {@code from} (étape « steer »). */
    static Vec2 steer(Vec2 from, Vec2 to, double max) {
        double d = from.distance(to);
        if (d <= max) return to;
        Vec2 dir = to.sub(from).normalized();
        return from.add(dir.scale(max));
    }

    /** Remonte les parents depuis le nœud relié au but, en plaçant le but en tête. */
    static List<Vec2> reconstruct(List<TreeNode> tree, int parentOfGoal, Vec2 goal) {
        List<Vec2> out = new ArrayList<>();
        out.add(goal);
        for (int idx = parentOfGoal; idx >= 0; idx = tree.get(idx).parent) {
            out.add(tree.get(idx).position);
        }
        Collections.reverse(out);
        return out;
    }

    /**
     * Sommet de l'arbre. {@code parent} est un index dans la liste {@code tree}
     * (-1 pour la racine) ; {@code cost} est le coût cumulé depuis le départ.
     * Partagé avec {@link ThetaStarRRT}, d'où la visibilité package.
     */
    static final class TreeNode {
        final Vec2 position;
        int parent;
        double cost;
        TreeNode(Vec2 position, int parent, double cost) {
            this.position = position;
            this.parent = parent;
            this.cost = cost;
        }
    }
}
