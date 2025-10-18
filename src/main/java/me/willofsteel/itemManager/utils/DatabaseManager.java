package me.willofsteel.itemManager.utils;

import me.willofsteel.itemManager.ItemManager;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class DatabaseManager {

    private final ItemManager plugin;
    private Connection connection;
    private final String databasePath;

    public DatabaseManager(ItemManager plugin) {
        this.plugin = plugin;
        this.databasePath = plugin.getDataFolder().getAbsolutePath() + File.separator + "itemmanager.db";
    }

    public void initializeDatabase() {
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }

            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);

            // Create tables
            createTables();

            plugin.getLogger().info("Database initialized successfully!");

        } catch (ClassNotFoundException | SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database!", e);
        }
    }

    private void createTables() throws SQLException {
        String createPlayerDataTable = """
            CREATE TABLE IF NOT EXISTS player_data (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid TEXT NOT NULL,
                data_type TEXT NOT NULL,
                data_value TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;

        String createItemLogsTable = """
            CREATE TABLE IF NOT EXISTS item_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid TEXT NOT NULL,
                action TEXT NOT NULL,
                item_data TEXT NOT NULL,
                executor TEXT NOT NULL,
                timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;

        try (Statement statement = connection.createStatement()) {
            statement.execute(createPlayerDataTable);
            statement.execute(createItemLogsTable);
        }
    }

    public void logItemAction(String playerUuid, String action, String itemData, String executor) {
        String sql = "INSERT INTO item_logs (player_uuid, action, item_data, executor) VALUES (?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerUuid);
            statement.setString(2, action);
            statement.setString(3, itemData);
            statement.setString(4, executor);
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to log item action", e);
        }
    }

    public List<String> getPlayerLogs(String playerUuid, int limit) {
        List<String> logs = new ArrayList<>();
        String sql = "SELECT action, item_data, executor, timestamp FROM item_logs WHERE player_uuid = ? ORDER BY timestamp DESC LIMIT ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerUuid);
            statement.setInt(2, limit);

            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String log = String.format("[%s] %s: %s (by %s)",
                        resultSet.getString("timestamp"),
                        resultSet.getString("action"),
                        resultSet.getString("item_data"),
                        resultSet.getString("executor")
                );
                logs.add(log);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to retrieve player logs", e);
        }

        return logs;
    }

    public void savePlayerData(String playerUuid, String dataType, String dataValue) {
        String sql = "INSERT OR REPLACE INTO player_data (player_uuid, data_type, data_value) VALUES (?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerUuid);
            statement.setString(2, dataType);
            statement.setString(3, dataValue);
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save player data", e);
        }
    }

    public String getPlayerData(String playerUuid, String dataType) {
        String sql = "SELECT data_value FROM player_data WHERE player_uuid = ? AND data_type = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerUuid);
            statement.setString(2, dataType);

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("data_value");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to retrieve player data", e);
        }

        return null;
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to close database connection", e);
        }
    }
}
