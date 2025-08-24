package jp.ertl.rfm.core;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public class RunnerState {
    
    public enum Status {
        ALIVE("生存"),
        CAUGHT("確保"),
        SURRENDERED("自首"),
        DISCONNECTED("切断");
        
        private final String displayName;
        
        Status(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    private final UUID uuid;
    private final String playerName;
    private Status status;
    private long currentBounty;
    private long totalEarnedBounty;
    private Location lastKnownLocation;
    private long joinTime;
    private long exitTime;
    
    public RunnerState(Player player) {
        this.uuid = player.getUniqueId();
        this.playerName = player.getName();
        this.status = Status.ALIVE;
        this.currentBounty = 0;
        this.totalEarnedBounty = 0;
        this.lastKnownLocation = player.getLocation();
        this.joinTime = System.currentTimeMillis();
        this.exitTime = 0;
    }
    
    public void addBounty(long amount) {
        if (status == Status.ALIVE) {
            this.currentBounty += amount;
            this.totalEarnedBounty += amount;
        }
    }
    
    public void setStatus(Status status) {
        this.status = status;
        if (status != Status.ALIVE) {
            this.exitTime = System.currentTimeMillis();
        }
    }
    
    public long getSurvivalTime() {
        if (exitTime > 0) {
            return exitTime - joinTime;
        }
        return System.currentTimeMillis() - joinTime;
    }
    
    public UUID getUuid() {
        return uuid;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public Status getStatus() {
        return status;
    }
    
    public long getCurrentBounty() {
        return currentBounty;
    }
    
    public long getTotalEarnedBounty() {
        return totalEarnedBounty;
    }
    
    public Location getLastKnownLocation() {
        return lastKnownLocation;
    }
    
    public void setLastKnownLocation(Location location) {
        this.lastKnownLocation = location;
    }
    
    public boolean isAlive() {
        return status == Status.ALIVE;
    }
}