package com.havkz.sybudebug.activity;

import com.havkz.sybudebug.activity.ActivityPoint.ActivityType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ActivityScanner {
    private static final int MARKER_CELL_SIZE = 4;

    private final ClientWorld world;
    private final Context context;

    public ActivityScanner(ClientWorld world) {
        this.world = world;
        this.context = new Context(world);
    }

    public ChunkActivityData scan(WorldChunk chunk, int resolution, boolean holes, boolean obsidian) {
        ChunkPos chunkPos = chunk.getPos();
        int gridSize = 16 / resolution + 1;
        int[] surfaceY = new int[gridSize * gridSize];
        Heightmap surface = chunk.getHeightmap(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES);

        for (int gz = 0; gz < gridSize; gz++) {
            for (int gx = 0; gx < gridSize; gx++) {
                int localX = Math.min(15, gx * resolution);
                int localZ = Math.min(15, gz * resolution);
                surfaceY[gz * gridSize + gx] = surface.get(localX, localZ);
            }
        }

        if (!holes && !obsidian) return new ChunkActivityData(chunkPos, resolution, surfaceY, List.of());

        Map<ActivityType, Set<Long>> markerCells = new EnumMap<>(ActivityType.class);
        for (ActivityType type : ActivityType.values()) markerCells.put(type, new HashSet<>());
        List<ActivityPoint> points = new ArrayList<>();
        List<BlockPos> obsidianBlocks = obsidian ? new ArrayList<>() : List.of();
        BlockPos.Mutable pos = new BlockPos.Mutable();
        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();
        int bottomY = world.getBottomY();

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int topY = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE).get(localX, localZ);
                for (int y = bottomY; y < topY; y++) {
                    pos.set(startX + localX, y, startZ + localZ);
                    BlockState state = context.get(pos.getX(), y, pos.getZ());
                    if (obsidian && state.isOf(Blocks.OBSIDIAN)) obsidianBlocks.add(pos.toImmutable());
                    if (!isPassable(state, pos)) continue;

                    if (holes && isHoleTop(pos)) add(points, markerCells, pos, ActivityType.HOLE);
                }
            }
        }

        for (BlockPos obsidianPos : obsidianBlocks) {
            if (!isRuinedPortalObsidian(obsidianPos)) add(points, markerCells, obsidianPos, ActivityType.OBSIDIAN);
        }

        return new ChunkActivityData(chunkPos, resolution, surfaceY, points);
    }

    private void add(List<ActivityPoint> points, Map<ActivityType, Set<Long>> cells, BlockPos pos, ActivityType type) {
        long cell = BlockPos.asLong(Math.floorDiv(pos.getX(), MARKER_CELL_SIZE), Math.floorDiv(pos.getY(), MARKER_CELL_SIZE), Math.floorDiv(pos.getZ(), MARKER_CELL_SIZE));
        if (cells.get(type).add(cell)) points.add(new ActivityPoint(pos.toImmutable(), type));
    }

    private boolean isHoleTop(BlockPos pos) {
        if (isHoleSection(pos.up())) return false;
        for (int depth = 0; depth < 3; depth++) if (!isHoleSection(pos.down(depth))) return false;
        return true;
    }

    private boolean isHoleSection(BlockPos pos) {
        if (!isPassable(pos)) return false;
        return isSolid(pos.north()) && isSolid(pos.south()) && isSolid(pos.west()) && isSolid(pos.east());
    }

    private boolean isPassable(BlockPos pos) {
        return isPassable(context.get(pos.getX(), pos.getY(), pos.getZ()), pos);
    }

    private boolean isPassable(BlockState state, BlockPos pos) {
        return state.isAir() || (state.getFluidState().isEmpty() && state.getCollisionShape(world, pos).isEmpty());
    }

    private boolean isSolid(BlockPos pos) {
        BlockState state = context.get(pos.getX(), pos.getY(), pos.getZ());
        return !state.isAir() && state.getFluidState().isEmpty() && !state.getCollisionShape(world, pos).isEmpty();
    }

    private boolean isRuinedPortalObsidian(BlockPos origin) {
        int netherrack = 0;
        int magma = 0;
        int lava = 0;
        int crying = 0;
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int dx = -5; dx <= 5; dx++) {
            for (int dy = -5; dy <= 5; dy++) {
                for (int dz = -5; dz <= 5; dz++) {
                    pos.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    BlockState state = context.get(pos.getX(), pos.getY(), pos.getZ());
                    if (state.isOf(Blocks.CRYING_OBSIDIAN)) crying++;
                    else if (state.isOf(Blocks.MAGMA_BLOCK)) magma++;
                    else if (state.isOf(Blocks.LAVA)) lava++;
                    else if (state.isOf(Blocks.NETHERRACK)) netherrack++;
                }
            }
        }
        return ruinedPortalEvidence(netherrack, magma, lava, crying);
    }

    public static boolean ruinedPortalEvidence(int netherrack, int magma, int lava, int cryingObsidian) {
        return cryingObsidian > 0 || netherrack >= 4 && (magma > 0 || lava > 0);
    }

    private static final class Context {
        private final ClientWorld world;
        private Chunk lastChunk;

        private Context(ClientWorld world) { this.world = world; }

        private BlockState get(int x, int y, int z) {
            if (world.isOutOfHeightLimit(y)) return Blocks.VOID_AIR.getDefaultState();
            int chunkX = x >> 4;
            int chunkZ = z >> 4;
            Chunk chunk = lastChunk;
            if (chunk == null || chunk.getPos().x != chunkX || chunk.getPos().z != chunkZ) {
                chunk = world.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
                if (chunk == null) return Blocks.BEDROCK.getDefaultState();
                lastChunk = chunk;
            }
            ChunkSection section = chunk.getSectionArray()[chunk.getSectionIndex(y)];
            return section == null ? Blocks.VOID_AIR.getDefaultState() : section.getBlockState(x & 15, y & 15, z & 15);
        }
    }
}
