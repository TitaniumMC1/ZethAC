package dev.zeth.zethac.checks.world;

import dev.zeth.zethac.ZethAC;
import dev.zeth.zethac.data.PlayerData;
import dev.zeth.zethac.manager.Check;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.Set;

public class XRayCheck extends Check {

    private static final Set<Material> VALUABLE_ORES = Set.of(
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.ANCIENT_DEBRIS,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE
    );

    public XRayCheck(ZethAC plugin) { super(plugin, "xray", CheckCategory.WORLD); }

    @Override
    public void onTick(PlayerData data) {
        Location loc = data.getLastBreakLocation();
        if (loc == null) return;
        Block block = loc.getBlock();
        if (!VALUABLE_ORES.contains(block.getType())) { decayOreWindow(data); return; }

        long window = plugin.getConfig().getLong("checks.xray.window", 30) * 1000L;
        long now = System.currentTimeMillis();
        if (now - data.getOreWindowStart() > window) {
            data.resetOreMine();
            data.setOreWindowStart(now);
        }

        data.incrementOreMine();
        int threshold = plugin.getConfig().getInt("checks.xray.ore-mine-threshold", 5);
        if (data.getOreMineCount() >= threshold) {
            flag(data, String.format("XRay suspect [%d ores in %ds, last=%s]",
                    data.getOreMineCount(), window / 1000, block.getType().name()));
        }
    }

    private void decayOreWindow(PlayerData data) {
        decay(data, 1);
    }
}
