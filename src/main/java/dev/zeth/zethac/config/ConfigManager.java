package dev.zeth.zethac.config;

import dev.zeth.zethac.ZethAC;

public class ConfigManager {
    private final ZethAC plugin;

    public ConfigManager(ZethAC plugin) { this.plugin = plugin; }

    public void reload() {
        plugin.reloadConfig();
        plugin.getCheckManager().getChecks().forEach(c -> c.loadConfig());
    }

    public String getPrefix() {
        return plugin.getConfig().getString("prefix", "&8[&bZeth&3AC&8] ").replace("&", "§");
    }

    public boolean isDebug() {
        return plugin.getConfig().getBoolean("debug", false);
    }
}
