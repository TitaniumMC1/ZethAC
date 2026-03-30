package dev.zeth.zethac.checks.player;

import dev.zeth.zethac.ZethAC;
import dev.zeth.zethac.data.PlayerData;
import dev.zeth.zethac.manager.Check;

public class FastPlaceCheck extends Check {
    public FastPlaceCheck(ZethAC plugin) { super(plugin, "fastplace", CheckCategory.PLAYER); }

    @Override
    public void onTick(PlayerData data) {
        int minTicks = plugin.getConfig().getInt("checks.fastplace.min-place-delay", 3);
        long minMs   = minTicks * 50L;

        long last    = data.getLastPlaceTime();
        long now     = System.currentTimeMillis();

        if (last > 0 && data.getBlocksPlacedThisTick() > 0) {
            long elapsed = now - last;
            if (elapsed < minMs) {
                flag(data, String.format("FastPlace [delay=%dms < %dms]", elapsed, minMs)); return;
            }
        }
        decay(data, 2);
    }
}
