package dev.zeth.zethac.alerts;

import dev.zeth.zethac.ZethAC;
import dev.zeth.zethac.data.PlayerData;
import dev.zeth.zethac.manager.Check;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class AlertManager {
    private final ZethAC plugin;

    public AlertManager(ZethAC plugin) { this.plugin = plugin; }

    public void sendAlert(PlayerData data, Check check, String detail, int vl) {
        String prefix = plugin.getConfig().getString("prefix", "&8[&bZeth&3AC&8] ")
                .replace("&", "§");
        String msg = String.format("%s§7%s §8| §b%s §8[§c%s§8] §8| §fVL: §c%d §8| §7%s §8| §7ping=%dms",
                prefix, data.getPlayer().getName(), check.getName(), check.getCategory().name(),
                vl, detail, data.getPing());

        if (plugin.getConfig().getBoolean("alerts.broadcast-to-staff", true)) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.hasPermission("zethac.alerts")) {
                    p.sendMessage(msg);
                }
                if (data.isVerboseMode() && p.hasPermission("zethac.verbose")) {
                    p.sendMessage("§8[§3VERBOSE§8] " + msg);
                }
            }
        }
        if (plugin.getConfig().getBoolean("alerts.console-alerts", true)) {
            plugin.getLogger().info(String.format("[ALERT] %s | %s | VL:%d | %s",
                    data.getPlayer().getName(), check.getName(), vl, detail));
        }

        String webhook = plugin.getConfig().getString("alerts.discord-webhook", "");
        if (!webhook.isEmpty()) {
            sendDiscordAlert(webhook, data, check, detail, vl);
        }
    }

    private void sendDiscordAlert(String webhookUrl, PlayerData data, Check check, String detail, int vl) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String json = String.format(
                        "{\"embeds\":[{\"title\":\"ZethAC Alert\",\"description\":\"**Player:** %s\\n**Check:** %s\\n**VL:** %d\\n**Detail:** %s\",\"color\":16711680}]}",
                        data.getPlayer().getName(), check.getName(), vl, detail);
                URL url = new URL(webhookUrl);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json");
                con.setDoOutput(true);
                try (OutputStream os = con.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }
                con.getInputStream().close();
            } catch (Exception ignored) {}
        });
    }
}
