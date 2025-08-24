package jp.ertl.rfm.admin;

import jp.ertl.rfm.RunForMoneyPlugin;
import jp.ertl.rfm.core.GameSession;
import jp.ertl.rfm.core.GameState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AdminCommandHandler implements CommandExecutor, TabCompleter {
    
    private final RunForMoneyPlugin plugin;
    
    public AdminCommandHandler(RunForMoneyPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, 
                           @NotNull String label, @NotNull String[] args) {
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "start":
                return handleStart(sender);
                
            case "stop":
                return handleStop(sender);
                
            case "pause":
                return handlePause(sender);
                
            case "resume":
                return handleResume(sender);
                
            case "status":
                return handleStatus(sender);
                
            case "reload":
                return handleReload(sender);
                
            case "addhunter":
                return handleAddHunter(sender, args);
                
            case "removehunter":
                return handleRemoveHunter(sender, args);
                
            case "setbounty":
                return handleSetBounty(sender, args);
                
            case "mission":
                return handleMission(sender, args);
                
            case "ranking":
                return handleRanking(sender, args);
                
            case "surrender":
                return handleSurrender(sender);
                
            default:
                sender.sendMessage(Component.text("不明なコマンド: " + subCommand, NamedTextColor.RED));
                sendHelp(sender);
                return true;
        }
    }
    
    private boolean handleStart(CommandSender sender) {
        if (!sender.hasPermission("rfm.admin.start")) {
            sender.sendMessage(Component.text("権限がありません", NamedTextColor.RED));
            return true;
        }
        
        GameSession current = plugin.getGameManager().getCurrentSession();
        if (current != null && current.getState().isActive()) {
            sender.sendMessage(Component.text("既にゲームが進行中です", NamedTextColor.RED));
            return true;
        }
        
        if (plugin.getGameManager().startGame()) {
            sender.sendMessage(Component.text("ゲームを開始しました", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("ゲームの開始に失敗しました", NamedTextColor.RED));
        }
        return true;
    }
    
    private boolean handleStop(CommandSender sender) {
        if (!sender.hasPermission("rfm.admin.stop")) {
            sender.sendMessage(Component.text("権限がありません", NamedTextColor.RED));
            return true;
        }
        
        GameSession current = plugin.getGameManager().getCurrentSession();
        if (current == null || !current.getState().isActive()) {
            sender.sendMessage(Component.text("アクティブなゲームがありません", NamedTextColor.RED));
            return true;
        }
        
        plugin.getGameManager().forceStop();
        sender.sendMessage(Component.text("ゲームを強制終了しました", NamedTextColor.YELLOW));
        return true;
    }
    
    private boolean handlePause(CommandSender sender) {
        if (!sender.hasPermission("rfm.admin.pause")) {
            sender.sendMessage(Component.text("権限がありません", NamedTextColor.RED));
            return true;
        }
        
        plugin.getGameManager().pauseGame();
        sender.sendMessage(Component.text("ゲームを一時停止しました", NamedTextColor.YELLOW));
        return true;
    }
    
    private boolean handleResume(CommandSender sender) {
        if (!sender.hasPermission("rfm.admin.resume")) {
            sender.sendMessage(Component.text("権限がありません", NamedTextColor.RED));
            return true;
        }
        
        plugin.getGameManager().resumeGame();
        sender.sendMessage(Component.text("ゲームを再開しました", NamedTextColor.GREEN));
        return true;
    }
    
    private boolean handleStatus(CommandSender sender) {
        GameSession session = plugin.getGameManager().getCurrentSession();
        
        if (session == null) {
            sender.sendMessage(Component.text("現在ゲームは開催されていません", NamedTextColor.GRAY));
            return true;
        }
        
        sender.sendMessage(Component.text(""));
        sender.sendMessage(Component.text("=== ゲーム状態 ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("状態: " + session.getState().getDisplayName(), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("残り時間: " + session.getFormattedTime(), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("逃走者: " + session.getAliveRunnersCount() + "/" + 
                                        session.getTotalRunnersCount(), NamedTextColor.GREEN));
        sender.sendMessage(Component.text("ハンター: " + session.getHunterCount(), NamedTextColor.RED));
        sender.sendMessage(Component.text("賞金/秒: ¥" + session.getBountyPerSecond(), NamedTextColor.AQUA));
        sender.sendMessage(Component.text(""));
        
        return true;
    }
    
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("rfm.admin.reload")) {
            sender.sendMessage(Component.text("権限がありません", NamedTextColor.RED));
            return true;
        }
        
        plugin.reloadConfig();
        sender.sendMessage(Component.text("設定をリロードしました", NamedTextColor.GREEN));
        return true;
    }
    
    private boolean handleAddHunter(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rfm.admin.hunter")) {
            sender.sendMessage(Component.text("権限がありません", NamedTextColor.RED));
            return true;
        }
        
        GameSession session = plugin.getGameManager().getCurrentSession();
        if (session == null || !session.getState().isActive()) {
            sender.sendMessage(Component.text("アクティブなゲームがありません", NamedTextColor.RED));
            return true;
        }
        
        int count = 1;
        if (args.length > 1) {
            try {
                count = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("無効な数値: " + args[1], NamedTextColor.RED));
                return true;
            }
        }
        
        for (int i = 0; i < count; i++) {
            plugin.getHunterManager().spawnHunter(session);
        }
        
        sender.sendMessage(Component.text(count + "体のハンターを追加しました", NamedTextColor.GREEN));
        return true;
    }
    
    private boolean handleRemoveHunter(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rfm.admin.hunter")) {
            sender.sendMessage(Component.text("権限がありません", NamedTextColor.RED));
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(Component.text("使用法: /rfm removehunter <ID>", NamedTextColor.RED));
            return true;
        }
        
        try {
            int hunterId = Integer.parseInt(args[1]);
            plugin.getHunterManager().removeHunter(hunterId);
            sender.sendMessage(Component.text("ハンター #" + hunterId + " を削除しました", NamedTextColor.GREEN));
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("無効なID: " + args[1], NamedTextColor.RED));
        }
        
        return true;
    }
    
    private boolean handleSetBounty(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rfm.admin.bounty")) {
            sender.sendMessage(Component.text("権限がありません", NamedTextColor.RED));
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(Component.text("使用法: /rfm setbounty <金額>", NamedTextColor.RED));
            return true;
        }
        
        GameSession session = plugin.getGameManager().getCurrentSession();
        if (session == null) {
            sender.sendMessage(Component.text("アクティブなゲームがありません", NamedTextColor.RED));
            return true;
        }
        
        try {
            long bounty = Long.parseLong(args[1]);
            session.setBountyPerSecond(bounty);
            sender.sendMessage(Component.text("賞金を ¥" + bounty + "/秒 に設定しました", NamedTextColor.GREEN));
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("無効な金額: " + args[1], NamedTextColor.RED));
        }
        
        return true;
    }
    
    private boolean handleMission(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rfm.admin.mission")) {
            sender.sendMessage(Component.text("権限がありません", NamedTextColor.RED));
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(Component.text("使用法: /rfm mission <complete|list> [mission_id]", NamedTextColor.RED));
            return true;
        }
        
        String action = args[1].toLowerCase();
        
        switch (action) {
            case "complete":
                if (args.length < 3) {
                    sender.sendMessage(Component.text("ミッションIDを指定してください", NamedTextColor.RED));
                    return true;
                }
                plugin.getMissionEngine().completeMission(args[2]);
                sender.sendMessage(Component.text("ミッション " + args[2] + " を完了しました", NamedTextColor.GREEN));
                break;
                
            case "list":
                sender.sendMessage(Component.text("=== ミッション一覧 ===", NamedTextColor.GOLD));
                plugin.getMissionEngine().getMissions().forEach(mission -> {
                    sender.sendMessage(Component.text("- " + mission.getId() + ": " + mission.getName(), 
                                                     NamedTextColor.YELLOW));
                });
                sender.sendMessage(Component.text("アクティブ: " + 
                    String.join(", ", plugin.getMissionEngine().getActiveMissions()), NamedTextColor.AQUA));
                break;
                
            default:
                sender.sendMessage(Component.text("不明なアクション: " + action, NamedTextColor.RED));
        }
        
        return true;
    }
    
    private boolean handleRanking(CommandSender sender, String[] args) {
        String orderBy = "total_bounty";
        if (args.length > 1) {
            switch (args[1].toLowerCase()) {
                case "wins":
                    orderBy = "total_wins";
                    break;
                case "survival":
                    orderBy = "longest_survival";
                    break;
                case "games":
                    orderBy = "total_games";
                    break;
            }
        }
        
        sender.sendMessage(Component.text(""));
        sender.sendMessage(Component.text("=== ランキング ===", NamedTextColor.GOLD));
        
        var topPlayers = plugin.getStorageManager().getTopPlayers(orderBy, 10);
        int rank = 1;
        for (var stats : topPlayers) {
            sender.sendMessage(Component.text(rank + ". " + stats.name, NamedTextColor.YELLOW)
                .append(Component.text(" - ", NamedTextColor.GRAY))
                .append(Component.text("総獲得: ¥" + stats.totalBounty + 
                                      " | 勝利: " + stats.totalWins + 
                                      " | ゲーム: " + stats.totalGames, NamedTextColor.AQUA)));
            rank++;
        }
        
        sender.sendMessage(Component.text(""));
        return true;
    }
    
    private boolean handleSurrender(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("このコマンドはプレイヤーのみ実行可能です", NamedTextColor.RED));
            return true;
        }
        
        if (!player.hasPermission("rfm.player.surrender")) {
            player.sendMessage(Component.text("権限がありません", NamedTextColor.RED));
            return true;
        }
        
        GameSession session = plugin.getGameManager().getCurrentSession();
        if (session == null || !session.getState().isActive()) {
            player.sendMessage(Component.text("アクティブなゲームがありません", NamedTextColor.RED));
            return true;
        }
        
        plugin.getGameManager().handlePlayerSurrender(player);
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text(""));
        sender.sendMessage(Component.text("=== RunForMoney コマンド ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/rfm start", NamedTextColor.YELLOW)
            .append(Component.text(" - ゲームを開始", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/rfm stop", NamedTextColor.YELLOW)
            .append(Component.text(" - ゲームを強制終了", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/rfm pause", NamedTextColor.YELLOW)
            .append(Component.text(" - ゲームを一時停止", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/rfm resume", NamedTextColor.YELLOW)
            .append(Component.text(" - ゲームを再開", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/rfm status", NamedTextColor.YELLOW)
            .append(Component.text(" - ゲーム状態を表示", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/rfm addhunter [数]", NamedTextColor.YELLOW)
            .append(Component.text(" - ハンターを追加", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/rfm mission <complete|list>", NamedTextColor.YELLOW)
            .append(Component.text(" - ミッション管理", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/rfm ranking [wins|survival|games]", NamedTextColor.YELLOW)
            .append(Component.text(" - ランキング表示", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/surrender", NamedTextColor.YELLOW)
            .append(Component.text(" - 自首する", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text(""));
    }
    
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, 
                                     @NotNull String label, @NotNull String[] args) {
        
        if (args.length == 1) {
            return Arrays.asList("start", "stop", "pause", "resume", "status", "reload", 
                                "addhunter", "removehunter", "setbounty", "mission", "ranking")
                .stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "mission":
                    return Arrays.asList("complete", "list");
                case "ranking":
                    return Arrays.asList("bounty", "wins", "survival", "games");
            }
        }
        
        return new ArrayList<>();
    }
}