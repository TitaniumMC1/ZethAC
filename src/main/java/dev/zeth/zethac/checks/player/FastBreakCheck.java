package dev.zeth.zethac.checks.player;

import dev.zeth.zethac.ZethAC;
import dev.zeth.zethac.data.PlayerData;
import dev.zeth.zethac.manager.Check;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class FastBreakCheck extends Check {
    public FastBreakCheck(ZethAC plugin) { super(plugin, "fastbreak", CheckCategory.PLAYER); }

    @Override
    public void onTick(PlayerData data) {
        Player player = data.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) { decay(data, 2); return; }

        Location loc = data.getLastBreakLocation();
        if (loc == null) { decay(data, 1); return; }

        long last = data.getLastBreakTime();
        if (last == 0) { decay(data, 1); return; }

        long elapsed = System.currentTimeMillis() - last;
        Block block = loc.getBlock();

        double expected = estimateBreakTime(data, block);
        if (expected > 0 && elapsed < expected * 0.5) {
            flag(data, String.format("FastBreak [%.0fms < %.0fms]", (double)elapsed, expected * 0.5)); return;
        }
        decay(data, 2);
    }

    private double estimateBreakTime(PlayerData data, Block block) {
        if (block == null || block.getType() == Material.AIR) return 0;
        float hardness = block.getType().getHardness();
        if (hardness < 0) return -1; // unbreakable

        PlayerInventory inv = data.getPlayer().getInventory();
        ItemStack hand = inv.getItemInMainHand();
        boolean correct = isCorrectTool(hand, block);
        float toolSpeed = getToolSpeed(hand);

        float damage = correct ? toolSpeed / hardness / 30f : 1f / hardness / 100f;

        // Efficiency enchantment
        int eff = hand.getEnchantmentLevel(Enchantment.EFFICIENCY);
        if (eff > 0 && correct) toolSpeed += eff * eff + 1;

        // Haste effect
        PotionEffectType hasteType = PotionEffectType.getByKey(NamespacedKey.minecraft("haste"));
        if (hasteType != null) {
            PotionEffect haste = data.getPlayer().getPotionEffect(hasteType);
            if (haste != null) damage *= 1 + (haste.getAmplifier() + 1) * 0.2;
        }

        // Mining fatigue
        PotionEffectType fatigueType = PotionEffectType.getByKey(NamespacedKey.minecraft("mining_fatigue"));
        if (fatigueType != null) {
            PotionEffect fatigue = data.getPlayer().getPotionEffect(fatigueType);
            if (fatigue != null) damage *= Math.pow(0.3, Math.min(fatigue.getAmplifier() + 1, 4));
        }

        if (damage >= 1) return 50; // instant mine = 1 tick = 50ms
        return Math.ceil(1.0 / damage) * 50.0;
    }

    private boolean isCorrectTool(ItemStack item, Block block) {
        if (item == null) return false;
        String mat = block.getType().name();
        String tool = item.getType().name();
        if ((mat.contains("ORE") || mat.contains("STONE") || mat.contains("ROCK")) && tool.contains("PICKAXE")) return true;
        if ((mat.contains("LOG") || mat.contains("WOOD") || mat.contains("PLANK")) && tool.contains("AXE")) return true;
        if ((mat.contains("DIRT") || mat.contains("GRASS") || mat.contains("SAND")) && tool.contains("SHOVEL")) return true;
        return false;
    }

    private float getToolSpeed(ItemStack item) {
        if (item == null) return 1f;
        String n = item.getType().name();
        if (n.contains("NETHERITE")) return 9f;
        if (n.contains("DIAMOND"))   return 8f;
        if (n.contains("IRON"))      return 6f;
        if (n.contains("STONE"))     return 4f;
        if (n.contains("WOOD") || n.contains("GOLD")) return 2f;
        return 1f;
    }
}
