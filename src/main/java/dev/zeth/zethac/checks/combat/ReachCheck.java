package dev.zeth.zethac.checks.combat;

import dev.zeth.zethac.ZethAC;
import dev.zeth.zethac.data.PlayerData;
import dev.zeth.zethac.manager.Check;

public class ReachCheck extends Check {
    public ReachCheck(ZethAC plugin) { super(plugin, "reach", CheckCategory.COMBAT); }

    @Override
    public void onTick(PlayerData data) {
        double maxReach  = plugin.getConfig().getDouble("checks.reach.max-reach", 3.3);
        boolean compPing = plugin.getConfig().getBoolean("checks.reach.compensate-ping", true);
        int pingCompMs   = plugin.getConfig().getInt("checks.reach.ping-compensation-ms", 200);

        if (data.getAttacksThisTick() == 0) { decay(data, 1); return; }

        double reach = data.getLastReach();
        if (reach <= 0) { decay(data, 1); return; }

        // FIX: compensar por ping — inspirado en GrimAC Reach.threshold
        double effectiveMax = maxReach;
        if (compPing) {
            int ping = data.getPing();
            // GrimAC: expande hitbox ~0.03 bloques por cada 100ms de ping
            effectiveMax += Math.min(ping / 1000.0 * 0.1, 0.3);
        }

        if (reach > effectiveMax) {
            flag(data, String.format("Reach [%.2f > %.2f]", reach, effectiveMax));
        } else {
            decay(data, 2);
        }
    }
}
