package com.havkz.sybudebug.detection;

import java.util.UUID;

public final class CoreSelfTest {
    public static void main(String[] args) {
        DetectionEngine engine = new DetectionEngine();
        UUID id = UUID.randomUUID();
        DetectionCandidate candidate = engine.signal(id, DetectionSignal.CHUNK_ACTIVITY, 1_000, null, "chunk");
        check(ConfidenceCalculator.calculate(candidate, 1_000) < 30, "weak signal must stay low");
        engine.signal(id, DetectionSignal.EXPLICIT_SPECTATOR, 1_001, 7, "spectator");
        engine.signal(id, DetectionSignal.SPECTATOR_WITH_UUID, 1_001, 7, "uuid");
        engine.signal(id, DetectionSignal.SPECTATOR_WITH_UUID, 1_002, 7, "uuid refresh");
        check(candidate.evidence().stream().filter(e -> e.signal() == DetectionSignal.SPECTATOR_WITH_UUID).count() == 1, "candidate keeps only latest evidence per signal");
        check(ConfidenceCalculator.calculate(candidate, 1_002) >= 90, "explicit spectator must be high confidence");
        candidate.position(1, 2, 3, "minecraft:overworld", 1_001);
        candidate.expireLivePosition(3_002, 2_000);
        check(!candidate.livePosition(), "live position must become last known");
        engine.tick(70_000);
        check(ConfidenceCalculator.calculate(candidate, 70_000) == 0, "evidence must decay");
        engine.clear();
        check(engine.candidates().isEmpty() && engine.history().snapshot().isEmpty(), "server reset must clear state");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
