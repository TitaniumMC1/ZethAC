package dev.zeth.zethac.checks.player;

import dev.zeth.zethac.ZethAC;
import dev.zeth.zethac.data.PlayerData;
import dev.zeth.zethac.manager.Check;

public class ScaffoldCheck extends Check {
    public ScaffoldCheck(ZethAC plugin) { super(plugin, "scaffold", CheckCategory.PLAYER); }

    @Override
    public void onTick(PlayerData data) {
        if (data.getBlocksPlacedThisTick() == 0) { decay(data, 2); return; }

        float minPitch  = (float) plugin.getConfig().getDouble("checks.scaffold.min-pitch", -85.0);
        double rotThr   = plugin.getConfig().getDouble("checks.scaffold.rotation-threshold", 180.0);

        float pitch = data.getPitch();
        if (pitch < minPitch) {
            flag(data, String.format("Scaffold angle [pitch=%.1f]", pitch)); return;
        }

        float dYaw = data.getDeltaYaw();
        if (dYaw > rotThr && data.isSprinting() && data.getBlocksPlacedThisTick() > 0) {
            flag(data, String.format("Scaffold rotation [dy=%.4f°]", dYaw)); return;
        }
        decay(data, 2);
    }
}
