package dev.zeth.zethac.checks.movement;

import dev.zeth.zethac.ZethAC;
import dev.zeth.zethac.data.PlayerData;
import dev.zeth.zethac.manager.Check;

public class StepCheck extends Check {
    public StepCheck(ZethAC plugin) { super(plugin, "step", CheckCategory.MOVEMENT); }

    @Override
    public void onTick(PlayerData data) {
        if (data.isFlying() || data.isGliding() || data.isSwimming()) { decay(data, 2); return; }

        double maxStep = plugin.getConfig().getDouble("checks.step.max-step-height", 0.65);
        double dy = data.getDeltaY();

        // Step: upward movement while on ground (or transitioning to ground)
        if (data.isOnGround() && data.wasOnGround() && dy > maxStep) {
            flag(data, String.format("Step [dy=%.4f]", dy)); return;
        }
        decay(data, 2);
    }
}
