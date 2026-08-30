package com.havkz.sybudebug.tracking;

import com.havkz.sybudebug.mixin.AzimuthTrackedWaypointAccessor;
import com.havkz.sybudebug.mixin.ChunkTrackedWaypointAccessor;
import com.havkz.sybudebug.mixin.PositionalTrackedWaypointAccessor;
import net.minecraft.network.packet.s2c.play.WaypointS2CPacket;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class WaypointTracker {
    private final Map<UUID, Entry> entries = new HashMap<>();

    public Entry accept(WaypointS2CPacket packet, String dimension, long now) {
        UUID uuid = packet.waypoint().getSource().left().orElse(null);
        if (uuid == null) return null;
        String operation = String.valueOf(packet.operation());
        if (operation.equals("UNTRACK")) {
            entries.remove(uuid);
            return new Entry(uuid, operation, null, null, null, dimension, now);
        }

        Object waypoint = packet.waypoint();
        Vec3i position = waypoint instanceof PositionalTrackedWaypointAccessor accessor ? accessor.sybuDebug$getPos() : null;
        ChunkPos chunk = waypoint instanceof ChunkTrackedWaypointAccessor accessor ? accessor.sybuDebug$getChunkPos() : null;
        Float azimuth = waypoint instanceof AzimuthTrackedWaypointAccessor accessor ? accessor.sybuDebug$getAzimuth() : null;
        Entry entry = new Entry(uuid, operation, position, chunk, azimuth, dimension, now);
        entries.put(uuid, entry);
        return entry;
    }

    public void clear() { entries.clear(); }

    public record Entry(UUID uuid, String operation, Vec3i position, ChunkPos chunk, Float azimuth, String dimension, long timestamp) { }
}
