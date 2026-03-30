package dev.zeth.zethac.checks.movement;

import dev.zeth.zethac.ZethAC;
import dev.zeth.zethac.data.PlayerData;
import dev.zeth.zethac.manager.Check;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

public class JesusCheck extends Check {
    public JesusCheck(ZethAC plugin) { super(plugin, "jesus", CheckCategory.MOVEMENT); }

    @Override
    public void onTick(PlayerData data) {
        if (data.isFlying() || data.isGliding() || data.isSwimming()) { decay(data, 2); return; }

        Location loc = data.getCurrentLocation();
        if (loc == null) return;
        Block below = loc.clone().subtract(0, 0.1, 0).getBlock();
        if (!isWater(below)) { decay(data, 2); return; }

        double waterThr = plugin.getConfig().getDouble("checks.jesus.water-speed-threshold", 0.25);
        double hSpeed = data.getHorizontalSpeed();
        double dy = data.getDeltaY();

        if (hSpeed > waterThr && data.isOnGround()) {
            flag(data, String.format("Jesus [h=%.4f on water]", hSpeed)); return;
        }
        if (Math.abs(dy) < 0.001 && !data.isOnGround() && data.getAirTicks() > 5) {
            flag(data, String.format("Jesus hover [dy=%.4f]", dy)); return;
        }
        decay(data, 2);
    }

    private boolean isWater(Block b) {
        Material t = b.getType();
        return t == Material.WATER || t == Material.BUBBLE_COLUMN;
    }
}
