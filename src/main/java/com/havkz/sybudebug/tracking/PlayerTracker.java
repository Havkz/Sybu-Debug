package com.havkz.sybudebug.tracking;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerTracker {
    private final Map<Integer, Entry> byEntityId = new HashMap<>();
    private final Map<UUID, Entry> byUuid = new HashMap<>();

    public Entry spawn(int entityId, UUID uuid, double x, double y, double z, String dimension, long now) {
        remove(uuid);
        Entry displaced = byEntityId.remove(entityId);
        if (displaced != null) byUuid.remove(displaced.uuid);
        Entry entry = new Entry(entityId, uuid, x, y, z, dimension, now);
        byEntityId.put(entityId, entry);
        byUuid.put(uuid, entry);
        return entry;
    }

    public Entry remove(int entityId, long now) {
        Entry entry = byEntityId.remove(entityId);
        if (entry != null) entry.removedAt = now;
        return entry;
    }

    public void remove(UUID uuid) {
        Entry entry = byUuid.remove(uuid);
        if (entry != null) byEntityId.remove(entry.entityId, entry);
    }

    public Entry get(UUID uuid) { return byUuid.get(uuid); }
    public Collection<Entry> liveEntries() { return byEntityId.values(); }

    public void prune(long now, long timeoutMs) {
        byUuid.values().removeIf(entry -> entry.removedAt > 0 && now - entry.removedAt > timeoutMs);
    }

    public void clear() {
        byEntityId.clear();
        byUuid.clear();
    }

    public static final class Entry {
        private final int entityId;
        private final UUID uuid;
        private double x, y, z;
        private String dimension;
        private long updatedAt;
        private long removedAt;

        private Entry(int entityId, UUID uuid, double x, double y, double z, String dimension, long now) {
            this.entityId = entityId;
            this.uuid = uuid;
            update(x, y, z, dimension, now);
        }

        public void update(double x, double y, double z, String dimension, long now) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.dimension = dimension;
            this.updatedAt = now;
            this.removedAt = 0;
        }

        public int entityId() { return entityId; }
        public UUID uuid() { return uuid; }
        public double x() { return x; }
        public double y() { return y; }
        public double z() { return z; }
        public String dimension() { return dimension; }
        public long updatedAt() { return updatedAt; }
        public long removedAt() { return removedAt; }
    }
}
