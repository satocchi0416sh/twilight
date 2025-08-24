package jp.ertl.rfm.storage;

import jp.ertl.rfm.RunForMoneyPlugin;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class StorageManager {
    
    private final RunForMoneyPlugin plugin;
    private Connection connection;
    
    public StorageManager(RunForMoneyPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void initialize() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + 
                plugin.getDataFolder().getAbsolutePath() + "/data.db");
            
            createTables();
            plugin.getLogger().info("データベースを初期化しました");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "データベースの初期化に失敗しました", e);
        }
    }
    
    private void createTables() throws SQLException {
        String createSessionTable = """
            CREATE TABLE IF NOT EXISTS sessions (
                id TEXT PRIMARY KEY,
                start_time INTEGER,
                end_time INTEGER,
                total_runners INTEGER,
                survivors INTEGER,
                total_bounty INTEGER
            )
            """;
        
        String createPlayerStatsTable = """
            CREATE TABLE IF NOT EXISTS player_stats (
                uuid TEXT PRIMARY KEY,
                name TEXT,
                total_games INTEGER DEFAULT 0,
                total_wins INTEGER DEFAULT 0,
                total_bounty INTEGER DEFAULT 0,
                longest_survival INTEGER DEFAULT 0,
                times_caught INTEGER DEFAULT 0,
                times_surrendered INTEGER DEFAULT 0
            )
            """;
        
        String createSessionResultsTable = """
            CREATE TABLE IF NOT EXISTS session_results (
                session_id TEXT,
                player_uuid TEXT,
                player_name TEXT,
                status TEXT,
                bounty INTEGER,
                survival_time INTEGER,
                PRIMARY KEY (session_id, player_uuid)
            )
            """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createSessionTable);
            stmt.execute(createPlayerStatsTable);
            stmt.execute(createSessionResultsTable);
        }
    }
    
    public void saveSessionResult(String sessionId, UUID playerUuid, String playerName, 
                                 String status, long bounty, long survivalTime) {
        String sql = """
            INSERT OR REPLACE INTO session_results 
            (session_id, player_uuid, player_name, status, bounty, survival_time)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, sessionId);
            pstmt.setString(2, playerUuid.toString());
            pstmt.setString(3, playerName);
            pstmt.setString(4, status);
            pstmt.setLong(5, bounty);
            pstmt.setLong(6, survivalTime);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "セッション結果の保存に失敗しました", e);
        }
    }
    
    public void updatePlayerStats(UUID playerUuid, String playerName, boolean survived, 
                                 long bounty, long survivalTime, String exitStatus) {
        String sql = """
            INSERT INTO player_stats 
            (uuid, name, total_games, total_wins, total_bounty, longest_survival, times_caught, times_surrendered)
            VALUES (?, ?, 1, ?, ?, ?, ?, ?)
            ON CONFLICT(uuid) DO UPDATE SET
            name = excluded.name,
            total_games = total_games + 1,
            total_wins = total_wins + excluded.total_wins,
            total_bounty = total_bounty + excluded.total_bounty,
            longest_survival = MAX(longest_survival, excluded.longest_survival),
            times_caught = times_caught + excluded.times_caught,
            times_surrendered = times_surrendered + excluded.times_surrendered
            """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUuid.toString());
            pstmt.setString(2, playerName);
            pstmt.setInt(3, survived ? 1 : 0);
            pstmt.setLong(4, bounty);
            pstmt.setLong(5, survivalTime);
            pstmt.setInt(6, "CAUGHT".equals(exitStatus) ? 1 : 0);
            pstmt.setInt(7, "SURRENDERED".equals(exitStatus) ? 1 : 0);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "プレイヤー統計の更新に失敗しました", e);
        }
    }
    
    public List<PlayerStats> getTopPlayers(String orderBy, int limit) {
        List<PlayerStats> results = new ArrayList<>();
        String sql = "SELECT * FROM player_stats ORDER BY " + orderBy + " DESC LIMIT ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                PlayerStats stats = new PlayerStats(
                    UUID.fromString(rs.getString("uuid")),
                    rs.getString("name"),
                    rs.getInt("total_games"),
                    rs.getInt("total_wins"),
                    rs.getLong("total_bounty"),
                    rs.getLong("longest_survival")
                );
                results.add(stats);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "ランキング取得に失敗しました", e);
        }
        
        return results;
    }
    
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogger().info("データベース接続を閉じました");
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "データベース接続のクローズに失敗しました", e);
            }
        }
    }
    
    public static class PlayerStats {
        public final UUID uuid;
        public final String name;
        public final int totalGames;
        public final int totalWins;
        public final long totalBounty;
        public final long longestSurvival;
        
        public PlayerStats(UUID uuid, String name, int totalGames, int totalWins, 
                         long totalBounty, long longestSurvival) {
            this.uuid = uuid;
            this.name = name;
            this.totalGames = totalGames;
            this.totalWins = totalWins;
            this.totalBounty = totalBounty;
            this.longestSurvival = longestSurvival;
        }
    }
}