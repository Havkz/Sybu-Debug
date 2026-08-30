package com.havkz.sybudebug.mixin;

import net.minecraft.util.math.Vec3i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.world.waypoint.TrackedWaypoint$Positional")
public interface PositionalTrackedWaypointAccessor {
    @Accessor("pos") Vec3i sybuDebug$getPos();
}
