package dev.zeth.zethac.checks.combat;

import dev.zeth.zethac.ZethAC;
import dev.zeth.zethac.data.PlayerData;
import dev.zeth.zethac.manager.Check;

public class CriticalsCheck extends Check {
    public CriticalsCheck(ZethAC plugin) { super(plugin, "criticals", CheckCategory.COMBAT); }

    @Override
    public void onTick(PlayerData data) {
        if (data.getAttacksThisTick() == 0) { decay(data, 1); return; }
        // Ground crit: player on ground but dealt critical
        if (data.isOnGround() && data.getFallDistance() < 0.01) {
            float fd = data.getPlayer().getFallDistance();
            if (fd < 0.1f) {
                flag(data, String.format("Ground crit [fd=%.2f]", fd)); return;
            }
        }
        decay(data, 2);
    }
}
