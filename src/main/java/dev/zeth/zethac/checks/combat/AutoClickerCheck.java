package dev.zeth.zethac.checks.combat;

import dev.zeth.zethac.ZethAC;
import dev.zeth.zethac.data.PlayerData;
import dev.zeth.zethac.manager.Check;

public class AutoClickerCheck extends Check {
    public AutoClickerCheck(ZethAC plugin) { super(plugin, "autoclicker", CheckCategory.COMBAT); }

    @Override
    public void onTick(PlayerData data) {
        double maxCPS  = plugin.getConfig().getDouble("checks.autoclicker.max-cps", 22);
        double varThr  = plugin.getConfig().getDouble("checks.autoclicker.consistency-threshold", 0.05);

        double cps = data.getCPS();
        if (cps > maxCPS) {
            flag(data, String.format("High CPS [%.1f > %.1f]", cps, maxCPS)); return;
        }

        double variance = data.getCPSVariance();
        if (cps > 10 && variance < varThr) {
            flag(data, String.format("Low CPS variance [var=%.4f cps=%.1f]", variance, cps)); return;
        }

        decay(data, 2);
    }
}
