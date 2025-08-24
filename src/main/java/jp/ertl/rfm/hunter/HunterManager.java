package jp.ertl.rfm.hunter;

import jp.ertl.rfm.RunForMoneyPlugin;
import jp.ertl.rfm.core.GameSession;
import jp.ertl.rfm.core.RunnerState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class HunterManager implements Listener {
    
    private final RunForMoneyPlugin plugin;
    private final Map<Integer, HunterState> hunters;
    private final Map<Integer, Entity> hunterEntities;
    private int nextHunterId = 1;
    private final Random random = new Random();
    
    public HunterManager(RunForMoneyPlugin plugin) {
        this.plugin = plugin;
        this.hunters = new HashMap<>();
        this.hunterEntities = new HashMap<>();
    }
    
    public void spawnInitialHunters(GameSession session) {
        int hunterCount = plugin.getConfig().getInt("hunter.initial-count", 3);
        
        for (int i = 0; i < hunterCount; i++) {
            spawnHunter(session);
        }
    }
    
    public int spawnHunter(GameSession session) {
        // プレイヤーの位置から離れた場所にスポーン
        Location spawnLocation = findSuitableSpawnLocation(session);
        if (spawnLocation == null) {
            plugin.getLogger().warning("ハンターのスポーン位置が見つかりません");
            return -1;
        }
        
        // 簡易実装：ゾンビをハンターとして使用
        Zombie hunter = (Zombie) spawnLocation.getWorld().spawnEntity(spawnLocation, EntityType.ZOMBIE);
        
        int hunterId = nextHunterId++;
        
        // ハンターの設定
        hunter.setCustomName("§c§lHUNTER #" + hunterId);
        hunter.setCustomNameVisible(true);
        hunter.setHealth(20.0);
        hunter.setAdult();
        hunter.setShouldBurnInDay(false);
        hunter.setCanPickupItems(false);
        hunter.setMetadata("rfm_hunter", new FixedMetadataValue(plugin, hunterId));
        
        // 装備を設定
        hunter.getEquipment().setHelmet(new org.bukkit.inventory.ItemStack(Material.IRON_HELMET));
        hunter.getEquipment().setChestplate(new org.bukkit.inventory.ItemStack(Material.IRON_CHESTPLATE));
        hunter.getEquipment().setLeggings(new org.bukkit.inventory.ItemStack(Material.IRON_LEGGINGS));
        hunter.getEquipment().setBoots(new org.bukkit.inventory.ItemStack(Material.IRON_BOOTS));
        hunter.getEquipment().setItemInMainHand(new org.bukkit.inventory.ItemStack(Material.IRON_SWORD));
        
        HunterState state = new HunterState(hunterId);
        state.setPatrolOrigin(spawnLocation);
        
        hunters.put(hunterId, state);
        hunterEntities.put(hunterId, hunter);
        session.addHunter(hunterId);
        
        return hunterId;
    }
    
    private Location findSuitableSpawnLocation(GameSession session) {
        // 簡易実装：ランダムなプレイヤーから離れた位置
        var runners = session.getAliveRunners();
        if (runners.isEmpty()) return null;
        
        RunnerState randomRunner = runners.get(random.nextInt(runners.size()));
        Player player = Bukkit.getPlayer(randomRunner.getUuid());
        if (player == null) return null;
        
        Location playerLoc = player.getLocation();
        
        // プレイヤーから30-50ブロック離れた位置にスポーン
        double distance = 30 + random.nextDouble() * 20;
        double angle = random.nextDouble() * 2 * Math.PI;
        
        double x = playerLoc.getX() + Math.cos(angle) * distance;
        double z = playerLoc.getZ() + Math.sin(angle) * distance;
        double y = playerLoc.getWorld().getHighestBlockYAt((int)x, (int)z) + 1;
        
        return new Location(playerLoc.getWorld(), x, y, z);
    }
    
    public void performScanTick(GameSession session) {
        hunters.forEach((hunterId, state) -> {
            if (!state.canScan()) return;
            
            Entity hunterEntity = hunterEntities.get(hunterId);
            if (hunterEntity == null || !hunterEntity.isValid()) return;
            
            switch (state.getMode()) {
                case PATROL:
                    scanForTargets(hunterEntity, state, session);
                    break;
                case CHASE:
                    updateChase(hunterEntity, state, session);
                    break;
                case RETURNING:
                    checkReturnComplete(hunterEntity, state);
                    break;
            }
        });
    }
    
    private void scanForTargets(Entity hunter, HunterState state, GameSession session) {
        Location hunterLoc = hunter.getLocation();
        double viewDistance = state.getViewDistance();
        
        for (RunnerState runner : session.getAliveRunners()) {
            Player player = Bukkit.getPlayer(runner.getUuid());
            if (player == null || player.getGameMode() == org.bukkit.GameMode.SPECTATOR) continue;
            
            Location playerLoc = player.getLocation();
            double distance = hunterLoc.distance(playerLoc);
            
            // 距離チェック
            if (distance > viewDistance) continue;
            
            // 視野角チェック
            if (!isInFieldOfView(hunter, playerLoc, state.getFieldOfView())) continue;
            
            // 視線チェック（RayTrace）
            if (!hasLineOfSight(hunter, player)) continue;
            
            // ターゲット発見！
            state.startChase(player);
            
            // 追跡開始の通知
            player.sendActionBar(Component.text("ハンターに見つかった！", NamedTextColor.RED));
            player.playSound(playerLoc, org.bukkit.Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 1.0f);
            
            break; // 最初に見つけたターゲットを追跡
        }
    }
    
    private void updateChase(Entity hunter, HunterState state, GameSession session) {
        Player target = Bukkit.getPlayer(state.getTargetPlayer());
        
        if (target == null || target.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
            state.stopChase();
            return;
        }
        
        Location hunterLoc = hunter.getLocation();
        Location targetLoc = target.getLocation();
        double distance = hunterLoc.distance(targetLoc);
        
        // 確保判定
        if (distance <= state.getCaptureRadius()) {
            plugin.getGameManager().handlePlayerCaught(target, state.getNpcId());
            state.stopChase();
            return;
        }
        
        // 視認チェック
        if (distance <= state.getViewDistance() && hasLineOfSight(hunter, target)) {
            state.regainSight(targetLoc);
            
            // ターゲットに向かって移動
            if (hunter instanceof Zombie zombie) {
                zombie.setTarget(target);
            }
            
            // 距離に応じた警告
            if (distance < 10) {
                target.sendActionBar(Component.text(String.format("ハンター接近！ %.1fm", distance), NamedTextColor.RED));
            }
        } else {
            state.loseSight();
            
            if (state.hasLostSightTooLong()) {
                state.stopChase();
                target.sendActionBar(Component.text("ハンターを撒いた！", NamedTextColor.GREEN));
            }
        }
    }
    
    private void checkReturnComplete(Entity hunter, HunterState state) {
        if (state.getPatrolOrigin() != null) {
            double distance = hunter.getLocation().distance(state.getPatrolOrigin());
            if (distance < 5.0) {
                state.returnToPatrol();
            }
        } else {
            state.returnToPatrol();
        }
    }
    
    private boolean isInFieldOfView(Entity hunter, Location targetLoc, double fovDegrees) {
        Vector hunterDirection = hunter.getLocation().getDirection();
        Vector toTarget = targetLoc.toVector().subtract(hunter.getLocation().toVector()).normalize();
        
        double angle = Math.toDegrees(Math.acos(hunterDirection.dot(toTarget)));
        return angle <= fovDegrees / 2;
    }
    
    private boolean hasLineOfSight(Entity hunter, Entity target) {
        Location hunterEye = hunter.getLocation().add(0, hunter.getHeight() * 0.85, 0);
        Location targetEye = target.getLocation().add(0, target.getHeight() * 0.5, 0);
        
        Vector direction = targetEye.toVector().subtract(hunterEye.toVector());
        double distance = direction.length();
        direction.normalize();
        
        RayTraceResult result = hunter.getWorld().rayTraceBlocks(
            hunterEye,
            direction,
            distance
        );
        
        return result == null || result.getHitBlock() == null;
    }
    
    public void removeHunter(int hunterId) {
        Entity hunter = hunterEntities.remove(hunterId);
        if (hunter != null) {
            hunter.remove();
        }
        hunters.remove(hunterId);
    }
    
    public void removeAllHunters() {
        hunterEntities.values().forEach(Entity::remove);
        hunterEntities.clear();
        hunters.clear();
    }
    
    @EventHandler
    public void onHunterDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity().hasMetadata("rfm_hunter")) {
            event.setCancelled(true);
        }
        
        if (event.getDamager().hasMetadata("rfm_hunter") && event.getEntity() instanceof Player player) {
            event.setCancelled(true);
            
            int hunterId = event.getDamager().getMetadata("rfm_hunter").get(0).asInt();
            HunterState state = hunters.get(hunterId);
            
            if (state != null && state.getMode() == HunterState.Mode.CHASE) {
                plugin.getGameManager().handlePlayerCaught(player, hunterId);
            }
        }
    }
    
    @EventHandler
    public void onHunterDeath(EntityDeathEvent event) {
        if (event.getEntity().hasMetadata("rfm_hunter")) {
            event.getDrops().clear();
            event.setDroppedExp(0);
        }
    }
}