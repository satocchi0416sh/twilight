package jp.ertl.rfm.region;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import jp.ertl.rfm.RunForMoneyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.logging.Level;

public class WorldGuardAdapter {
    
    private final RunForMoneyPlugin plugin;
    private WorldGuardPlugin worldGuard;
    
    public WorldGuardAdapter(RunForMoneyPlugin plugin) {
        this.plugin = plugin;
        
        try {
            this.worldGuard = (WorldGuardPlugin) Bukkit.getPluginManager().getPlugin("WorldGuard");
            if (worldGuard != null) {
                plugin.getLogger().info("WorldGuard連携が有効になりました");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "WorldGuard連携の初期化に失敗しました", e);
        }
    }
    
    public boolean isInRegion(Player player, String regionName) {
        if (worldGuard == null) return false;
        
        try {
            Location loc = player.getLocation();
            RegionManager regionManager = getRegionManager(loc.getWorld());
            if (regionManager == null) return false;
            
            ProtectedRegion region = regionManager.getRegion(regionName);
            if (region == null) return false;
            
            return region.contains(BukkitAdapter.asBlockVector(loc));
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "リージョンチェック中にエラーが発生しました", e);
            return false;
        }
    }
    
    public void setRegionFlag(String regionName, String flagName, String value) {
        if (worldGuard == null) return;
        
        try {
            for (World world : Bukkit.getWorlds()) {
                RegionManager regionManager = getRegionManager(world);
                if (regionManager == null) continue;
                
                ProtectedRegion region = regionManager.getRegion(regionName);
                if (region == null) continue;
                
                Flag<?> flag = WorldGuard.getInstance().getFlagRegistry().get(flagName);
                if (flag instanceof StateFlag stateFlag) {
                    StateFlag.State state = StateFlag.State.valueOf(value.toUpperCase());
                    region.setFlag(stateFlag, state);
                    plugin.getLogger().info("リージョン " + regionName + " のフラグ " + flagName + 
                                          " を " + value + " に設定しました");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "フラグ設定中にエラーが発生しました", e);
        }
    }
    
    public void denyEntry(String regionName) {
        setRegionFlag(regionName, "entry", "deny");
    }
    
    public void allowEntry(String regionName) {
        setRegionFlag(regionName, "entry", "allow");
    }
    
    public void setMessage(String regionName, String type, String message) {
        if (worldGuard == null) return;
        
        try {
            for (World world : Bukkit.getWorlds()) {
                RegionManager regionManager = getRegionManager(world);
                if (regionManager == null) continue;
                
                ProtectedRegion region = regionManager.getRegion(regionName);
                if (region == null) continue;
                
                Flag<?> flag = WorldGuard.getInstance().getFlagRegistry().get(type + "-message");
                if (flag != null) {
                    region.setFlag(flag, message);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "メッセージ設定中にエラーが発生しました", e);
        }
    }
    
    private RegionManager getRegionManager(World world) {
        if (worldGuard == null || world == null) return null;
        
        try {
            return WorldGuard.getInstance().getPlatform().getRegionContainer()
                .get(BukkitAdapter.adapt(world));
        } catch (Exception e) {
            return null;
        }
    }
    
    public boolean isAvailable() {
        return worldGuard != null;
    }
}