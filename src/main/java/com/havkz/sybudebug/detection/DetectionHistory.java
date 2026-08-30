package com.havkz.sybudebug.detection;

import java.util.ArrayDeque;
import java.util.List;

public final class DetectionHistory {
    private final int capacity;
    private final ArrayDeque<PacketEvidence> entries = new ArrayDeque<>();

    public DetectionHistory(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("capacity must be positive");
        this.capacity = capacity;
    }

    public void add(PacketEvidence evidence) {
        if (entries.size() == capacity) entries.removeFirst();
        entries.addLast(evidence);
    }

    public List<PacketEvidence> snapshot() { return List.copyOf(entries); }
    public void clear() { entries.clear(); }
}
