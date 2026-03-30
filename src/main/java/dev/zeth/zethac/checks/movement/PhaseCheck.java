package dev.zeth.zethac.checks.movement;

import dev.zeth.zethac.ZethAC;
import dev.zeth.zethac.data.PlayerData;
import dev.zeth.zethac.manager.Check;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Set;

/**
 * PhaseCheck — inspirado en GrimAC Phase check.
 *
 * FIXES vs original ZethAC:
 * 1. Usa isPassable() antes de flagear — trapdoors abiertas, puertas, plantas, slabs, etc.
 *    no se detectan como phase.
 * 2. Compensación de ping: threshold se escala con la latencia del jugador.
 * 3. Gracia de teleportación: 20 ticks después de /tp no se flagea.
 * 4. No se flagea si el jugador vuela, planea o nada.
 * 5. Lista de materiales extra que son visualmente sólidos pero el jugador puede estar en ellos.
 */
public class PhaseCheck extends Check {

    private static final Set<String> EXEMPT_SUFFIXES = Set.of(
            "TRAPDOOR", "DOOR", "GATE", "SLAB", "STAIRS", "CARPET",
            "SKULL", "HEAD", "BANNER", "SIGN", "BED", "ANVIL",
            "CAMPFIRE", "LANTERN", "CHAIN", "CANDLE", "FENCE",
            "CORAL", "SEAGRASS", "KELP", "VINE", "FERN", "GRASS",
            "FLOWER", "MUSHROOM", "SPROUTS", "SENSOR", "SHRIEKER",
            "CLUSTER", "DRIPSTONE", "LECTERN", "GRINDSTONE", "BARREL"
    );

    public PhaseCheck(ZethAC plugin) {
        super(plugin, "phase", CheckCategory.MOVEMENT);
    }

    @Override
    public void onTick(PlayerData data) {
        // No chequear si vuela, planea, nada
        if (data.isFlying() || data.isGliding() || data.isSwimming()) {
            decay(data, 2); return;
        }

        Player player = data.getPlayer();
        switch (player.getGameMode()) {
            case CREATIVE: case SPECTATOR: decay(data, 2); return;
            default: break;
        }

        // FIX #3: Gracia tras teleport
        int teleportGrace = plugin.getConfig().getInt("checks.phase.teleport-grace-ticks", 20);
        if (data.getTicksSinceLastTeleport() < teleportGrace) {
            decay(data, 1); return;
        }

        Location from = data.getLastLocation();
        Location to   = data.getCurrentLocation();
        if (from == null || to == null) return;
        if (!from.getWorld().equals(to.getWorld())) return;

        double threshold    = plugin.getConfig().getDouble("checks.phase.block-penetration-threshold", 0.12);
        boolean ignorePass  = plugin.getConfig().getBoolean("checks.phase.ignore-passable-blocks", true);

        // FIX #2: escalar threshold con ping
        double pingCompensation = Math.min(player.getPing() / 1000.0 * 0.08, 0.10);
        double effectiveThreshold = threshold + pingCompensation;

        // Verificar bloque en la posición actual (pies)
        Block feetBlock = to.getBlock();
        if (isBadBlock(feetBlock, ignorePass)) {
            double pen = penetrationDepth(to.getX(), to.getY(), to.getZ(), feetBlock);
            if (pen > effectiveThreshold) {
                flag(data, "Phase through " + feetBlock.getType().name());
                return;
            }
        }

        decay(data, 2);
    }

    /** Devuelve true si el bloque es sólido Y no es legítimamente pasable */
    private boolean isBadBlock(Block block, boolean ignorePass) {
        Material type = block.getType();
        if (type.isAir()) return false;

        // FIX #1: usar isPassable() de la Bukkit API
        if (ignorePass && block.isPassable()) return false;

        // FIX #1: lista de materiales que parecen sólidos pero son pasables
        String name = type.name();
        for (String suffix : EXEMPT_SUFFIXES) {
            if (name.contains(suffix)) return false;
        }

        return type.isSolid();
    }

    private double penetrationDepth(double px, double py, double pz, Block block) {
        double dx = Math.max(0, 0.5 - Math.abs(px - (block.getX() + 0.5)));
        double dy = Math.max(0, 0.5 - Math.abs(py - (block.getY() + 0.5)));
        double dz = Math.max(0, 0.5 - Math.abs(pz - (block.getZ() + 0.5)));
        return Math.min(dx, Math.min(dy, dz));
    }
}
