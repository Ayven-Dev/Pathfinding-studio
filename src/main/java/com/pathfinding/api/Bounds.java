package com.pathfinding.api;

/** Boîte englobante axis-aligned. Sert à l'échantillonnage uniforme et au clipping UI. */
public record Bounds(double minX, double minY, double maxX, double maxY) {

    public double width()  { return maxX - minX; }
    public double height() { return maxY - minY; }

    public boolean contains(Vec2 p) {
        return p.x() >= minX && p.x() <= maxX
            && p.y() >= minY && p.y() <= maxY;
    }
}
