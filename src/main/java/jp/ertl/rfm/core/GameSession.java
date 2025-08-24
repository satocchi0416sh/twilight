package jp.ertl.rfm.core;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class GameSession {
    
    private final String sessionId;
    private GameState state;
    private final Map<UUID, RunnerState> runners;
    private final Set<Integer> hunterIds;
    
    private long startTime;
    private long endTime;
    private long pausedTime;
    private long totalPausedDuration;
    
    private int totalDuration; // 秒
    private int remainingTime; // 秒
    private long bountyPerSecond;
    private long maxBounty;
    
    private String areaId;
    private String rulesetId;
    
    public GameSession(String sessionId) {
        this.sessionId = sessionId;
        this.state = GameState.IDLE;
        this.runners = new ConcurrentHashMap<>();
        this.hunterIds = Collections.synchronizedSet(new HashSet<>());
        
        this.totalDuration = 600; // デフォルト10分
        this.remainingTime = totalDuration;
        this.bountyPerSecond = 100; // デフォルト100円/秒
        this.maxBounty = 1000000; // デフォルト100万円上限
    }
    
    public void addRunner(Player player) {
        if (!runners.containsKey(player.getUniqueId())) {
            runners.put(player.getUniqueId(), new RunnerState(player));
        }
    }
    
    public void removeRunner(UUID uuid) {
        runners.remove(uuid);
    }
    
    public RunnerState getRunner(UUID uuid) {
        return runners.get(uuid);
    }
    
    public void addHunter(int npcId) {
        hunterIds.add(npcId);
    }
    
    public void removeHunter(int npcId) {
        hunterIds.remove(npcId);
    }
    
    public void start() {
        this.state = GameState.RUNNING;
        this.startTime = System.currentTimeMillis();
        this.remainingTime = totalDuration;
    }
    
    public void pause() {
        if (state == GameState.RUNNING) {
            this.state = GameState.PAUSED;
            this.pausedTime = System.currentTimeMillis();
        }
    }
    
    public void resume() {
        if (state == GameState.PAUSED) {
            this.state = GameState.RUNNING;
            long pauseDuration = System.currentTimeMillis() - pausedTime;
            this.totalPausedDuration += pauseDuration;
            this.pausedTime = 0;
        }
    }
    
    public void end() {
        this.state = GameState.ENDING;
        this.endTime = System.currentTimeMillis();
    }
    
    public void decrementTime() {
        if (state == GameState.RUNNING && remainingTime > 0) {
            remainingTime--;
        }
    }
    
    public void distributeBounty() {
        if (state == GameState.RUNNING) {
            long amount = Math.min(bountyPerSecond, maxBounty);
            runners.values().stream()
                .filter(RunnerState::isAlive)
                .forEach(runner -> runner.addBounty(amount));
        }
    }
    
    public int getAliveRunnersCount() {
        return (int) runners.values().stream()
            .filter(RunnerState::isAlive)
            .count();
    }
    
    public int getTotalRunnersCount() {
        return runners.size();
    }
    
    public int getHunterCount() {
        return hunterIds.size();
    }
    
    public List<RunnerState> getAliveRunners() {
        return runners.values().stream()
            .filter(RunnerState::isAlive)
            .collect(Collectors.toList());
    }
    
    public Collection<RunnerState> getAllRunners() {
        return runners.values();
    }
    
    public String getFormattedTime() {
        int minutes = remainingTime / 60;
        int seconds = remainingTime % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
    
    // Getters and Setters
    public String getSessionId() {
        return sessionId;
    }
    
    public GameState getState() {
        return state;
    }
    
    public void setState(GameState state) {
        this.state = state;
    }
    
    public int getRemainingTime() {
        return remainingTime;
    }
    
    public void setRemainingTime(int remainingTime) {
        this.remainingTime = remainingTime;
    }
    
    public int getTotalDuration() {
        return totalDuration;
    }
    
    public void setTotalDuration(int totalDuration) {
        this.totalDuration = totalDuration;
        this.remainingTime = totalDuration;
    }
    
    public long getBountyPerSecond() {
        return bountyPerSecond;
    }
    
    public void setBountyPerSecond(long bountyPerSecond) {
        this.bountyPerSecond = bountyPerSecond;
    }
    
    public long getMaxBounty() {
        return maxBounty;
    }
    
    public void setMaxBounty(long maxBounty) {
        this.maxBounty = maxBounty;
    }
    
    public String getAreaId() {
        return areaId;
    }
    
    public void setAreaId(String areaId) {
        this.areaId = areaId;
    }
    
    public String getRulesetId() {
        return rulesetId;
    }
    
    public void setRulesetId(String rulesetId) {
        this.rulesetId = rulesetId;
    }
}