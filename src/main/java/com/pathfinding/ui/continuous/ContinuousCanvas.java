package com.pathfinding.ui.continuous;

import com.pathfinding.api.Obstacle;
import com.pathfinding.api.Vec2;
import com.pathfinding.ui.theme.Theme;
import com.pathfinding.world.ContinuousWorld;

import javax.swing.JPanel;
import javax.swing.event.MouseInputAdapter;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Canevas du mode "carte continue". L'utilisateur place des sommets de
 * polygone par clics successifs (le polygone en cours est affiché en
 * pointillés). Double-clic ou bouton "Valider" pour finaliser. En mode
 * "Erase" un clic sur un obstacle le supprime.
 */
public final class ContinuousCanvas extends JPanel {

    public enum EditMode { START, GOAL, POLYGON, ERASE }

    private ContinuousWorld world;
    private double scale = 16.0;       // pixels par unité monde
    private EditMode mode = EditMode.POLYGON;
    private final List<Vec2> currentPolygon = new ArrayList<>();
    private List<Vec2> path;
    private List<Vec2> explored;
    private boolean showExplored = false;
    private Consumer<String> statusListener;

    public ContinuousCanvas(ContinuousWorld world) {
        this.world = world;
        setBackground(Theme.CANVAS_BG);
        recomputeSize();
        MouseHandler h = new MouseHandler();
        addMouseListener(h);
        addMouseMotionListener(h);
    }

    public void setWorld(ContinuousWorld world) {
        this.world = world;
        this.currentPolygon.clear();
        this.path = null;
        recomputeSize();
        revalidate();
        repaint();
    }

    public ContinuousWorld getWorld() { return world; }

    public void setMode(EditMode m) {
        this.mode = m;
        if (m != EditMode.POLYGON) currentPolygon.clear();
        repaint();
    }

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

    public double scale() { return scale; }

    public void setScale(double s) {
        this.scale = Math.max(4, Math.min(60, s));
        recomputeSize();
        revalidate();
        repaint();
    }

    /** Valide le polygone en cours s'il a au moins 3 sommets. */
    public boolean commitCurrentPolygon() {
        if (currentPolygon.size() < 3) return false;
        world.addObstacle(new Obstacle(new ArrayList<>(currentPolygon)));
        currentPolygon.clear();
        repaint();
        return true;
    }

    public void cancelCurrentPolygon() {
        currentPolygon.clear();
        repaint();
    }

    private void recomputeSize() {
        setPreferredSize(new Dimension(
                (int) (world.bounds().width() * scale) + 1,
                (int) (world.bounds().height() * scale) + 1));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Bordure subtile du canevas
        g2.setColor(Theme.BORDER);
        g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);

        // Quadrillage discret pour aider à se repérer
        g2.setColor(Theme.GRID_LINE);
        int W = (int) world.bounds().width();
        int H = (int) world.bounds().height();
        for (int x = 0; x <= W; x++) g2.drawLine(px(x), 0, px(x), py(H));
        for (int y = 0; y <= H; y++) g2.drawLine(0, py(y), px(W), py(y));

        // Couche "exploré" : petits points rouges aux positions visitées par
        // la recherche. Pour les algos sampling-based (RRT*/Theta*-RRT*),
        // ce sont les nœuds de l'arbre ; pour les algos de grille (via
        // rasterisation), ce sont les centres des cases expansées.
        if (showExplored && explored != null && !explored.isEmpty()) {
            int dotR = (int) Math.max(2, scale / 5);
            g2.setColor(Theme.EXPLORED_DOT);
            for (Vec2 v : explored) {
                g2.fillOval(px(v.x()) - dotR / 2, py(v.y()) - dotR / 2, dotR, dotR);
            }
        }

        // Obstacles
        g2.setColor(Theme.OBSTACLE);
        for (Obstacle o : world.obstacles()) {
            Polygon poly = toAwtPolygon(o.vertices());
            g2.fillPolygon(poly);
        }
        g2.setColor(new Color(0, 0, 0, 60));
        g2.setStroke(new BasicStroke(1.2f));
        for (Obstacle o : world.obstacles()) {
            Polygon poly = toAwtPolygon(o.vertices());
            g2.drawPolygon(poly);
        }

        // Départ / arrivée
        if (world.start() != null) drawDot(g2, world.start(), Theme.START);
        if (world.goal()  != null) drawDot(g2, world.goal(),  Theme.GOAL);

        // Polygone en cours de dessin (pointillés)
        if (!currentPolygon.isEmpty()) {
            g2.setColor(Theme.ACCENT);
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    1.0f, new float[]{6f, 4f}, 0));
            for (int i = 1; i < currentPolygon.size(); i++) {
                Vec2 a = currentPolygon.get(i - 1);
                Vec2 b = currentPolygon.get(i);
                g2.drawLine(px(a.x()), py(a.y()), px(b.x()), py(b.y()));
            }
            // sommets en cours
            g2.setStroke(new BasicStroke(1f));
            for (Vec2 v : currentPolygon) {
                int x = px(v.x()), y = py(v.y());
                g2.setColor(Color.WHITE);
                g2.fillOval(x - 4, y - 4, 8, 8);
                g2.setColor(Theme.ACCENT);
                g2.drawOval(x - 4, y - 4, 8, 8);
            }
        }

        // Chemin trouvé
        if (path != null && path.size() >= 2) {
            g2.setStroke(new BasicStroke((float) Math.max(2, scale / 6),
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(Theme.PATH);
            for (int i = 1; i < path.size(); i++) {
                Vec2 a = path.get(i - 1);
                Vec2 b = path.get(i);
                g2.drawLine(px(a.x()), py(a.y()), px(b.x()), py(b.y()));
            }
            int dot = (int) Math.max(5, scale / 4);
            for (Vec2 v : path) {
                g2.fillOval(px(v.x()) - dot / 2, py(v.y()) - dot / 2, dot, dot);
            }
        }

        g2.dispose();
    }

    private void drawDot(Graphics2D g2, Vec2 p, Color c) {
        int d = (int) Math.max(10, scale / 2);
        int x = px(p.x()), y = py(p.y());
        g2.setColor(Color.WHITE);
        g2.fillOval(x - d / 2 - 1, y - d / 2 - 1, d + 2, d + 2);
        g2.setColor(c);
        g2.fillOval(x - d / 2, y - d / 2, d, d);
    }

    private Polygon toAwtPolygon(List<Vec2> verts) {
        Polygon p = new Polygon();
        for (Vec2 v : verts) p.addPoint(px(v.x()), py(v.y()));
        return p;
    }

    private int px(double x) { return (int) Math.round(x * scale); }
    private int py(double y) { return (int) Math.round(y * scale); }

    private Vec2 worldAt(MouseEvent e) {
        return new Vec2(e.getX() / scale, e.getY() / scale);
    }

    private final class MouseHandler extends MouseInputAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            Vec2 p = worldAt(e);
            if (!world.bounds().contains(p)) return;
            switch (mode) {
                case START -> {
                    if (e.getButton() == MouseEvent.BUTTON1) world.setStart(p);
                }
                case GOAL -> {
                    if (e.getButton() == MouseEvent.BUTTON1) world.setGoal(p);
                }
                case POLYGON -> {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        // Double-clic = on valide
                        if (e.getClickCount() >= 2 && currentPolygon.size() >= 3) {
                            commitCurrentPolygon();
                        } else {
                            currentPolygon.add(p);
                        }
                    } else if (e.getButton() == MouseEvent.BUTTON3) {
                        // Clic droit = annuler ou valider selon nombre de sommets
                        if (currentPolygon.size() >= 3) commitCurrentPolygon();
                        else cancelCurrentPolygon();
                    }
                }
                case ERASE -> {
                    if (e.getButton() == MouseEvent.BUTTON1) world.removeObstacleAt(p);
                }
            }
            if (statusListener != null) statusListener.accept("Point " + p);
            repaint();
        }
        @Override
        public void mouseMoved(MouseEvent e) {
            if (statusListener != null) statusListener.accept("Point " + worldAt(e));
        }
    }
}
