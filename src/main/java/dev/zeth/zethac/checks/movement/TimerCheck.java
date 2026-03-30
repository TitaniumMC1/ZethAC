package dev.zeth.zethac.checks.movement;

import dev.zeth.zethac.ZethAC;
import dev.zeth.zethac.data.PlayerData;
import dev.zeth.zethac.manager.Check;

public class TimerCheck extends Check {
    public TimerCheck(ZethAC plugin) { super(plugin, "timer", CheckCategory.MOVEMENT); }

    @Override
    public void onTick(PlayerData data) {
        double maxMPS = plugin.getConfig().getDouble("checks.timer.max-moves-per-second", 22.0);

        long now = System.currentTimeMillis();
        long windowMs = now - data.getMoveWindowStart();
        if (windowMs < 1000) { decay(data, 1); return; }

        double movesPerSec = data.getMoveCount() / (windowMs / 1000.0);
        if (movesPerSec > maxMPS) {
            flag(data, String.format("Timer [%.1f moves/s > %.1f]", movesPerSec, maxMPS)); return;
        }
        decay(data, 2);
    }
}
