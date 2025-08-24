package jp.ertl.rfm.hunter;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public class HunterState {
    
    public enum Mode {
        PATROL("巡回"),
        CHASE("追跡"),
        RETURNING("復帰");
        
        private final String displayName;
        
        Mode(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    private final int npcId;
    private Mode mode;
    private UUID targetPlayer;
    private Location lastKnownTargetLocation;
    private Location patrolOrigin;
    private String patrolRouteId;
    
    private double viewDistance;
    private double fieldOfView;
    private double chaseSpeed;
    private double patrolSpeed;
    private double captureRadius;
    
    private long lastScanTime;
    private long chaseStartTime;
    private long loseSightTime;
    
    public HunterState(int npcId) {
        this.npcId = npcId;
        this.mode = Mode.PATROL;
        this.viewDistance = 30.0;
        this.fieldOfView = 70.0;
        this.chaseSpeed = 1.2;
        this.patrolSpeed = 1.0;
        this.captureRadius = 1.5;
        this.lastScanTime = 0;
    }
    
    public void startChase(Player target) {
        this.mode = Mode.CHASE;
        this.targetPlayer = target.getUniqueId();
        this.lastKnownTargetLocation = target.getLocation();
        this.chaseStartTime = System.currentTimeMillis();
        this.loseSightTime = 0;
    }
    
    public void loseSight() {
        if (loseSightTime == 0) {
            loseSightTime = System.currentTimeMillis();
        }
    }
    
    public void regainSight(Location targetLocation) {
        this.lastKnownTargetLocation = targetLocation;
        this.loseSightTime = 0;
    }
    
    public void stopChase() {
        this.mode = Mode.RETURNING;
        this.targetPlayer = null;
        this.lastKnownTargetLocation = null;
        this.chaseStartTime = 0;
        this.loseSightTime = 0;
    }
    
    public void returnToPatrol() {
        this.mode = Mode.PATROL;
        this.targetPlayer = null;
        this.lastKnownTargetLocation = null;
    }
    
    public boolean hasLostSightTooLong() {
        return loseSightTime > 0 && 
               (System.currentTimeMillis() - loseSightTime) > 5000; // 5秒見失ったら諦める
    }
    
    public boolean canScan() {
        long now = System.currentTimeMillis();
        if (now - lastScanTime >= 200) { // 200ms = 4 ticks
            lastScanTime = now;
            return true;
        }
        return false;
    }
    
    // Getters and Setters
    public int getNpcId() {
        return npcId;
    }
    
    public Mode getMode() {
        return mode;
    }
    
    public void setMode(Mode mode) {
        this.mode = mode;
    }
    
    public UUID getTargetPlayer() {
        return targetPlayer;
    }
    
    public Location getLastKnownTargetLocation() {
        return lastKnownTargetLocation;
    }
    
    public Location getPatrolOrigin() {
        return patrolOrigin;
    }
    
    public void setPatrolOrigin(Location patrolOrigin) {
        this.patrolOrigin = patrolOrigin;
    }
    
    public String getPatrolRouteId() {
        return patrolRouteId;
    }
    
    public void setPatrolRouteId(String patrolRouteId) {
        this.patrolRouteId = patrolRouteId;
    }
    
    public double getViewDistance() {
        return viewDistance;
    }
    
    public void setViewDistance(double viewDistance) {
        this.viewDistance = viewDistance;
    }
    
    public double getFieldOfView() {
        return fieldOfView;
    }
    
    public void setFieldOfView(double fieldOfView) {
        this.fieldOfView = fieldOfView;
    }
    
    public double getChaseSpeed() {
        return chaseSpeed;
    }
    
    public void setChaseSpeed(double chaseSpeed) {
        this.chaseSpeed = chaseSpeed;
    }
    
    public double getPatrolSpeed() {
        return patrolSpeed;
    }
    
    public void setPatrolSpeed(double patrolSpeed) {
        this.patrolSpeed = patrolSpeed;
    }
    
    public double getCaptureRadius() {
        return captureRadius;
    }
    
    public void setCaptureRadius(double captureRadius) {
        this.captureRadius = captureRadius;
    }
}