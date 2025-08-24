package jp.ertl.rfm.econ;

import jp.ertl.rfm.RunForMoneyPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.logging.Level;

public class VaultAdapter {
    
    private final RunForMoneyPlugin plugin;
    private Economy economy;
    
    public VaultAdapter(RunForMoneyPlugin plugin) {
        this.plugin = plugin;
    }
    
    public boolean initialize() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault が見つかりません");
            return false;
        }
        
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("エコノミープラグインが見つかりません");
            return false;
        }
        
        economy = rsp.getProvider();
        plugin.getLogger().info("Vault経済連携が有効になりました: " + economy.getName());
        return true;
    }
    
    public boolean deposit(Player player, double amount) {
        if (economy == null) return false;
        
        try {
            var response = economy.depositPlayer(player, amount);
            if (response.transactionSuccess()) {
                plugin.getLogger().info(player.getName() + " に " + amount + " を入金しました");
                return true;
            } else {
                plugin.getLogger().warning("入金に失敗: " + response.errorMessage);
                return false;
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "入金処理中にエラーが発生しました", e);
            return false;
        }
    }
    
    public boolean withdraw(Player player, double amount) {
        if (economy == null) return false;
        
        try {
            var response = economy.withdrawPlayer(player, amount);
            if (response.transactionSuccess()) {
                plugin.getLogger().info(player.getName() + " から " + amount + " を引き出しました");
                return true;
            } else {
                plugin.getLogger().warning("引き出しに失敗: " + response.errorMessage);
                return false;
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "引き出し処理中にエラーが発生しました", e);
            return false;
        }
    }
    
    public double getBalance(Player player) {
        if (economy == null) return 0;
        
        try {
            return economy.getBalance(player);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "残高取得中にエラーが発生しました", e);
            return 0;
        }
    }
    
    public String format(double amount) {
        if (economy == null) return String.format("%.2f", amount);
        return economy.format(amount);
    }
    
    public boolean isAvailable() {
        return economy != null;
    }
    
    public Economy getEconomy() {
        return economy;
    }
}