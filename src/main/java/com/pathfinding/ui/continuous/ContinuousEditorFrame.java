package com.pathfinding.ui.continuous;

import com.pathfinding.api.Obstacle;
import com.pathfinding.api.PathRequest;
import com.pathfinding.api.PathResult;
import com.pathfinding.api.Pathfinder;
import com.pathfinding.api.Vec2;
import com.pathfinding.benchmark.Benchmark;
import com.pathfinding.benchmark.BenchmarkResult;
import com.pathfinding.ui.MainMenu;
import com.pathfinding.ui.shared.AlgorithmRegistry;
import com.pathfinding.ui.shared.ResultsCard;
import com.pathfinding.ui.shared.SidebarSection;
import com.pathfinding.ui.theme.Card;
import com.pathfinding.ui.theme.PillButton;
import com.pathfinding.ui.theme.Screen;
import com.pathfinding.ui.theme.SegmentedControl;
import com.pathfinding.ui.theme.Theme;
import com.pathfinding.world.ContinuousWorld;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Map;

/**
 * Fenêtre du mode « carte continue » : on dessine des obstacles polygonaux et
 * on lance les planificateurs qui raisonnent en coordonnées réelles.
 *
 * <p>Le menu d'algorithmes provient de {@link AlgorithmRegistry#forContinuous()},
 * qui ne retient que les algorithmes sachant traiter une carte continue
 * (RRT*, Theta*-RRT*). On peut donc passer directement le
 * {@link ContinuousWorld} à chaque recherche, sans conversion.</p>
 */
public final class ContinuousEditorFrame extends JFrame {

    private ContinuousWorld world;
    private final ContinuousCanvas canvas;
    private final ResultsCard results = new ResultsCard();
    private final JLabel statusLabel = new JLabel(" ");

    private final JComboBox<AlgorithmRegistry.Entry> algoCombo;
    private final JSpinner runsSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 200, 1));

    public ContinuousEditorFrame() {
        super("Pathfinding — Mode carte continue");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        world = new ContinuousWorld(60, 35);
        world.setStart(new Vec2(4, 17));
        world.setGoal(new Vec2(55, 17));
        seedDemoObstacles(world);

        canvas = new ContinuousCanvas(world);
        canvas.setStatusListener(s -> statusLabel.setText("  " + s));

        algoCombo = new JComboBox<>(AlgorithmRegistry.forContinuous().toArray(new AlgorithmRegistry.Entry[0]));
        algoCombo.setRenderer(new AlgorithmCellRenderer());

        getContentPane().setBackground(Theme.BG_WINDOW);
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(
                Theme.PADDING_MD, Theme.PADDING_MD, Theme.PADDING_MD, Theme.PADDING_MD));
        setLayout(new BorderLayout(Theme.PADDING_MD, Theme.PADDING_MD));

        add(buildTopBar(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildSidebar(), BorderLayout.EAST);

        setSize(Screen.editorSize());
        setMinimumSize(Screen.editorMin());
        setLocationRelativeTo(null);
    }

    /** Quelques obstacles non rectangulaires pour illustrer le rendu polygonal au démarrage. */
    private static void seedDemoObstacles(ContinuousWorld w) {
        w.addObstacle(new Obstacle(List.of(            // triangle
                new Vec2(15, 8), new Vec2(22, 14), new Vec2(15, 20))));
        w.addObstacle(new Obstacle(List.of(            // pentagone irrégulier
                new Vec2(28, 5), new Vec2(34, 7), new Vec2(36, 13),
                new Vec2(30, 16), new Vec2(26, 11))));
        w.addObstacle(new Obstacle(List.of(            // forme en L (concave)
                new Vec2(40, 18), new Vec2(48, 18), new Vec2(48, 22),
                new Vec2(44, 22), new Vec2(44, 30), new Vec2(40, 30))));
        w.addObstacle(new Obstacle(List.of(            // losange
                new Vec2(20, 24), new Vec2(26, 26), new Vec2(24, 32), new Vec2(18, 30))));
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);

        PillButton back = new PillButton("← Retour", PillButton.Style.GHOST);
        back.addActionListener(e -> { dispose(); MainMenu.open(); });
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        left.add(back);
        bar.add(left, BorderLayout.WEST);

        JLabel title = new JLabel("Mode carte continue");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_PRIMARY);
        title.setHorizontalAlignment(JLabel.CENTER);
        bar.add(title, BorderLayout.CENTER);

        statusLabel.setFont(Theme.FONT_CAPTION);
        statusLabel.setForeground(Theme.TEXT_SECONDARY);
        bar.add(statusLabel, BorderLayout.EAST);

        return bar;
    }

    private Card buildCenter() {
        Card card = new Card(new BorderLayout());
        JScrollPane scroll = new JScrollPane(canvas,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.CANVAS_BG);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildSidebar() {
        // BorderLayout : contrôles scrollables au centre, résultats fixés en bas.
        JPanel column = new JPanel(new BorderLayout(0, Theme.PADDING_MD));
        column.setOpaque(false);
        column.setPreferredSize(new Dimension(Screen.sidebarWidth(), 100));

        Card controls = new Card();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));

        SegmentedControl modeControl = new SegmentedControl(
                List.of("Départ", "Arrivée", "Polygone", "Effacer"), 2);
        modeControl.onChange(i -> canvas.setMode(switch (i) {
            case 0 -> ContinuousCanvas.EditMode.START;
            case 1 -> ContinuousCanvas.EditMode.GOAL;
            case 2 -> ContinuousCanvas.EditMode.POLYGON;
            default -> ContinuousCanvas.EditMode.ERASE;
        }));

        PillButton commit = new PillButton("Valider polygone en cours", PillButton.Style.SECONDARY);
        commit.addActionListener(e -> {
            if (!canvas.commitCurrentPolygon()) {
                statusLabel.setText("  Polygone trop petit (3 sommets minimum).");
            }
        });
        PillButton cancel = new PillButton("Annuler polygone", PillButton.Style.GHOST);
        cancel.addActionListener(e -> canvas.cancelCurrentPolygon());

        controls.add(new SidebarSection("Édition")
                .row(modeControl)
                .gap(4)
                .paragraph("Clic = nouveau sommet. Double-clic ou clic droit pour valider le polygone.")
                .row(commit)
                .row(cancel));

        JSlider zoomSlider = new JSlider(6, 36, (int) canvas.scale());
        zoomSlider.addChangeListener(e -> canvas.setScale(zoomSlider.getValue()));

        JCheckBox showExploredBox = new JCheckBox("Afficher les cases parcourues");
        showExploredBox.setToolTipText(
                "Nœuds de l'arbre exploré. Le trajet final reste en noir.");
        showExploredBox.addActionListener(e -> canvas.setShowExplored(showExploredBox.isSelected()));

        controls.add(new SidebarSection("Affichage")
                .row(new JLabel("Zoom"))
                .row(zoomSlider)
                .row(showExploredBox));

        PillButton clearObs = new PillButton("Effacer obstacles", PillButton.Style.SECONDARY);
        clearObs.addActionListener(e -> { world.clearObstacles(); canvas.clearPath(); canvas.repaint(); });
        PillButton clearAll = new PillButton("Tout effacer", PillButton.Style.SECONDARY);
        clearAll.addActionListener(e -> { world.clearAll(); canvas.clearPath(); canvas.repaint(); });
        controls.add(new SidebarSection("Nettoyage").row(clearObs).row(clearAll));

        controls.add(new SidebarSection("Algorithme").row(algoCombo));

        PillButton runBtn = new PillButton("Lancer", PillButton.Style.PRIMARY);
        runBtn.addActionListener(e -> runAlgorithm());

        JPanel benchRow = new JPanel(new BorderLayout(8, 0));
        benchRow.setOpaque(false);
        benchRow.add(new JLabel("Runs"), BorderLayout.WEST);
        benchRow.add(runsSpinner, BorderLayout.CENTER);

        PillButton benchBtn = new PillButton("Benchmark tous", PillButton.Style.SECONDARY);
        benchBtn.addActionListener(e -> runBenchmark());

        controls.add(new SidebarSection("Exécution")
                .row(runBtn)
                .gap(4)
                .row(benchRow)
                .row(benchBtn));

        controls.add(Box.createVerticalGlue());

        JScrollPane controlScroll = new JScrollPane(controls,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        controlScroll.setBorder(BorderFactory.createEmptyBorder());
        controlScroll.setOpaque(false);
        controlScroll.getViewport().setOpaque(false);
        controlScroll.getVerticalScrollBar().setUnitIncrement(16);

        column.add(controlScroll, BorderLayout.CENTER);
        column.add(results, BorderLayout.SOUTH);
        return column;
    }

    private void runAlgorithm() {
        if (world.start() == null || world.goal() == null) {
            JOptionPane.showMessageDialog(this, "Place un départ et une arrivée.",
                    "Manque", JOptionPane.WARNING_MESSAGE);
            return;
        }
        AlgorithmRegistry.Entry entry = (AlgorithmRegistry.Entry) algoCombo.getSelectedItem();
        if (entry == null) return;
        Pathfinder pf = entry.build();
        PathRequest req = new PathRequest(world, System.nanoTime());
        statusLabel.setText("  Exécution " + pf.name() + "…");
        new SwingWorker<PathResult, Void>() {
            @Override protected PathResult doInBackground() { return pf.find(req); }
            @Override protected void done() {
                try {
                    PathResult r = get();
                    canvas.setPath(r.success() ? r.path() : null);
                    canvas.setExplored(r.explored());
                    results.showRun(r);
                    statusLabel.setText("  " + (r.success()
                            ? String.format("OK · %.2f ms · long. %.2f", r.elapsedMillis(), r.pathLength())
                            : "Échec: " + r.failureReason()));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ContinuousEditorFrame.this, ex.toString(),
                            "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void runBenchmark() {
        if (world.start() == null || world.goal() == null) {
            JOptionPane.showMessageDialog(this, "Place un départ et une arrivée.",
                    "Manque", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int runs = (int) runsSpinner.getValue();
        PathRequest req = new PathRequest(world, System.nanoTime());
        statusLabel.setText("  Benchmark · " + runs + " runs/algo…");
        new SwingWorker<Map<String, BenchmarkResult>, Void>() {
            @Override
            protected Map<String, BenchmarkResult> doInBackground() {
                Benchmark b = new Benchmark().runs(runs);
                for (AlgorithmRegistry.Entry e : AlgorithmRegistry.forContinuous()) b.register(e.build());
                return b.run(req);
            }
            @Override protected void done() {
                try {
                    Map<String, BenchmarkResult> res = get();
                    results.showBenchmark(res);
                    AlgorithmRegistry.Entry sel = (AlgorithmRegistry.Entry) algoCombo.getSelectedItem();
                    if (sel != null) {
                        BenchmarkResult r = res.get(sel.build().name());
                        if (r != null && r.lastResult() != null) {
                            canvas.setPath(r.lastResult().path());
                            canvas.setExplored(r.lastResult().explored());
                        }
                    }
                    statusLabel.setText("  Benchmark terminé (" + res.size() + " algos).");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ContinuousEditorFrame.this, ex.toString(),
                            "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    /** Affiche le libellé lisible de l'algorithme dans la liste déroulante. */
    private static final class AlgorithmCellRenderer extends javax.swing.DefaultListCellRenderer {
        @Override
        public java.awt.Component getListCellRendererComponent(
                javax.swing.JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof AlgorithmRegistry.Entry e) setText(e.name());
            return this;
        }
    }
}
