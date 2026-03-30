package dev.zeth.zethac.manager;

import dev.zeth.zethac.ZethAC;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.BanList;
import org.bukkit.entity.Player;

import java.util.Date;

public class PunishmentManager {
    private final ZethAC plugin;

    public PunishmentManager(ZethAC plugin) { this.plugin = plugin; }

    public void punish(Player player, String check, String punishment, int vl) {
        switch (punishment.toLowerCase()) {
            case "ban":  ban(player, check);  break;
            case "kick": kick(player, check); break;
            case "alert": break; // already alerted
        }
    }

    public void kick(Player player, String check) {
        String msg = plugin.getConfig().getString("punishments.kick-message",
                "&cYou have been kicked for cheating.\n&7Plugin: ZethAC");
        msg = msg.replace("&", "§").replace("{check}", check);
        player.kick(LegacyComponentSerializer.legacySection().deserialize(msg));
        plugin.getLogger().info(String.format("[ZethAC] Kicked %s for check: %s", player.getName(), check));
    }

    public void ban(Player player, String check) {
        String msg = plugin.getConfig().getString("punishments.ban-message",
                "&cYou have been banned for cheating.\n&7Plugin: ZethAC");
        msg = msg.replace("&", "§").replace("{check}", check);
        int durMin = plugin.getConfig().getInt("punishments.ban-duration", -1);
        Date expiry = durMin < 0 ? null : new Date(System.currentTimeMillis() + durMin * 60_000L);
        plugin.getServer().getBanList(BanList.Type.USERNAME).addBan(player.getName(), msg, expiry, "ZethAC");
        player.kickPlayer(msg.replace("§", "&").replace("&", "§"));
        plugin.getLogger().info(String.format("[ZethAC] Banned %s for check: %s", player.getName(), check));
    }
}
