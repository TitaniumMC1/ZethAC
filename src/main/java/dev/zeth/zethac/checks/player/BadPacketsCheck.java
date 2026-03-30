package dev.zeth.zethac.checks.player;

import dev.zeth.zethac.ZethAC;
import dev.zeth.zethac.data.PlayerData;
import dev.zeth.zethac.manager.Check;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.meta.BookMeta;

public class BadPacketsCheck extends Check {
    public BadPacketsCheck(ZethAC plugin) { super(plugin, "badpackets", CheckCategory.PLAYER); }

    @Override
    public void onTick(PlayerData data) {
        float pitch = data.getPitch();
        if (pitch > 90.5f || pitch < -90.5f) {
            flag(data, String.format("Invalid pitch [%.2f]", pitch)); return;
        }
        decay(data, 2);
    }

    public void checkBook(PlayerData data, PlayerEditBookEvent event) {
        int maxPages = plugin.getConfig().getInt("checks.badpackets.max-book-pages", 50);
        BookMeta meta = event.getNewBookMeta();
        if (meta.getPageCount() > maxPages) {
            flag(data, "BookOverflow [pages=" + meta.getPageCount() + "]");
        }
    }
}
