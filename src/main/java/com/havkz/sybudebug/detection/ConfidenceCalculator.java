package com.havkz.sybudebug.detection;

import java.util.EnumSet;

public final class ConfidenceCalculator {
    private ConfidenceCalculator() {}

    public static int calculate(DetectionCandidate candidate, long now) {
        EnumSet<DetectionSignal> signals = EnumSet.noneOf(DetectionSignal.class);
        for (PacketEvidence evidence : candidate.evidence()) if (evidence.activeAt(now)) signals.add(evidence.signal());
        int score = signals.stream().mapToInt(DetectionSignal::weight).sum();
        return Math.min(score, 100);
    }

    public static Level level(int confidence) {
        if (confidence < 30) return Level.IGNORE;
        if (confidence < 50) return Level.LOW;
        if (confidence < 70) return Level.POSSIBLE;
        if (confidence < 90) return Level.LIKELY;
        return Level.CONFIRMED;
    }

    public enum Level { IGNORE, LOW, POSSIBLE, LIKELY, CONFIRMED }
}
