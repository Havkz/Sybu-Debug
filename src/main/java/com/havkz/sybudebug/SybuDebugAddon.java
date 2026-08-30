package com.havkz.sybudebug;

import com.havkz.sybudebug.modules.SpectatorDetector;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public final class SybuDebugAddon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("Sybu Debug");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Sybu Debug by V0trex");
        Modules.get().add(new SpectatorDetector());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.havkz.sybudebug";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("Havkz", "Sybu-Debug");
    }
}
