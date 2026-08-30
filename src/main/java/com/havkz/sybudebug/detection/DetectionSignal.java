package com.havkz.sybudebug.detection;

public enum DetectionSignal {
    EXPLICIT_SPECTATOR(80, 60_000),
    SPECTATOR_WITH_UUID(10, 60_000),
    LIVE_POSITION(10, 2_000),
    SPECTATOR_ENTITY_REMOVED(15, 15_000),
    WAYPOINT_CORRELATION(15, 10_000),
    PLAYER_INFO_WITHOUT_ENTITY(10, 5_000),
    RECENT_ENTITY_REMOVAL(5, 5_000),
    CHUNK_ACTIVITY(3, 2_000),
    ENTITY_ACTIVITY(3, 2_000);

    private final int weight;
    private final long lifetimeMs;

    DetectionSignal(int weight, long lifetimeMs) {
        this.weight = weight;
        this.lifetimeMs = lifetimeMs;
    }

    public int weight() { return weight; }
    public long lifetimeMs() { return lifetimeMs; }
}
