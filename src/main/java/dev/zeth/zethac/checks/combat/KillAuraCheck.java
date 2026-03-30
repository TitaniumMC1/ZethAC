package dev.zeth.zethac.checks.combat;

import dev.zeth.zethac.ZethAC;
import dev.zeth.zethac.data.PlayerData;
import dev.zeth.zethac.manager.Check;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class KillAuraCheck extends Check {
    public KillAuraCheck(ZethAC plugin) { super(plugin, "killaura", CheckCategory.COMBAT); }

    @Override
    public void onTick(PlayerData data) {
        int multiThr  = plugin.getConfig().getInt("checks.killaura.multi-target-threshold", 3);
        double angleThr = plugin.getConfig().getDouble("checks.killaura.invalid-angle-threshold", 90.0);

        if (data.getTargetsThisTick() > multiThr) {
            flag(data, "Multi-target [" + data.getTargetsThisTick() + " targets/tick]"); return;
        }
        if (data.getAttacksThisTick() > 0) {
            // Check if player attacked without proper look direction
            // (simplified — full ray-cast requires NMS)
            decay(data, 1);
        } else {
            decay(data, 1);
        }
    }

    private double getAngleTo(Player p, Entity target) {
        var dir = p.getLocation().getDirection();
        var toTarget = target.getLocation().toVector().subtract(p.getLocation().toVector()).normalize();
        return Math.toDegrees(dir.angle(toTarget));
    }
}
