package jp.ertl.rfm.mission;

import jp.ertl.rfm.RunForMoneyPlugin;
import jp.ertl.rfm.core.GameSession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.time.Duration;
import java.util.*;

public class MissionEngine {
    
    private final RunForMoneyPlugin plugin;
    private final List<Mission> missions;
    private final Set<String> completedMissions;
    private final Map<String, Long> activeMissions;
    
    public MissionEngine(RunForMoneyPlugin plugin) {
        this.plugin = plugin;
        this.missions = new ArrayList<>();
        this.completedMissions = new HashSet<>();
        this.activeMissions = new HashMap<>();
        
        loadMissions();
    }
    
    private void loadMissions() {
        File missionsDir = new File(plugin.getDataFolder(), "missions");
        if (!missionsDir.exists()) {
            missionsDir.mkdirs();
            saveDefaultMissions();
        }
        
        File[] files = missionsDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                try {
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                    Mission mission = loadMission(config);
                    if (mission != null) {
                        missions.add(mission);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("ミッション読み込み失敗: " + file.getName());
                    e.printStackTrace();
                }
            }
        }
        
        plugin.getLogger().info(missions.size() + "個のミッションを読み込みました");
    }
    
    private Mission loadMission(YamlConfiguration config) {
        String id = config.getString("id");
        String name = config.getString("name");
        String description = config.getString("description");
        
        if (id == null || name == null) return null;
        
        Mission mission = new Mission(id, name, description);
        
        // トリガー設定
        ConfigurationSection triggerSection = config.getConfigurationSection("trigger");
        if (triggerSection != null) {
            String type = triggerSection.getString("type");
            mission.setTriggerType(Mission.TriggerType.valueOf(type.toUpperCase()));
            mission.setTriggerValue(triggerSection.getString("value"));
        }
        
        // アクション設定
        List<Map<?, ?>> actions = config.getMapList("actions");
        for (Map<?, ?> actionMap : actions) {
            String type = (String) actionMap.get("type");
            Map<String, Object> params = new HashMap<>();
            actionMap.forEach((k, v) -> {
                if (!k.equals("type")) {
                    params.put(k.toString(), v);
                }
            });
            mission.addAction(new MissionAction(type, params));
        }
        
        mission.setDuration(config.getInt("duration", 60));
        mission.setRepeatable(config.getBoolean("repeatable", false));
        
        return mission;
    }
    
    private void saveDefaultMissions() {
        // デフォルトミッションを作成
        File defaultMission = new File(plugin.getDataFolder(), "missions/hunter_release.yml");
        if (!defaultMission.exists()) {
            plugin.saveResource("missions/hunter_release.yml", false);
        }
    }
    
    public void checkMissions(GameSession session) {
        for (Mission mission : missions) {
            if (shouldTrigger(mission, session)) {
                startMission(mission, session);
            }
        }
        
        // アクティブミッションの時間切れチェック
        Iterator<Map.Entry<String, Long>> it = activeMissions.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> entry = it.next();
            if (System.currentTimeMillis() > entry.getValue()) {
                failMission(entry.getKey());
                it.remove();
            }
        }
    }
    
    private boolean shouldTrigger(Mission mission, GameSession session) {
        if (completedMissions.contains(mission.getId()) && !mission.isRepeatable()) {
            return false;
        }
        
        if (activeMissions.containsKey(mission.getId())) {
            return false;
        }
        
        switch (mission.getTriggerType()) {
            case TIME:
                int triggerTime = parseTime(mission.getTriggerValue());
                int elapsedTime = session.getTotalDuration() - session.getRemainingTime();
                return elapsedTime == triggerTime;
                
            case PLAYER_COUNT:
                int targetCount = Integer.parseInt(mission.getTriggerValue());
                return session.getAliveRunnersCount() <= targetCount;
                
            case RANDOM:
                double chance = Double.parseDouble(mission.getTriggerValue());
                return Math.random() < chance / 100.0;
                
            default:
                return false;
        }
    }
    
    private int parseTime(String timeStr) {
        if (timeStr.startsWith("T+")) {
            String[] parts = timeStr.substring(2).split(":");
            int minutes = Integer.parseInt(parts[0]);
            int seconds = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            return minutes * 60 + seconds;
        }
        return 0;
    }
    
    private void startMission(Mission mission, GameSession session) {
        activeMissions.put(mission.getId(), System.currentTimeMillis() + mission.getDuration() * 1000L);
        
        // ミッション開始通知
        Bukkit.getOnlinePlayers().forEach(player -> {
            player.showTitle(Title.title(
                Component.text("MISSION", NamedTextColor.GOLD),
                Component.text(mission.getName(), NamedTextColor.YELLOW),
                Title.Times.times(
                    Duration.ofMillis(500),
                    Duration.ofSeconds(3),
                    Duration.ofMillis(500)
                )
            ));
            
            if (mission.getDescription() != null) {
                player.sendMessage(Component.text(""));
                player.sendMessage(Component.text("=== MISSION START ===", NamedTextColor.GOLD));
                player.sendMessage(Component.text(mission.getDescription(), NamedTextColor.YELLOW));
                player.sendMessage(Component.text("制限時間: " + mission.getDuration() + "秒", NamedTextColor.RED));
                player.sendMessage(Component.text(""));
            }
        });
        
        // BossBarを更新
        plugin.getUIManager().showMissionBossBar(mission.getName(), mission.getDuration());
        
        // ミッション固有のアクションを実行
        executeMissionActions(mission, session, true);
    }
    
    public void completeMission(String missionId) {
        Mission mission = getMissionById(missionId);
        if (mission == null) return;
        
        activeMissions.remove(missionId);
        completedMissions.add(missionId);
        
        Bukkit.broadcast(Component.text("MISSION COMPLETE: " + mission.getName(), NamedTextColor.GREEN));
        
        GameSession session = plugin.getGameManager().getCurrentSession();
        if (session != null) {
            executeMissionActions(mission, session, false);
        }
    }
    
    private void failMission(String missionId) {
        Mission mission = getMissionById(missionId);
        if (mission == null) return;
        
        Bukkit.broadcast(Component.text("MISSION FAILED: " + mission.getName(), NamedTextColor.RED));
    }
    
    private void executeMissionActions(Mission mission, GameSession session, boolean isStart) {
        for (MissionAction action : mission.getActions()) {
            if (isStart && action.getType().startsWith("on_complete_")) continue;
            if (!isStart && !action.getType().startsWith("on_complete_")) continue;
            
            executeAction(action, session);
        }
    }
    
    private void executeAction(MissionAction action, GameSession session) {
        switch (action.getType()) {
            case "add_hunter":
                int count = ((Number) action.getParams().get("count")).intValue();
                for (int i = 0; i < count; i++) {
                    plugin.getHunterManager().spawnHunter(session);
                }
                break;
                
            case "multiply_bounty":
                double multiplier = ((Number) action.getParams().get("multiplier")).doubleValue();
                long newBounty = (long) (session.getBountyPerSecond() * multiplier);
                session.setBountyPerSecond(newBounty);
                break;
                
            case "message":
                String message = (String) action.getParams().get("text");
                Bukkit.broadcast(Component.text(message, NamedTextColor.YELLOW));
                break;
                
            case "region_flag":
                if (plugin.getWorldGuardAdapter() != null) {
                    String region = (String) action.getParams().get("region");
                    String flag = (String) action.getParams().get("flag");
                    String value = (String) action.getParams().get("value");
                    plugin.getWorldGuardAdapter().setRegionFlag(region, flag, value);
                }
                break;
        }
    }
    
    private Mission getMissionById(String id) {
        return missions.stream()
            .filter(m -> m.getId().equals(id))
            .findFirst()
            .orElse(null);
    }
    
    public List<Mission> getMissions() {
        return new ArrayList<>(missions);
    }
    
    public Set<String> getActiveMissions() {
        return new HashSet<>(activeMissions.keySet());
    }
}