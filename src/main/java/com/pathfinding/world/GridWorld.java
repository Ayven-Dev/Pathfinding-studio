package com.pathfinding.world;

import com.pathfinding.api.Bounds;
import com.pathfinding.api.GridLike;
import com.pathfinding.api.Vec2;
import com.pathfinding.api.World;

import java.util.Arrays;

/**
 * Monde "grille discrète" : un quadrillage rectangulaire de cases, chacune
 * étant soit libre soit un obstacle. Les coordonnées continues sont
 * interprétées comme étant à l'échelle d'une case (1 unité = 1 case), et le
 * centre de la case (cx, cy) est à (cx + 0.5, cy + 0.5).
 *
 * <p>Implémente à la fois {@link World} (pour les algos any-angle) et
 * {@link GridLike} (pour les algos spécifiques grille).</p>
 */
public final class GridWorld implements World, GridLike {

    public enum CellType { EMPTY, OBSTACLE, START, GOAL }

    private final int width;
    private final int height;
    private final CellType[][] cells;
    private int startX = -1, startY = -1;
    private int goalX  = -1, goalY  = -1;

    public GridWorld(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Dimensions positives requises");
        }
        this.width = width;
        this.height = height;
        this.cells = new CellType[width][height];
        for (CellType[] col : cells) Arrays.fill(col, CellType.EMPTY);
    }

    // ---------- mutations (utilisées par l'éditeur UI) ----------

    public void setObstacle(int x, int y, boolean obstacle) {
        if (!inBounds(x, y)) return;
        if (obstacle) {
            if (cells[x][y] == CellType.START) startX = startY = -1;
            if (cells[x][y] == CellType.GOAL)  goalX  = goalY  = -1;
            cells[x][y] = CellType.OBSTACLE;
        } else if (cells[x][y] == CellType.OBSTACLE) {
            cells[x][y] = CellType.EMPTY;
        }
    }

    public void setStart(int x, int y) {
        if (!inBounds(x, y) || cells[x][y] == CellType.OBSTACLE) return;
        if (startX >= 0) cells[startX][startY] = CellType.EMPTY;
        startX = x; startY = y;
        cells[x][y] = CellType.START;
    }

    public void setGoal(int x, int y) {
        if (!inBounds(x, y) || cells[x][y] == CellType.OBSTACLE) return;
        if (goalX >= 0) cells[goalX][goalY] = CellType.EMPTY;
        goalX = x; goalY = y;
        cells[x][y] = CellType.GOAL;
    }

    public void clearAll() {
        for (CellType[] col : cells) Arrays.fill(col, CellType.EMPTY);
        startX = startY = goalX = goalY = -1;
    }

    public void clearObstacles() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (cells[x][y] == CellType.OBSTACLE) cells[x][y] = CellType.EMPTY;
            }
        }
    }

    public CellType cellAt(int x, int y) { return cells[x][y]; }

    public boolean inBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    // ---------- GridLike ----------

    @Override public int gridWidth()  { return width; }
    @Override public int gridHeight() { return height; }
    @Override public boolean isFreeCell(int cx, int cy) {
        return inBounds(cx, cy) && cells[cx][cy] != CellType.OBSTACLE;
    }
    @Override public int startCellX() { return startX; }
    @Override public int startCellY() { return startY; }
    @Override public int goalCellX()  { return goalX; }
    @Override public int goalCellY()  { return goalY; }

    // ---------- World ----------

    @Override
    public Vec2 start() {
        return startX < 0 ? null : Vec2.ofCellCenter(startX, startY);
    }

    @Override
    public Vec2 goal() {
        return goalX < 0 ? null : Vec2.ofCellCenter(goalX, goalY);
    }

    @Override
    public Bounds bounds() {
        return new Bounds(0, 0, width, height);
    }

    @Override
    public boolean isFree(Vec2 p) {
        int cx = (int) Math.floor(p.x());
        int cy = (int) Math.floor(p.y());
        return isFreeCell(cx, cy);
    }

    /**
     * Visibilité directe via parcours type Bresenham. Chaque case rencontrée
     * doit être libre — utilisé par Theta* et compagnie.
     */
    @Override
    public boolean lineOfSight(Vec2 a, Vec2 b) {
        int x0 = (int) Math.floor(a.x());
        int y0 = (int) Math.floor(a.y());
        int x1 = (int) Math.floor(b.x());
        int y1 = (int) Math.floor(b.y());
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        int x = x0, y = y0;
        while (true) {
            if (!isFreeCell(x, y)) return false;
            if (x == x1 && y == y1) return true;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x += sx; }
            if (e2 < dx)  { err += dx; y += sy; }
        }
    }
}
