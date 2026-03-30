package dev.zeth.zethac.events;

import dev.zeth.zethac.ZethAC;
import dev.zeth.zethac.checks.player.BadPacketsCheck;
import dev.zeth.zethac.data.PlayerData;
import dev.zeth.zethac.manager.Check;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.util.Vector;

import java.util.Optional;

public class PacketListener implements Listener {
    private final ZethAC plugin;

    public PacketListener(ZethAC plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();
        if (player.hasPermission("zethac.bypass")) return;
        PlayerData data = plugin.getPlayerDataManager().get(player);
        var to = e.getTo();
        if (to == null) return;
        data.updateRotation(to.getYaw(), to.getPitch());
        data.updatePosition(to, player.isOnGround());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        Player player = e.getPlayer();
        if (player.hasPermission("zethac.bypass")) return;
        PlayerData data = plugin.getPlayerDataManager().get(player);
        data.onTeleport();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player player)) return;
        if (player.hasPermission("zethac.bypass")) return;
        PlayerData data = plugin.getPlayerDataManager().get(player);

        data.setLastAttackTime(System.currentTimeMillis());
        Entity target = e.getEntity();
        data.incrementTargetsHit(target.getUniqueId());

        double dist = player.getLocation().distance(target.getLocation());
        data.setLastReach(dist);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onArmSwing(PlayerAnimationEvent e) {
        if (e.getAnimationType() != PlayerAnimationType.ARM_SWING) return;
        if (e.getPlayer().hasPermission("zethac.bypass")) return;
        plugin.getPlayerDataManager().get(e.getPlayer()).setSwungArm(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onVelocity(PlayerVelocityEvent e) {
        if (e.getPlayer().hasPermission("zethac.bypass")) return;
        Vector v = e.getVelocity();
        plugin.getPlayerDataManager().get(e.getPlayer())
                .setExpectedVelocity(v.getX(), v.getY(), v.getZ());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        Player player = e.getPlayer();
        if (player.hasPermission("zethac.bypass")) return;
        PlayerData data = plugin.getPlayerDataManager().get(player);
        data.incrementBlocksPlaced();
        data.setLastPlaceTime(System.currentTimeMillis());
        data.onBlockChange();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        Player player = e.getPlayer();
        if (player.hasPermission("zethac.bypass")) return;
        PlayerData data = plugin.getPlayerDataManager().get(player);
        data.incrementBlocksBroken();
        data.setLastBreakTime(System.currentTimeMillis());
        data.setLastBreakLocation(e.getBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (player.hasPermission("zethac.bypass")) return;
        PlayerData data = plugin.getPlayerDataManager().get(player);
        data.incrementInventoryAction();
        data.setLastInventoryAction(System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBookEdit(PlayerEditBookEvent e) {
        Player player = e.getPlayer();
        if (player.hasPermission("zethac.bypass")) return;
        PlayerData data = plugin.getPlayerDataManager().get(player);
        runCheck(BadPacketsCheck.class, data, check -> ((BadPacketsCheck) check).checkBook(data, e));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPotionEffect(EntityPotionEffectEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;
        if (player.hasPermission("zethac.bypass")) return;
        plugin.getPlayerDataManager().get(player).onPotionChange();
    }

    private void runCheck(Class<? extends Check> clazz, PlayerData data, java.util.function.Consumer<Check> action) {
        Optional<Check> check = plugin.getCheckManager().getChecks().stream()
                .filter(c -> c.getClass() == clazz).findFirst();
        check.ifPresent(action);
    }
}
