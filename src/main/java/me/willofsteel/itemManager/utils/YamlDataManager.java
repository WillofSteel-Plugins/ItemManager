package me.willofsteel.itemManager.utils;

import me.willofsteel.itemManager.ItemManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class YamlDataManager {

    private final ItemManager plugin;
    private final File dataFolder;

    public YamlDataManager(ItemManager plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "data");

        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    public void savePlayerInventoryBackup(String playerUuid, List<String> inventoryData) {
        File playerFile = new File(dataFolder, playerUuid + "_backups.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);

        String timestamp = String.valueOf(System.currentTimeMillis());
        config.set("backups." + timestamp + ".inventory", inventoryData);
        config.set("backups." + timestamp + ".created", System.currentTimeMillis());

        try {
            config.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save player inventory backup", e);
        }
    }

    public List<String> getPlayerInventoryBackups(String playerUuid) {
        File playerFile = new File(dataFolder, playerUuid + "_backups.yml");
        if (!playerFile.exists()) {
            return new ArrayList<>();
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        List<String> backups = new ArrayList<>();

        if (config.getConfigurationSection("backups") != null) {
            for (String timestamp : config.getConfigurationSection("backups").getKeys(false)) {
                long created = config.getLong("backups." + timestamp + ".created");
                backups.add("Backup " + timestamp + " (Created: " + new java.util.Date(created) + ")");
            }
        }

        return backups;
    }

    public void saveItemTemplate(String templateName, String itemData) {
        File templatesFile = new File(dataFolder, "item_templates.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(templatesFile);

        config.set("templates." + templateName, itemData);
        config.set("templates." + templateName + "_created", System.currentTimeMillis());

        try {
            config.save(templatesFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save item template", e);
        }
    }

    public String getItemTemplate(String templateName) {
        File templatesFile = new File(dataFolder, "item_templates.yml");
        if (!templatesFile.exists()) {
            return null;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(templatesFile);
        return config.getString("templates." + templateName);
    }

    public List<String> getItemTemplateNames() {
        File templatesFile = new File(dataFolder, "item_templates.yml");
        if (!templatesFile.exists()) {
            return new ArrayList<>();
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(templatesFile);
        List<String> templates = new ArrayList<>();

        if (config.getConfigurationSection("templates") != null) {
            for (String key : config.getConfigurationSection("templates").getKeys(false)) {
                if (!key.endsWith("_created")) {
                    templates.add(key);
                }
            }
        }

        return templates;
    }

    public void savePlayerSettings(String playerUuid, String setting, Object value) {
        File playerFile = new File(dataFolder, playerUuid + "_settings.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);

        config.set("settings." + setting, value);
        config.set("last_updated", System.currentTimeMillis());

        try {
            config.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save player settings", e);
        }
    }

    public Object getPlayerSetting(String playerUuid, String setting, Object defaultValue) {
        File playerFile = new File(dataFolder, playerUuid + "_settings.yml");
        if (!playerFile.exists()) {
            return defaultValue;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        return config.get("settings." + setting, defaultValue);
    }
}
