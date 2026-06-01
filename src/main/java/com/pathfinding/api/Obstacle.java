package com.pathfinding.api;

import java.util.List;

/**
 * Obstacle dans un monde continu. Pour ce prototype, un obstacle est un
 * polygone (potentiellement non convexe) défini par une liste de sommets dans
 * l'ordre (sens horaire ou trigo, peu importe).
 *
 * <p>Les tests géométriques utilisés :
 * <ul>
 *   <li>{@link #contains(Vec2)} : test point-dans-polygone par lancer de
 *       rayon (ray casting).</li>
 *   <li>{@link #intersectsSegment(Vec2, Vec2)} : test segment-vs-polygone
 *       (intersection avec n'importe quel côté ou point intérieur).</li>
 * </ul>
 */
public final class Obstacle {

    private final List<Vec2> vertices;
    private final Bounds bbox;

    public Obstacle(List<Vec2> vertices) {
        if (vertices == null || vertices.size() < 3) {
            throw new IllegalArgumentException("Un polygone doit avoir au moins 3 sommets");
        }
        this.vertices = List.copyOf(vertices);
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        for (Vec2 v : this.vertices) {
            minX = Math.min(minX, v.x()); minY = Math.min(minY, v.y());
            maxX = Math.max(maxX, v.x()); maxY = Math.max(maxY, v.y());
        }
        this.bbox = new Bounds(minX, minY, maxX, maxY);
    }

    public List<Vec2> vertices() { return vertices; }

    public Bounds boundingBox() { return bbox; }

    /** Point dans le polygone — algorithme du lancer de rayon horizontal. */
    public boolean contains(Vec2 p) {
        if (!bbox.contains(p)) return false;
        boolean inside = false;
        int n = vertices.size();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            Vec2 vi = vertices.get(i);
            Vec2 vj = vertices.get(j);
            boolean intersect = ((vi.y() > p.y()) != (vj.y() > p.y()))
                    && (p.x() < (vj.x() - vi.x()) * (p.y() - vi.y())
                                / (vj.y() - vi.y() + 1e-12) + vi.x());
            if (intersect) inside = !inside;
        }
        return inside;
    }

    /** Vrai si le segment [a, b] touche ou traverse le polygone. */
    public boolean intersectsSegment(Vec2 a, Vec2 b) {
        // Si une extrémité est strictement à l'intérieur, c'est une collision.
        if (contains(a) || contains(b)) return true;
        int n = vertices.size();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            if (segmentsIntersect(a, b, vertices.get(j), vertices.get(i))) {
                return true;
            }
        }
        return false;
    }

    /** Intersection segment-segment 2D (inclut les cas dégénérés colinéaires). */
    private static boolean segmentsIntersect(Vec2 p1, Vec2 p2, Vec2 p3, Vec2 p4) {
        double d1 = cross(p4.sub(p3), p1.sub(p3));
        double d2 = cross(p4.sub(p3), p2.sub(p3));
        double d3 = cross(p2.sub(p1), p3.sub(p1));
        double d4 = cross(p2.sub(p1), p4.sub(p1));
        if (((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0))
                && ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0))) {
            return true;
        }
        if (d1 == 0 && onSegment(p3, p4, p1)) return true;
        if (d2 == 0 && onSegment(p3, p4, p2)) return true;
        if (d3 == 0 && onSegment(p1, p2, p3)) return true;
        if (d4 == 0 && onSegment(p1, p2, p4)) return true;
        return false;
    }

    private static double cross(Vec2 a, Vec2 b) { return a.x() * b.y() - a.y() * b.x(); }

    private static boolean onSegment(Vec2 a, Vec2 b, Vec2 p) {
        return Math.min(a.x(), b.x()) <= p.x() && p.x() <= Math.max(a.x(), b.x())
            && Math.min(a.y(), b.y()) <= p.y() && p.y() <= Math.max(a.y(), b.y());
    }
}
