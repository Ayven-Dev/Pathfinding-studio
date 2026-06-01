package com.pathfinding.algorithms;

import com.pathfinding.algorithms.internal.KdTree;
import com.pathfinding.api.Bounds;
import com.pathfinding.api.PathRequest;
import com.pathfinding.api.PathResult;
import com.pathfinding.api.Pathfinder;
import com.pathfinding.api.Vec2;
import com.pathfinding.api.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Theta*-RRT* : RRT* dont on raccourcit les liaisons à la manière de Theta*.
 *
 * <p>RRT* relie chaque nouveau point à un voisin proche, ce qui produit des
 * chemins « en zigzag » même en terrain dégagé. Ici, juste après avoir choisi
 * le meilleur parent, on tente de remonter encore : tant qu'un ancêtre plus
 * lointain reste en vue directe du nouveau point, on s'y raccroche
 * directement. Les segments deviennent quasi rectilignes et les virages
 * cumulés chutent — un chemin plus direct et moins sinueux.</p>
 *
 * <p>Tout le reste (échantillonnage, KdTree, reconnexion, raccordement au but)
 * est identique à {@link RRTStar}, dont on réutilise les routines statiques et
 * le type de nœud. Seul le bloc « remontée des ancêtres » est ajouté.</p>
 */
public final class ThetaStarRRT implements Pathfinder {

    private final int maxIterations;
    private final double stepSize;
    private final double goalBias;
    private final double rewireRadius;
    private final double goalThreshold;

    public ThetaStarRRT() {
        this(3000, 1.5, 0.10, 3.0, 1.0);
    }

    public ThetaStarRRT(int maxIterations, double stepSize, double goalBias,
                        double rewireRadius, double goalThreshold) {
        this.maxIterations = maxIterations;
        this.stepSize = stepSize;
        this.goalBias = goalBias;
        this.rewireRadius = rewireRadius;
        this.goalThreshold = goalThreshold;
    }

    @Override public String name() { return "Theta*-RRT*"; }

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

        List<RRTStar.TreeNode> tree = new ArrayList<>(maxIterations + 1);
        KdTree spatial = new KdTree();
        tree.add(new RRTStar.TreeNode(start, -1, 0));
        spatial.insert(start, 0);

        int bestGoalParent = -1;
        double bestGoalCost = Double.POSITIVE_INFINITY;

        int expanded = 0, generated = 1;
        List<Integer> nearBuf = new ArrayList<>();

        for (int iter = 0; iter < maxIterations; iter++) {
            Vec2 qRand = (rng.nextDouble() < goalBias) ? goal : RRTStar.sampleFree(w, B, rng);
            if (qRand == null) continue;

            int nearestIdx = spatial.nearest(qRand);
            RRTStar.TreeNode nearest = tree.get(nearestIdx);

            Vec2 qNew = RRTStar.steer(nearest.position, qRand, stepSize);
            if (!w.isFree(qNew) || !w.lineOfSight(nearest.position, qNew)) continue;
            expanded++;

            nearBuf.clear();
            spatial.range(qNew, rewireRadius, nearBuf::add);

            // Choisir le meilleur parent dans le voisinage (comme RRT*).
            int bestParent = nearestIdx;
            double bestCost = nearest.cost + nearest.position.distance(qNew);
            for (int i = 0, n = nearBuf.size(); i < n; i++) {
                int idx = nearBuf.get(i);
                if (idx == nearestIdx) continue;
                RRTStar.TreeNode cand = tree.get(idx);
                double c = cand.cost + cand.position.distance(qNew);
                if (c < bestCost && w.lineOfSight(cand.position, qNew)) {
                    bestParent = idx;
                    bestCost = c;
                }
            }

            // Ajout Theta* : tant qu'un ancêtre plus haut reste visible en ligne
            // droite, on saute par-dessus le parent intermédiaire. C'est ce qui
            // « tend » le chemin au lieu de suivre les coudes de l'arbre.
            int ancestor = tree.get(bestParent).parent;
            while (ancestor >= 0 && w.lineOfSight(tree.get(ancestor).position, qNew)) {
                RRTStar.TreeNode a = tree.get(ancestor);
                double c = a.cost + a.position.distance(qNew);
                if (c < bestCost) {
                    bestParent = ancestor;
                    bestCost = c;
                }
                ancestor = a.parent;
            }

            int newIdx = tree.size();
            tree.add(new RRTStar.TreeNode(qNew, bestParent, bestCost));
            spatial.insert(qNew, newIdx);
            generated++;

            // Reconnexion des voisins via qNew si cela les rend moins chers (comme RRT*).
            for (int i = 0, n = nearBuf.size(); i < n; i++) {
                int idx = nearBuf.get(i);
                if (idx == bestParent) continue;
                RRTStar.TreeNode cand = tree.get(idx);
                double c = bestCost + qNew.distance(cand.position);
                if (c < cand.cost && w.lineOfSight(qNew, cand.position)) {
                    cand.parent = newIdx;
                    cand.cost = c;
                }
            }

            if (qNew.distance(goal) <= goalThreshold && w.lineOfSight(qNew, goal)) {
                double total = bestCost + qNew.distance(goal);
                if (total < bestGoalCost) {
                    bestGoalCost = total;
                    bestGoalParent = newIdx;
                }
            }
        }

        List<Vec2> explored = new ArrayList<>(tree.size());
        for (RRTStar.TreeNode n : tree) explored.add(n.position);

        if (bestGoalParent < 0) {
            return rb.success(false).failureReason("Pas de chemin trouvé en " + maxIterations + " itérations")
                    .explored(explored)
                    .nodesExpanded(expanded).nodesGenerated(generated)
                    .elapsedNanos(System.nanoTime() - t0).build();
        }

        List<Vec2> path = RRTStar.reconstruct(tree, bestGoalParent, goal);
        rb.success(true).path(path).explored(explored)
                .nodesExpanded(expanded).nodesGenerated(generated)
                .maxOpenSize(generated).elapsedNanos(System.nanoTime() - t0);
        PathResult.fillGeometry(rb, path);
        return rb.build();
    }
}
