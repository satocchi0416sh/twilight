package jp.ertl.rfm.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PlayerSurrenderEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final long bountySecured;
    private boolean cancelled;
    
    public PlayerSurrenderEvent(Player player, long bountySecured) {
        this.player = player;
        this.bountySecured = bountySecured;
        this.cancelled = false;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public long getBountySecured() {
        return bountySecured;
    }
    
    @Override
    public boolean isCancelled() {
        return cancelled;
    }
    
    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
    
    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
}