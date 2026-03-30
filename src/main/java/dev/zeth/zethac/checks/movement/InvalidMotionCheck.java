package dev.zeth.zethac.checks.movement;

import dev.zeth.zethac.ZethAC;
import dev.zeth.zethac.data.PlayerData;
import dev.zeth.zethac.manager.Check;
import org.bukkit.NamespacedKey;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * InvalidMotionCheck — inspirado en GrimAC PredictionRunner.
 *
 * FIXES vs original ZethAC:
 * 1. vl-threshold era 4 — el más bajo de todos los checks, causaba kicks inmediatos.
 *    Ahora es 8 (configurable).
 * 2. Fórmula de Jump Boost corregida: vanilla usa 0.42 + 0.1*(amplifier+1).
 * 3. Tolerancia de floating-point (jump-velocity-tolerance=0.005) para evitar
 *    que diferencias menores entre cliente y servidor causen flags.
 * 4. Solo verifica el primer tick del salto (wasOnGround=true, isOnGround=false).
 * 5. No verifica si vuela, planea o nada.
 * 6. Umbral de 50% (no exacto) para detectar solo saltos claramente anómalos.
 */
public class InvalidMotionCheck extends Check {

    private static final double VANILLA_JUMP_VELOCITY = 0.42;

    public InvalidMotionCheck(ZethAC plugin) {
        super(plugin, "invalidmotion", CheckCategory.MOVEMENT);
    }

    @Override
    public void onTick(PlayerData data) {
        if (data.isFlying() || data.isGliding() || data.isSwimming()) {
            decay(data, 2); return;
        }

        double tolerance = plugin.getConfig().getDouble("checks.invalidmotion.jump-velocity-tolerance", 0.005);

        double deltaY     = data.getDeltaY();
        double lastDeltaY = data.getLastDeltaY();
        boolean wasOnGround = data.wasOnGround();
        boolean isOnGround  = data.isOnGround();

        // FIX #4: Solo verificar el tick del salto exacto
        if (!wasOnGround || isOnGround) {
            // No es un salto, verificar mid-air boost (aceleración vertical en el aire)
            if (!isOnGround && !wasOnGround && data.getAirTicks() > 1) {
                // FIX #6: Solo flagea si la aceleración es grande y positiva (boost hack)
                double acceleration = deltaY - lastDeltaY;
                // Vanilla gravity = -0.08 por tick, friction * ~0.98
                // Un boost hack tendría aceleración positiva o mucho menor caída
                if (acceleration > 0.05 && deltaY > 0.1) {
                    flag(data, String.format("Mid-air boost [ddy=%.4f]", acceleration));
                    return;
                }
            }
            decay(data, 1);
            return;
        }

        // El jugador acaba de saltar — calcular velocidad esperada
        double expectedJump = VANILLA_JUMP_VELOCITY;

        PotionEffectType jumpBoostType = PotionEffectType.getByKey(NamespacedKey.minecraft("jump_boost"));
        if (jumpBoostType != null) {
            PotionEffect jumpBoost = data.getPlayer().getPotionEffect(jumpBoostType);
            if (jumpBoost != null) {
                // FIX #2: fórmula vanilla corregida
                expectedJump = VANILLA_JUMP_VELOCITY + 0.1 * (jumpBoost.getAmplifier() + 1);
            }
        }

        // FIX #3 + #6: Solo flagea si la diferencia supera 50% + tolerance
        double diff = Math.abs(deltaY - expectedJump);
        if (diff > expectedJump * 0.5 + tolerance) {
            flag(data, String.format("Invalid jump [dy=%.4f exp=%.4f]", deltaY, expectedJump));
        } else {
            decay(data, 2);
        }
    }
}
