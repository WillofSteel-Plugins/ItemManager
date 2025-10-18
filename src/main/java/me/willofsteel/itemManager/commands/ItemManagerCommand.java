package me.willofsteel.itemManager.commands;

import me.willofsteel.itemManager.ItemManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ItemManagerCommand implements CommandExecutor, TabCompleter {

    private final ItemManager plugin;

    public ItemManagerCommand(ItemManager plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "help":
                if (!sender.hasPermission("ItemManager.Help")) {
                    sender.sendMessage("§cYou don't have permission to use this command.");
                    return true;
                }
                showHelp(sender);
                break;

            case "reload":
                if (!sender.hasPermission("ItemManager.Reload")) {
                    sender.sendMessage("§cYou don't have permission to use this command.");
                    return true;
                }
                plugin.getConfigManager().reload();
                sender.sendMessage("§aConfiguration reloaded!");
                break;

            case "give":
                if (!sender.hasPermission("ItemManager.Give")) {
                    sender.sendMessage("§cYou don't have permission to use this command.");
                    return true;
                }
                handleGive(sender, Arrays.copyOfRange(args, 1, args.length));
                break;

            case "check":
                if (!sender.hasPermission("ItemManager.Check")) {
                    sender.sendMessage("§cYou don't have permission to use this command.");
                    return true;
                }
                handleCheck(sender, Arrays.copyOfRange(args, 1, args.length));
                break;

            case "remove":
                if (!sender.hasPermission("ItemManager.Remove")) {
                    sender.sendMessage("§cYou don't have permission to use this command.");
                    return true;
                }
                handleRemove(sender, Arrays.copyOfRange(args, 1, args.length));
                break;

            case "rename":
                if (!sender.hasPermission("ItemManager.Rename")) {
                    sender.sendMessage("§cYou don't have permission to use this command.");
                    return true;
                }
                handleRename(sender, Arrays.copyOfRange(args, 1, args.length));
                break;

            case "lore":
                handleLoreCommand(sender, Arrays.copyOfRange(args, 1, args.length));
                break;

            case "enchant":
                handleEnchantCommand(sender, Arrays.copyOfRange(args, 1, args.length));
                break;

            case "glow":
                handleGlowCommand(sender, Arrays.copyOfRange(args, 1, args.length));
                break;

            default:
                sender.sendMessage("§cUnknown command. Use /IM help for available commands.");
                break;
        }

        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage("§6=== ItemManager Commands ===");
        sender.sendMessage("§e/IM help §7- Show this help menu");
        sender.sendMessage("§e/IM reload §7- Reload configuration");
        sender.sendMessage("§e/IM give <player> <item> [modifiers] §7- Give items to players");
        sender.sendMessage("§e/IM check <player> <item> §7- Check if player has item");
        sender.sendMessage("§e/IM remove <player> <item> §7- Remove items from player");
        sender.sendMessage("§e/IM rename <name> §7- Rename item in hand");
        sender.sendMessage("§e/IM lore <set|add|remove> [text] §7- Modify item lore");
        sender.sendMessage("§e/IM enchant <enchantment> [level] §7- Add enchantment to item");
        sender.sendMessage("§e/IM enchant remove [enchantment] §7- Remove enchantments");
        sender.sendMessage("§e/IM glow §7- Add glow effect to item");
        sender.sendMessage("§e/IM glow remove §7- Remove glow effect");
        sender.sendMessage("§7Use abbreviations with {name} syntax!");
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /IM give <player> <item> [modifiers]");
            return;
        }

        String playerName = args[0];
        String itemString = plugin.getConfigManager().resolveAbbreviation(args[1], true);

        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage("§cPlayer not found!");
            return;
        }

        try {
            ItemStack item = parseItemString(itemString, args);

            if (item == null) {
                sender.sendMessage("§cInvalid item: " + itemString);
                return;
            }

            target.getInventory().addItem(item);
            sender.sendMessage("§aGave " + item.getAmount() + "x " + item.getType() + " to " + target.getName());

            // Log the action
            plugin.getDatabaseManager().logItemAction(
                    target.getUniqueId().toString(),
                    "GIVE",
                    item.toString(),
                    sender.getName()
            );

        } catch (Exception e) {
            sender.sendMessage("§cError giving item: " + e.getMessage());
        }
    }

    private void handleCheck(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /IM check <player> <item>");
            return;
        }

        String playerName = args[0];
        String itemString = plugin.getConfigManager().resolveAbbreviation(args[1], false);

        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage("§cPlayer not found!");
            return;
        }

        try {
            Material material = parseMaterial(itemString);
            if (material == null) {
                sender.sendMessage("§cInvalid material: " + itemString);
                return;
            }

            int count = 0;
            for (ItemStack item : target.getInventory().getContents()) {
                if (item != null && item.getType() == material) {
                    count += item.getAmount();
                }
            }

            sender.sendMessage("§a" + target.getName() + " has " + count + "x " + material);

        } catch (Exception e) {
            sender.sendMessage("§cError checking item: " + e.getMessage());
        }
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /IM remove <player> <item> [amount]");
            return;
        }

        String playerName = args[0];
        String itemString = plugin.getConfigManager().resolveAbbreviation(args[1], false);
        int amount = args.length > 2 ? parseAmount(args[2]) : 1;

        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage("§cPlayer not found!");
            return;
        }

        try {
            Material material = parseMaterial(itemString);
            if (material == null) {
                sender.sendMessage("§cInvalid material: " + itemString);
                return;
            }

            int removed = removeItems(target, material, amount);
            sender.sendMessage("§aRemoved " + removed + "x " + material + " from " + target.getName());

            // Log the action
            plugin.getDatabaseManager().logItemAction(
                    target.getUniqueId().toString(),
                    "REMOVE",
                    material + " x" + removed,
                    sender.getName()
            );

        } catch (Exception e) {
            sender.sendMessage("§cError removing item: " + e.getMessage());
        }
    }

    private void handleRename(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return;
        }

        if (args.length == 0) {
            sender.sendMessage("§cUsage: /IM rename <name>");
            return;
        }

        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() == Material.AIR) {
            sender.sendMessage("§cYou must hold an item to rename!");
            return;
        }

        String name = String.join(" ", args);
        name = plugin.getConfigManager().processSymbols(name);

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§r" + name);
            item.setItemMeta(meta);
            sender.sendMessage("§aItem renamed to: " + name);
        }
    }

    private void handleLoreCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§cUsage: /IM lore <set|add|remove> [text/line]");
            return;
        }

        String action = args[0].toLowerCase();
        switch (action) {
            case "set":
                if (!sender.hasPermission("ItemManager.Lore.Set")) {
                    sender.sendMessage("§cYou don't have permission to use this command.");
                    return;
                }
                handleLoreSet(sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            case "add":
                if (!sender.hasPermission("ItemManager.Lore.Add")) {
                    sender.sendMessage("§cYou don't have permission to use this command.");
                    return;
                }
                handleLoreAdd(sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            case "remove":
                if (!sender.hasPermission("ItemManager.Lore.Remove")) {
                    sender.sendMessage("§cYou don't have permission to use this command.");
                    return;
                }
                handleLoreRemove(sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            default:
                sender.sendMessage("§cUsage: /IM lore <set|add|remove> [text/line]");
                break;
        }
    }

    private void handleLoreSet(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return;
        }

        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() == Material.AIR) {
            sender.sendMessage("§cYou must hold an item to set lore!");
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (args.length == 0) {
                meta.setLore(new ArrayList<>());
                sender.sendMessage("§aLore cleared!");
            } else {
                String loreText = String.join(" ", args);
                loreText = plugin.getConfigManager().processSymbols(loreText);
                List<String> lore = Arrays.asList(loreText.split("\n"));
                meta.setLore(lore);
                sender.sendMessage("§aLore set!");
            }
            item.setItemMeta(meta);
        }
    }

    private void handleLoreAdd(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return;
        }

        if (args.length == 0) {
            sender.sendMessage("§cUsage: /IM lore add <text>");
            return;
        }

        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() == Material.AIR) {
            sender.sendMessage("§cYou must hold an item to add lore!");
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            String loreText = String.join(" ", args);
            loreText = plugin.getConfigManager().processSymbols(loreText);
            lore.add(loreText);
            meta.setLore(lore);
            item.setItemMeta(meta);
            sender.sendMessage("§aLore line added!");
        }
    }

    private void handleLoreRemove(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return;
        }

        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() == Material.AIR) {
            sender.sendMessage("§cYou must hold an item to remove lore!");
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasLore()) {
            List<String> lore = new ArrayList<>(meta.getLore());
            if (args.length > 0) {
                try {
                    int line = Integer.parseInt(args[0]) - 1;
                    if (line >= 0 && line < lore.size()) {
                        lore.remove(line);
                        meta.setLore(lore);
                        item.setItemMeta(meta);
                        sender.sendMessage("§aLore line " + (line + 1) + " removed!");
                    } else {
                        sender.sendMessage("§cInvalid line number!");
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cInvalid line number!");
                }
            } else {
                meta.setLore(new ArrayList<>());
                item.setItemMeta(meta);
                sender.sendMessage("§aAll lore removed!");
            }
        } else {
            sender.sendMessage("§cItem has no lore to remove!");
        }
    }

    private void handleEnchantCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!sender.hasPermission("ItemManager.Enchant")) {
                sender.sendMessage("§cYou don't have permission to use this command.");
                return;
            }
            sender.sendMessage("§cUsage: /IM enchant <enchantment> [level] or /IM enchant remove [enchantment]");
        } else if (args[0].equalsIgnoreCase("remove")) {
            if (!sender.hasPermission("ItemManager.Enchant.Remove")) {
                sender.sendMessage("§cYou don't have permission to use this command.");
                return;
            }
            handleEnchantRemove(sender, Arrays.copyOfRange(args, 1, args.length));
        } else {
            if (!sender.hasPermission("ItemManager.Enchant")) {
                sender.sendMessage("§cYou don't have permission to use this command.");
                return;
            }
            handleEnchant(sender, args);
        }
    }

    private void handleEnchant(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return;
        }

        if (args.length == 0) {
            sender.sendMessage("§cUsage: /IM enchant <enchantment> [level]");
            return;
        }

        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() == Material.AIR) {
            sender.sendMessage("§cYou must hold an item to enchant!");
            return;
        }

        try {
            Enchantment enchantment = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(args[0].toLowerCase()));
            if (enchantment == null) {
                sender.sendMessage("§cInvalid enchantment: " + args[0]);
                return;
            }

            int level = args.length > 1 ? Integer.parseInt(args[1]) : 1;
            item.addUnsafeEnchantment(enchantment, level);
            sender.sendMessage("§aAdded " + enchantment.getKey().getKey() + " " + level + " to item!");

        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid level number!");
        } catch (Exception e) {
            sender.sendMessage("§cError adding enchantment: " + e.getMessage());
        }
    }

    private void handleEnchantRemove(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return;
        }

        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() == Material.AIR) {
            sender.sendMessage("§cYou must hold an item to remove enchantments!");
            return;
        }

        if (args.length == 0) {
            // Remove all enchantments
            for (Enchantment enchantment : item.getEnchantments().keySet()) {
                item.removeEnchantment(enchantment);
            }
            sender.sendMessage("§aRemoved all enchantments!");
        } else {
            try {
                Enchantment enchantment = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(args[0].toLowerCase()));
                if (enchantment == null) {
                    sender.sendMessage("§cInvalid enchantment: " + args[0]);
                    return;
                }

                if (item.getEnchantments().containsKey(enchantment)) {
                    item.removeEnchantment(enchantment);
                    sender.sendMessage("§aRemoved " + enchantment.getKey().getKey() + " from item!");
                } else {
                    sender.sendMessage("§cItem doesn't have that enchantment!");
                }
            } catch (Exception e) {
                sender.sendMessage("§cError removing enchantment: " + e.getMessage());
            }
        }
    }

    private void handleGlowCommand(CommandSender sender, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("remove")) {
            if (!sender.hasPermission("ItemManager.Glow.Remove")) {
                sender.sendMessage("§cYou don't have permission to use this command.");
                return;
            }
            handleGlowRemove(sender, Arrays.copyOfRange(args, 1, args.length));
        } else {
            if (!sender.hasPermission("ItemManager.Glow")) {
                sender.sendMessage("§cYou don't have permission to use this command.");
                return;
            }
            handleGlow(sender, args);
        }
    }

    private void handleGlow(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return;
        }

        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() == Material.AIR) {
            sender.sendMessage("§cYou must hold an item to add glow!");
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
            sender.sendMessage("§aAdded glow effect to item!");
        }
    }

    private void handleGlowRemove(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return;
        }

        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() == Material.AIR) {
            sender.sendMessage("§cYou must hold an item to remove glow!");
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta.hasEnchant(Enchantment.LURE) && meta.getEnchantLevel(Enchantment.LURE) == 1) {
                meta.removeEnchant(Enchantment.LURE);
                meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
                item.setItemMeta(meta);
                sender.sendMessage("§aRemoved glow effect from item!");
            } else {
                sender.sendMessage("§cItem doesn't have glow effect!");
            }
        }
    }

    // Utility methods
    private ItemStack parseItemString(String itemString, String[] args) {
        Material material = parseMaterial(itemString);
        if (material == null) return null;

        int amount = 1;
        ItemStack item = new ItemStack(material, amount);

        // Parse modifiers
        for (int i = 2; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("amt:")) {
                amount = parseAmount(arg.substring(4));
                item.setAmount(amount);
            }
            // Add more modifiers as needed
        }

        return item;
    }

    private Material parseMaterial(String materialString) {
        if (materialString.startsWith("mat:")) {
            materialString = materialString.substring(4);
        }

        try {
            return Material.valueOf(materialString.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private int parseAmount(String amountString) {
        try {
            return Math.max(1, Integer.parseInt(amountString));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private int removeItems(Player player, Material material, int amount) {
        int removed = 0;
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length && removed < amount; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == material) {
                int toRemove = Math.min(item.getAmount(), amount - removed);
                item.setAmount(item.getAmount() - toRemove);
                if (item.getAmount() <= 0) {
                    contents[i] = null;
                }
                removed += toRemove;
            }
        }

        player.getInventory().setContents(contents);
        return removed;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("help", "reload", "give", "check", "remove",
                    "rename", "lore", "enchant", "glow");
            for (String subCommand : subCommands) {
                if (subCommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("lore")) {
                List<String> loreActions = Arrays.asList("set", "add", "remove");
                for (String action : loreActions) {
                    if (action.toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(action);
                    }
                }
            } else if (subCommand.equals("enchant")) {
                completions.add("remove");
            } else if (subCommand.equals("glow")) {
                completions.add("remove");
            } else if (subCommand.equals("give") || subCommand.equals("check") || subCommand.equals("remove")) {
                // Add online players
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(player.getName());
                    }
                }
            }
        }

        return completions;
    }
}
