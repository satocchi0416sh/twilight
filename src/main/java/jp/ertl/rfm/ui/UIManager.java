package jp.ertl.rfm.ui;

import jp.ertl.rfm.RunForMoneyPlugin;
import jp.ertl.rfm.core.GameSession;
import jp.ertl.rfm.core.RunnerState;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UIManager implements Listener {
    
    private final RunForMoneyPlugin plugin;
    private final Map<UUID, Scoreboard> playerScoreboards;
    private final Map<UUID, BossBar> playerBossBars;
    private String currentMissionText = "";
    
    public UIManager(RunForMoneyPlugin plugin) {
        this.plugin = plugin;
        this.playerScoreboards = new HashMap<>();
        this.playerBossBars = new HashMap<>();
    }
    
    public void setupUI(GameSession session) {
        Bukkit.getOnlinePlayers().forEach(player -> {
            setupScoreboard(player, session);
            setupBossBar(player, session);
        });
    }
    
    private void setupScoreboard(Player player, GameSession session) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard board = manager.getNewScoreboard();
        
        Objective objective = board.registerNewObjective(
            "rfm_sidebar",
            Criteria.DUMMY,
            Component.text("RUN FOR MONEY", NamedTextColor.GOLD, TextDecoration.BOLD)
        );
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        
        updateScoreboard(player, board, session);
        
        player.setScoreboard(board);
        playerScoreboards.put(player.getUniqueId(), board);
    }
    
    private void updateScoreboard(Player player, Scoreboard board, GameSession session) {
        Objective objective = board.getObjective("rfm_sidebar");
        if (objective == null) return;
        
        // 既存のエントリをクリア
        board.getEntries().forEach(board::resetScores);
        
        RunnerState runner = session.getRunner(player.getUniqueId());
        
        // スコアボードの内容を設定（下から上の順）
        int score = 0;
        
        // 空行
        objective.getScore("§r").setScore(score++);
        
        // ミッション情報
        if (!currentMissionText.isEmpty()) {
            objective.getScore("§e" + currentMissionText).setScore(score++);
            objective.getScore("§6§lMISSION").setScore(score++);
            objective.getScore("§f").setScore(score++);
        }
        
        // 逃走者数
        String runnersText = String.format("§a%d§f/§7%d", 
            session.getAliveRunnersCount(), 
            session.getTotalRunnersCount());
        objective.getScore(runnersText).setScore(score++);
        objective.getScore("§b逃走者").setScore(score++);
        objective.getScore("§1").setScore(score++);
        
        // ハンター数
        objective.getScore("§c" + session.getHunterCount()).setScore(score++);
        objective.getScore("§4ハンター").setScore(score++);
        objective.getScore("§2").setScore(score++);
        
        // 現在の賞金
        String bountyText = runner != null ? 
            formatMoney(runner.getCurrentBounty()) : "§7---";
        objective.getScore("§e" + bountyText).setScore(score++);
        objective.getScore("§6現賞金").setScore(score++);
        objective.getScore("§3").setScore(score++);
        
        // 残り時間
        objective.getScore("§b" + session.getFormattedTime()).setScore(score++);
        objective.getScore("§9残り時間").setScore(score++);
    }
    
    private void setupBossBar(Player player, GameSession session) {
        BossBar bossBar = BossBar.bossBar(
            Component.text("残り時間: " + session.getFormattedTime(), NamedTextColor.YELLOW),
            1.0f,
            BossBar.Color.YELLOW,
            BossBar.Overlay.PROGRESS
        );
        
        player.showBossBar(bossBar);
        playerBossBars.put(player.getUniqueId(), bossBar);
    }
    
    public void updateUI(GameSession session) {
        Bukkit.getOnlinePlayers().forEach(player -> {
            updatePlayerScoreboard(player, session);
            updatePlayerBossBar(player, session);
        });
    }
    
    private void updatePlayerScoreboard(Player player, GameSession session) {
        Scoreboard board = playerScoreboards.get(player.getUniqueId());
        if (board != null) {
            updateScoreboard(player, board, session);
        }
    }
    
    private void updatePlayerBossBar(Player player, GameSession session) {
        BossBar bossBar = playerBossBars.get(player.getUniqueId());
        if (bossBar != null) {
            float progress = (float) session.getRemainingTime() / session.getTotalDuration();
            bossBar.progress(Math.max(0, Math.min(1, progress)));
            bossBar.name(Component.text("残り時間: " + session.getFormattedTime(), NamedTextColor.YELLOW));
            
            // 残り時間に応じて色を変更
            if (session.getRemainingTime() <= 30) {
                bossBar.color(BossBar.Color.RED);
            } else if (session.getRemainingTime() <= 60) {
                bossBar.color(BossBar.Color.PINK);
            } else {
                bossBar.color(BossBar.Color.YELLOW);
            }
        }
    }
    
    public void showMissionBossBar(String missionName, int duration) {
        Component missionText = Component.text("MISSION: " + missionName, NamedTextColor.GOLD, TextDecoration.BOLD);
        
        playerBossBars.values().forEach(bossBar -> {
            bossBar.name(missionText);
            bossBar.color(BossBar.Color.PURPLE);
            bossBar.overlay(BossBar.Overlay.NOTCHED_20);
        });
        
        currentMissionText = missionName;
        
        // 指定時間後に通常表示に戻す
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            currentMissionText = "";
            GameSession session = plugin.getGameManager().getCurrentSession();
            if (session != null) {
                updateUI(session);
            }
        }, duration * 20L);
    }
    
    public void clearUI() {
        playerBossBars.forEach((uuid, bossBar) -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.hideBossBar(bossBar);
            }
        });
        playerBossBars.clear();
        
        playerScoreboards.forEach((uuid, scoreboard) -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
        });
        playerScoreboards.clear();
    }
    
    private String formatMoney(long amount) {
        if (amount >= 1000000) {
            return String.format("¥%.1fM", amount / 1000000.0);
        } else if (amount >= 1000) {
            return String.format("¥%.1fK", amount / 1000.0);
        } else {
            return String.format("¥%d", amount);
        }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        GameSession session = plugin.getGameManager().getCurrentSession();
        if (session != null && session.getState().isActive()) {
            setupScoreboard(event.getPlayer(), session);
            setupBossBar(event.getPlayer(), session);
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        
        BossBar bossBar = playerBossBars.remove(uuid);
        if (bossBar != null) {
            event.getPlayer().hideBossBar(bossBar);
        }
        
        playerScoreboards.remove(uuid);
    }
}