package dev.zitrone.pluginhider;

import org.bukkit.plugin.java.JavaPlugin;

public final class PluginHiderPlugin extends JavaPlugin {
    private PluginHiderSettings settings;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.settings = PluginHiderSettings.fromConfig(getConfig());
        getServer().getPluginManager().registerEvents(new PluginHiderListener(this.settings), this);
    }
}
