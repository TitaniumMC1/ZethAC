package dev.zeth.zethac.api;

import dev.zeth.zethac.data.PlayerData;
import dev.zeth.zethac.manager.Check;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ViolationEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final PlayerData playerData;
    private final Check check;
    private final String detail;
    private final int violationLevel;
    private boolean cancelled;

    public ViolationEvent(Player player, PlayerData playerData, Check check, String detail, int vl) {
        this.player = player; this.playerData = playerData;
        this.check = check; this.detail = detail; this.violationLevel = vl;
    }

    public Player     getPlayer()        { return player; }
    public PlayerData getPlayerData()    { return playerData; }
    public Check      getCheck()         { return check; }
    public String     getDetail()        { return detail; }
    public int        getViolationLevel(){ return violationLevel; }
    public boolean    isCancelled()      { return cancelled; }
    public void       setCancelled(boolean v) { cancelled = v; }
    public HandlerList getHandlers()     { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
