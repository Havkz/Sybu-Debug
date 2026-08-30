package com.havkz.sybudebug.detection;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class DetectionCandidate {
    private final UUID uuid;
    private String username;
    private Integer entityId;
    private String dimension;
    private String gameMode;
    private long firstSeen;
    private long lastSeen;
    private long lastExactPosition;
    private boolean livePosition;
    private double x, y, z;
    private final List<PacketEvidence> evidence = new ArrayList<>();

    DetectionCandidate(UUID uuid, long now) {
        this.uuid = uuid;
        this.firstSeen = this.lastSeen = now;
    }

    public void add(PacketEvidence item) {
        evidence.removeIf(existing -> existing.signal() == item.signal());
        evidence.add(item);
        lastSeen = Math.max(lastSeen, item.timestamp());
    }
    public void prune(long now) { evidence.removeIf(item -> !item.activeAt(now)); }
    public List<PacketEvidence> evidence() { return List.copyOf(evidence); }
    public UUID uuid() { return uuid; }
    public String username() { return username; }
    public void username(String value) { username = value; }
    public Integer entityId() { return entityId; }
    public void entityId(Integer value) { entityId = value; }
    public String dimension() { return dimension; }
    public String gameMode() { return gameMode; }
    public void gameMode(String value) { gameMode = value; }
    public long firstSeen() { return firstSeen; }
    public long lastSeen() { return lastSeen; }
    public long lastExactPosition() { return lastExactPosition; }
    public boolean livePosition() { return livePosition; }
    public void markPositionStale() { livePosition = false; }
    public void expireLivePosition(long now, long timeoutMs) {
        if (now - lastExactPosition > timeoutMs) livePosition = false;
    }
    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }

    public void position(double x, double y, double z, String dimension, long now) {
        this.x = x; this.y = y; this.z = z;
        this.dimension = dimension;
        this.lastExactPosition = this.lastSeen = now;
        this.livePosition = true;
    }
}
