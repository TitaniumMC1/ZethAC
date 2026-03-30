package dev.zeth.zethac.checks.player;

import dev.zeth.zethac.ZethAC;
import dev.zeth.zethac.data.PlayerData;
import dev.zeth.zethac.manager.Check;

/**
 * ChestStealerCheck — inspirado en GrimAC PacketOrderA (inventory click ordering).
 *
 * FIXES vs original ZethAC:
 * 1. min-action-delay era 2 ticks (100ms) — Vanilla permite shift-click cada 1 tick (50ms).
 *    El default ahora es 1 tick, y se usa ms en vez de ticks para precisión con lag.
 * 2. Shift-click: se permite delay de 10ms mínimo (casi-instantáneo es legítimo).
 * 3. max-actions-per-tick: si el jugador hace más de N acciones en 1 tick, flag.
 * 4. No flagea en el primer click (last == 0 significa que no hay dato previo).
 * 5. Inspirado en GrimAC: usa timestamps de ms para ignorar la imprecisión de ticks.
 */
public class ChestStealerCheck extends Check {

    public ChestStealerCheck(ZethAC plugin) {
        super(plugin, "chest-stealer", CheckCategory.PLAYER);
    }

    @Override
    public void onTick(PlayerData data) {
        int minDelayTicks   = plugin.getConfig().getInt("checks.chest-stealer.min-action-delay", 1);
        int maxActionsPerTick = plugin.getConfig().getInt("checks.chest-stealer.max-actions-per-tick", 2);
        boolean allowShift  = plugin.getConfig().getBoolean("checks.chest-stealer.allow-shift-click", true);

        // FIX #1: usar ms para precisión real, no ticks
        long minDelayMs = minDelayTicks * 50L;

        int  actions = data.getInventoryActionsThisTick();
        long last    = data.getLastInventoryAction();
        long now     = System.currentTimeMillis();

        // FIX #3: demasiadas acciones en 1 tick = automatización
        if (actions > maxActionsPerTick) {
            flag(data, "ChestStealer [" + actions + " actions/tick]");
            decay(data, 1);
            return;
        }

        // FIX #4: solo verificar delay si tenemos dato previo
        if (actions > 0 && last > 0) {
            long elapsed = now - last;

            // FIX #2: shift-click puede ser muy rápido (10ms mínimo), click normal = minDelayMs
            long effectiveMin = allowShift ? 10L : minDelayMs;

            if (elapsed < effectiveMin) {
                flag(data, "ChestStealer [delay=" + elapsed + "ms]");
                decay(data, 1);
            } else {
                decay(data, 2);
            }
        } else {
            decay(data, 1);
        }
    }
}
