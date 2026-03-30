package dev.zeth.zethac.api;

import dev.zeth.zethac.ZethAC;
import dev.zeth.zethac.data.PlayerData;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ZethACAPI {
    private static ZethACAPI instance;
    private final ZethAC plugin;

    private ZethACAPI(ZethAC plugin) { this.plugin = plugin; }

    public static void init(ZethAC plugin) { instance = new ZethACAPI(plugin); }
    public static ZethACAPI get() {
        if (instance == null) throw new IllegalStateException("ZethAC is not loaded.");
        return instance;
    }

    public int        getViolations(Player p, String check)  { return get(p).getViolations(check); }
    public Map<String,Integer> getAllViolations(Player p)     { return get(p).getAllViolations(); }
    public List<PlayerData.ViolationEntry> getViolationLog(Player p) { return get(p).getViolationLog(); }
    public PlayerData getPlayerData(Player p)                { return get(p); }
    public double     getCPS(Player p)                       { return get(p).getCPS(); }
    public double     getLastReach(Player p)                 { return get(p).getLastReach(); }
    public String     getVersion()                           { return plugin.getDescription().getVersion(); }

    public void setCheckEnabled(String name, boolean enabled) {
        plugin.getCheckManager().getCheck(name).ifPresent(c -> c.setEnabled(enabled));
    }
    public boolean isCheckEnabled(String name) {
        return plugin.getCheckManager().getCheck(name).map(c -> c.isEnabled()).orElse(false);
    }
    public List<String> getCheckNames() {
        return plugin.getCheckManager().getChecks().stream().map(c -> c.getName()).collect(Collectors.toList());
    }

    private PlayerData get(Player p) { return plugin.getPlayerDataManager().get(p); }
}
