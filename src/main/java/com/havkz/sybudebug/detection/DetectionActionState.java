package com.havkz.sybudebug.detection;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class DetectionActionState {
    private final Set<UUID> panic = new HashSet<>();
    private final Set<UUID> logoff = new HashSet<>();

    public boolean shouldPanic(UUID uuid, int confidence, boolean enabled, int threshold) {
        return enabled && confidence >= threshold && panic.add(uuid);
    }

    public boolean shouldLogoff(UUID uuid, int confidence, boolean enabled, int threshold) {
        return enabled && confidence >= threshold && logoff.add(uuid);
    }

    public void remove(UUID uuid) {
        panic.remove(uuid);
        logoff.remove(uuid);
    }

    public void clear() {
        panic.clear();
        logoff.clear();
    }
}
