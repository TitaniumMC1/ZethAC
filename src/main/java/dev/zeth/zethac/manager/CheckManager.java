package dev.zeth.zethac.manager;

import dev.zeth.zethac.ZethAC;
import dev.zeth.zethac.data.PlayerData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class CheckManager {
    private final ZethAC plugin;
    private final List<Check> checks = new ArrayList<>();

    public CheckManager(ZethAC plugin) { this.plugin = plugin; }

    public void register(Check check) { checks.add(check); }

    public void tickAll(PlayerData data) {
        for (Check c : checks) {
            if (c.isEnabled()) c.onTick(data);
        }
    }

    public List<Check> getChecks()   { return Collections.unmodifiableList(checks); }
    public int getCheckCount()       { return checks.size(); }

    public Optional<Check> getCheck(String name) {
        return checks.stream().filter(c -> c.getName().equalsIgnoreCase(name)).findFirst();
    }

    public boolean isCheckable(PlayerData data) {
        var p = data.getPlayer();
        return p.isOnline() && !p.hasPermission("zethac.bypass") || p.hasPermission("zethac.bypass.override");
    }
}
