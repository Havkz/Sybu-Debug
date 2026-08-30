package com.havkz.sybudebug.mixin;

import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.world.waypoint.TrackedWaypoint$ChunkBased")
public interface ChunkTrackedWaypointAccessor {
    @Accessor("chunkPos") ChunkPos sybuDebug$getChunkPos();
}
