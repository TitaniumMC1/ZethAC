package dev.zeth.zethac.checks.movement;

import dev.zeth.zethac.ZethAC;
import dev.zeth.zethac.data.PlayerData;
import dev.zeth.zethac.manager.Check;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

public class FlyCheck extends Check {
    public FlyCheck(ZethAC plugin) { super(plugin, "fly", CheckCategory.MOVEMENT); }

    @Override
    public void onTick(PlayerData data) {
        if (data.isFlying() || data.isGliding() || data.isSwimming()) { decay(data, 2); return; }
        GameMode gm = data.getPlayer().getGameMode();
        if (gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR || data.getPlayer().getAllowFlight()) {
            decay(data, 2); return;
        }
        if (isNearClimbable(data)) { decay(data, 2); return; }

        double maxY  = plugin.getConfig().getDouble("checks.fly.max-y-delta", 0.65);
        double hover = plugin.getConfig().getDouble("checks.fly.hover-threshold", 0.005);
        int air = data.getAirTicks();
        double dy = data.getDeltaY();

        if (air > 0 && dy > maxY) {
            flag(data, String.format("Fly ascend [dy=%.4f air=%d]", dy, air)); return;
        }
        if (air > 20 && Math.abs(dy) < hover) {
            flag(data, String.format("Hover [dy=%.4f air=%d]", dy, air)); return;
        }
        if (air > 60 && dy > -0.1) {
            flag(data, String.format("Sustained air [air=%d dy=%.4f]", air, dy)); return;
        }
        decay(data, 2);
    }

    private boolean isNearClimbable(PlayerData data) {
        Location loc = data.getCurrentLocation();
        if (loc == null) return false;
        Block b = loc.getBlock();
        Material t = b.getType();
        return t == Material.LADDER || t == Material.VINE ||
               t.name().contains("SCAFFOLD") || b.isPassable() && t.name().contains("TRAPDOOR");
    }
}
