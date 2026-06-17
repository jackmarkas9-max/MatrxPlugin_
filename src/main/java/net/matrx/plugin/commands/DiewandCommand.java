package net.matrx.plugin.commands;

import net.matrx.plugin.managers.*;
import net.matrx.plugin.models.*;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;
import java.util.stream.Collectors;

public class DiewandCommand implements CommandExecutor, TabCompleter {
    private final DeathZoneManager dzManager;

    public DiewandCommand(DeathZoneManager dzManager) {
        this.dzManager = dzManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("matrx.admin")) {
            player.sendMessage(ChatColor.RED + "No permission!");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "wand" -> giveWand(player);
            case "pos1" -> setPos1(player);
            case "pos2" -> setPos2(player);
            case "save" -> saveZone(player, args);
            case "remove" -> removeZone(player, args);
            case "list" -> listZones(player);
            case "tp" -> teleportZone(player, args);
            default -> sendHelp(player);
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GRAY + "╔══════════════════════════════╗");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "  &c&l✦ Diewand Tool"));
        player.sendMessage(ChatColor.GRAY + "  /diewand wand" + ChatColor.WHITE + " - Get the wand");
        player.sendMessage(ChatColor.GRAY + "  /diewand pos1" + ChatColor.WHITE + " - Set corner 1");
        player.sendMessage(ChatColor.GRAY + "  /diewand pos2" + ChatColor.WHITE + " - Set corner 2");
        player.sendMessage(ChatColor.GRAY + "  /diewand save <id>" + ChatColor.WHITE + " - Save death zone");
        player.sendMessage(ChatColor.GRAY + "  /diewand remove <id>" + ChatColor.WHITE + " - Remove zone");
        player.sendMessage(ChatColor.GRAY + "  /diewand list" + ChatColor.WHITE + " - List zones");
        player.sendMessage(ChatColor.GRAY + "  /diewand tp <id>" + ChatColor.WHITE + " - Teleport to zone");
        player.sendMessage(ChatColor.GRAY + "╚══════════════════════════════╝");
    }

    private void giveWand(Player player) {
        ItemStack wand = new ItemStack(Material.STICK);
        ItemMeta meta = wand.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "✦ Diewand");
        meta.setLore(List.of(
            ChatColor.GRAY + "Left-click block = Pos 1",
            ChatColor.GRAY + "Right-click block = Pos 2",
            ChatColor.GRAY + "/diewand save <id> to save"
        ));
        wand.setItemMeta(meta);
        player.getInventory().addItem(wand);
        player.sendMessage(ChatColor.GREEN + "You received the Diewand! Left/right click blocks to set corners.");
    }

    private void setPos1(Player player) {
        Location loc = player.getLocation();
        dzManager.setSelection(player, 0, loc);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
            "&aPos 1 set: &f" + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ()));
        player.spawnParticle(Particle.FLAME, loc, 30, 0.5, 0.5, 0.5, 0.01);
    }

    private void setPos2(Player player) {
        Location loc = player.getLocation();
        dzManager.setSelection(player, 1, loc);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
            "&aPos 2 set: &f" + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ()));
        player.spawnParticle(Particle.FLAME, loc, 30, 0.5, 0.5, 0.5, 0.01);
    }

    private void saveZone(Player player, String[] args) {
        Location[] sel = dzManager.getSelection(player);
        if (sel == null || sel[0] == null || sel[1] == null) {
            player.sendMessage(ChatColor.RED + "Set both corners first! Use /diewand pos1 and pos2");
            return;
        }
        if (!sel[0].getWorld().equals(sel[1].getWorld())) {
            player.sendMessage(ChatColor.RED + "Corners must be in the same world!");
            return;
        }

        int id;
        if (args.length >= 2) {
            try {
                id = Integer.parseInt(args[1]);
                if (dzManager.get(id) != null) {
                    player.sendMessage(ChatColor.RED + "Death zone #" + id + " already exists! Remove it first or use another ID.");
                    return;
                }
            } catch (NumberFormatException e) {
                id = dzManager.nextId();
                player.sendMessage(ChatColor.YELLOW + "Invalid ID. Using next available: " + id);
            }
        } else {
            id = dzManager.nextId();
        }

        DeathZone zone = new DeathZone(
            id, sel[0].getWorld().getName(),
            sel[0].getX(), sel[0].getY(), sel[0].getZ(),
            sel[1].getX(), sel[1].getY(), sel[1].getZ()
        );
        dzManager.add(zone);
        dzManager.clearSelection(player);

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
            "&aDeath zone saved! " + zone.format()));
        player.spawnParticle(Particle.FLASH, zone.getCenter(player.getWorld()), 1, 0, 0, 0, 0);
        player.spawnParticle(Particle.FIREWORK, zone.getCenter(player.getWorld()), 50, 2, 2, 2, 0.05);
    }

    private void removeZone(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /diewand remove <id>");
            return;
        }
        try {
            int id = Integer.parseInt(args[1]);
            if (dzManager.get(id) == null) {
                player.sendMessage(ChatColor.RED + "Death zone #" + id + " not found.");
                return;
            }
            dzManager.remove(id);
            player.sendMessage(ChatColor.GREEN + "Removed death zone #" + id);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid ID.");
        }
    }

    private void listZones(Player player) {
        if (dzManager.getDeathZones().isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No death zones configured.");
            return;
        }
        player.sendMessage(ChatColor.GRAY + "╔══════════════════════════════╗");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "  &c✦ Death Zones"));
        for (DeathZone zone : dzManager.getDeathZones().values()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "  " + zone.format()));
        }
        player.sendMessage(ChatColor.GRAY + "╚══════════════════════════════╝");
    }

    private void teleportZone(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /diewand tp <id>");
            return;
        }
        try {
            int id = Integer.parseInt(args[1]);
            DeathZone zone = dzManager.get(id);
            if (zone == null) {
                player.sendMessage(ChatColor.RED + "Death zone #" + id + " not found.");
                return;
            }
            player.teleport(zone.getCenter(player.getWorld()));
            player.sendMessage(ChatColor.GREEN + "Teleported to death zone #" + id);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid ID.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("matrx.admin")) return List.of();
        if (args.length == 1) {
            return List.of("wand", "pos1", "pos2", "save", "remove", "list", "tp");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("tp"))) {
            return dzManager.getDeathZones().keySet().stream()
                .map(String::valueOf)
                .filter(s -> s.startsWith(args[1]))
                .collect(Collectors.toList());
        }
        return List.of();
    }
}
