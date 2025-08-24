package jp.ertl.rfm.core;

import jp.ertl.rfm.RunForMoneyPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class GameTickTask extends BukkitRunnable {
    
    private final RunForMoneyPlugin plugin;
    private final GameManager gameManager;
    
    public GameTickTask(RunForMoneyPlugin plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }
    
    @Override
    public void run() {
        GameSession session = gameManager.getCurrentSession();
        if (session == null || session.getState() != GameState.RUNNING) {
            return;
        }
        
        // 時間を減らす
        session.decrementTime();
        
        // 賞金を配布
        session.distributeBounty();
        
        // UIを更新
        plugin.getUIManager().updateUI(session);
        
        // ハンターの視認判定
        plugin.getHunterManager().performScanTick(session);
        
        // ミッションのチェック
        plugin.getMissionEngine().checkMissions(session);
        
        // 時間切れチェック
        if (session.getRemainingTime() <= 0) {
            gameManager.endGame();
        }
    }
}