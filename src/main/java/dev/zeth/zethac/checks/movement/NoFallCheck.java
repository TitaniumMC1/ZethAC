package dev.zeth.zethac.checks.movement;

import dev.zeth.zethac.ZethAC;
import dev.zeth.zethac.data.PlayerData;
import dev.zeth.zethac.manager.Check;

public class NoFallCheck extends Check {
    public NoFallCheck(ZethAC plugin) { super(plugin, "nofall", CheckCategory.MOVEMENT); }

    @Override
    public void onTick(PlayerData data) {
        if (data.isFlying() || data.isGliding() || data.isSwimming()) { decay(data, 2); return; }

        double minFall = plugin.getConfig().getDouble("checks.nofall.min-fall-distance", 3.0);

        if (!data.wasOnGround() && data.isOnGround()) {
            float serverFall = data.getPlayer().getFallDistance();
            double acFall = data.getFallDistance();
            if (acFall > minFall && serverFall < 0.5f) {
                flag(data, String.format("NoFall [fall=%.2f blocks]", acFall)); return;
            }
        }
        decay(data, 2);
    }
}
