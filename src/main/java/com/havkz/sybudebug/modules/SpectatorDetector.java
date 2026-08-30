package com.havkz.sybudebug.modules;

import com.havkz.sybudebug.SybuDebugAddon;
import com.havkz.sybudebug.detection.DetectionCandidate;
import com.havkz.sybudebug.detection.DetectionEngine;
import com.havkz.sybudebug.detection.DetectionSignal;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.EntityType;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.world.GameMode;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SpectatorDetector extends Module {
    private final DetectionEngine engine = new DetectionEngine();
    private final Map<Integer, UUID> entityIds = new HashMap<>();

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
            case EntitySpawnS2CPacket spawn when spawn.getEntityType() == EntityType.PLAYER -> handlePlayerSpawn(spawn, now);
            case EntitiesDestroyS2CPacket destroy -> destroy.getEntityIds().forEach(id -> handleEntityRemove(id, now));
            default -> { }
        }
        engine.tick(now);
    }

    private void handlePlayerList(PlayerListS2CPacket packet, long now) {
        if (!packet.getActions().contains(PlayerListS2CPacket.Action.ADD_PLAYER)
            && !packet.getActions().contains(PlayerListS2CPacket.Action.UPDATE_GAME_MODE)) return;

        for (PlayerListS2CPacket.Entry entry : packet.getEntries()) {
            if (entry.profileId().equals(mc.getSession().getUuidOrNull())) continue;
            if (entry.gameMode() != GameMode.SPECTATOR) {
                engine.remove(entry.profileId());
                continue;
            }

            DetectionCandidate candidate = engine.signal(entry.profileId(), DetectionSignal.EXPLICIT_SPECTATOR, now, null, "player-info gamemode spectator");
            engine.signal(entry.profileId(), DetectionSignal.SPECTATOR_WITH_UUID, now, null, "player-info uuid");
            if (entry.profile() != null) candidate.username(entry.profile().name());
        }
    }

    private void handlePlayerSpawn(EntitySpawnS2CPacket packet, long now) {
        entityIds.put(packet.getEntityId(), packet.getUuid());
        DetectionCandidate candidate = engine.get(packet.getUuid());
        if (candidate == null) return;
        candidate.entityId(packet.getEntityId());
        candidate.position(packet.getX(), packet.getY(), packet.getZ(), dimension(), now);
        engine.signal(packet.getUuid(), DetectionSignal.LIVE_POSITION, now, packet.getEntityId(), "player spawn position");
    }

    private void handleEntityRemove(int entityId, long now) {
        UUID uuid = entityIds.remove(entityId);
        if (uuid == null || engine.get(uuid) == null) return;
        engine.signal(uuid, DetectionSignal.SPECTATOR_ENTITY_REMOVED, now, entityId, "spectator entity removed");
        engine.signal(uuid, DetectionSignal.RECENT_ENTITY_REMOVAL, now, entityId, "recent player entity removal");
    }

    private void removePlayer(UUID uuid) {
        engine.remove(uuid);
        entityIds.values().removeIf(uuid::equals);
    }

    private String dimension() {
        return mc.world == null ? null : mc.world.getRegistryKey().getValue().toString();
    }

    private void reset() {
        engine.clear();
        entityIds.clear();
    }
}
