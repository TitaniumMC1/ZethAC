package dev.zeth.zethac.data;

import dev.zeth.zethac.ZethAC;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager implements Listener {
    private final ZethAC plugin;
    private final Map<UUID, PlayerData> dataMap = new ConcurrentHashMap<>();

    public PlayerDataManager(ZethAC plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        // Register already-online players (reload scenario)
        plugin.getServer().getOnlinePlayers().forEach(p -> dataMap.put(p.getUniqueId(), new PlayerData(p)));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        dataMap.put(e.getPlayer().getUniqueId(), new PlayerData(e.getPlayer()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        dataMap.remove(e.getPlayer().getUniqueId());
    }

    public PlayerData get(Player player) {
        return dataMap.computeIfAbsent(player.getUniqueId(), k -> new PlayerData(player));
    }

    public Collection<PlayerData> getAll()  { return dataMap.values(); }
    public void saveAll() { /* future: persist to disk */ }
}
