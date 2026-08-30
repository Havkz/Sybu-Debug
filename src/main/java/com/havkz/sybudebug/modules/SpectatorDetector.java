package com.havkz.sybudebug.modules;

import com.havkz.sybudebug.SybuDebugAddon;
import com.havkz.sybudebug.detection.DetectionCandidate;
import com.havkz.sybudebug.detection.DetectionEngine;
import com.havkz.sybudebug.detection.DetectionSignal;
import com.havkz.sybudebug.detection.ConfidenceCalculator;
import com.havkz.sybudebug.detection.DetectionActionState;
import com.havkz.sybudebug.tracking.PlayerTracker;
import com.havkz.sybudebug.tracking.WaypointTracker;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.network.packet.s2c.play.WaypointS2CPacket;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SpectatorDetector extends Module {
    private static final long LAST_KNOWN_TIMEOUT_MS = 15_000;
    private static final long CORRELATION_WINDOW_MS = 500;
    private static final long JOIN_GRACE_MS = 3_000;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup sgActions = settings.createGroup("Actions");
    private final SettingGroup sgDebug = settings.createGroup("Debug");

    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder().name("ignore-friends").description("Ignores Meteor friends.").defaultValue(true).build());
    private final Setting<Boolean> chatWarning = sgGeneral.add(new BoolSetting.Builder().name("chat-warning").description("Shows detection warnings in chat.").defaultValue(true).build());
    private final Setting<Integer> warningConfidence = sgGeneral.add(new IntSetting.Builder().name("warning-confidence").description("Minimum confidence for chat warnings.").defaultValue(60).range(0, 100).sliderRange(0, 100).build());
    private final Setting<Double> warningCooldown = sgGeneral.add(new DoubleSetting.Builder().name("warning-cooldown").description("Seconds between warnings for one candidate.").defaultValue(5).min(0).sliderMax(30).build());
    private final Setting<Double> exactPositionRange = sgGeneral.add(new DoubleSetting.Builder().name("exact-position-range").description("Maximum range for detections with an exact position.").defaultValue(256).min(1).sliderMax(512).build());

    private final Setting<Boolean> renderTracer = sgRender.add(new BoolSetting.Builder().name("render-tracer").description("Draws a tracer to an exact client-known position.").defaultValue(true).build());
    private final Setting<Integer> minimumConfidence = sgRender.add(new IntSetting.Builder().name("minimum-confidence").description("Minimum confidence to render.").defaultValue(60).range(0, 100).sliderRange(0, 100).build());
    private final Setting<TracerMode> tracerMode = sgRender.add(new EnumSetting.Builder<TracerMode>().name("tracer-mode").description("Whether last-known positions may be rendered.").defaultValue(TracerMode.LIVE_ONLY).build());
    private final Setting<Double> maximumDistance = sgRender.add(new DoubleSetting.Builder().name("maximum-distance").description("Maximum rendered distance.").defaultValue(256).min(1).sliderMax(512).build());
    private final Setting<Boolean> renderBox = sgRender.add(new BoolSetting.Builder().name("render-box").description("Draws a box at the exact position.").defaultValue(true).build());
    private final Setting<Boolean> renderName = sgRender.add(new BoolSetting.Builder().name("render-name").description("Shows the known player name.").defaultValue(true).build());
    private final Setting<Boolean> renderDistance = sgRender.add(new BoolSetting.Builder().name("render-distance").description("Shows distance in the position label.").defaultValue(true).build());
    private final Setting<Boolean> renderConfidence = sgRender.add(new BoolSetting.Builder().name("render-confidence").description("Shows confidence in the position label.").defaultValue(true).build());
    private final Setting<SettingColor> renderColor = sgRender.add(new ColorSetting.Builder().name("color").description("Tracer and box color.").defaultValue(new SettingColor(255, 80, 80, 180)).build());

    private final Setting<PanicMode> panicOnDetect = sgActions.add(new EnumSetting.Builder<PanicMode>().name("panic-on-detect").description("Disables modules when detection reaches the threshold.").defaultValue(PanicMode.OFF).build());
    private final Setting<Integer> panicConfidence = sgActions.add(new IntSetting.Builder().name("panic-confidence").description("Confidence required for panic.").defaultValue(85).range(0, 100).sliderRange(0, 100).visible(() -> panicOnDetect.get() != PanicMode.OFF).build());
    private final Setting<List<Module>> selectedModules = sgActions.add(new ModuleListSetting.Builder().name("selected-modules").description("Modules disabled by selected panic mode.").visible(() -> panicOnDetect.get() == PanicMode.DISABLE_SELECTED_MODULES).build());
    private final Setting<Boolean> keepDetectorActive = sgActions.add(new BoolSetting.Builder().name("keep-detector-active").description("Keeps SpectatorDetector active during panic.").defaultValue(true).visible(() -> panicOnDetect.get() != PanicMode.OFF).build());
    private final Setting<Boolean> logoffOnDetect = sgActions.add(new BoolSetting.Builder().name("logoff-on-detect").description("Disconnects cleanly when detection reaches the threshold.").defaultValue(false).build());
    private final Setting<Integer> logoffConfidence = sgActions.add(new IntSetting.Builder().name("logoff-confidence").description("Confidence required to disconnect.").defaultValue(90).range(0, 100).sliderRange(0, 100).visible(logoffOnDetect::get).build());
    private final Setting<Boolean> debug = sgDebug.add(new BoolSetting.Builder().name("debug").description("Logs detector decisions without chat spam.").defaultValue(false).build());

    private final DetectionEngine engine = new DetectionEngine();
    private final DetectionActionState actionState = new DetectionActionState();
    private final WaypointTracker waypointTracker = new WaypointTracker();
    private final PlayerTracker playerTracker = new PlayerTracker();
    private final Map<UUID, Long> playerInfoSeen = new HashMap<>();
    private final Map<UUID, Long> lastWarnings = new HashMap<>();
    private long graceUntil;
    private long lastAnomalyScan;

    public SpectatorDetector() {
        super(SybuDebugAddon.CATEGORY, "spectator-detector", "Detects nearby spectator players from client-visible evidence.");
    }

    @Override
    public void onActivate() { reset(); }

    @Override
    public void onDeactivate() { reset(); }

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) { reset(); }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) { reset(); }

    @EventHandler
    private void onPacket(PacketEvent.Receive event) {
        Packet<?> packet = event.packet;
        if (mc.isOnThread()) handle(packet);
        else mc.execute(() -> handle(packet));
    }

    private void handle(Packet<?> packet) {
        long now = System.currentTimeMillis();
        switch (packet) {
            case PlayerListS2CPacket update -> handlePlayerList(update, now);
            case PlayerRemoveS2CPacket remove -> remove.profileIds().forEach(this::removePlayer);
            case PlayerRespawnS2CPacket ignored -> reset();
            case EntitySpawnS2CPacket spawn when spawn.getEntityType() == EntityType.PLAYER -> handlePlayerSpawn(spawn, now);
            case EntitiesDestroyS2CPacket destroy -> destroy.getEntityIds().forEach(id -> handleEntityRemove(id, now));
            case WaypointS2CPacket waypoint -> handleWaypoint(waypoint, now);
            default -> { }
        }
        engine.tick(now);
        evaluate(now);
    }

    private void handlePlayerList(PlayerListS2CPacket packet, long now) {
        if (!packet.getActions().contains(PlayerListS2CPacket.Action.ADD_PLAYER)
            && !packet.getActions().contains(PlayerListS2CPacket.Action.UPDATE_GAME_MODE)) return;

        for (PlayerListS2CPacket.Entry entry : packet.getEntries()) {
            if (entry.profileId().equals(mc.getSession().getUuidOrNull())) continue;
            playerInfoSeen.putIfAbsent(entry.profileId(), now);
            if (entry.gameMode() != GameMode.SPECTATOR) {
                clearDetection(entry.profileId());
                continue;
            }

            DetectionCandidate candidate = engine.signal(entry.profileId(), DetectionSignal.EXPLICIT_SPECTATOR, now, null, "player-info gamemode spectator");
            engine.signal(entry.profileId(), DetectionSignal.SPECTATOR_WITH_UUID, now, null, "player-info uuid");
            candidate.gameMode(entry.gameMode().name());
            if (entry.profile() != null) candidate.username(entry.profile().name());
            applyTrackedPlayer(candidate, now);
            if (debug.get()) SybuDebugAddon.LOG.info("Spectator player-info: {} ({})", candidate.username(), candidate.uuid());
        }
    }

    private void handlePlayerSpawn(EntitySpawnS2CPacket packet, long now) {
        playerTracker.spawn(packet.getEntityId(), packet.getUuid(), packet.getX(), packet.getY(), packet.getZ(), dimension(), now);
        DetectionCandidate candidate = engine.get(packet.getUuid());
        if (candidate == null) return;
        candidate.entityId(packet.getEntityId());
        candidate.position(packet.getX(), packet.getY(), packet.getZ(), dimension(), now);
        engine.signal(packet.getUuid(), DetectionSignal.LIVE_POSITION, now, packet.getEntityId(), "player spawn position");
    }

    private void handleEntityRemove(int entityId, long now) {
        PlayerTracker.Entry tracked = playerTracker.remove(entityId, now);
        if (tracked == null || engine.get(tracked.uuid()) == null) return;
        DetectionCandidate candidate = engine.get(tracked.uuid());
        candidate.position(tracked.x(), tracked.y(), tracked.z(), tracked.dimension(), tracked.updatedAt());
        candidate.markPositionStale();
        engine.signal(tracked.uuid(), DetectionSignal.SPECTATOR_ENTITY_REMOVED, now, entityId, "spectator entity removed");
        engine.signal(tracked.uuid(), DetectionSignal.RECENT_ENTITY_REMOVAL, now, entityId, "recent player entity removal");
    }

    private void removePlayer(UUID uuid) {
        clearDetection(uuid);
        playerTracker.remove(uuid);
        playerInfoSeen.remove(uuid);
    }

    private void clearDetection(UUID uuid) {
        engine.remove(uuid);
        lastWarnings.remove(uuid);
        actionState.remove(uuid);
    }

    private void handleWaypoint(WaypointS2CPacket packet, long now) {
        WaypointTracker.Entry waypoint = waypointTracker.accept(packet, dimension(), now);
        if (waypoint == null) return;
        DetectionCandidate candidate = engine.get(waypoint.uuid());
        if (candidate == null) return;
        engine.signal(waypoint.uuid(), DetectionSignal.WAYPOINT_CORRELATION, now, candidate.entityId(), "locator " + waypoint.operation());
        if (waypoint.position() != null) {
            candidate.position(waypoint.position().getX(), waypoint.position().getY(), waypoint.position().getZ(), waypoint.dimension(), now);
        }
        if (debug.get()) SybuDebugAddon.LOG.info("Locator {} for {} type={} position={} chunk={} azimuth={}", waypoint.operation(), waypoint.uuid(), packet.waypoint().getClass().getSimpleName(), waypoint.position(), waypoint.chunk(), waypoint.azimuth());
    }

    private String dimension() {
        return mc.world == null ? null : mc.world.getRegistryKey().getValue().toString();
    }

    private void reset() {
        engine.clear();
        waypointTracker.clear();
        playerTracker.clear();
        playerInfoSeen.clear();
        lastWarnings.clear();
        actionState.clear();
        lastAnomalyScan = 0;
        graceUntil = System.currentTimeMillis() + JOIN_GRACE_MS;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        long now = System.currentTimeMillis();
        refreshLivePositions(now);
        scanPlayerInfoAnomalies(now);
        playerTracker.prune(now, LAST_KNOWN_TIMEOUT_MS);
        engine.tick(now);
        evaluate(now);
    }

    private void evaluate(long now) {
        if (now < graceUntil || mc.player == null || mc.world == null) return;
        for (DetectionCandidate candidate : List.copyOf(engine.candidates())) {
            if (ignored(candidate)) continue;
            int confidence = ConfidenceCalculator.calculate(candidate, now);
            if (!withinDetectionRange(candidate, now)) continue;
            if (confidence >= warningConfidence.get() && chatWarning.get() && now - lastWarnings.getOrDefault(candidate.uuid(), 0L) >= warningCooldown.get() * 1000) {
                lastWarnings.put(candidate.uuid(), now);
                warn(candidate, confidence, now);
            }
            if (actionState.shouldPanic(candidate.uuid(), confidence, panicOnDetect.get() != PanicMode.OFF, panicConfidence.get())) panic(candidate, confidence);
            if (actionState.shouldLogoff(candidate.uuid(), confidence, logoffOnDetect.get(), logoffConfidence.get())) logoff(candidate, confidence);
        }
    }

    private boolean ignored(DetectionCandidate candidate) {
        if (candidate.uuid().equals(mc.player.getUuid())) return true;
        if (!ignoreFriends.get() || mc.getNetworkHandler() == null) return false;
        var entry = mc.getNetworkHandler().getPlayerListEntry(candidate.uuid());
        return entry != null && Friends.get().isFriend(entry);
    }

    private void warn(DetectionCandidate candidate, int confidence, long now) {
        if (positionKnown(candidate, now)) {
            int distance = (int) Math.round(mc.player.getEntityPos().distanceTo(new Vec3d(candidate.x(), candidate.y(), candidate.z())));
            info("Spectator detected: %s (%dm, %d%%)%s", name(candidate), distance, confidence, candidate.livePosition() ? "" : " [last known]");
        } else info("Possible hidden spectator nearby (%d%%); position unknown", confidence);
    }

    private void panic(DetectionCandidate candidate, int confidence) {
        if (debug.get()) SybuDebugAddon.LOG.info("Panic action: {} confidence {}", candidate.uuid(), confidence);
        List<Module> targets = panicOnDetect.get() == PanicMode.DISABLE_ALL_MODULES
            ? new ArrayList<>(Modules.get().getActive()) : new ArrayList<>(selectedModules.get());
        for (Module module : targets) if (module.isActive() && (!keepDetectorActive.get() || module != this)) module.disable();
    }

    private void logoff(DetectionCandidate candidate, int confidence) {
        if (debug.get()) SybuDebugAddon.LOG.info("Logoff action: {} confidence {}", candidate.uuid(), confidence);
        if (mc.player != null) mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(Text.literal("SpectatorDetector: spectator detected")));
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.player == null || mc.world == null || mc.options.hudHidden) return;
        long now = System.currentTimeMillis();
        for (DetectionCandidate candidate : engine.candidates()) {
            int confidence = ConfidenceCalculator.calculate(candidate, now);
            if (confidence < minimumConfidence.get() || ignored(candidate) || !positionUsable(candidate, now)) continue;
            Vec3d target = new Vec3d(candidate.x(), candidate.y(), candidate.z());
            double distance = mc.player.getEntityPos().distanceTo(target);
            if (distance > maximumDistance.get()) continue;
            Color color = renderColor.get();
            if (renderTracer.get()) event.renderer.line(RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z, target.x, target.y + 0.9, target.z, color);
            if (renderBox.get()) event.renderer.box(target.x - 0.3, target.y, target.z - 0.3, target.x + 0.3, target.y + 1.8, target.z + 0.3, color, color, ShapeMode.Both, 0);
            if (renderName.get() || renderDistance.get() || renderConfidence.get()) renderLabel(candidate, confidence, distance);
        }
    }

    private boolean positionUsable(DetectionCandidate candidate, long now) {
        if (!positionKnown(candidate, now)) return false;
        return candidate.livePosition() || tracerMode.get() == TracerMode.LIVE_AND_LAST_KNOWN;
    }

    private boolean positionKnown(DetectionCandidate candidate, long now) {
        if (candidate.dimension() == null || !candidate.dimension().equals(dimension())) return false;
        return candidate.lastExactPosition() > 0 && now - candidate.lastExactPosition() <= LAST_KNOWN_TIMEOUT_MS;
    }

    private void refreshLivePositions(long now) {
        if (mc.world == null) return;
        for (PlayerTracker.Entry tracked : playerTracker.liveEntries()) {
            Entity entity = mc.world.getEntityById(tracked.entityId());
            if (entity == null) continue;
            Vec3d pos = entity.getEntityPos();
            tracked.update(pos.x, pos.y, pos.z, dimension(), now);
            DetectionCandidate candidate = engine.get(tracked.uuid());
            if (candidate == null) continue;
            candidate.position(pos.x, pos.y, pos.z, dimension(), now);
            engine.signal(candidate.uuid(), DetectionSignal.LIVE_POSITION, now, tracked.entityId(), "tracked player entity position");
        }
    }

    private void applyTrackedPlayer(DetectionCandidate candidate, long now) {
        PlayerTracker.Entry tracked = playerTracker.get(candidate.uuid());
        if (tracked == null) return;
        candidate.entityId(tracked.entityId());
        if (tracked.removedAt() == 0) {
            candidate.position(tracked.x(), tracked.y(), tracked.z(), tracked.dimension(), tracked.updatedAt());
            engine.signal(candidate.uuid(), DetectionSignal.LIVE_POSITION, now, tracked.entityId(), "tracked player entity position");
        } else if (now - tracked.removedAt() <= CORRELATION_WINDOW_MS) {
            candidate.position(tracked.x(), tracked.y(), tracked.z(), tracked.dimension(), tracked.updatedAt());
            candidate.markPositionStale();
            engine.signal(candidate.uuid(), DetectionSignal.SPECTATOR_ENTITY_REMOVED, now, tracked.entityId(), "spectator after recent entity removal");
            engine.signal(candidate.uuid(), DetectionSignal.RECENT_ENTITY_REMOVAL, now, tracked.entityId(), "recent player entity removal");
        }
    }

    private void scanPlayerInfoAnomalies(long now) {
        if (mc.world == null || mc.getNetworkHandler() == null || now - lastAnomalyScan < 1_000) return;
        lastAnomalyScan = now;
        for (Map.Entry<UUID, Long> entry : playerInfoSeen.entrySet()) {
            if (now - entry.getValue() < JOIN_GRACE_MS) continue;
            var playerInfo = mc.getNetworkHandler().getPlayerListEntry(entry.getKey());
            if (playerInfo == null) continue;
            if (playerInfo.getGameMode() == GameMode.SPECTATOR) {
                DetectionCandidate spectator = engine.signal(entry.getKey(), DetectionSignal.EXPLICIT_SPECTATOR, now, null, "current player-info gamemode spectator");
                engine.signal(entry.getKey(), DetectionSignal.SPECTATOR_WITH_UUID, now, null, "current player-info uuid");
                spectator.username(playerInfo.getProfile().name());
                spectator.gameMode(playerInfo.getGameMode().name());
            }
            if (mc.world.getPlayerByUuid(entry.getKey()) != null) continue;
            DetectionCandidate candidate = engine.signal(entry.getKey(), DetectionSignal.PLAYER_INFO_WITHOUT_ENTITY, now, null, "player-info exists without entity");
            candidate.username(playerInfo.getProfile().name());
            candidate.gameMode(playerInfo.getGameMode().name());
        }
    }

    private boolean withinDetectionRange(DetectionCandidate candidate, long now) {
        return !positionKnown(candidate, now) || mc.player.getEntityPos().squaredDistanceTo(new Vec3d(candidate.x(), candidate.y(), candidate.z())) <= exactPositionRange.get() * exactPositionRange.get();
    }

    private void renderLabel(DetectionCandidate candidate, int confidence, double distance) {
        StringBuilder label = new StringBuilder();
        if (renderName.get()) label.append(name(candidate));
        if (renderDistance.get()) append(label, Math.round(distance) + "m");
        if (renderConfidence.get()) append(label, "Confidence " + confidence + "%");
        if (!candidate.livePosition()) append(label, "Last Known");
        Vector3d pos = new Vector3d(candidate.x(), candidate.y() + 2.1, candidate.z());
        if (!NametagUtils.to2D(pos, 1)) return;
        TextRenderer text = TextRenderer.get();
        NametagUtils.begin(pos);
        text.beginBig();
        double width = text.getWidth(label.toString());
        Renderer2D.COLOR.begin();
        Renderer2D.COLOR.quad(-width / 2 - 2, -text.getHeight() - 2, width + 4, text.getHeight() + 4, new Color(0, 0, 0, 120));
        Renderer2D.COLOR.render();
        text.render(label.toString(), -width / 2, -text.getHeight(), Color.WHITE);
        text.end();
        NametagUtils.end();
    }

    private static void append(StringBuilder text, String value) {
        if (!text.isEmpty()) text.append(" | ");
        text.append(value);
    }

    private static String name(DetectionCandidate candidate) {
        return candidate.username() == null ? candidate.uuid().toString() : candidate.username();
    }

    public enum TracerMode { LIVE_ONLY, LIVE_AND_LAST_KNOWN }
    public enum PanicMode { OFF, DISABLE_ALL_MODULES, DISABLE_SELECTED_MODULES }
}
