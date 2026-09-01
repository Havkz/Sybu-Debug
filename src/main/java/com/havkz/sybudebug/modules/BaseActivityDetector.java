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
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.ArrayDeque;
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
    private static final int RESOLUTION = 16;
    private static final int MAX_CACHED_CHUNKS = 4096;
    private static final int RECOLORS_PER_TICK = 4;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Boolean> enableHoles = sgGeneral.add(new BoolSetting.Builder()
        .name("holes").description("Shows enclosed vertical holes as player activity.").defaultValue(true).onChanged(value -> rescan()).build());
    private final Setting<Boolean> enableObsidian = sgGeneral.add(new BoolSetting.Builder()
        .name("obsidian-activity").description("Also treats player-placed-looking obsidian as activity.").defaultValue(false).onChanged(value -> rescan()).build());
    private final Setting<SettingColor> activityColor = sgGeneral.add(new ColorSetting.Builder()
        .name("activity-color").description("Color and transparency near holes or obsidian.")
        .defaultValue(new SettingColor(255, 40, 40, 70)).onChanged(value -> queueRecolorAll()).build());
    private final Setting<SettingColor> untouchedColor = sgGeneral.add(new ColorSetting.Builder()
        .name("untouched-color").description("Color and transparency far from known activity.")
        .defaultValue(new SettingColor(40, 220, 40, 70)).onChanged(value -> queueRecolorAll()).build());
    private final Setting<SettingColor> gridLineColor = sgGeneral.add(new ColorSetting.Builder()
        .name("grid-line-color").description("Color and transparency of the chunk boundary grid.")
        .defaultValue(new SettingColor(255, 255, 255, 140)).build());
    private final Setting<Integer> activityRadius = sgGeneral.add(new IntSetting.Builder()
        .name("red-radius").description("Meters around a hole or obsidian marker that fade from red toward green.").defaultValue(64)
        .range(8, 2048).sliderRange(8, 512).onChanged(value -> queueRecolorAll()).build());

    private final Map<Long, ChunkActivityData> chunks = new LinkedHashMap<>();
    private final PriorityQueue<QueuedChunk> scanQueue = new PriorityQueue<>((a, b) -> Double.compare(a.distanceSquared, b.distanceSquared));
    private final Set<Long> queuedChunks = new HashSet<>();
    private final Set<Long> inFlight = new HashSet<>();
    private final Set<Long> rescanAfterFlight = new HashSet<>();
    private final ArrayDeque<Long> recolorQueue = new ArrayDeque<>();
    private final Set<Long> queuedRecolors = new HashSet<>();
    private ExecutorService scanExecutor;
    private int generation;
    private int playerChunkX = Integer.MIN_VALUE;
    private int playerChunkZ = Integer.MIN_VALUE;
    private int ticks;

    public BaseActivityDetector() {
        super(SybuDebugAddon.CATEGORY, "BaseGrid", "Probability grid for base locations inferred from areas without visible player activity.");
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
        if (!recolorQueue.isEmpty()) {
            List<ActivityPoint> points = allActivityPoints();
            for (int i = 0; i < RECOLORS_PER_TICK && !recolorQueue.isEmpty(); i++) {
                long key = recolorQueue.removeFirst();
                queuedRecolors.remove(key);
                ChunkActivityData data = chunks.get(key);
                if (data != null) updateColors(data, points);
            }
        }
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
                SybuDebugAddon.LOG.warn("BaseGrid failed to scan chunk {}", queued.pos, exception);
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
        boolean activityChanged = previous == null ? !data.activityPoints().isEmpty() : !previous.activityPoints().equals(data.activityPoints());
        if (activityChanged) queueRecolorAround(data.chunkPos()); else updateColors(data);
        trimCache();
        if (rescanAfterFlight.remove(key)) enqueue(data.chunkPos().x, data.chunkPos().z, true);
    }

    private void queueRecolorAround(ChunkPos center) {
        double reach = activityRadius.get() + 24.0, reachSquared = reach * reach;
        for (ChunkActivityData data : chunks.values()) if (ActivityHeatmap.horizontalDistanceSquared(
            center.getCenterX(), center.getCenterZ(), data.chunkPos().getCenterX(), data.chunkPos().getCenterZ()) <= reachSquared)
            queueRecolor(data.key());
    }

    private void queueRecolorAll() { for (long key : chunks.keySet()) queueRecolor(key); }

    private void queueRecolor(long key) { if (queuedRecolors.add(key)) recolorQueue.addLast(key); }

    private void updateColors(ChunkActivityData data) { updateColors(data, allActivityPoints()); }

    private void updateColors(ChunkActivityData data, List<ActivityPoint> points) {
        int radius = activityRadius.get();
        double farSquared = (double) radius * radius;
        int redBlocks = 0;
        for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            double worldX = data.chunkPos().getStartX() + x + 0.5;
            double worldZ = data.chunkPos().getStartZ() + z + 0.5;
            for (ActivityPoint point : points) if (ActivityHeatmap.horizontalDistanceSquared(
                worldX, worldZ, point.position().getX() + 0.5, point.position().getZ() + 0.5) <= farSquared) {
                redBlocks++;
                break;
            }
        }
        data.chunkColor(ActivityHeatmap.color(redBlocks > 128 ? 0 : 1, activityColor.get(), untouchedColor.get()));
    }

    private List<ActivityPoint> allActivityPoints() {
        List<ActivityPoint> points = new ArrayList<>();
        for (ChunkActivityData data : chunks.values()) points.addAll(data.activityPoints());
        return points;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.player == null || mc.world == null) return;
        int sharedChunkY = sharedChunkY();
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (ChunkActivityData data : chunks.values()) {
            if (!isRenderedChunk(data.chunkPos().x, data.chunkPos().z)) continue;
            minX = Math.min(minX, data.chunkPos().x); maxX = Math.max(maxX, data.chunkPos().x);
            minZ = Math.min(minZ, data.chunkPos().z); maxZ = Math.max(maxZ, data.chunkPos().z);
        }
        if (minX <= maxX) {
            renderMergedChunks(event, minX, maxX, minZ, maxZ, sharedChunkY + 0.03);
            renderGrid(event, minX, maxX, minZ, maxZ, sharedChunkY + 0.04);
        }
    }

    private void renderMergedChunks(Render3DEvent event, int minX, int maxX, int minZ, int maxZ, double y) {
        for (int z = minZ; z <= maxZ; z++) {
            int runStart = minX;
            Color runColor = renderedColor(minX, z);
            for (int x = minX + 1; x <= maxX + 1; x++) {
                Color next = x <= maxX ? renderedColor(x, z) : null;
                if (ActivityHeatmap.sameColor(runColor, next)) continue;
                if (runColor != null) renderRun(event, runStart, x, z, y, runColor);
                runStart = x;
                runColor = next;
            }
        }
    }

    private Color renderedColor(int chunkX, int chunkZ) {
        ChunkActivityData data = chunks.get(ChunkPos.toLong(chunkX, chunkZ));
        return data != null && isRenderedChunk(chunkX, chunkZ) ? data.chunkColor() : null;
    }

    private void renderRun(Render3DEvent event, int startX, int endX, int chunkZ, double y, Color color) {
        double x0 = startX * 16.0, x1 = endX * 16.0;
        double z0 = chunkZ * 16.0, z1 = z0 + 16;
        event.renderer.quad(x0, y, z0, x0, y, z1, x1, y, z1, x1, y, z0, color, color, color, color);
    }

    private void renderGrid(Render3DEvent event, int minX, int maxX, int minZ, int maxZ, double y) {
        Color lineColor = gridLineColor.get();
        double x0 = minX * 16.0, x1 = (maxX + 1) * 16.0;
        double z0 = minZ * 16.0, z1 = (maxZ + 1) * 16.0;
        for (int x = minX; x <= maxX + 1; x++) event.renderer.line(x * 16.0, y, z0, x * 16.0, y, z1, lineColor);
        for (int z = minZ; z <= maxZ + 1; z++) event.renderer.line(x0, y, z * 16.0, x1, y, z * 16.0, lineColor);
    }

    private int sharedChunkY() {
        long sum = 0;
        int count = 0;
        for (ChunkActivityData data : chunks.values()) if (isRenderedChunk(data.chunkPos().x, data.chunkPos().z) && data.chunkColor() != null) {
            sum += data.averageSurfaceY();
            count++;
        }
        return ActivityHeatmap.roundedAverage(sum, count);
    }

    private boolean isRenderedChunk(int chunkX, int chunkZ) {
        ChunkActivityData data = chunks.get(ChunkPos.toLong(chunkX, chunkZ));
        return data != null && data.chunkColor() != null && withinRenderDistance(data.chunkPos())
            && mc.world.getChunkManager().isChunkLoaded(chunkX, chunkZ);
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
            Thread thread = new Thread(runnable, "BaseGrid scanner");
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
        chunks.clear(); scanQueue.clear(); queuedChunks.clear(); inFlight.clear(); rescanAfterFlight.clear();
        recolorQueue.clear(); queuedRecolors.clear();
        scanExecutor = null; playerChunkX = playerChunkZ = Integer.MIN_VALUE; ticks = 0;
    }

    private record QueuedChunk(ChunkPos pos, double distanceSquared) {}
}
