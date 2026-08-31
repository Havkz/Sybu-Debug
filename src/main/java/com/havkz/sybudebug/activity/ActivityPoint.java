package com.havkz.sybudebug.activity;

import net.minecraft.util.math.BlockPos;

public record ActivityPoint(BlockPos position, ActivityType type) {
    public enum ActivityType { HOLE, OBSIDIAN }
}
