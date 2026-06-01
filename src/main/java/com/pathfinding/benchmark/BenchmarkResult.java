package com.pathfinding.benchmark;

import com.pathfinding.api.PathResult;

import java.util.List;

/**
 * Agrégat d'une série de runs d'un même algorithme : moyenne / min / max /
 * écart-type du temps, plus le résultat brut du dernier run (chemin, longueur,
 * compteurs).
 */
public record BenchmarkResult(
        String algorithmName,
        int runs,
        boolean success,
        double avgMillis,
        double minMillis,
        double maxMillis,
        double stdDevMillis,
        int nodesExpanded,
        int nodesGenerated,
        int maxOpenSize,
        double pathLength,
        double totalTurnDegrees,
        int turnCount,
        int pathSteps,
        String failureReason,
        PathResult lastResult) {

    public static BenchmarkResult of(String name, List<PathResult> results) {
        int n = results.size();
        if (n == 0) {
            return new BenchmarkResult(name, 0, false, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    "aucun run", null);
        }
        PathResult last = results.get(n - 1);
        double sum = 0, sumSq = 0, min = Double.POSITIVE_INFINITY, max = 0;
        for (PathResult r : results) {
            double ms = r.elapsedMillis();
            sum += ms;
            sumSq += ms * ms;
            min = Math.min(min, ms);
            max = Math.max(max, ms);
        }
        double avg = sum / n;
        double var = Math.max(0, sumSq / n - avg * avg);
        return new BenchmarkResult(
                name,
                n,
                last.success(),
                avg,
                min,
                max,
                Math.sqrt(var),
                last.nodesExpanded(),
                last.nodesGenerated(),
                last.maxOpenSize(),
                last.pathLength(),
                last.totalTurnDegrees(),
                last.turnCount(),
                last.path().size(),
                last.failureReason(),
                last);
    }
}
