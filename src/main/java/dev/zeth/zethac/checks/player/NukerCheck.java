package dev.zeth.zethac.checks.player;

import dev.zeth.zethac.ZethAC;
import dev.zeth.zethac.data.PlayerData;
import dev.zeth.zethac.manager.Check;
import org.bukkit.GameMode;

public class NukerCheck extends Check {
    public NukerCheck(ZethAC plugin) { super(plugin, "nuker", CheckCategory.PLAYER); }

    @Override
    public void onTick(PlayerData data) {
        if (data.getPlayer().getGameMode() == GameMode.CREATIVE) { decay(data, 2); return; }
        int maxBlocks = plugin.getConfig().getInt("checks.nuker.max-blocks-per-tick", 1);
        int broken = data.getBlocksBrokenThisTick();
        if (broken > maxBlocks) {
            flag(data, "Nuker [" + broken + " blocks/tick]"); return;
        }
        decay(data, 2);
    }
}
