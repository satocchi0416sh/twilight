package jp.ertl.rfm.api.events;

import jp.ertl.rfm.core.GameSession;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class GameStartEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final GameSession session;
    
    public GameStartEvent(GameSession session) {
        this.session = session;
    }
    
    public GameSession getSession() {
        return session;
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