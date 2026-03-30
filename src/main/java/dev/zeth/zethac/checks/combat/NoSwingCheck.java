package dev.zeth.zethac.checks.combat;

import dev.zeth.zethac.ZethAC;
import dev.zeth.zethac.data.PlayerData;
import dev.zeth.zethac.manager.Check;

public class NoSwingCheck extends Check {
    public NoSwingCheck(ZethAC plugin) { super(plugin, "noswing", CheckCategory.COMBAT); }

    @Override
    public void onTick(PlayerData data) {
        if (data.getAttacksThisTick() > 0 && !data.isSwungArm()) {
            data.incrementNoSwing();
            int streak = data.getConsecutiveNoSwing();
            if (streak >= 3) {
                flag(data, "No arm swing [" + streak + " consecutive]"); return;
            }
        } else {
            data.resetNoSwing();
            decay(data, 2);
        }
    }
}
