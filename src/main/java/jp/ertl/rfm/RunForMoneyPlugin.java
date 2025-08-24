package jp.ertl.rfm;

import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import jp.ertl.rfm.admin.AdminCommandHandler;
import jp.ertl.rfm.core.GameManager;
import jp.ertl.rfm.core.GameSession;
import jp.ertl.rfm.core.GameTickTask;
import jp.ertl.rfm.econ.VaultAdapter;
import jp.ertl.rfm.hunter.HunterManager;
import jp.ertl.rfm.mission.MissionEngine;
import jp.ertl.rfm.placeholder.RFMPlaceholderExpansion;
import jp.ertl.rfm.region.WorldGuardAdapter;
import jp.ertl.rfm.storage.StorageManager;
import jp.ertl.rfm.ui.UIManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class RunForMoneyPlugin extends JavaPlugin {
    
    private static RunForMoneyPlugin instance;
    
    private GameManager gameManager;
    private UIManager uiManager;
    private HunterManager hunterManager;
    private MissionEngine missionEngine;
    private StorageManager storageManager;
    private VaultAdapter vaultAdapter;
    private WorldGuardAdapter worldGuardAdapter;
    private AdminCommandHandler adminCommandHandler;
    
    @Override
    public void onEnable() {
        instance = this;
        
        saveDefaultConfig();
        
        if (!checkDependencies()) {
            getLogger().severe("必要な依存プラグインが見つかりません！無効化します...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        
        initializeManagers();
        registerCommands();
        registerListeners();
        registerPlaceholders();
        
        getLogger().info("RunForMoney プラグインが有効化されました！");
    }
    
    @Override
    public void onDisable() {
        if (gameManager != null && gameManager.getCurrentSession() != null) {
            gameManager.forceStop();
        }
        
        if (storageManager != null) {
            storageManager.close();
        }
        
        getLogger().info("RunForMoney プラグインが無効化されました。");
    }
    
    private boolean checkDependencies() {
        boolean allPresent = true;
        
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null) {
            getLogger().warning("WorldGuard が見つかりません - エリア制御機能は無効です");
        }
        
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("Vault が見つかりません - 経済機能は無効です");
        }
        
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().warning("PlaceholderAPI が見つかりません - プレースホルダー機能は無効です");
        }
        
        if (Bukkit.getPluginManager().getPlugin("Citizens") == null) {
            getLogger().warning("Citizens が見つかりません - NPC機能は無効です");
        }
        
        if (Bukkit.getPluginManager().getPlugin("DecentHolograms") == null) {
            getLogger().warning("DecentHolograms が見つかりません - ホログラム機能は無効です");
        }
        
        return allPresent;
    }
    
    private void initializeManagers() {
        storageManager = new StorageManager(this);
        storageManager.initialize();
        
        gameManager = new GameManager(this);
        uiManager = new UIManager(this);
        hunterManager = new HunterManager(this);
        missionEngine = new MissionEngine(this);
        
        Plugin vault = Bukkit.getPluginManager().getPlugin("Vault");
        if (vault != null) {
            vaultAdapter = new VaultAdapter(this);
            vaultAdapter.initialize();
        }
        
        Plugin worldGuard = Bukkit.getPluginManager().getPlugin("WorldGuard");
        if (worldGuard != null) {
            worldGuardAdapter = new WorldGuardAdapter(this);
        }
        
        adminCommandHandler = new AdminCommandHandler(this);
    }
    
    private void registerCommands() {
        var command = getCommand("rfm");
        if (command != null) {
            command.setExecutor(adminCommandHandler);
            command.setTabCompleter(adminCommandHandler);
        }
    }
    
    private void registerListeners() {
        var pm = Bukkit.getPluginManager();
        pm.registerEvents(gameManager, this);
        pm.registerEvents(uiManager, this);
        pm.registerEvents(hunterManager, this);
    }
    
    private void registerPlaceholders() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new RFMPlaceholderExpansion(this).register();
        }
    }
    
    public static RunForMoneyPlugin getInstance() {
        return instance;
    }
    
    public GameManager getGameManager() {
        return gameManager;
    }
    
    public UIManager getUIManager() {
        return uiManager;
    }
    
    public HunterManager getHunterManager() {
        return hunterManager;
    }
    
    public MissionEngine getMissionEngine() {
        return missionEngine;
    }
    
    public StorageManager getStorageManager() {
        return storageManager;
    }
    
    public VaultAdapter getVaultAdapter() {
        return vaultAdapter;
    }
    
    public WorldGuardAdapter getWorldGuardAdapter() {
        return worldGuardAdapter;
    }
}