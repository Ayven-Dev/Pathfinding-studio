package com.pathfinding.ui.grid;

import com.pathfinding.api.Vec2;
import com.pathfinding.ui.theme.Theme;
import com.pathfinding.world.GridWorld;
import com.pathfinding.world.GridWorld.CellType;

import javax.swing.JPanel;
import javax.swing.event.MouseInputAdapter;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Canevas de dessin de grille. Gère l'affichage des cases, du chemin trouvé,
 * et la saisie souris pour placer départ/arrivée et tracer des obstacles
 * "à main levée".
 */
public final class GridCanvas extends JPanel {

    public enum EditMode { START, GOAL, OBSTACLE, ERASE }

    private GridWorld world;
    private int cellSize = 24;
    private EditMode mode = EditMode.OBSTACLE;
    private List<Vec2> path;
    private List<Vec2> explored;
    private boolean showExplored = false;
    private Consumer<String> statusListener;

    public GridCanvas(GridWorld world) {
        this.world = world;
        setBackground(Theme.CANVAS_BG);
        recomputeSize();
        MouseHandler h = new MouseHandler();
        addMouseListener(h);
        addMouseMotionListener(h);
    }

    public void setWorld(GridWorld world) {
        this.world = world;
        this.path = null;
        recomputeSize();
        revalidate();
        repaint();
    }

    public GridWorld getWorld() { return world; }

    public void setMode(EditMode m) { this.mode = m; }

    public EditMode getMode() { return mode; }

    public void setPath(List<Vec2> path) { this.path = path; repaint(); }

    public void clearPath() {
        this.path = null;
        this.explored = null;
        repaint();
    }

    public void setExplored(List<Vec2> explored) { this.explored = explored; repaint(); }

    public void setShowExplored(boolean v) { this.showExplored = v; repaint(); }

    public boolean isShowExplored() { return showExplored; }

    public void setStatusListener(Consumer<String> l) { this.statusListener = l; }

    public int cellSize() { return cellSize; }

    public void setCellSize(int s) {
        this.cellSize = Math.max(6, Math.min(60, s));
        recomputeSize();
        revalidate();
        repaint();
    }

    private void recomputeSize() {
        setPreferredSize(new Dimension(
                world.gridWidth() * cellSize + 1,
                world.gridHeight() * cellSize + 1));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int W = world.gridWidth(), H = world.gridHeight();

        // Couche "exploré" en rouge, dessinée en premier de sorte que les
        // obstacles, départ et arrivée la masquent (logique : ces 3 types ne
        // doivent pas porter d'indication "exploré"). Le chemin final est
        // également exclu de l'ensemble peint.
        if (showExplored && explored != null && !explored.isEmpty()) {
            Set<Long> pathSet = pathCellKeys(H);
            g2.setColor(Theme.EXPLORED);
            for (Vec2 v : explored) {
                int cx = (int) Math.floor(v.x());
                int cy = (int) Math.floor(v.y());
                if (cx < 0 || cy < 0 || cx >= W || cy >= H) continue;
                if (pathSet.contains(((long) cx) * H + cy)) continue;
                g2.fillRect(cx * cellSize, cy * cellSize, cellSize, cellSize);
            }
        }

        // Cases pleines (obstacles, départ, arrivée)
        for (int x = 0; x < W; x++) {
            for (int y = 0; y < H; y++) {
                CellType t = world.cellAt(x, y);
                Color fill = switch (t) {
                    case OBSTACLE -> Theme.OBSTACLE;
                    case START    -> Theme.START;
                    case GOAL     -> Theme.GOAL;
                    case EMPTY    -> null;
                };
                if (fill != null) {
                    g2.setColor(fill);
                    if (t == CellType.OBSTACLE) {
                        g2.fillRect(x * cellSize, y * cellSize, cellSize, cellSize);
                    } else {
                        // Départ/arrivée : pastille ronde, plus moderne
                        int pad = Math.max(2, cellSize / 6);
                        g2.fillOval(x * cellSize + pad, y * cellSize + pad,
                                cellSize - 2 * pad, cellSize - 2 * pad);
                    }
                }
            }
        }

        // Lignes de grille
        g2.setColor(Theme.GRID_LINE);
        for (int x = 0; x <= W; x++) {
            g2.drawLine(x * cellSize, 0, x * cellSize, H * cellSize);
        }
        for (int y = 0; y <= H; y++) {
            g2.drawLine(0, y * cellSize, W * cellSize, y * cellSize);
        }

        // Chemin trouvé
        if (path != null && path.size() >= 2) {
            g2.setStroke(new BasicStroke(Math.max(2f, cellSize / 6f),
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(Theme.PATH);
            for (int i = 1; i < path.size(); i++) {
                Vec2 a = path.get(i - 1);
                Vec2 b = path.get(i);
                g2.drawLine(px(a.x()), py(a.y()), px(b.x()), py(b.y()));
            }
            int dot = Math.max(4, cellSize / 4);
            for (Vec2 v : path) {
                g2.fillOval(px(v.x()) - dot / 2, py(v.y()) - dot / 2, dot, dot);
            }
        }

        g2.dispose();
    }

    private int px(double x) { return (int) Math.round(x * cellSize); }
    private int py(double y) { return (int) Math.round(y * cellSize); }

    /** Index linéarisés des cases du chemin final — utilisés pour exclure du rouge. */
    private Set<Long> pathCellKeys(int height) {
        Set<Long> s = new HashSet<>();
        if (path == null) return s;
        for (Vec2 v : path) {
            int cx = (int) Math.floor(v.x());
            int cy = (int) Math.floor(v.y());
            s.add(((long) cx) * height + cy);
        }
        return s;
    }

    private int[] cellAt(MouseEvent e) {
        int x = e.getX() / cellSize;
        int y = e.getY() / cellSize;
        return world.inBounds(x, y) ? new int[]{x, y} : null;
    }

    private void apply(int[] cell, boolean leftButton) {
        if (cell == null) return;
        int cx = cell[0], cy = cell[1];
        switch (mode) {
            case START -> { if (leftButton) world.setStart(cx, cy); }
            case GOAL  -> { if (leftButton) world.setGoal(cx, cy); }
            case OBSTACLE -> world.setObstacle(cx, cy, leftButton);
            case ERASE -> {
                if (world.cellAt(cx, cy) == CellType.OBSTACLE) world.setObstacle(cx, cy, false);
            }
        }
        if (statusListener != null) statusListener.accept("Case (" + cx + "," + cy + ")");
        repaint();
    }

    private final class MouseHandler extends MouseInputAdapter {
        @Override public void mousePressed(MouseEvent e) {
            apply(cellAt(e), e.getButton() == MouseEvent.BUTTON1);
        }
        @Override public void mouseDragged(MouseEvent e) {
            boolean left = (e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0;
            apply(cellAt(e), left);
        }
        @Override public void mouseMoved(MouseEvent e) {
            int[] c = cellAt(e);
            if (c != null && statusListener != null) {
                statusListener.accept("Case (" + c[0] + "," + c[1] + ")");
            }
        }
    }
}
