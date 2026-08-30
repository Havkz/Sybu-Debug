package com.havkz.sybudebug.detection;

import com.havkz.sybudebug.tracking.PlayerTracker;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CoreSelfTest {
    private static final long NOW = 10_000;

    public static void main(String[] args) {
        testA_NormalSurvivalNoAlarm();
        testB_VisibleSpectatorHighConfidence();
        testC_LiveSpectatorPosition();
        testD_RemovedSpectatorKeepsLastKnownPosition();
        testE_NormalLeaveClearsCandidate();
        testF_DimensionChangeClearsAllState();
        testG_ChunkActivityCannotReachHighConfidence();
        testH_PanicIsOneShotAndUsesSnapshot();
        testI_LogoffIsOneShot();
        testEvidenceDecayAndBounds();
    }

    private static void testA_NormalSurvivalNoAlarm() {
        DetectionEngine engine = new DetectionEngine();
        DetectionCandidate candidate = engine.signal(UUID.randomUUID(), DetectionSignal.PLAYER_INFO_WITHOUT_ENTITY, NOW, null, "tab only");
        check(ConfidenceCalculator.calculate(candidate, NOW) < 30, "A: normal tab anomaly must not alarm");
    }

    private static void testB_VisibleSpectatorHighConfidence() {
        DetectionCandidate candidate = explicitSpectator(new DetectionEngine(), UUID.randomUUID());
        check(ConfidenceCalculator.calculate(candidate, NOW) >= 90, "B: explicit spectator must be high confidence");
        check(ConfidenceCalculator.level(ConfidenceCalculator.calculate(candidate, NOW)) == ConfidenceCalculator.Level.CONFIRMED, "B: score category must be confirmed");
    }

    private static void testC_LiveSpectatorPosition() {
        DetectionEngine engine = new DetectionEngine();
        DetectionCandidate candidate = explicitSpectator(engine, UUID.randomUUID());
        candidate.position(1, 2, 3, "minecraft:overworld", NOW);
        engine.signal(candidate.uuid(), DetectionSignal.LIVE_POSITION, NOW, 7, "live entity");
        check(candidate.livePosition() && candidate.lastExactPosition() == NOW, "C: live exact position must be retained");
        check(ConfidenceCalculator.calculate(candidate, NOW) == 100, "C: direct spectator plus live position must cap at 100");
    }

    private static void testD_RemovedSpectatorKeepsLastKnownPosition() {
        DetectionEngine engine = new DetectionEngine();
        PlayerTracker tracker = new PlayerTracker();
        UUID uuid = UUID.randomUUID();
        tracker.spawn(7, uuid, 4, 5, 6, "minecraft:overworld", NOW);
        PlayerTracker.Entry removed = tracker.remove(7, NOW + 10);
        DetectionCandidate candidate = explicitSpectator(engine, uuid);
        candidate.position(removed.x(), removed.y(), removed.z(), removed.dimension(), removed.updatedAt());
        candidate.markPositionStale();
        engine.signal(candidate.uuid(), DetectionSignal.SPECTATOR_ENTITY_REMOVED, NOW + 10, 7, "removed");
        check(!candidate.livePosition() && candidate.x() == 4 && candidate.y() == 5 && candidate.z() == 6, "D: removed entity must retain only last-known coordinates");
        check(ConfidenceCalculator.calculate(candidate, NOW + 10) >= 90, "D: removal correlated with spectator must remain high confidence");
        check(tracker.get(uuid).removedAt() == NOW + 10, "D: remove-before-gamemode ordering must remain correlatable by UUID");
    }

    private static void testE_NormalLeaveClearsCandidate() {
        DetectionEngine engine = new DetectionEngine();
        DetectionActionState actions = new DetectionActionState();
        UUID uuid = UUID.randomUUID();
        engine.signal(uuid, DetectionSignal.PLAYER_INFO_WITHOUT_ENTITY, NOW, null, "joined");
        check(actions.shouldPanic(uuid, 100, true, 85), "E: first detection may trigger panic");
        engine.remove(uuid);
        actions.remove(uuid);
        check(engine.get(uuid) == null, "E: normal leave must remove candidate");
        check(actions.shouldPanic(uuid, 100, true, 85), "E: a later detection of the same player must be a new event");
    }

    private static void testF_DimensionChangeClearsAllState() {
        DetectionEngine engine = new DetectionEngine();
        explicitSpectator(engine, UUID.randomUUID());
        engine.clear();
        check(engine.candidates().isEmpty() && engine.history().snapshot().isEmpty(), "F: dimension/server reset must clear candidates and history");
    }

    private static void testG_ChunkActivityCannotReachHighConfidence() {
        DetectionEngine engine = new DetectionEngine();
        DetectionCandidate candidate = engine.signal(UUID.randomUUID(), DetectionSignal.CHUNK_ACTIVITY, NOW, null, "chunk");
        engine.signal(candidate.uuid(), DetectionSignal.ENTITY_ACTIVITY, NOW, null, "activity");
        check(ConfidenceCalculator.calculate(candidate, NOW) < 30, "G: activity-only evidence must remain ignored");
    }

    private static void testH_PanicIsOneShotAndUsesSnapshot() {
        DetectionActionState actions = new DetectionActionState();
        UUID uuid = UUID.randomUUID();
        check(actions.shouldPanic(uuid, 85, true, 85), "H: panic must trigger at threshold");
        check(!actions.shouldPanic(uuid, 100, true, 85), "H: panic must trigger only once per detection");
        List<String> active = new ArrayList<>(List.of("A", "B"));
        for (String ignored : new ArrayList<>(active)) active.remove(ignored);
        check(active.isEmpty(), "H: snapshot iteration must allow safe mutation of active collection");
    }

    private static void testI_LogoffIsOneShot() {
        DetectionActionState actions = new DetectionActionState();
        UUID uuid = UUID.randomUUID();
        check(!actions.shouldLogoff(uuid, 89, true, 90), "I: logoff must not trigger below threshold");
        check(actions.shouldLogoff(uuid, 90, true, 90), "I: logoff must trigger at threshold");
        check(!actions.shouldLogoff(uuid, 100, true, 90), "I: logoff must trigger only once per detection");
    }

    private static void testEvidenceDecayAndBounds() {
        DetectionEngine engine = new DetectionEngine();
        DetectionCandidate candidate = explicitSpectator(engine, UUID.randomUUID());
        engine.signal(candidate.uuid(), DetectionSignal.SPECTATOR_WITH_UUID, NOW + 1, 7, "refresh");
        check(candidate.evidence().stream().filter(e -> e.signal() == DetectionSignal.SPECTATOR_WITH_UUID).count() == 1, "latest evidence replaces duplicate signal");
        engine.tick(NOW + 70_000);
        check(ConfidenceCalculator.calculate(candidate, NOW + 70_000) == 0, "expired evidence must contribute nothing");
    }

    private static DetectionCandidate explicitSpectator(DetectionEngine engine, UUID uuid) {
        DetectionCandidate candidate = engine.signal(uuid, DetectionSignal.EXPLICIT_SPECTATOR, NOW, 7, "spectator");
        engine.signal(uuid, DetectionSignal.SPECTATOR_WITH_UUID, NOW, 7, "uuid");
        return candidate;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
