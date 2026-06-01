package com.pathfinding.world;

import com.pathfinding.api.Bounds;
import com.pathfinding.api.Obstacle;
import com.pathfinding.api.Vec2;
import com.pathfinding.api.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Monde continu : zone rectangulaire à coordonnées réelles peuplée
 * d'obstacles polygonaux (formes quelconques, pas forcément cubiques).
 *
 * <p>Les tests {@link #isFree(Vec2)} et {@link #lineOfSight(Vec2, Vec2)} sont
 * géométriques exacts : on teste l'appartenance d'un point à un polygone par
 * lancer de rayon et l'intersection segment/polygone par décomposition en
 * tests segment/segment.</p>
 */
public final class ContinuousWorld implements World {

    private final Bounds bounds;
    private final List<Obstacle> obstacles = new ArrayList<>();
    private Vec2 start;
    private Vec2 goal;

    public ContinuousWorld(double width, double height) {
        this.bounds = new Bounds(0, 0, width, height);
    }

    public ContinuousWorld(Bounds bounds) {
        this.bounds = bounds;
    }

    // ---------- mutations (éditeur UI) ----------

    public void addObstacle(Obstacle o) { obstacles.add(o); }

    public boolean removeObstacleAt(Vec2 p) {
        for (int i = obstacles.size() - 1; i >= 0; i--) {
            if (obstacles.get(i).contains(p)) {
                obstacles.remove(i);
                return true;
            }
        }
        return false;
    }

    public void clearObstacles() { obstacles.clear(); }

    public void clearAll() {
        obstacles.clear();
        start = null;
        goal = null;
    }

    public void setStart(Vec2 p) {
        if (p != null && bounds.contains(p) && !insideAnyObstacle(p)) start = p;
    }

    public void setGoal(Vec2 p) {
        if (p != null && bounds.contains(p) && !insideAnyObstacle(p)) goal = p;
    }

    public List<Obstacle> obstacles() { return Collections.unmodifiableList(obstacles); }

    private boolean insideAnyObstacle(Vec2 p) {
        for (Obstacle o : obstacles) if (o.contains(p)) return true;
        return false;
    }

    // ---------- World ----------

    @Override public Vec2 start()    { return start; }
    @Override public Vec2 goal()     { return goal; }
    @Override public Bounds bounds() { return bounds; }

    @Override
    public boolean isFree(Vec2 p) {
        if (!bounds.contains(p)) return false;
        return !insideAnyObstacle(p);
    }

    @Override
    public boolean lineOfSight(Vec2 a, Vec2 b) {
        // Hors limites = pas de visibilité
        if (!bounds.contains(a) || !bounds.contains(b)) return false;
        for (Obstacle o : obstacles) {
            if (o.intersectsSegment(a, b)) return false;
        }
        return true;
    }
}
