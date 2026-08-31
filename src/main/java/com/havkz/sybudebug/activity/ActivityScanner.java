package com.havkz.sybudebug.activity;

import com.havkz.sybudebug.activity.ActivityPoint.ActivityType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ActivityScanner {
    private static final int MARKER_CELL_SIZE = 4;
    private static final int HOLE_DEPTH = 5;
    private static final int HOLE_SEARCH_DEPTH = 12;
    private static final byte AIR = 0, SOLID = 1, OBSIDIAN = 2, NETHERRACK = 3, MAGMA = 4, LAVA = 5, CRYING_OBSIDIAN = 6;

    private ActivityScanner() {}

    public static Snapshot snapshot(WorldChunk chunk, int bottomY, int height) {
        byte[] blocks = new byte[16 * height * 16];
        ChunkSection[] sections = chunk.getSectionArray();
        for (int y = bottomY; y < bottomY + height; y++) {
            ChunkSection section = sections[chunk.getSectionIndex(y)];
            if (section == null || section.isEmpty()) continue;
            for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++)
                blocks[index(x, y - bottomY, z)] = classify(section.getBlockState(x, y & 15, z));
        }
        return new Snapshot(chunk.getPos(), bottomY, height, blocks);
    }

    public static ChunkActivityData scan(Snapshot snapshot, int resolution, boolean holes, boolean obsidian) {
        int[] ground = groundSurface(snapshot);
        int gridSize = 16 / resolution + 1;
        int[] surfaceY = new int[gridSize * gridSize];
        for (int gz = 0; gz < gridSize; gz++) for (int gx = 0; gx < gridSize; gx++)
            surfaceY[gz * gridSize + gx] = ground[Math.min(15, gz * resolution) * 16 + Math.min(15, gx * resolution)];
        smoothSurface(surfaceY, gridSize, 5);
        if (!holes && !obsidian) return new ChunkActivityData(snapshot.pos(), resolution, surfaceY, List.of());

        Map<ActivityType, Set<Long>> markerCells = new EnumMap<>(ActivityType.class);
        for (ActivityType type : ActivityType.values()) markerCells.put(type, new HashSet<>());
        List<ActivityPoint> points = new ArrayList<>();
        int startX = snapshot.pos().getStartX(), startZ = snapshot.pos().getStartZ();
        if (holes) for (int z = 1; z < 15; z++) for (int x = 1; x < 15; x++) {
            int surface = ground[z * 16 + x];
            for (int y = surface; y >= Math.max(snapshot.bottomY(), surface - HOLE_SEARCH_DEPTH); y--) {
                if (isHoleTop(snapshot, x, y, z)) {
                    add(points, markerCells, new BlockPos(startX + x, y, startZ + z), ActivityType.HOLE);
                    break;
                }
            }
        }
        if (obsidian) for (int y = snapshot.bottomY(); y < snapshot.topY(); y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            if (snapshot.get(x, y, z) == OBSIDIAN && !isRuinedPortalObsidian(snapshot, x, y, z))
                add(points, markerCells, new BlockPos(startX + x, y, startZ + z), ActivityType.OBSIDIAN);
        }
        return new ChunkActivityData(snapshot.pos(), resolution, surfaceY, points);
    }

    private static int[] groundSurface(Snapshot snapshot) {
        int[] result = new int[256];
        for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            int found = snapshot.bottomY();
            for (int y = snapshot.topY() - 1; y >= snapshot.bottomY(); y--) {
                if (!solid(snapshot.get(x, y, z))) continue;
                int support = 0;
                for (int d = 0; d < 8; d++) if (solid(snapshot.get(x, y - d, z))) support++;
                if (support >= 6) { found = y + 1; break; }
            }
            result[z * 16 + x] = found;
        }
        return result;
    }

    public static void smoothSurface(int[] heights, int size, int maxStep) {
        int[] source = heights.clone(), window = new int[9];
        for (int z = 0; z < size; z++) for (int x = 0; x < size; x++) {
            int count = 0;
            for (int dz = -1; dz <= 1; dz++) for (int dx = -1; dx <= 1; dx++) {
                int nx = x + dx, nz = z + dz;
                if (nx >= 0 && nx < size && nz >= 0 && nz < size) window[count++] = source[nz * size + nx];
            }
            Arrays.sort(window, 0, count);
            heights[z * size + x] = window[count / 2];
        }
        for (int pass = 0; pass < 2; pass++) for (int z = 0; z < size; z++) for (int x = 0; x < size; x++) {
            int i = z * size + x;
            if (x > 0) heights[i] = clamp(heights[i], heights[i - 1] - maxStep, heights[i - 1] + maxStep);
            if (z > 0) heights[i] = clamp(heights[i], heights[i - size] - maxStep, heights[i - size] + maxStep);
        }
    }

    private static boolean isHoleTop(Snapshot s, int x, int y, int z) {
        if (isHoleSection(s, x, y + 1, z)) return false;
        for (int depth = 0; depth < HOLE_DEPTH; depth++) if (!isHoleSection(s, x, y - depth, z)) return false;
        return true;
    }

    private static boolean isHoleSection(Snapshot s, int x, int y, int z) {
        return s.get(x, y, z) == AIR && solid(s.get(x - 1, y, z)) && solid(s.get(x + 1, y, z))
            && solid(s.get(x, y, z - 1)) && solid(s.get(x, y, z + 1));
    }

    private static boolean isRuinedPortalObsidian(Snapshot s, int ox, int oy, int oz) {
        int netherrack = 0, magma = 0, lava = 0, crying = 0;
        for (int dx = -5; dx <= 5; dx++) for (int dy = -5; dy <= 5; dy++) for (int dz = -5; dz <= 5; dz++) {
            byte block = s.get(ox + dx, oy + dy, oz + dz);
            if (block == CRYING_OBSIDIAN) crying++; else if (block == MAGMA) magma++; else if (block == LAVA) lava++; else if (block == NETHERRACK) netherrack++;
        }
        return ruinedPortalEvidence(netherrack, magma, lava, crying);
    }

    private static void add(List<ActivityPoint> points, Map<ActivityType, Set<Long>> cells, BlockPos pos, ActivityType type) {
        long cell = BlockPos.asLong(Math.floorDiv(pos.getX(), MARKER_CELL_SIZE), Math.floorDiv(pos.getY(), MARKER_CELL_SIZE), Math.floorDiv(pos.getZ(), MARKER_CELL_SIZE));
        if (cells.get(type).add(cell)) points.add(new ActivityPoint(pos, type));
    }

    private static byte classify(BlockState state) {
        if (state.isAir()) return AIR;
        if (state.isOf(Blocks.OBSIDIAN)) return OBSIDIAN;
        if (state.isOf(Blocks.CRYING_OBSIDIAN)) return CRYING_OBSIDIAN;
        if (state.isOf(Blocks.NETHERRACK)) return NETHERRACK;
        if (state.isOf(Blocks.MAGMA_BLOCK)) return MAGMA;
        if (state.isOf(Blocks.LAVA)) return LAVA;
        return SOLID;
    }

    private static boolean solid(byte block) { return block != AIR && block != LAVA; }
    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private static int index(int x, int y, int z) { return (y * 16 + z) * 16 + x; }

    public static boolean ruinedPortalEvidence(int netherrack, int magma, int lava, int cryingObsidian) {
        return cryingObsidian > 0 || netherrack >= 4 && (magma > 0 || lava > 0);
    }

    public record Snapshot(ChunkPos pos, int bottomY, int height, byte[] blocks) {
        public int topY() { return bottomY + height; }
        private byte get(int x, int y, int z) {
            if (x < 0 || x >= 16 || z < 0 || z >= 16 || y < bottomY || y >= topY()) return SOLID;
            return blocks[index(x, y - bottomY, z)];
        }
    }
}
