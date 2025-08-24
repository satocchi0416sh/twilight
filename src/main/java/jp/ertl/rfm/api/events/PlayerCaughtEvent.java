package jp.ertl.rfm.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PlayerCaughtEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final int hunterId;
    private final long bountyLost;
    private boolean cancelled;
    
    public PlayerCaughtEvent(Player player, int hunterId, long bountyLost) {
        this.player = player;
        this.hunterId = hunterId;
        this.bountyLost = bountyLost;
        this.cancelled = false;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public int getHunterId() {
        return hunterId;
    }
    
    public long getBountyLost() {
        return bountyLost;
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