package jp.ertl.rfm.core;

import jp.ertl.rfm.RunForMoneyPlugin;
import jp.ertl.rfm.api.events.GameEndEvent;
import jp.ertl.rfm.api.events.GameStartEvent;
import jp.ertl.rfm.api.events.PlayerCaughtEvent;
import jp.ertl.rfm.api.events.PlayerSurrenderEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.UUID;

public class GameManager implements Listener {
    
    private final RunForMoneyPlugin plugin;
    private GameSession currentSession;
    private BukkitTask gameTickTask;
    
    public GameManager(RunForMoneyPlugin plugin) {
        this.plugin = plugin;
    }
    
    public boolean startGame() {
        if (currentSession != null && currentSession.getState().isActive()) {
            return false;
        }
        
        String sessionId = UUID.randomUUID().toString();
        currentSession = new GameSession(sessionId);
        
        loadConfiguration();
        
        currentSession.setState(GameState.LOBBY);
        
        Bukkit.getOnlinePlayers().forEach(player -> {
            if (player.hasPermission("rfm.player.join")) {
                currentSession.addRunner(player);
            }
        });
        
        startCountdown();
        
        return true;
    }
    
    private void startCountdown() {
        currentSession.setState(GameState.COUNTDOWN);
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (currentSession != null && currentSession.getState() == GameState.COUNTDOWN) {
                actuallyStartGame();
            }
        }, 200L); // 10秒のカウントダウン
        
        // カウントダウン表示
        for (int i = 10; i > 0; i--) {
            final int count = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Bukkit.getOnlinePlayers().forEach(player -> {
                    player.showTitle(Title.title(
                        Component.text(String.valueOf(count), NamedTextColor.GOLD),
                        Component.text("ゲーム開始まで", NamedTextColor.YELLOW),
                        Title.Times.times(
                            Duration.ofMillis(250),
                            Duration.ofMillis(500),
                            Duration.ofMillis(250)
                        )
                    ));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                });
            }, (10 - i) * 20L);
        }
    }
    
    private void actuallyStartGame() {
        currentSession.start();
        
        Bukkit.getPluginManager().callEvent(new GameStartEvent(currentSession));
        
        gameTickTask = new GameTickTask(plugin, this).runTaskTimer(plugin, 0L, 20L);
        
        plugin.getUIManager().setupUI(currentSession);
        plugin.getHunterManager().spawnInitialHunters(currentSession);
        
        Bukkit.getOnlinePlayers().forEach(player -> {
            player.showTitle(Title.title(
                Component.text("逃走開始！", NamedTextColor.RED),
                Component.text("ハンターから逃げ切れ！", NamedTextColor.YELLOW),
                Title.Times.times(
                    Duration.ofMillis(500),
                    Duration.ofSeconds(2),
                    Duration.ofMillis(500)
                )
            ));
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.0f);
        });
    }
    
    public void endGame() {
        if (currentSession == null) return;
        
        currentSession.end();
        
        if (gameTickTask != null) {
            gameTickTask.cancel();
            gameTickTask = null;
        }
        
        plugin.getUIManager().clearUI();
        plugin.getHunterManager().removeAllHunters();
        
        showResults();
        
        Bukkit.getPluginManager().callEvent(new GameEndEvent(currentSession));
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            processRewards();
            currentSession = null;
        }, 200L); // 10秒後にセッションをクリア
    }
    
    private void showResults() {
        currentSession.setState(GameState.RESULT);
        
        Component title = Component.text("ゲーム終了！", NamedTextColor.GOLD);
        Component subtitle = Component.text("生存者: " + currentSession.getAliveRunnersCount() + 
                                          " / " + currentSession.getTotalRunnersCount(), NamedTextColor.YELLOW);
        
        Bukkit.getOnlinePlayers().forEach(player -> {
            player.showTitle(Title.title(
                title,
                subtitle,
                Title.Times.times(
                    Duration.ofMillis(500),
                    Duration.ofSeconds(5),
                    Duration.ofMillis(1000)
                )
            ));
            
            RunnerState runner = currentSession.getRunner(player.getUniqueId());
            if (runner != null) {
                player.sendMessage(Component.text(""));
                player.sendMessage(Component.text("=== ゲーム結果 ===", NamedTextColor.GOLD));
                player.sendMessage(Component.text("状態: " + runner.getStatus().getDisplayName(), NamedTextColor.YELLOW));
                player.sendMessage(Component.text("獲得賞金: " + formatMoney(runner.getCurrentBounty()), NamedTextColor.GREEN));
                player.sendMessage(Component.text("生存時間: " + formatTime(runner.getSurvivalTime()), NamedTextColor.AQUA));
                player.sendMessage(Component.text(""));
            }
        });
    }
    
    private void processRewards() {
        if (plugin.getVaultAdapter() == null) return;
        
        currentSession.getAllRunners().forEach(runner -> {
            if (runner.getCurrentBounty() > 0) {
                Player player = Bukkit.getPlayer(runner.getUuid());
                if (player != null) {
                    double amount = runner.getCurrentBounty() / 100.0; // 円をサーバー通貨に変換
                    plugin.getVaultAdapter().deposit(player, amount);
                    player.sendMessage(Component.text("賞金 " + formatMoney(runner.getCurrentBounty()) + 
                                                     " が口座に振り込まれました！", NamedTextColor.GREEN));
                }
            }
        });
    }
    
    public void pauseGame() {
        if (currentSession != null && currentSession.getState() == GameState.RUNNING) {
            currentSession.pause();
            Bukkit.broadcast(Component.text("ゲームが一時停止されました", NamedTextColor.YELLOW));
        }
    }
    
    public void resumeGame() {
        if (currentSession != null && currentSession.getState() == GameState.PAUSED) {
            currentSession.resume();
            Bukkit.broadcast(Component.text("ゲームが再開されました", NamedTextColor.GREEN));
        }
    }
    
    public void forceStop() {
        if (currentSession != null) {
            endGame();
            Bukkit.broadcast(Component.text("ゲームが強制終了されました", NamedTextColor.RED));
        }
    }
    
    public void handlePlayerCaught(Player player, int hunterId) {
        if (currentSession == null) return;
        
        RunnerState runner = currentSession.getRunner(player.getUniqueId());
        if (runner != null && runner.isAlive()) {
            runner.setStatus(RunnerState.Status.CAUGHT);
            
            PlayerCaughtEvent event = new PlayerCaughtEvent(player, hunterId, runner.getCurrentBounty());
            Bukkit.getPluginManager().callEvent(event);
            
            if (!event.isCancelled()) {
                runner.setStatus(RunnerState.Status.CAUGHT);
                player.setGameMode(org.bukkit.GameMode.SPECTATOR);
                
                Bukkit.broadcast(Component.text(player.getName() + " が確保されました！", NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_HURT, 1.0f, 0.5f);
                
                checkGameEnd();
            }
        }
    }
    
    public void handlePlayerSurrender(Player player) {
        if (currentSession == null) return;
        
        RunnerState runner = currentSession.getRunner(player.getUniqueId());
        if (runner != null && runner.isAlive()) {
            PlayerSurrenderEvent event = new PlayerSurrenderEvent(player, runner.getCurrentBounty());
            Bukkit.getPluginManager().callEvent(event);
            
            if (!event.isCancelled()) {
                runner.setStatus(RunnerState.Status.SURRENDERED);
                player.setGameMode(org.bukkit.GameMode.SPECTATOR);
                
                Bukkit.broadcast(Component.text(player.getName() + " が自首しました", NamedTextColor.YELLOW));
                player.sendMessage(Component.text("賞金 " + formatMoney(runner.getCurrentBounty()) + 
                                                 " を確定させました", NamedTextColor.GREEN));
                
                checkGameEnd();
            }
        }
    }
    
    private void checkGameEnd() {
        if (currentSession.getAliveRunnersCount() == 0) {
            endGame();
        }
    }
    
    private void loadConfiguration() {
        var config = plugin.getConfig();
        currentSession.setTotalDuration(config.getInt("game.duration", 600));
        currentSession.setBountyPerSecond(config.getLong("game.bounty-per-second", 100));
        currentSession.setMaxBounty(config.getLong("game.max-bounty", 1000000));
        currentSession.setAreaId(config.getString("game.area-id", "default"));
    }
    
    private String formatMoney(long amount) {
        return String.format("¥%,d", amount);
    }
    
    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d分%d秒", minutes, seconds);
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (currentSession != null && currentSession.getState().canJoin()) {
            currentSession.addRunner(event.getPlayer());
            event.getPlayer().sendMessage(Component.text("ゲームに参加しました！", NamedTextColor.GREEN));
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (currentSession != null) {
            RunnerState runner = currentSession.getRunner(event.getPlayer().getUniqueId());
            if (runner != null && runner.isAlive()) {
                runner.setStatus(RunnerState.Status.DISCONNECTED);
                checkGameEnd();
            }
        }
    }
    
    public GameSession getCurrentSession() {
        return currentSession;
    }
}