package dev.zeth.zethac.checks.combat;

import dev.zeth.zethac.ZethAC;
import dev.zeth.zethac.data.PlayerData;
import dev.zeth.zethac.manager.Check;

/**
 * VelocityCheck — inspirado en GrimAC Knockback check.
 *
 * FIXES vs original ZethAC:
 * 1. knockback-grace-ticks: GrimAC espera confirmación de transacción antes de verificar.
 *    Aquí simulamos eso con N ticks de gracia después de recibir knockback.
 * 2. min-h-reduction bajado a 0.5 (original: 0.6 — demasiado estricto con lag).
 * 3. min-v-reduction bajado a 0.6 (original: 0.7).
 * 4. No verifica si no hay velocidad aplicada.
 * 5. No verifica si vuela o planea.
 */
public class VelocityCheck extends Check {

    public VelocityCheck(ZethAC plugin) {
        super(plugin, "velocity", CheckCategory.COMBAT);
    }

    @Override
    public void onTick(PlayerData data) {
        if (data.isFlying() || data.isGliding()) { decay(data, 2); return; }

        // FIX #4: Solo verificar si hay velocidad pendiente
        if (!data.isVelocityApplied()) { decay(data, 1); return; }

        // FIX #1: Gracia de ticks inspirada en GrimAC transaction system
        int grace = plugin.getConfig().getInt("checks.velocity.knockback-grace-ticks", 10);
        if (data.getTicksSinceVelocityApplied() < grace) { decay(data, 1); return; }

        double minH = plugin.getConfig().getDouble("checks.velocity.min-h-reduction", 0.5);
        double minV = plugin.getConfig().getDouble("checks.velocity.min-v-reduction", 0.6);

        double expH = Math.sqrt(
                data.getExpectedVelocityX() * data.getExpectedVelocityX() +
                data.getExpectedVelocityZ() * data.getExpectedVelocityZ()
        );
        double actH = data.getHorizontalSpeed();
        double expV = data.getExpectedVelocityY();
        double actV = data.getDeltaY();

        if (expH < 0.001) { decay(data, 1); return; }

        if (actH < expH * minH) {
            flag(data, String.format("Anti-KB H [ratio=%.2f]", actH / expH));
        } else if (expV > 0.01 && actV < expV * minV) {
            flag(data, String.format("Anti-KB V [ratio=%.2f]", actV / expV));
        } else {
            decay(data, 2);
            data.clearVelocity();
        }
    }
}
