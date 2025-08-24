package jp.ertl.rfm.placeholder;

import jp.ertl.rfm.RunForMoneyPlugin;
import jp.ertl.rfm.core.GameSession;
import jp.ertl.rfm.core.RunnerState;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RFMPlaceholderExpansion extends PlaceholderExpansion {
    
    private final RunForMoneyPlugin plugin;
    
    public RFMPlaceholderExpansion(RunForMoneyPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public @NotNull String getIdentifier() {
        return "rfm";
    }
    
    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }
    
    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        GameSession session = plugin.getGameManager().getCurrentSession();
        
        if (session == null) {
            return getDefaultValue(params);
        }
        
        RunnerState runner = player != null ? session.getRunner(player.getUniqueId()) : null;
        
        switch (params.toLowerCase()) {
            case "state":
                return session.getState().getDisplayName();
                
            case "timeleft":
            case "time_left":
                return session.getFormattedTime();
                
            case "timeleft_seconds":
                return String.valueOf(session.getRemainingTime());
                
            case "bounty":
                return runner != null ? formatMoney(runner.getCurrentBounty()) : "0";
                
            case "bounty_raw":
                return runner != null ? String.valueOf(runner.getCurrentBounty()) : "0";
                
            case "bounty_per_second":
                return String.valueOf(session.getBountyPerSecond());
                
            case "status":
                return runner != null ? runner.getStatus().getDisplayName() : "観戦";
                
            case "hunters":
            case "hunter_count":
                return String.valueOf(session.getHunterCount());
                
            case "runners":
            case "runner_count":
                return String.valueOf(session.getAliveRunnersCount());
                
            case "runners_total":
                return String.valueOf(session.getTotalRunnersCount());
                
            case "survival_time":
                if (runner != null) {
                    long seconds = runner.getSurvivalTime() / 1000;
                    return String.format("%d:%02d", seconds / 60, seconds % 60);
                }
                return "0:00";
                
            case "session_id":
                return session.getSessionId();
                
            case "area":
            case "area_id":
                return session.getAreaId() != null ? session.getAreaId() : "default";
                
            default:
                return null;
        }
    }
    
    private String getDefaultValue(String params) {
        switch (params.toLowerCase()) {
            case "state":
                return "待機中";
            case "timeleft":
            case "time_left":
                return "--:--";
            case "timeleft_seconds":
            case "bounty":
            case "bounty_raw":
            case "bounty_per_second":
            case "hunters":
            case "hunter_count":
            case "runners":
            case "runner_count":
            case "runners_total":
                return "0";
            case "status":
                return "---";
            case "survival_time":
                return "0:00";
            case "session_id":
            case "area":
            case "area_id":
                return "---";
            default:
                return null;
        }
    }
    
    private String formatMoney(long amount) {
        if (amount >= 1000000) {
            return String.format("%.1fM", amount / 1000000.0);
        } else if (amount >= 1000) {
            return String.format("%.1fK", amount / 1000.0);
        } else {
            return String.valueOf(amount);
        }
    }
}