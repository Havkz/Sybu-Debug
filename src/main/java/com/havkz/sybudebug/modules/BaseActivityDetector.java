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
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

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
    private static final int WORKERS = 5;
    private static final int RESOLUTION = 8;
    private static final int MAX_CACHED_CHUNKS = 4096;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Boolean> enableHoles = sgGeneral.add(new BoolSetting.Builder()
        .name("holes").description("Shows enclosed vertical holes as player activity.").defaultValue(true).onChanged(value -> rescan()).build());
    private final Setting<Boolean> enableObsidian = sgGeneral.add(new BoolSetting.Builder()
        .name("obsidian-activity").description("Also treats player-placed-looking obsidian as activity.").defaultValue(false).onChanged(value -> rescan()).build());
    private final Setting<SettingColor> activityColor = sgGeneral.add(new ColorSetting.Builder()
        .name("activity-color").description("Color and transparency near holes or obsidian.")
        .defaultValue(new SettingColor(255, 40, 40, 70)).onChanged(value -> recolorAll()).build());
    private final Setting<SettingColor> untouchedColor = sgGeneral.add(new ColorSetting.Builder()
        .name("untouched-color").description("Color and transparency far from known activity.")
        .defaultValue(new SettingColor(40, 220, 40, 70)).onChanged(value -> recolorAll()).build());
    private final Setting<Integer> activityRadius = sgGeneral.add(new IntSetting.Builder()
        .name("red-radius").description("Meters around a hole or obsidian marker that fade from red toward green.").defaultValue(64)
        .range(8, 2048).sliderRange(8, 512).onChanged(value -> recolorAll()).build());
    private final Setting<RenderMode> renderMode = sgGeneral.add(new EnumSetting.Builder<RenderMode>()
        .name("render-mode").description("Smooth Layer follows terrain; Chunk Based is one flat binary-color quad per chunk.")
        .defaultValue(RenderMode.SMOOTH_LAYER).build());

    private final Map<Long, ChunkActivityData> chunks = new LinkedHashMap<>();
    private final PriorityQueue<QueuedChunk> scanQueue = new PriorityQueue<>((a, b) -> Double.compare(a.distanceSquared, b.distanceSquared));
    private final Set<Long> queuedChunks = new HashSet<>();
    private final Set<Long> inFlight = new HashSet<>();
    private final Set<Long> rescanAfterFlight = new HashSet<>();
    private final List<ActivityPoint> nearbyPoints = new ArrayList<>();
    private ExecutorService scanExecutor;
    private int generation;
    private int playerChunkX = Integer.MIN_VALUE;
    private int playerChunkZ = Integer.MIN_VALUE;
    private int ticks;

    public BaseActivityDetector() {
        super(SybuDebugAddon.CATEGORY, "LümmelFinder", "Shows holes as red activity spreading into green untouched terrain.");
    }

    @Override public void onActivate() { clear(); seedLoadedChunks(); }
    @Override public void onDeactivate() { clear(); }
    @EventHandler private void onGameJoined(GameJoinedEvent event) { clear(); seedLoadedChunks(); }
    @EventHandler private void onGameLeft(GameLeftEvent event) { clear(); }

    @EventHandler
    private void onChunkData(ChunkDataEvent event) {
        ChunkPos pos = event.chunk().getPos();
        if (mc.isOnThread()) enqueue(pos.x, pos.z, false); else mc.execute(() -> enqueue(pos.x, pos.z, false));
    }

    @EventHandler
    private void onBlockUpdate(BlockUpdateEvent event) {
        int chunkX = event.pos.getX() >> 4, chunkZ = event.pos.getZ() >> 4;
        enqueue(chunkX, chunkZ, true);
        int localX = event.pos.getX() & 15, localZ = event.pos.getZ() & 15;
        if (localX == 0) enqueue(chunkX - 1, chunkZ, true); else if (localX == 15) enqueue(chunkX + 1, chunkZ, true);
        if (localZ == 0) enqueue(chunkX, chunkZ - 1, true); else if (localZ == 15) enqueue(chunkX, chunkZ + 1, true);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.world == null || mc.player == null) return;
        reprioritizeIfNeeded();
        for (int i = 0; i < WORKERS && inFlight.size() < WORKERS && !scanQueue.isEmpty(); i++) analyze(scanQueue.remove());
        if (++ticks % 20 == 0) {
            ensureLoadedChunksQueued();
            pruneUnloaded();
        }
    }

    private void analyze(QueuedChunk queued) {
        long key = queued.pos.toLong();
        queuedChunks.remove(key);
        if (!withinRenderDistance(queued.pos) || !mc.world.getChunkManager().isChunkLoaded(queued.pos.x, queued.pos.z)) return;
        Chunk chunk = mc.world.getChunk(queued.pos.x, queued.pos.z, ChunkStatus.FULL, false);
        if (!(chunk instanceof WorldChunk worldChunk)) return;
        boolean obsidian = enableObsidian.get();
        boolean holes = enableHoles.get();
        ActivityScanner.Snapshot snapshot = ActivityScanner.snapshot(mc.world, worldChunk, mc.world.getBottomY(), mc.world.getHeight(), obsidian);
        int currentGeneration = generation;
        inFlight.add(key);
        scanExecutor.submit(() -> {
            try {
                ChunkActivityData data = ActivityScanner.scan(snapshot, RESOLUTION, holes, obsidian);
                mc.execute(() -> installResult(key, currentGeneration, data));
            } catch (RuntimeException exception) {
                SybuDebugAddon.LOG.warn("LümmelFinder failed to scan chunk {}", queued.pos, exception);
                mc.execute(() -> {
                    if (currentGeneration != generation) return;
                    inFlight.remove(key);
                    enqueue(queued.pos.x, queued.pos.z, true);
                });
            }
        });
    }

    private void installResult(long key, int resultGeneration, ChunkActivityData data) {
        if (resultGeneration != generation) return;
        inFlight.remove(key);
        if (mc.world == null || !isActive() || !mc.world.getChunkManager().isChunkLoaded(data.chunkPos().x, data.chunkPos().z)) return;
        ChunkActivityData previous = chunks.put(key, data);
        smoothChunkEdges(data);
        boolean activityChanged = previous == null ? !data.activityPoints().isEmpty() : !previous.activityPoints().equals(data.activityPoints());
        if (activityChanged) recolorAround(data.chunkPos()); else updateColors(data);
        trimCache();
        if (rescanAfterFlight.remove(key)) enqueue(data.chunkPos().x, data.chunkPos().z, true);
    }

    private void recolorAround(ChunkPos center) {
        int chunkRadius = loadedChunkRadius();
        for (int x = center.x - chunkRadius; x <= center.x + chunkRadius; x++) for (int z = center.z - chunkRadius; z <= center.z + chunkRadius; z++) {
            ChunkActivityData data = chunks.get(ChunkPos.toLong(x, z));
            if (data != null) updateColors(data);
        }
    }

    private void recolorAll() { for (ChunkActivityData data : chunks.values()) updateColors(data); }

    private void updateColors(ChunkActivityData data) {
        int radius = activityRadius.get();
        double farSquared = (double) radius * radius;
        nearbyPoints.clear();
        int chunkRadius = loadedChunkRadius();
        for (int x = data.chunkPos().x - chunkRadius; x <= data.chunkPos().x + chunkRadius; x++)
            for (int z = data.chunkPos().z - chunkRadius; z <= data.chunkPos().z + chunkRadius; z++) {
                ChunkActivityData nearby = chunks.get(ChunkPos.toLong(x, z));
                if (nearby != null) nearbyPoints.addAll(nearby.activityPoints());
            }
        for (int gz = 0; gz < data.gridSize(); gz++) for (int gx = 0; gx < data.gridSize(); gx++) {
            int index = data.index(gx, gz);
            double worldX = data.chunkPos().getStartX() + gx * data.resolution();
            double worldZ = data.chunkPos().getStartZ() + gz * data.resolution();
            double nearest = farSquared;
            for (ActivityPoint point : nearbyPoints) nearest = Math.min(nearest,
                ActivityHeatmap.horizontalDistanceSquared(worldX, worldZ, point.position().getX() + 0.5, point.position().getZ() + 0.5));
            data.color(index, ActivityHeatmap.color(ActivityHeatmap.normalize(nearest, Math.min(8, radius - 1), radius),
                activityColor.get(), untouchedColor.get()));
        }
        int redBlocks = 0;
        for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            double worldX = data.chunkPos().getStartX() + x + 0.5;
            double worldZ = data.chunkPos().getStartZ() + z + 0.5;
            for (ActivityPoint point : nearbyPoints) if (ActivityHeatmap.horizontalDistanceSquared(
                worldX, worldZ, point.position().getX() + 0.5, point.position().getZ() + 0.5) <= farSquared) {
                redBlocks++;
                break;
            }
        }
        data.chunkColor(ActivityHeatmap.color(redBlocks > 128 ? 0 : 1, activityColor.get(), untouchedColor.get()));
    }

    private int loadedChunkRadius() {
        int requested = (activityRadius.get() + 15) / 16 + 1;
        int loadedDiameter = mc.options.getViewDistance().getValue() * 2 + 2;
        return Math.min(requested, loadedDiameter);
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.player == null || mc.world == null) return;
        for (ChunkActivityData data : chunks.values()) {
            if (!withinRenderDistance(data.chunkPos()) || !mc.world.getChunkManager().isChunkLoaded(data.chunkPos().x, data.chunkPos().z) || data.color(0) == null) continue;
            if (renderMode.get() == RenderMode.CHUNK_BASED) {
                renderChunk(event, data);
                continue;
            }
            renderSmoothChunk(event, data);
        }
    }

    private void renderSmoothChunk(Render3DEvent event, ChunkActivityData data) {
        int last = data.gridSize() - 1;
        int i00 = data.index(0, 0), i01 = data.index(0, last), i11 = data.index(last, last), i10 = data.index(last, 0);
        double x0 = data.chunkPos().getStartX(), z0 = data.chunkPos().getStartZ();
        double x1 = x0 + 16, z1 = z0 + 16;
        event.renderer.quad(x0, data.surfaceY(i00) + 0.03, z0, x0, data.surfaceY(i01) + 0.03, z1,
            x1, data.surfaceY(i11) + 0.03, z1, x1, data.surfaceY(i10) + 0.03, z0,
            data.color(i00), data.color(i01), data.color(i11), data.color(i10));
    }

    private void renderChunk(Render3DEvent event, ChunkActivityData data) {
        var color = data.chunkColor();
        double y = data.averageSurfaceY() + 0.03;
        double x0 = data.chunkPos().getStartX(), z0 = data.chunkPos().getStartZ();
        double x1 = x0 + 16, z1 = z0 + 16;
        event.renderer.quad(x0, y, z0, x0, y, z1, x1, y, z1, x1, y, z0, color, color, color, color);
    }

    private void enqueue(int chunkX, int chunkZ, boolean force) {
        if (mc.world == null || !mc.world.getChunkManager().isChunkLoaded(chunkX, chunkZ)) return;
        Chunk chunk = mc.world.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
        if (!(chunk instanceof WorldChunk worldChunk) || !withinRenderDistance(worldChunk.getPos())) return;
        long key = worldChunk.getPos().toLong();
        if (!force && chunks.containsKey(key)) return;
        if (force && inFlight.contains(key)) { rescanAfterFlight.add(key); return; }
        if (!inFlight.contains(key) && queuedChunks.add(key)) scanQueue.add(new QueuedChunk(worldChunk.getPos(), distanceSquared(worldChunk.getPos())));
    }

    private void seedLoadedChunks() {
        if (mc.world == null || mc.player == null) return;
        scanExecutor = Executors.newFixedThreadPool(WORKERS, runnable -> {
            Thread thread = new Thread(runnable, "LummelFinder scanner");
            thread.setDaemon(true);
            return thread;
        });
        ensureLoadedChunksQueued();
    }

    private boolean withinRenderDistance(ChunkPos pos) {
        if (mc.player == null) return false;
        int viewDistance = mc.options.getViewDistance().getValue() + 1;
        int px = mc.player.getBlockX() >> 4, pz = mc.player.getBlockZ() >> 4;
        return Math.abs(pos.x - px) <= viewDistance && Math.abs(pos.z - pz) <= viewDistance;
    }

    private double distanceSquared(ChunkPos pos) {
        return mc.player == null ? Double.MAX_VALUE : ActivityHeatmap.horizontalDistanceSquared(mc.player.getX(), mc.player.getZ(), pos.getCenterX(), pos.getCenterZ());
    }

    private void reprioritizeIfNeeded() {
        int x = mc.player.getBlockX() >> 4, z = mc.player.getBlockZ() >> 4;
        if (x == playerChunkX && z == playerChunkZ) return;
        playerChunkX = x; playerChunkZ = z;
        List<QueuedChunk> queued = new ArrayList<>(scanQueue);
        scanQueue.clear();
        for (QueuedChunk chunk : queued) {
            if (withinRenderDistance(chunk.pos)) scanQueue.add(new QueuedChunk(chunk.pos, distanceSquared(chunk.pos)));
            else queuedChunks.remove(chunk.pos.toLong());
        }
        ensureLoadedChunksQueued();
    }

    private void ensureLoadedChunksQueued() {
        for (Chunk chunk : Utils.chunks(true)) if (chunk instanceof WorldChunk worldChunk && withinRenderDistance(worldChunk.getPos()))
            enqueue(worldChunk.getPos().x, worldChunk.getPos().z, false);
    }

    private void smoothChunkEdges(ChunkActivityData data) {
        smoothEdge(data, chunks.get(ChunkPos.toLong(data.chunkPos().x - 1, data.chunkPos().z)), true, false);
        smoothEdge(data, chunks.get(ChunkPos.toLong(data.chunkPos().x + 1, data.chunkPos().z)), true, true);
        smoothEdge(data, chunks.get(ChunkPos.toLong(data.chunkPos().x, data.chunkPos().z - 1)), false, false);
        smoothEdge(data, chunks.get(ChunkPos.toLong(data.chunkPos().x, data.chunkPos().z + 1)), false, true);
        smoothCorner(data.chunkPos().x, data.chunkPos().z);
        smoothCorner(data.chunkPos().x + 1, data.chunkPos().z);
        smoothCorner(data.chunkPos().x, data.chunkPos().z + 1);
        smoothCorner(data.chunkPos().x + 1, data.chunkPos().z + 1);
    }

    private void smoothCorner(int boundaryX, int boundaryZ) {
        ChunkActivityData[] touching = {
            chunks.get(ChunkPos.toLong(boundaryX - 1, boundaryZ - 1)), chunks.get(ChunkPos.toLong(boundaryX, boundaryZ - 1)),
            chunks.get(ChunkPos.toLong(boundaryX - 1, boundaryZ)), chunks.get(ChunkPos.toLong(boundaryX, boundaryZ))
        };
        int sum = 0, count = 0;
        for (ChunkActivityData chunk : touching) if (chunk != null) {
            int last = chunk.gridSize() - 1;
            int gx = chunk.chunkPos().x < boundaryX ? last : 0;
            int gz = chunk.chunkPos().z < boundaryZ ? last : 0;
            sum += chunk.surfaceY(chunk.index(gx, gz)); count++;
        }
        if (count < 2) return;
        int average = Math.round((float) sum / count);
        for (ChunkActivityData chunk : touching) if (chunk != null) {
            int last = chunk.gridSize() - 1;
            int gx = chunk.chunkPos().x < boundaryX ? last : 0;
            int gz = chunk.chunkPos().z < boundaryZ ? last : 0;
            chunk.surfaceY(chunk.index(gx, gz), average);
        }
    }

    private void smoothEdge(ChunkActivityData data, ChunkActivityData neighbor, boolean xEdge, boolean positive) {
        if (neighbor == null || neighbor.resolution() != data.resolution()) return;
        int last = data.gridSize() - 1;
        for (int i = 0; i <= last; i++) {
            int a = xEdge ? data.index(positive ? last : 0, i) : data.index(i, positive ? last : 0);
            int b = xEdge ? neighbor.index(positive ? 0 : last, i) : neighbor.index(i, positive ? 0 : last);
            int average = (data.surfaceY(a) + neighbor.surfaceY(b)) / 2;
            data.surfaceY(a, average); neighbor.surfaceY(b, average);
        }
    }

    private void pruneUnloaded() {
        if (mc.world == null) return;
        Iterator<Map.Entry<Long, ChunkActivityData>> iterator = chunks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, ChunkActivityData> entry = iterator.next();
            ChunkPos pos = entry.getValue().chunkPos();
            if (mc.world.getChunkManager().isChunkLoaded(pos.x, pos.z)) continue;
            iterator.remove();
        }
    }

    private void trimCache() {
        while (chunks.size() > MAX_CACHED_CHUNKS) {
            Iterator<Long> iterator = chunks.keySet().iterator(); iterator.next(); iterator.remove();
        }
    }

    private void rescan() { if (isActive()) { clear(); seedLoadedChunks(); } }

    private void clear() {
        generation++;
        if (scanExecutor != null) scanExecutor.shutdownNow();
        chunks.clear(); scanQueue.clear(); queuedChunks.clear(); inFlight.clear(); rescanAfterFlight.clear(); nearbyPoints.clear();
        scanExecutor = null; playerChunkX = playerChunkZ = Integer.MIN_VALUE; ticks = 0;
    }

    private record QueuedChunk(ChunkPos pos, double distanceSquared) {}

    public enum RenderMode {
        SMOOTH_LAYER("Smooth Layer"), CHUNK_BASED("Chunk Based");
        private final String title;
        RenderMode(String title) { this.title = title; }
        @Override public String toString() { return title; }
    }
}
