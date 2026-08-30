package com.havkz.sybudebug.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.world.waypoint.TrackedWaypoint$Azimuth")
public interface AzimuthTrackedWaypointAccessor {
    @Accessor("azimuth") float sybuDebug$getAzimuth();
}
