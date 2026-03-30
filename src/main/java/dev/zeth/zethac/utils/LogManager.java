package dev.zeth.zethac.utils;

import dev.zeth.zethac.ZethAC;
import org.bukkit.entity.Player;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogManager {
    private final ZethAC plugin;
    private BufferedWriter writer;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public LogManager(ZethAC plugin) {
        this.plugin = plugin;
        if (plugin.getConfig().getBoolean("log-to-file", true)) {
            String path = plugin.getConfig().getString("log-file", "logs/zethac.log");
            File file = new File(plugin.getDataFolder(), path);
            file.getParentFile().mkdirs();
            try {
                writer = new BufferedWriter(new FileWriter(file, true));
            } catch (IOException e) {
                plugin.getLogger().warning("Could not open log file: " + e.getMessage());
            }
        }
    }

    public void logViolation(Player player, String check, String detail, int vl) {
        if (writer == null) return;
        String line = String.format("[%s] %s | %s | VL:%d | %s",
                LocalDateTime.now().format(dtf), player.getName(), check, vl, detail);
        try { writer.write(line); writer.newLine(); writer.flush(); }
        catch (IOException ignored) {}
    }

    public void close() {
        if (writer != null) try { writer.close(); } catch (IOException ignored) {}
    }
}
