package com.havkz.sybudebug.detection;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DetectionEngine {
    private static final long CANDIDATE_TIMEOUT_MS = 60_000;
    private final Map<UUID, DetectionCandidate> candidates = new HashMap<>();
    private final DetectionHistory history = new DetectionHistory(512);

    public DetectionCandidate signal(UUID uuid, DetectionSignal signal, long now, Integer entityId, String detail) {
        DetectionCandidate candidate = candidates.computeIfAbsent(uuid, key -> new DetectionCandidate(key, now));
        PacketEvidence evidence = new PacketEvidence(now, signal, uuid, entityId, detail);
        candidate.add(evidence);
        history.add(evidence);
        return candidate;
    }

    public DetectionCandidate get(UUID uuid) { return candidates.get(uuid); }
    public void remove(UUID uuid) { candidates.remove(uuid); }
    public Collection<DetectionCandidate> candidates() { return candidates.values(); }
    public DetectionHistory history() { return history; }

    public void tick(long now) {
        candidates.values().forEach(candidate -> {
            candidate.prune(now);
            candidate.expireLivePosition(now, 2_000);
        });
        candidates.values().removeIf(candidate -> candidate.evidence().isEmpty() && now - candidate.lastSeen() > CANDIDATE_TIMEOUT_MS);
    }

    public void clear() { candidates.clear(); history.clear(); }
}
