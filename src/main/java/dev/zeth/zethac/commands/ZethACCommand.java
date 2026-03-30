package dev.zeth.zethac.commands;

import dev.zeth.zethac.ZethAC;
import dev.zeth.zethac.data.PlayerData;
import dev.zeth.zethac.manager.Check;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ZethACCommand implements CommandExecutor, TabCompleter {
    private final ZethAC plugin;
    private final String PREFIX;

    public ZethACCommand(ZethAC plugin) {
        this.plugin = plugin;
        this.PREFIX = plugin.getConfigManager().getPrefix();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("zethac.admin")) { send(sender, "&cNo permission."); return true; }
        if (args.length == 0) { sendHelp(sender); return true; }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.getConfigManager().reload();
                send(sender, "&aConfiguration reloaded.");
            }
            case "alerts" -> {
                if (!(sender instanceof Player p)) { send(sender, "&cOnly players."); return true; }
                PlayerData data = plugin.getPlayerDataManager().get(p);
                data.setReceiveAlerts(!data.isReceiveAlerts());
                send(sender, data.isReceiveAlerts() ? "&aAlerts &aEnabled" : "&cAlerts &cDisabled");
            }
            case "verbose" -> {
                if (!(sender instanceof Player p)) { send(sender, "&cOnly players."); return true; }
                PlayerData data = plugin.getPlayerDataManager().get(p);
                data.setVerboseMode(!data.isVerboseMode());
                send(sender, data.isVerboseMode() ? "&aVerbose &aEnabled" : "&cVerbose &cDisabled");
            }
            case "info" -> {
                if (args.length < 2) { send(sender, "&cUsage: /zac info <player>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { send(sender, "&cPlayer not found."); return true; }
                sendPlayerInfo(sender, target);
            }
            case "logs" -> {
                if (args.length < 2) { send(sender, "&cUsage: /zac logs <player>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { send(sender, "&cPlayer not found."); return true; }
                sendViolationLog(sender, target);
            }
            case "kick" -> {
                if (args.length < 2) { send(sender, "&cUsage: /zac kick <player>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { send(sender, "&cPlayer not found."); return true; }
                plugin.getPunishmentManager().kick(target, "Manual kick by " + sender.getName());
            }
            case "checks" -> sendChecks(sender);
            case "toggle" -> {
                if (args.length < 2) { send(sender, "&cUsage: /zac toggle <check>"); return true; }
                plugin.getCheckManager().getCheck(args[1]).ifPresentOrElse(c -> {
                    c.setEnabled(!c.isEnabled());
                    send(sender, "&7Check &b" + c.getName() + "&7: " + (c.isEnabled() ? "&aEnabled" : "&cDisabled"));
                }, () -> send(sender, "&cCheck not found."));
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender s) {
        send(s, "&b/zac reload &7- Reload config");
        send(s, "&b/zac alerts &7- Toggle alerts");
        send(s, "&b/zac verbose &7- Toggle verbose");
        send(s, "&b/zac info <player> &7- Player info");
        send(s, "&b/zac logs <player> &7- Violation log");
        send(s, "&b/zac kick <player> &7- Kick player");
        send(s, "&b/zac toggle <check> &7- Toggle check");
        send(s, "&b/zac checks &7- List all checks");
    }

    private void sendPlayerInfo(CommandSender s, Player target) {
        PlayerData data = plugin.getPlayerDataManager().get(target);
        send(s, "&bPlayer: &f" + target.getName());
        send(s, "&7Ping: &f" + data.getPing() + "ms");
        send(s, "&7CPS: &f" + String.format("%.1f", data.getCPS()));
        send(s, "&7Reach: &f" + String.format("%.2f", data.getLastReach()));
        data.getAllViolations().forEach((check, vl) -> {
            if (vl > 0) send(s, "  &7" + check + ": &c" + vl + " VL");
        });
    }

    private void sendViolationLog(CommandSender s, Player target) {
        PlayerData data = plugin.getPlayerDataManager().get(target);
        var log = data.getViolationLog();
        if (log.isEmpty()) { send(s, "&7No violations logged."); return; }
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
        log.stream().skip(Math.max(0, log.size() - 10)).forEach(e -> {
            String time = LocalDateTime.ofInstant(Instant.ofEpochMilli(e.timestamp), ZoneId.systemDefault()).format(dtf);
            send(s, "&8[" + time + "] &b" + e.checkName + " &7- " + e.detail);
        });
    }

    private void sendChecks(CommandSender s) {
        plugin.getCheckManager().getChecks().forEach(c ->
                send(s, (c.isEnabled() ? "&a✔ " : "&c✘ ") + "&b" + c.getName() +
                        " &8[" + c.getCategory().name() + "] &7VL:" + c.getVlThreshold()));
    }

    private void send(CommandSender s, String msg) {
        s.sendMessage(color(PREFIX + msg));
    }

    private Component color(String s) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s);
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String alias, String[] args) {
        if (!s.hasPermission("zethac.admin")) return List.of();
        if (args.length == 1) return List.of("reload","alerts","verbose","info","logs","kick","checks","toggle")
                .stream().filter(x -> x.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        if (args.length == 2 && List.of("info","logs","kick").contains(args[0].toLowerCase())) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("toggle")) {
            return plugin.getCheckManager().getChecks().stream().map(Check::getName)
                    .filter(n -> n.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        }
        return List.of();
    }
}
