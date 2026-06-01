package com.pathfinding.algorithms.internal;

import com.pathfinding.api.Vec2;

import java.util.function.IntConsumer;

/**
 * Arbre kd 2D pour requêtes de plus proche voisin et de voisinage radial.
 * Sert aux planificateurs sampling-based (RRT*, Theta*-RRT*) : remplace une
 * recherche linéaire O(n) sur l'arbre en croissance par une descente
 * récursive O(log n) en moyenne. Pour 3000 nœuds, le gain dépasse 10x.
 *
 * <p>Chaque entrée stocke un point 2D et un identifiant entier externe
 * (typiquement l'index dans un {@code ArrayList&lt;TreeNode&gt;}).</p>
 *
 * <p>Implémentation simple, non-équilibrée : on insère dans l'ordre, donc
 * l'arbre peut dégénérer si les points sont quasi-colinéaires. Pour de
 * l'échantillonnage uniforme la profondeur reste O(log n) en pratique.</p>
 */
public final class KdTree {

    private Node root;
    private int size;

    public int size() { return size; }
    public boolean isEmpty() { return root == null; }

    /** Ajoute un point avec son identifiant externe. */
    public void insert(Vec2 point, int dataIdx) {
        root = insertRec(root, point, dataIdx, 0);
        size++;
    }

    private static Node insertRec(Node n, Vec2 point, int dataIdx, int depth) {
        if (n == null) return new Node(point, dataIdx, depth & 1);
        double cmp = (n.axis == 0 ? point.x() - n.point.x() : point.y() - n.point.y());
        if (cmp < 0) n.left  = insertRec(n.left,  point, dataIdx, depth + 1);
        else         n.right = insertRec(n.right, point, dataIdx, depth + 1);
        return n;
    }

    /** Renvoie l'identifiant du point le plus proche de {@code q}, ou -1 si l'arbre est vide. */
    public int nearest(Vec2 q) {
        if (root == null) return -1;
        State s = new State();
        s.bestIdx = -1;
        s.bestDistSq = Double.POSITIVE_INFINITY;
        nearestRec(root, q, s);
        return s.bestIdx;
    }

    private static void nearestRec(Node n, Vec2 q, State s) {
        if (n == null) return;
        double dSq = n.point.distanceSq(q);
        if (dSq < s.bestDistSq) {
            s.bestDistSq = dSq;
            s.bestIdx = n.data;
        }
        double diff = (n.axis == 0 ? q.x() - n.point.x() : q.y() - n.point.y());
        Node near = diff < 0 ? n.left : n.right;
        Node far  = diff < 0 ? n.right : n.left;
        nearestRec(near, q, s);
        // On ne descend dans la branche "lointaine" que si la sphère du
        // meilleur courant peut la traverser : test diff² < bestDistSq.
        if (diff * diff < s.bestDistSq) nearestRec(far, q, s);
    }

    /**
     * Énumère tous les identifiants dont le point se trouve dans un rayon
     * {@code radius} autour de {@code q}. Passé à {@code consumer.accept(idx)}.
     */
    public void range(Vec2 q, double radius, IntConsumer consumer) {
        rangeRec(root, q, radius * radius, consumer);
    }

    private static void rangeRec(Node n, Vec2 q, double rSq, IntConsumer consumer) {
        if (n == null) return;
        if (n.point.distanceSq(q) <= rSq) consumer.accept(n.data);
        double diff = (n.axis == 0 ? q.x() - n.point.x() : q.y() - n.point.y());
        Node near = diff < 0 ? n.left : n.right;
        Node far  = diff < 0 ? n.right : n.left;
        rangeRec(near, q, rSq, consumer);
        if (diff * diff <= rSq) rangeRec(far, q, rSq, consumer);
    }

    private static final class Node {
        final Vec2 point;
        final int data;
        final int axis;   // 0 = x, 1 = y (alterne en descendant)
        Node left, right;
        Node(Vec2 point, int data, int axis) {
            this.point = point;
            this.data = data;
            this.axis = axis;
        }
    }

    /** Petit conteneur mutable pour la descente du plus proche voisin. */
    private static final class State {
        int bestIdx;
        double bestDistSq;
    }
}
