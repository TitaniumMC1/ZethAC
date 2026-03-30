package dev.zeth.zethac.checks.movement;

import dev.zeth.zethac.ZethAC;
import dev.zeth.zethac.data.PlayerData;
import dev.zeth.zethac.manager.Check;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

public class GroundSpoofCheck extends Check {
    public GroundSpoofCheck(ZethAC plugin) { super(plugin, "groundspoof", CheckCategory.MOVEMENT); }

    @Override
    public void onTick(PlayerData data) {
        if (data.isFlying() || data.isGliding() || data.isSwimming()) { decay(data, 2); return; }
        if (data.isOnGround()) {
            Location loc = data.getCurrentLocation();
            if (loc != null && !isSolidAt(loc.clone().subtract(0, 0.05, 0))) {
                double dy = data.getDeltaY();
                if (dy > 0.01) {
                    flag(data, String.format("GroundSpoof [no solid below, dy=%.4f]", dy)); return;
                }
            }
        }
        decay(data, 2);
    }

    private boolean isSolidAt(Location loc) {
        Block b = loc.getBlock();
        return b.getType().isSolid() && !b.isPassable();
    }
}
