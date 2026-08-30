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
}
