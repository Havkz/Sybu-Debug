package com.havkz.sybudebug.modules;

import com.havkz.sybudebug.SybuDebugAddon;
import meteordevelopment.meteorclient.systems.modules.Module;

public final class SpectatorDetector extends Module {
    public SpectatorDetector() {
        super(SybuDebugAddon.CATEGORY, "spectator-detector", "Detects nearby spectator players from client-visible evidence.");
    }
}
