package me.willofsteel.itemManager.utils;

import me.willofsteel.itemManager.ItemManager;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private final ItemManager plugin;
    private final Map<String, String> giveAbbreviations;
    private final Map<String, String> checkRemoveAbbreviations;

    public ConfigManager(ItemManager plugin) {
        this.plugin = plugin;
        this.giveAbbreviations = new HashMap<>();
        this.checkRemoveAbbreviations = new HashMap<>();
        loadAbbreviations();
    }

    private void loadAbbreviations() {
        ConfigurationSection abbreviations = plugin.getConfig().getConfigurationSection("abbreviations");
        if (abbreviations != null) {
            ConfigurationSection give = abbreviations.getConfigurationSection("give");
            if (give != null) {
                for (String key : give.getKeys(false)) {
                    giveAbbreviations.put(key, give.getString(key));
                }
            }

            ConfigurationSection checkRemove = abbreviations.getConfigurationSection("check/remove");
            if (checkRemove != null) {
                for (String key : checkRemove.getKeys(false)) {
                    checkRemoveAbbreviations.put(key, checkRemove.getString(key));
                }
            }
        }
    }

    public String getGiveAbbreviation(String key) {
        return giveAbbreviations.get(key);
    }

    public String getCheckRemoveAbbreviation(String key) {
        return checkRemoveAbbreviations.get(key);
    }

    public String getSpaceSymbol() {
        return plugin.getConfig().getString("symbols.space", "_");
    }

    public String getNewLineSymbol() {
        return plugin.getConfig().getString("symbols.new_line", "|");
    }

    public String resolveAbbreviation(String input, boolean isGiveCommand) {
        if (input.contains("{") && input.contains("}")) {
            String abbreviation = input.substring(input.indexOf("{") + 1, input.indexOf("}"));
            String replacement = isGiveCommand ?
                    getGiveAbbreviation(abbreviation) :
                    getCheckRemoveAbbreviation(abbreviation);

            if (replacement != null) {
                return input.replace("{" + abbreviation + "}", replacement);
            }
        }
        return input;
    }

    public String processSymbols(String input) {
        return input.replace(getSpaceSymbol(), " ")
                .replace(getNewLineSymbol(), "\n");
    }

    public boolean isDatabaseEnabled() {
        return plugin.getConfig().getBoolean("storage.use_database", true);
    }

    public void reload() {
        plugin.getConfigManager().reload();
        giveAbbreviations.clear();
        checkRemoveAbbreviations.clear();
        loadAbbreviations();
    }
}
