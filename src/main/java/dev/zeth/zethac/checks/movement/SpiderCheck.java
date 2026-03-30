package dev.zeth.zethac.checks.movement;

import dev.zeth.zethac.ZethAC;
import dev.zeth.zethac.data.PlayerData;
import dev.zeth.zethac.manager.Check;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

public class SpiderCheck extends Check {
    public SpiderCheck(ZethAC plugin) { super(plugin, "spider", CheckCategory.MOVEMENT); }

    @Override
    public void onTick(PlayerData data) {
        if (data.isFlying() || data.isGliding() || data.isSwimming()) { decay(data, 2); return; }

        int air = data.getAirTicks();
        double dy = data.getDeltaY();

        if (air > 0 && dy > 0.05) {
            Location loc = data.getCurrentLocation();
            if (loc != null) {
                Block adjacent = loc.clone().add(0.4, 0.5, 0).getBlock();
                if (isClimbable(adjacent)) { decay(data, 2); return; }
                adjacent = loc.clone().add(-0.4, 0.5, 0).getBlock();
                if (isClimbable(adjacent)) { decay(data, 2); return; }
                adjacent = loc.clone().add(0, 0.5, 0.4).getBlock();
                if (isClimbable(adjacent)) { decay(data, 2); return; }
                adjacent = loc.clone().add(0, 0.5, -0.4).getBlock();
                if (isClimbable(adjacent)) { decay(data, 2); return; }
                flag(data, String.format("Spider [dy=%.4f air=%d]", dy, air)); return;
            }
        }
        decay(data, 2);
    }

    private boolean isClimbable(Block b) {
        Material t = b.getType();
        return t == Material.LADDER || t == Material.VINE ||
               t.name().contains("CHAIN") || t == Material.SCAFFOLDING;
    }
}
