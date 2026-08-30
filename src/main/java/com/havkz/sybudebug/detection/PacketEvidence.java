package com.havkz.sybudebug.detection;

import java.util.UUID;

public record PacketEvidence(long timestamp, DetectionSignal signal, UUID uuid, Integer entityId, String detail) {
    public boolean activeAt(long now) {
        return now >= timestamp && now - timestamp <= signal.lifetimeMs();
    }
}
