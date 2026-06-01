package com.pathfinding.api;

import java.util.Collections;
import java.util.List;

/**
 * Résultat d'un appel au planificateur : le chemin (s'il existe), ses
 * propriétés géométriques et l'ensemble des compteurs de profilage.
 *
 * <p>Toutes les positions sont exprimées en {@link Vec2} (coordonnées réelles)
 * pour unifier sortie discrète et sortie continue.</p>
 */
public final class PathResult {

    private final boolean success;
    private final List<Vec2> path;
    private final List<Vec2> explored;
    private final long elapsedNanos;
    private final int nodesExpanded;
    private final int nodesGenerated;
    private final int maxOpenSize;
    private final double pathLength;
    private final double totalTurnDegrees;
    private final int turnCount;
    private final String algorithmName;
    private final String failureReason;

    private PathResult(Builder b) {
        this.success = b.success;
        this.path = b.path == null ? List.of() : Collections.unmodifiableList(b.path);
        this.explored = b.explored == null ? List.of() : Collections.unmodifiableList(b.explored);
        this.elapsedNanos = b.elapsedNanos;
        this.nodesExpanded = b.nodesExpanded;
        this.nodesGenerated = b.nodesGenerated;
        this.maxOpenSize = b.maxOpenSize;
        this.pathLength = b.pathLength;
        this.totalTurnDegrees = b.totalTurnDegrees;
        this.turnCount = b.turnCount;
        this.algorithmName = b.algorithmName == null ? "?" : b.algorithmName;
        this.failureReason = b.failureReason;
    }

    public boolean success()         { return success; }
    public List<Vec2> path()         { return path; }
    /** Positions visitées par la recherche (toutes les expansions). Peut être vide. */
    public List<Vec2> explored()     { return explored; }
    public long elapsedNanos()       { return elapsedNanos; }
    public double elapsedMillis()    { return elapsedNanos / 1_000_000.0; }
    public int nodesExpanded()       { return nodesExpanded; }
    public int nodesGenerated()      { return nodesGenerated; }
    public int maxOpenSize()         { return maxOpenSize; }
    public double pathLength()       { return pathLength; }
    public double totalTurnDegrees() { return totalTurnDegrees; }
    public int turnCount()           { return turnCount; }
    public String algorithmName()    { return algorithmName; }
    public String failureReason()    { return failureReason; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private boolean success;
        private List<Vec2> path;
        private List<Vec2> explored;
        private long elapsedNanos;
        private int nodesExpanded;
        private int nodesGenerated;
        private int maxOpenSize;
        private double pathLength;
        private double totalTurnDegrees;
        private int turnCount;
        private String algorithmName;
        private String failureReason;

        public Builder success(boolean v)         { this.success = v; return this; }
        public Builder path(List<Vec2> v)         { this.path = v; return this; }
        public Builder explored(List<Vec2> v)     { this.explored = v; return this; }
        public Builder elapsedNanos(long v)       { this.elapsedNanos = v; return this; }
        public Builder nodesExpanded(int v)       { this.nodesExpanded = v; return this; }
        public Builder nodesGenerated(int v)      { this.nodesGenerated = v; return this; }
        public Builder maxOpenSize(int v)         { this.maxOpenSize = v; return this; }
        public Builder pathLength(double v)       { this.pathLength = v; return this; }
        public Builder totalTurnDegrees(double v) { this.totalTurnDegrees = v; return this; }
        public Builder turnCount(int v)           { this.turnCount = v; return this; }
        public Builder algorithmName(String v)    { this.algorithmName = v; return this; }
        public Builder failureReason(String v)    { this.failureReason = v; return this; }

        public PathResult build() { return new PathResult(this); }
    }

    /** Calcule longueur, virages cumulés et nombre de virages à partir d'une polyligne. */
    public static void fillGeometry(Builder b, List<Vec2> path) {
        if (path == null || path.size() < 2) {
            b.pathLength(0).totalTurnDegrees(0).turnCount(0);
            return;
        }
        double length = 0;
        double totalTurn = 0;
        int turns = 0;
        double prevAngle = Double.NaN;
        for (int i = 1; i < path.size(); i++) {
            Vec2 p0 = path.get(i - 1);
            Vec2 p1 = path.get(i);
            double dx = p1.x() - p0.x();
            double dy = p1.y() - p0.y();
            length += Math.sqrt(dx * dx + dy * dy);
            double angle = Math.atan2(dy, dx);
            if (!Double.isNaN(prevAngle)) {
                double diff = Math.abs(Math.toDegrees(normalizeAngle(angle - prevAngle)));
                if (diff > 1e-6) {
                    totalTurn += diff;
                    turns++;
                }
            }
            prevAngle = angle;
        }
        b.pathLength(length).totalTurnDegrees(totalTurn).turnCount(turns);
    }

    private static double normalizeAngle(double a) {
        while (a > Math.PI) a -= 2 * Math.PI;
        while (a < -Math.PI) a += 2 * Math.PI;
        return a;
    }
}
