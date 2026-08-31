package com.havkz.sybudebug.activity;

import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.util.math.ChunkPos;

import java.util.List;

public final class ChunkActivityData {
    private final ChunkPos chunkPos;
    private final int resolution;
    private final int gridSize;
    private final int[] surfaceY;
    private final double[] nearestDistanceSquared;
    private final Color[] colors;
    private final List<ActivityPoint> activityPoints;
    private boolean lowActivityCandidate;
    private double candidateDistance;

    public ChunkActivityData(ChunkPos chunkPos, int resolution, int[] surfaceY, List<ActivityPoint> activityPoints) {
        this.chunkPos = chunkPos;
        this.resolution = resolution;
        this.gridSize = 16 / resolution + 1;
        this.surfaceY = surfaceY;
        this.activityPoints = List.copyOf(activityPoints);
        this.nearestDistanceSquared = new double[surfaceY.length];
        this.colors = new Color[surfaceY.length];
    }

    public ChunkPos chunkPos() { return chunkPos; }
    public long key() { return chunkPos.toLong(); }
    public int resolution() { return resolution; }
    public int gridSize() { return gridSize; }
    public int surfaceY(int index) { return surfaceY[index]; }
    public double nearestDistanceSquared(int index) { return nearestDistanceSquared[index]; }
    public void nearestDistanceSquared(int index, double value) { nearestDistanceSquared[index] = value; }
    public Color color(int index) { return colors[index]; }
    public void color(int index, Color value) { colors[index] = value; }
    public List<ActivityPoint> activityPoints() { return activityPoints; }
    public boolean lowActivityCandidate() { return lowActivityCandidate; }
    public void lowActivityCandidate(boolean value) { lowActivityCandidate = value; }
    public double candidateDistance() { return candidateDistance; }
    public void candidateDistance(double value) { candidateDistance = value; }
    public int index(int gridX, int gridZ) { return gridZ * gridSize + gridX; }
}
