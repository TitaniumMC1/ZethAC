package dev.zeth.zethac.manager;

import dev.zeth.zethac.ZethAC;
import dev.zeth.zethac.api.ViolationEvent;
import dev.zeth.zethac.data.PlayerData;

public abstract class Check {

    public enum CheckCategory { COMBAT, MOVEMENT, PLAYER, WORLD }

    protected final ZethAC plugin;
    public final String name;
    public final CheckCategory category;

    private boolean enabled = true;
    private int vlThreshold = 10;
    private String punishment = "kick";

    public Check(ZethAC plugin, String name, CheckCategory category) {
        this.plugin   = plugin;
        this.name     = name;
        this.category = category;
        loadConfig();
    }

    public void loadConfig() {
        var cfg = plugin.getConfig().getConfigurationSection("checks." + name);
        if (cfg == null) return;
        enabled     = cfg.getBoolean("enabled", true);
        vlThreshold = cfg.getInt("vl-threshold", 10);
        punishment  = cfg.getString("punishment", "kick");
    }

    public abstract void onTick(PlayerData data);

    public int flag(PlayerData data, String detail) {
        if (!enabled) return 0;

        int vl = data.addViolation(name, detail);

        // Fire API event
        ViolationEvent event = new ViolationEvent(data.getPlayer(), data, this, detail, vl);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return vl;

        // Alert staff
        plugin.getAlertManager().sendAlert(data, this, detail, vl);

        // Log
        plugin.getLogManager().logViolation(data.getPlayer(), name, detail, vl);

        // Punish if threshold crossed
        if (vl >= vlThreshold) {
            plugin.getPunishmentManager().punish(data.getPlayer(), name, punishment, vl);
            data.decayViolation(name, vlThreshold / 2);
        }

        return vl;
    }

    public void decay(PlayerData data, int amount) {
        data.decayViolation(name, amount);
    }

    public String  getName()       { return name; }
    public CheckCategory getCategory() { return category; }
    public boolean isEnabled()     { return enabled; }
    public int     getVlThreshold(){ return vlThreshold; }
    public String  getPunishment() { return punishment; }
    public void    setEnabled(boolean v) { enabled = v; }
}
