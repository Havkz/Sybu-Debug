package com.havkz.sybudebug.activity;

import com.havkz.sybudebug.activity.ActivityPoint.ActivityType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
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

    public static Snapshot snapshot(ClientWorld world, WorldChunk chunk, int bottomY, int height, boolean fullScan) {
        byte[] blocks = new byte[16 * height * 16];
        byte[] west = new byte[height * 16], east = new byte[height * 16], north = new byte[height * 16], south = new byte[height * 16];
        int[] ground = new int[256];
        ChunkSection[] sections = chunk.getSectionArray();
        int topY = bottomY + height;
        Heightmap surface = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE);
        for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            int found = bottomY;
            for (int y = Math.min(topY - 1, surface.get(x, z)); y >= bottomY; y--) {
                if (!solid(get(sections, chunk, bottomY, topY, x, y, z))) continue;
                int support = 0;
                for (int d = 0; d < 8; d++) if (solid(get(sections, chunk, bottomY, topY, x, y - d, z))) support++;
                if (support >= 6) { found = y + 1; break; }
            }
            ground[z * 16 + x] = found;
        }
        smoothSurface(ground, 16, 5);
        for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            int found = ground[z * 16 + x];
            for (int y = Math.max(bottomY, found - HOLE_SEARCH_DEPTH - HOLE_DEPTH); y <= Math.min(topY - 1, found + 2); y++)
                blocks[index(x, y - bottomY, z)] = get(sections, chunk, bottomY, topY, x, y, z);
        }
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int i = 0; i < 16; i++) {
            int westGround = ground[i * 16], eastGround = ground[i * 16 + 15];
            int northGround = ground[i], southGround = ground[15 * 16 + i];
            fillBorder(world, west, i, chunk.getPos().getStartX() - 1, chunk.getPos().getStartZ() + i, westGround, bottomY, topY, pos);
            fillBorder(world, east, i, chunk.getPos().getEndX() + 1, chunk.getPos().getStartZ() + i, eastGround, bottomY, topY, pos);
            fillBorder(world, north, i, chunk.getPos().getStartX() + i, chunk.getPos().getStartZ() - 1, northGround, bottomY, topY, pos);
            fillBorder(world, south, i, chunk.getPos().getStartX() + i, chunk.getPos().getEndZ() + 1, southGround, bottomY, topY, pos);
        }
        if (fullScan) for (int y = bottomY; y < topY; y++) {
            ChunkSection section = sections[chunk.getSectionIndex(y)];
            if (section == null || section.isEmpty()) continue;
            for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) blocks[index(x, y - bottomY, z)] = classify(section.getBlockState(x, y & 15, z));
        }
        return new Snapshot(chunk.getPos(), bottomY, height, blocks, ground, west, east, north, south);
    }

    public static ChunkActivityData scan(Snapshot snapshot, int resolution, boolean holes, boolean obsidian) {
        int[] ground = snapshot.ground().clone();
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
        if (holes) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
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

    public static boolean detectsHole(Snapshot snapshot, int x, int y, int z) { return isHoleTop(snapshot, x, y, z); }

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

    private static byte get(ChunkSection[] sections, WorldChunk chunk, int bottomY, int topY, int x, int y, int z) {
        if (y < bottomY || y >= topY) return SOLID;
        ChunkSection section = sections[chunk.getSectionIndex(y)];
        return section == null || section.isEmpty() ? AIR : classify(section.getBlockState(x, y & 15, z));
    }

    private static void fillBorder(ClientWorld world, byte[] border, int column, int worldX, int worldZ, int ground, int bottomY, int topY, BlockPos.Mutable pos) {
        for (int y = Math.max(bottomY, ground - HOLE_SEARCH_DEPTH - HOLE_DEPTH); y <= Math.min(topY - 1, ground + 2); y++) {
            pos.set(worldX, y, worldZ);
            border[(y - bottomY) * 16 + column] = classify(world.getBlockState(pos));
        }
    }

    private static boolean solid(byte block) { return block != AIR && block != LAVA; }
    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private static int index(int x, int y, int z) { return (y * 16 + z) * 16 + x; }

    public static boolean ruinedPortalEvidence(int netherrack, int magma, int lava, int cryingObsidian) {
        return cryingObsidian > 0 || netherrack >= 4 && (magma > 0 || lava > 0);
    }

    public record Snapshot(ChunkPos pos, int bottomY, int height, byte[] blocks, int[] ground, byte[] west, byte[] east, byte[] north, byte[] south) {
        public int topY() { return bottomY + height; }
        private byte get(int x, int y, int z) {
            if (y < bottomY || y >= topY()) return SOLID;
            int localY = y - bottomY;
            if (x == -1 && z >= 0 && z < 16) return west[localY * 16 + z];
            if (x == 16 && z >= 0 && z < 16) return east[localY * 16 + z];
            if (z == -1 && x >= 0 && x < 16) return north[localY * 16 + x];
            if (z == 16 && x >= 0 && x < 16) return south[localY * 16 + x];
            if (x < 0 || x >= 16 || z < 0 || z >= 16) return SOLID;
            return blocks[index(x, y - bottomY, z)];
        }
    }
}
