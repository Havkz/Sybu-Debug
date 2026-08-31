package com.havkz.sybudebug.modules;

import com.havkz.sybudebug.SybuDebugAddon;
import com.havkz.sybudebug.activity.ActivityHeatmap;
import com.havkz.sybudebug.activity.ActivityPoint;
import com.havkz.sybudebug.activity.ActivityScanner;
import com.havkz.sybudebug.activity.ChunkActivityData;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.BlockUpdateEvent;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class BaseActivityDetector extends Module {
    private static final int MAX_CACHED_CHUNKS = 2048;
    private static final int SNAPSHOTS_PER_TICK = 1;
    private static final int MAX_IN_FLIGHT = 2;
    private static final int HEATMAPS_UPDATED_PER_TICK = 2;
    private static final double LOW_SAMPLE_RATIO = 0.75;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgDetection = settings.createGroup("Detection");
    private final SettingGroup sgRendering = settings.createGroup("Rendering");

    private final Setting<Boolean> enableHoles = sgGeneral.add(new BoolSetting.Builder()
        .name("enable-holes")
        .description("Uses enclosed vertical holes as visible player-activity markers.")
        .defaultValue(true)
        .onChanged(value -> rescan())
        .build());

    private final Setting<Boolean> enableObsidian = sgGeneral.add(new BoolSetting.Builder()
        .name("enable-obsidian-activity")
        .description("Uses visible obsidian as player activity, excluding likely ruined portals.")
        .defaultValue(true)
        .onChanged(value -> rescan())
        .build());

    private final Setting<Integer> searchRadius = sgDetection.add(new IntSetting.Builder()
        .name("activity-search-radius")
        .description("Maximum horizontal distance used when searching for activity.")
        .defaultValue(256)
        .range(64, 512)
        .sliderRange(64, 512)
        .onChanged(value -> markAllDirty())
        .build());

    private final Setting<Integer> lowActivityThreshold = sgDetection.add(new IntSetting.Builder()
        .name("low-activity-threshold")
        .description("Minimum distance from known activity for a sample to count as low activity.")
        .defaultValue(128)
        .range(32, 256)
        .sliderRange(32, 256)
        .onChanged(value -> markAllDirty())
        .build());

    private final Setting<Integer> minimumRegionChunks = sgDetection.add(new IntSetting.Builder()
        .name("minimum-candidate-region-size")
        .description("Minimum number of connected low-activity chunks required for a notification.")
        .defaultValue(2)
        .range(1, 4)
        .sliderRange(1, 4)
        .onChanged(value -> markAllDirty())
        .build());

    private final Setting<Double> notificationCooldown = sgDetection.add(new DoubleSetting.Builder()
        .name("notification-cooldown")
        .description("Minimum seconds between low-activity notifications.")
        .defaultValue(60)
        .range(5, 300)
        .sliderRange(5, 300)
        .build());

    private final Setting<Boolean> renderOverlay = sgRendering.add(new BoolSetting.Builder()
        .name("render-overlay")
        .description("Renders the cached activity-density overlay.")
        .defaultValue(true)
        .build());

    private final Setting<Integer> overlayAlpha = sgRendering.add(new IntSetting.Builder()
        .name("overlay-alpha")
        .description("Transparency of the terrain overlay.")
        .defaultValue(70)
        .range(10, 200)
        .sliderRange(10, 200)
        .onChanged(value -> markAllDirty())
        .build());

    private final Setting<SampleResolution> sampleResolution = sgRendering.add(new EnumSetting.Builder<SampleResolution>()
        .name("sample-resolution")
        .description("Horizontal spacing between cached surface samples.")
        .defaultValue(SampleResolution.BLOCKS_4)
        .onChanged(value -> rescan())
        .build());

    private final Setting<Integer> nearDistance = sgRendering.add(new IntSetting.Builder()
        .name("near-distance")
        .description("Distance displayed as high activity (red).")
        .defaultValue(24)
        .range(0, 128)
        .sliderRange(0, 128)
        .onChanged(value -> markAllDirty())
        .build());

    private final Setting<Integer> farDistance = sgRendering.add(new IntSetting.Builder()
        .name("far-distance")
        .description("Distance displayed as low activity (green).")
        .defaultValue(160)
        .range(32, 384)
        .sliderRange(32, 384)
        .onChanged(value -> markAllDirty())
        .build());

    private final Setting<Integer> maxAnalysisDistance = sgRendering.add(new IntSetting.Builder()
        .name("max-analysis-distance")
        .description("Maximum horizontal distance for analysis, caching, and rendering.")
        .defaultValue(256)
        .range(64, 512)
        .sliderRange(64, 512)
        .onChanged(value -> rescan())
        .build());

    private final Map<Long, ChunkActivityData> chunks = new LinkedHashMap<>();
    private final PriorityQueue<QueuedChunk> scanQueue = new PriorityQueue<>((a, b) -> Double.compare(a.distanceSquared, b.distanceSquared));
    private final Set<Long> queuedChunks = new HashSet<>();
    private final ArrayDeque<Long> dirtyQueue = new ArrayDeque<>();
    private final Set<Long> dirtyChunks = new HashSet<>();
    private final Set<Long> notifiedChunks = new HashSet<>();
    private final List<ActivityPoint> nearbyPoints = new ArrayList<>();
    private final Set<Long> inFlight = new HashSet<>();
    private ExecutorService scanExecutor;
    private int generation;
    private int playerChunkX = Integer.MIN_VALUE;
    private int playerChunkZ = Integer.MIN_VALUE;
    private int ticks;
    private long lastNotification;

    public BaseActivityDetector() {
        super(SybuDebugAddon.CATEGORY, "LümmelFinder", "Visualizes low visible player-activity areas from holes and obsidian.");
    }

    @Override
    public void onActivate() {
        clear();
        seedLoadedChunks();
    }

    @Override
    public void onDeactivate() { clear(); }

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        clear();
        seedLoadedChunks();
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) { clear(); }

    @EventHandler
    private void onChunkData(ChunkDataEvent event) {
        ChunkPos pos = event.chunk().getPos();
        if (mc.isOnThread()) enqueue(pos.x, pos.z);
        else mc.execute(() -> enqueue(pos.x, pos.z));
    }

    @EventHandler
    private void onBlockUpdate(BlockUpdateEvent event) {
        int chunkX = event.pos.getX() >> 4;
        int chunkZ = event.pos.getZ() >> 4;
        enqueue(chunkX, chunkZ);
        int localX = event.pos.getX() & 15;
        int localZ = event.pos.getZ() & 15;
        if (localX == 0) enqueue(chunkX - 1, chunkZ);
        else if (localX == 15) enqueue(chunkX + 1, chunkZ);
        if (localZ == 0) enqueue(chunkX, chunkZ - 1);
        else if (localZ == 15) enqueue(chunkX, chunkZ + 1);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.world == null || mc.player == null) return;
        reprioritizeIfNeeded();
        for (int i = 0; i < SNAPSHOTS_PER_TICK && inFlight.size() < MAX_IN_FLIGHT && !scanQueue.isEmpty(); i++) analyze(scanQueue.remove());
        for (int i = 0; i < HEATMAPS_UPDATED_PER_TICK && !dirtyQueue.isEmpty(); i++) updateHeatmap(dirtyQueue.removeFirst());
        if (++ticks % 20 == 0) prune();
    }

    private void analyze(QueuedChunk queued) {
        long key = queued.pos.toLong();
        queuedChunks.remove(key);
        if (!withinRange(queued.pos) || !mc.world.getChunkManager().isChunkLoaded(queued.pos.x, queued.pos.z)) return;
        Chunk chunk = mc.world.getChunk(queued.pos.x, queued.pos.z, ChunkStatus.FULL, false);
        if (!(chunk instanceof WorldChunk worldChunk)) return;
        ActivityScanner.Snapshot snapshot = ActivityScanner.snapshot(worldChunk, mc.world.getBottomY(), mc.world.getHeight());
        int currentGeneration = generation;
        int resolution = sampleResolution.get().blocks;
        boolean holes = enableHoles.get(), obsidian = enableObsidian.get();
        inFlight.add(key);
        scanExecutor.submit(() -> {
            try {
                ChunkActivityData data = ActivityScanner.scan(snapshot, resolution, holes, obsidian);
                mc.execute(() -> installResult(key, currentGeneration, data));
            } catch (RuntimeException exception) {
                SybuDebugAddon.LOG.warn("LümmelFinder failed to scan chunk {}", queued.pos, exception);
                mc.execute(() -> { if (currentGeneration == generation) inFlight.remove(key); });
            }
        });
    }

    private void installResult(long key, int resultGeneration, ChunkActivityData data) {
        if (resultGeneration != generation) return;
        inFlight.remove(key);
        if (mc.world == null || !isActive() || !withinRange(data.chunkPos())) return;
        chunks.put(key, data);
        smoothChunkEdges(data);
        markNearbyDirty(data.chunkPos());
        trimCache();
    }

    private void updateHeatmap(long key) {
        dirtyChunks.remove(key);
        ChunkActivityData data = chunks.get(key);
        if (data == null) return;

        int radius = searchRadius.get();
        double radiusSquared = (double) radius * radius;
        double thresholdSquared = (double) lowActivityThreshold.get() * lowActivityThreshold.get();
        int chunkRadius = (radius + 15) / 16;
        int lowSamples = 0;
        double candidateDistanceSquared = radiusSquared;

        nearbyPoints.clear();
        for (int cx = data.chunkPos().x - chunkRadius; cx <= data.chunkPos().x + chunkRadius; cx++) {
            for (int cz = data.chunkPos().z - chunkRadius; cz <= data.chunkPos().z + chunkRadius; cz++) {
                ChunkActivityData nearby = chunks.get(ChunkPos.toLong(cx, cz));
                if (nearby != null) nearbyPoints.addAll(nearby.activityPoints());
            }
        }

        for (int gz = 0; gz < data.gridSize(); gz++) {
            for (int gx = 0; gx < data.gridSize(); gx++) {
                int index = data.index(gx, gz);
                double worldX = data.chunkPos().getStartX() + gx * data.resolution();
                double worldZ = data.chunkPos().getStartZ() + gz * data.resolution();
                double nearest = radiusSquared;

                for (ActivityPoint point : nearbyPoints) {
                    double distance = ActivityHeatmap.horizontalDistanceSquared(worldX, worldZ, point.position().getX() + 0.5, point.position().getZ() + 0.5);
                    if (distance < nearest) nearest = distance;
                }

                data.nearestDistanceSquared(index, nearest);
                data.color(index, ActivityHeatmap.color(ActivityHeatmap.normalize(nearest, nearDistance.get(), farDistance.get()), overlayAlpha.get()));
                if (nearest >= thresholdSquared) {
                    lowSamples++;
                    candidateDistanceSquared = Math.min(candidateDistanceSquared, nearest);
                }
            }
        }

        data.lowActivityCandidate(lowSamples >= Math.ceil(data.gridSize() * data.gridSize() * LOW_SAMPLE_RATIO));
        data.candidateDistance(Math.sqrt(candidateDistanceSquared));
        maybeNotify(data);
    }

    private void maybeNotify(ChunkActivityData data) {
        if (!data.lowActivityCandidate() || notifiedChunks.contains(data.key())) return;
        Set<Long> region = connectedCandidates(data, minimumRegionChunks.get());
        if (region.size() < minimumRegionChunks.get()) return;
        long now = System.currentTimeMillis();
        if (now - lastNotification < notificationCooldown.get() * 1000) return;

        notifiedChunks.addAll(region);
        lastNotification = now;
        int x = data.chunkPos().getCenterX();
        int z = data.chunkPos().getCenterZ();
        info("Low-activity area detected near %d, %d: ~%d blocks from nearest known activity.", x, z, Math.round(data.candidateDistance()));
    }

    private Set<Long> connectedCandidates(ChunkActivityData start, int required) {
        Set<Long> visited = new HashSet<>();
        ArrayDeque<ChunkPos> queue = new ArrayDeque<>();
        queue.add(start.chunkPos());
        while (!queue.isEmpty() && visited.size() < required) {
            ChunkPos pos = queue.removeFirst();
            long key = pos.toLong();
            ChunkActivityData data = chunks.get(key);
            if (data == null || !data.lowActivityCandidate() || !visited.add(key)) continue;
            queue.add(new ChunkPos(pos.x + 1, pos.z));
            queue.add(new ChunkPos(pos.x - 1, pos.z));
            queue.add(new ChunkPos(pos.x, pos.z + 1));
            queue.add(new ChunkPos(pos.x, pos.z - 1));
        }
        return visited;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!renderOverlay.get() || mc.player == null || mc.world == null) return;
        double maxDistanceSquared = (double) maxAnalysisDistance.get() * maxAnalysisDistance.get();
        for (ChunkActivityData data : chunks.values()) {
            double centerX = data.chunkPos().getCenterX();
            double centerZ = data.chunkPos().getCenterZ();
            if (ActivityHeatmap.horizontalDistanceSquared(mc.player.getX(), mc.player.getZ(), centerX, centerZ) > maxDistanceSquared || data.color(0) == null) continue;

            for (int gz = 0; gz < data.gridSize() - 1; gz++) {
                for (int gx = 0; gx < data.gridSize() - 1; gx++) {
                    int i00 = data.index(gx, gz);
                    int i01 = data.index(gx, gz + 1);
                    int i11 = data.index(gx + 1, gz + 1);
                    int i10 = data.index(gx + 1, gz);
                    double x0 = data.chunkPos().getStartX() + gx * data.resolution();
                    double z0 = data.chunkPos().getStartZ() + gz * data.resolution();
                    double x1 = x0 + data.resolution();
                    double z1 = z0 + data.resolution();
                    event.renderer.quad(
                        x0, data.surfaceY(i00) + 0.03, z0,
                        x0, data.surfaceY(i01) + 0.03, z1,
                        x1, data.surfaceY(i11) + 0.03, z1,
                        x1, data.surfaceY(i10) + 0.03, z0,
                        data.color(i00), data.color(i01), data.color(i11), data.color(i10));
                }
            }
        }
    }

    private void enqueue(int chunkX, int chunkZ) {
        if (mc.world == null || !mc.world.getChunkManager().isChunkLoaded(chunkX, chunkZ)) return;
        Chunk chunk = mc.world.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
        if (!(chunk instanceof WorldChunk worldChunk) || !withinRange(worldChunk.getPos())) return;
        long key = worldChunk.getPos().toLong();
        if (!inFlight.contains(key) && queuedChunks.add(key)) scanQueue.add(new QueuedChunk(worldChunk.getPos(), distanceSquared(worldChunk.getPos())));
    }

    private void seedLoadedChunks() {
        if (mc.world == null || mc.player == null) return;
        scanExecutor = Executors.newFixedThreadPool(MAX_IN_FLIGHT, runnable -> {
            Thread thread = new Thread(runnable, "LummelFinder scanner");
            thread.setDaemon(true);
            return thread;
        });
        for (Chunk chunk : Utils.chunks(true)) if (chunk instanceof WorldChunk worldChunk && withinRange(worldChunk.getPos())) {
            long key = worldChunk.getPos().toLong();
            if (queuedChunks.add(key)) scanQueue.add(new QueuedChunk(worldChunk.getPos(), distanceSquared(worldChunk.getPos())));
        }
    }

    private boolean withinRange(ChunkPos pos) {
        if (mc.player == null) return false;
        double distanceSquared = ActivityHeatmap.horizontalDistanceSquared(mc.player.getX(), mc.player.getZ(), pos.getCenterX(), pos.getCenterZ());
        double max = maxAnalysisDistance.get() + 24.0;
        return distanceSquared <= max * max;
    }

    private double distanceSquared(ChunkPos pos) {
        return mc.player == null ? Double.MAX_VALUE : ActivityHeatmap.horizontalDistanceSquared(mc.player.getX(), mc.player.getZ(), pos.getCenterX(), pos.getCenterZ());
    }

    private void reprioritizeIfNeeded() {
        int x = mc.player.getBlockX() >> 4, z = mc.player.getBlockZ() >> 4;
        if (x == playerChunkX && z == playerChunkZ) return;
        playerChunkX = x;
        playerChunkZ = z;
        List<QueuedChunk> queued = new ArrayList<>(scanQueue);
        scanQueue.clear();
        for (QueuedChunk chunk : queued) scanQueue.add(new QueuedChunk(chunk.pos, distanceSquared(chunk.pos)));
    }

    private void smoothChunkEdges(ChunkActivityData data) {
        smoothEdge(data, chunks.get(ChunkPos.toLong(data.chunkPos().x - 1, data.chunkPos().z)), true, false);
        smoothEdge(data, chunks.get(ChunkPos.toLong(data.chunkPos().x + 1, data.chunkPos().z)), true, true);
        smoothEdge(data, chunks.get(ChunkPos.toLong(data.chunkPos().x, data.chunkPos().z - 1)), false, false);
        smoothEdge(data, chunks.get(ChunkPos.toLong(data.chunkPos().x, data.chunkPos().z + 1)), false, true);
    }

    private void smoothEdge(ChunkActivityData data, ChunkActivityData neighbor, boolean xEdge, boolean positive) {
        if (neighbor == null || neighbor.resolution() != data.resolution()) return;
        int last = data.gridSize() - 1;
        for (int i = 0; i <= last; i++) {
            int a = xEdge ? data.index(positive ? last : 0, i) : data.index(i, positive ? last : 0);
            int b = xEdge ? neighbor.index(positive ? 0 : last, i) : neighbor.index(i, positive ? 0 : last);
            int average = (data.surfaceY(a) + neighbor.surfaceY(b)) / 2;
            data.surfaceY(a, average);
            neighbor.surfaceY(b, average);
        }
    }

    private void markNearbyDirty(ChunkPos pos) {
        int radius = (searchRadius.get() + 15) / 16;
        for (int x = pos.x - radius; x <= pos.x + radius; x++) {
            for (int z = pos.z - radius; z <= pos.z + radius; z++) markDirty(ChunkPos.toLong(x, z));
        }
    }

    private void markAllDirty() {
        for (long key : chunks.keySet()) markDirty(key);
    }

    private void markDirty(long key) {
        if (chunks.containsKey(key) && dirtyChunks.add(key)) dirtyQueue.addLast(key);
    }

    private void prune() {
        if (mc.world == null) return;
        List<ChunkPos> removed = new ArrayList<>();
        Iterator<Map.Entry<Long, ChunkActivityData>> iterator = chunks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, ChunkActivityData> entry = iterator.next();
            ChunkPos pos = entry.getValue().chunkPos();
            if (mc.world.getChunkManager().isChunkLoaded(pos.x, pos.z) && withinRange(pos)) continue;
            iterator.remove();
            dirtyChunks.remove(entry.getKey());
            notifiedChunks.remove(entry.getKey());
            removed.add(pos);
        }
        for (ChunkPos pos : removed) markNearbyDirty(pos);
    }

    private void trimCache() {
        while (chunks.size() > MAX_CACHED_CHUNKS) {
            Iterator<Map.Entry<Long, ChunkActivityData>> iterator = chunks.entrySet().iterator();
            Map.Entry<Long, ChunkActivityData> eldest = iterator.next();
            iterator.remove();
            dirtyChunks.remove(eldest.getKey());
            notifiedChunks.remove(eldest.getKey());
            markNearbyDirty(eldest.getValue().chunkPos());
        }
    }

    private void rescan() {
        if (!isActive()) return;
        clear();
        seedLoadedChunks();
    }

    private void clear() {
        generation++;
        if (scanExecutor != null) scanExecutor.shutdownNow();
        chunks.clear();
        scanQueue.clear();
        queuedChunks.clear();
        dirtyQueue.clear();
        dirtyChunks.clear();
        notifiedChunks.clear();
        nearbyPoints.clear();
        scanExecutor = null;
        inFlight.clear();
        playerChunkX = playerChunkZ = Integer.MIN_VALUE;
        ticks = 0;
        lastNotification = 0;
    }

    public enum SampleResolution {
        BLOCK_1(1), BLOCKS_2(2), BLOCKS_4(4), BLOCKS_8(8);

        private final int blocks;
        SampleResolution(int blocks) { this.blocks = blocks; }
    }

    private record QueuedChunk(ChunkPos pos, double distanceSquared) {}
}
