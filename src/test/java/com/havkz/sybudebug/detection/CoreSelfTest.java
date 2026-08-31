package com.havkz.sybudebug.detection;

import com.havkz.sybudebug.activity.ActivityHeatmap;
import com.havkz.sybudebug.activity.ActivityScanner;
import com.havkz.sybudebug.tracking.PlayerTracker;
import meteordevelopment.meteorclient.utils.render.color.Color;

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
        testActivityMathAndPortalFilter();
        testSurfaceSmoothing();
        testHighSurfaceOutliers();
        testSurfaceHoleIsActivity();
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

    private static void testActivityMathAndPortalFilter() {
        check(ActivityHeatmap.horizontalDistanceSquared(0, 0, 1, 1) == 2, "activity distance must use Euclidean X/Z geometry");
        check(ActivityHeatmap.normalize(24 * 24, 24, 160) == 0, "near activity must normalize to red");
        check(ActivityHeatmap.normalize(160 * 160, 24, 160) == 1, "far activity must normalize to green");
        Color mixed = ActivityHeatmap.color(0.5, new Color(200, 0, 20, 40), new Color(0, 100, 220, 160));
        check(mixed.r == 100 && mixed.g == 50 && mixed.b == 120 && mixed.a == 100, "custom endpoint colors and alpha must blend");
        check(ActivityScanner.ruinedPortalEvidence(4, 1, 0, 0), "netherrack plus magma must identify portal evidence");
        check(ActivityScanner.ruinedPortalEvidence(0, 0, 0, 1), "crying obsidian must identify portal evidence");
        check(!ActivityScanner.ruinedPortalEvidence(8, 0, 0, 0), "netherrack alone must not hide player obsidian");
    }

    private static void testSurfaceSmoothing() {
        int[] heights = {64, 64, 64, 64, 200, 64, 64, 64, 64};
        ActivityScanner.smoothSurface(heights, 3, 5);
        check(heights[4] == 64, "surface median must reject isolated sky structures");
        for (int z = 0; z < 3; z++) for (int x = 0; x < 3; x++) {
            int i = z * 3 + x;
            if (x > 0) check(Math.abs(heights[i] - heights[i - 1]) <= 5, "surface x slope must be bounded");
            if (z > 0) check(Math.abs(heights[i] - heights[i - 3]) <= 5, "surface z slope must be bounded");
        }
    }

    private static void testHighSurfaceOutliers() {
        int[] heights = new int[256];
        java.util.Arrays.fill(heights, 64);
        for (int z = 4; z < 10; z++) for (int x = 4; x < 10; x++) heights[z * 16 + x] = 120;
        ActivityScanner.filterHighOutliers(heights, 12);
        check(heights[8 * 16 + 8] == 64, "high building must not lift the terrain carpet");
    }

    private static void testSurfaceHoleIsActivity() {
        int height = 128;
        byte[] blocks = new byte[16 * height * 16];
        java.util.Arrays.fill(blocks, (byte) 1);
        int[] ground = new int[256];
        java.util.Arrays.fill(ground, 65);
        for (int y = 60; y <= 64; y++) blocks[(y * 16 + 8) * 16 + 8] = 0;
        byte[] border = new byte[height * 16];
        java.util.Arrays.fill(border, (byte) 1);
        ActivityScanner.Snapshot snapshot = new ActivityScanner.Snapshot(null, 0, height, blocks, ground, border, border, border, border);
        check(ActivityScanner.detectsHole(snapshot, 8, 64, 8), "surface hole must create red activity evidence");
        for (int y = 60; y <= 64; y++) blocks[(y * 16 + 8) * 16] = 0;
        check(ActivityScanner.detectsHole(snapshot, 0, 64, 8), "chunk-edge hole must use the neighbor halo");
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
