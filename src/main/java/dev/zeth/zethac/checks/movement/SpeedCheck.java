package dev.zeth.zethac.checks.movement;

import dev.zeth.zethac.ZethAC;
import dev.zeth.zethac.data.PlayerData;
import dev.zeth.zethac.manager.Check;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * SpeedCheck — inspirado en GrimAC NoSlow / PredictionRunner.
 *
 * FIXES vs original ZethAC:
 * 1. max-speed-buffer subido a 0.18 (original: 0.12 — demasiado estricto con lag).
 * 2. Fórmula Speed effect corregida: vanilla +20% por nivel, no +10%.
 * 3. Detecta correctamente Blue Ice vs Packed Ice vs Ice (velocidades distintas).
 * 4. Gracia de ticks al cambiar de bloque o recibir efecto de poción.
 * 5. Fórmula Slowness corregida: vanilla -15% por nivel.
 */
public class SpeedCheck extends Check {

    private static final double BASE_WALK  = 0.2806;
    private static final double SPRINT_MULT = 1.3;
    private static final double SNEAK_MULT  = 0.3;

    public SpeedCheck(ZethAC plugin) {
        super(plugin, "speed", CheckCategory.MOVEMENT);
    }

    @Override
    public void onTick(PlayerData data) {
        if (data.isFlying() || data.isGliding() || data.isSwimming()) {
            decay(data, 2); return;
        }

        // FIX #4: Gracia al cambiar de bloque o poción
        int blockGrace  = plugin.getConfig().getInt("checks.speed.block-change-grace-ticks", 10);
        int potionGrace = plugin.getConfig().getInt("checks.speed.potion-effect-grace-ticks", 10);
        if (data.getTicksSinceBlockChange() < blockGrace ||
            data.getTicksSincePotionChange() < potionGrace) {
            decay(data, 1); return;
        }

        double buffer   = plugin.getConfig().getDouble("checks.speed.max-speed-buffer", 0.18);
        double hSpeed   = data.getHorizontalSpeed();
        double expected = getExpectedSpeed(data);

        if (hSpeed > expected + buffer) {
            flag(data, String.format("Speed [h=%.4f exp=%.4f]", hSpeed, expected));
        } else {
            decay(data, 2);
        }
    }

    private double getExpectedSpeed(PlayerData data) {
        double base = BASE_WALK;
        if (data.isSprinting())     base *= SPRINT_MULT;
        else if (data.isSneaking()) base *= SNEAK_MULT;

        // FIX #3: Detectar tipo específico de hielo
        Location loc = data.getCurrentLocation();
        if (loc != null) {
            Block below = loc.clone().subtract(0, 0.1, 0).getBlock();
            Material type = below.getType();
            double iceMult = plugin.getConfig().getDouble("checks.speed.ice-multiplier", 1.55);
            if      (type == Material.BLUE_ICE)   base *= iceMult + 0.45;
            else if (type == Material.PACKED_ICE)  base *= iceMult + 0.20;
            else if (type == Material.ICE)         base *= iceMult;
            else if (type == Material.SOUL_SAND) {
                double sandMult = plugin.getConfig().getDouble("checks.speed.soul-sand-multiplier", 0.4);
                base *= sandMult;
            }
        }

        // FIX #2: Speed effect — vanilla: +20% por nivel (amplifier+1)
        PotionEffectType speedType = PotionEffectType.getByKey(NamespacedKey.minecraft("speed"));
        if (speedType != null) {
            PotionEffect speedEff = data.getPlayer().getPotionEffect(speedType);
            if (speedEff != null) base *= (1.0 + 0.2 * (speedEff.getAmplifier() + 1));
        }

        // FIX #5: Slowness effect — vanilla: -15% por nivel
        PotionEffectType slowType = PotionEffectType.getByKey(NamespacedKey.minecraft("slowness"));
        if (slowType != null) {
            PotionEffect slowEff = data.getPlayer().getPotionEffect(slowType);
            if (slowEff != null) base *= Math.max(0.1, 1.0 - 0.15 * (slowEff.getAmplifier() + 1));
        }

        return base;
    }
}
