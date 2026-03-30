package dev.zeth.zethac.checks.combat;

import dev.zeth.zethac.ZethAC;
import dev.zeth.zethac.data.PlayerData;
import dev.zeth.zethac.manager.Check;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * AimAssistCheck — inspirado en GrimAC AimProcessor + AimModulo360.
 *
 * FIXES vs original ZethAC:
 * 1. max-yaw-speed subido a 120°/tick (original: 90 — muy estricto con high-sens).
 * 2. gcd-threshold bajado a 0.0005 (original: 0.001 — kickeaba mice de calidad).
 * 3. require-combat: solo analiza aim si el jugador atacó recientemente.
 * 4. target-switch-grace: gracia al cambiar de target (giro rápido legítimo).
 * 5. Perfect snap mejorado: requiere patrón en 3+ frames consecutivos, no 2.
 * 6. Inspirado en GrimAC AimModulo360: detecta rotaciones que son múltiplos exactos de 360°.
 */
public class AimAssistCheck extends Check {

    public AimAssistCheck(ZethAC plugin) {
        super(plugin, "aimassist", CheckCategory.COMBAT);
    }

    @Override
    public void onTick(PlayerData data) {
        // FIX #3: No chequear fuera de combate
        boolean requireCombat = plugin.getConfig().getBoolean("checks.aimassist.require-combat", true);
        if (requireCombat && data.getAttacksThisTick() == 0) {
            decay(data, 1); return;
        }

        // FIX #4: Gracia al cambiar de target
        int targetGrace = plugin.getConfig().getInt("checks.aimassist.target-switch-grace-ticks", 5);
        if (data.getTicksSinceTargetSwitch() < targetGrace) {
            decay(data, 1); return;
        }

        double maxYaw   = plugin.getConfig().getDouble("checks.aimassist.max-yaw-speed", 120.0);
        double maxPitch = plugin.getConfig().getDouble("checks.aimassist.max-pitch-speed", 80.0);
        double gcdThr   = plugin.getConfig().getDouble("checks.aimassist.gcd-threshold", 0.0005);
        double perfThr  = plugin.getConfig().getDouble("checks.aimassist.perfect-angle-threshold", 0.005);

        float dYaw   = data.getDeltaYaw();
        float dPitch = data.getDeltaPitch();

        // FIX #1: Velocidad de rotación con thresholds correctos
        if (dYaw > maxYaw) {
            flag(data, String.format("Yaw speed [%.2f°/tick]", dYaw));
            return;
        }
        if (dPitch > maxPitch) {
            flag(data, String.format("Pitch speed [%.2f°/tick]", dPitch));
            return;
        }

        Deque<float[]> histDeque = data.getRotationHistory();
        if (histDeque.size() < 4) { decay(data, 1); return; }
        List<float[]> history = new ArrayList<>(histDeque);

        // FIX #6: AimModulo360 — rotación que es múltiplo exacto de 360° indica bot
        float lastDeltaYaw = Math.abs(history.get(0)[0] - history.get(1)[0]);
        if (lastDeltaYaw % 360.0f < 0.001f && lastDeltaYaw > 0) {
            flag(data, String.format("Yaw modulo360 [%.2f°]", lastDeltaYaw));
            return;
        }

        // FIX #2: GCD analysis mejorado — solo flagea si GCD es consistentemente bajo
        double gcd = computeGCD(history);
        if (gcd > 0 && gcd < gcdThr) {
            flag(data, String.format("GCD snap [gcd=%.6f]", gcd));
            return;
        }

        // FIX #5: Perfect snap — requiere patrón idéntico en 3+ frames
        if (history.size() >= 4) {
            float dy0 = Math.abs(history.get(0)[0] - history.get(1)[0]);
            float dy1 = Math.abs(history.get(1)[0] - history.get(2)[0]);
            float dy2 = Math.abs(history.get(2)[0] - history.get(3)[0]);
            float dp0 = Math.abs(history.get(0)[1] - history.get(1)[1]);
            float dp1 = Math.abs(history.get(1)[1] - history.get(2)[1]);
            float dp2 = Math.abs(history.get(2)[1] - history.get(3)[1]);

            boolean identicalYaw   = Math.abs(dy0 - dy1) < perfThr && Math.abs(dy1 - dy2) < perfThr;
            boolean identicalPitch = Math.abs(dp0 - dp1) < perfThr && Math.abs(dp1 - dp2) < perfThr;
            boolean tinyDeltas     = dy0 < perfThr && dp0 < perfThr;

            if (identicalYaw && identicalPitch && tinyDeltas && dy0 > 0) {
                flag(data, String.format("Perfect snap [dy=%.4f dp=%.4f]", dy0, dp0));
                return;
            }
        }

        decay(data, 2);
    }

    /** GCD analysis inspirado en GrimAC AimProcessor. */
    private double computeGCD(List<float[]> history) {
        double g = 0;
        for (int i = 1; i < history.size(); i++) {
            double dY = Math.abs(history.get(i)[0] - history.get(i-1)[0]);
            double dP = Math.abs(history.get(i)[1] - history.get(i-1)[1]);
            if (dY > 1e-9) g = g == 0 ? dY : gcd(g, dY);
            if (dP > 1e-9) g = g == 0 ? dP : gcd(g, dP);
        }
        return g;
    }

    private double gcd(double a, double b) {
        while (b > 1e-10) { double t = b; b = a % b; a = t; }
        return a;
    }
}
