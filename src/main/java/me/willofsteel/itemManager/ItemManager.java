package me.willofsteel.itemManager;

import me.willofsteel.itemManager.commands.ItemManagerCommand;
import me.willofsteel.itemManager.utils.ConfigManager;
import me.willofsteel.itemManager.utils.DatabaseManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class ItemManager extends JavaPlugin {

    private ConfigManager configManager;
    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        configManager = new ConfigManager(this);

        databaseManager = new DatabaseManager(this);
        databaseManager.initializeDatabase();

        ItemManagerCommand commandHandler = new ItemManagerCommand(this);
        getCommand("itemmanager").setExecutor(commandHandler);
        getCommand("itemmanager").setTabCompleter(commandHandler);

        getLogger().info("ItemManager has been enabled!");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.closeConnection();
        }
        getLogger().info("ItemManager has been disabled!");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}
