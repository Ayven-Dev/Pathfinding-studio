package com.pathfinding.api;

/**
 * Point 2D à coordonnées réelles. Type fondamental partagé par tous les
 * algorithmes : les algos de grille travaillent au centre des cases
 * (x + 0.5, y + 0.5) et les algos continus utilisent des coordonnées libres.
 */
public record Vec2(double x, double y) {

    public static final Vec2 ZERO = new Vec2(0, 0);

    /** Crée un Vec2 à partir d'indices de case discrète (centre de la case). */
    public static Vec2 ofCellCenter(int cx, int cy) {
        return new Vec2(cx + 0.5, cy + 0.5);
    }

    public Vec2 add(Vec2 o)        { return new Vec2(x + o.x, y + o.y); }
    public Vec2 sub(Vec2 o)        { return new Vec2(x - o.x, y - o.y); }
    public Vec2 scale(double s)    { return new Vec2(x * s, y * s); }
    public double dot(Vec2 o)      { return x * o.x + y * o.y; }
    public double length()         { return Math.sqrt(x * x + y * y); }
    public double lengthSq()       { return x * x + y * y; }
    public double distance(Vec2 o) { return sub(o).length(); }
    public double distanceSq(Vec2 o) {
        double dx = x - o.x, dy = y - o.y;
        return dx * dx + dy * dy;
    }

    /** Vecteur unitaire, ou (0,0) si vecteur nul. */
    public Vec2 normalized() {
        double len = length();
        return len < 1e-12 ? ZERO : new Vec2(x / len, y / len);
    }

    /** Cap en radians (0 = +X, π/2 = +Y). */
    public double heading() {
        return Math.atan2(y, x);
    }

    @Override
    public String toString() {
        return String.format("(%.2f, %.2f)", x, y);
    }
}
