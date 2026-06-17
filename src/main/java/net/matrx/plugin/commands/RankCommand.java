package net.matrx.plugin.commands;

import net.matrx.plugin.managers.*;
import net.matrx.plugin.models.*;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.*;
import java.util.stream.Collectors;

public class RankCommand implements CommandExecutor, TabCompleter {
    private final RankManager rankManager;
    private final PlayerManager playerManager;
    private final AnimationManager animationManager;

    public RankCommand(RankManager rankManager, PlayerManager playerManager, AnimationManager animationManager) {
        this.rankManager = rankManager;
        this.playerManager = playerManager;
        this.animationManager = animationManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Usage: /rank <subcommand>");
                return true;
            }
            showPlayerRank(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            case "set" -> handleSet(sender, args);
            case "create" -> handleCreate(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "setprefix" -> handleSetPrefix(sender, args);
            case "setdisplay" -> handleSetDisplay(sender, args);
            case "setxp" -> handleSetXp(sender, args);
            case "addperm" -> handleAddPerm(sender, args);
            case "removeperm" -> handleRemovePerm(sender, args);
            default -> sender.sendMessage(ChatColor.RED + "Unknown subcommand. Available: list, info, set, create, delete, setprefix, setdisplay, setxp, addperm, removeperm");
        }
        return true;
    }

    private void showPlayerRank(Player player) {
        ParkourPlayer pPlayer = playerManager.getPlayer(player);
        Rank rank = rankManager.getRank(pPlayer.getRankName());
        if (rank == null) {
            player.sendMessage(ChatColor.RED + "Your rank could not be found!");
            return;
        }
        Rank nextRank = rankManager.getNextRank(rank);

        player.sendMessage(ChatColor.GRAY + "╔══════════════════════════════╗");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "  &6Your Rank: " + rank.getPrefix()));
        player.sendMessage(ChatColor.GRAY + "  XP: " + ChatColor.YELLOW + pPlayer.getXp());
        if (nextRank != null) {
            long needed = nextRank.getXpRequired() - pPlayer.getXp();
            player.sendMessage(ChatColor.GRAY + "  Next Rank: " + nextRank.getDisplayName());
            player.sendMessage(ChatColor.GRAY + "  XP Needed: " + ChatColor.YELLOW + Math.max(0, needed));
        } else {
            player.sendMessage(ChatColor.GOLD + "  You have reached the highest rank!");
        }
        player.sendMessage(ChatColor.GRAY + "╚══════════════════════════════╝");
    }

    private void handleList(CommandSender sender) {
        sender.sendMessage(ChatColor.GRAY + "╔══════════════════════════════╗");
        sender.sendMessage(ChatColor.GOLD + "  Available Ranks:");
        for (Rank rank : rankManager.getRanks().values()) {
            String unlockStatus = rank.isUnlockable()
                ? ChatColor.GREEN + " (" + rank.getXpRequired() + " XP)"
                : ChatColor.RED + " (Admin)";
            sender.sendMessage(ChatColor.GRAY + "  " + rank.getDisplayName() + unlockStatus);
        }
        sender.sendMessage(ChatColor.GRAY + "╚══════════════════════════════╝");
    }

    private void handleInfo(CommandSender sender, String[] args) {
        String rankName = args.length > 1 ? args[1] : (sender instanceof Player p ? playerManager.getPlayer(p).getRankName() : null);
        if (rankName == null) {
            sender.sendMessage(ChatColor.RED + "Usage: /rank info [name]");
            return;
        }
        Rank rank = rankManager.getRank(rankName);
        if (rank == null) {
            sender.sendMessage(ChatColor.RED + "Rank not found: " + rankName);
            return;
        }
        sender.sendMessage(ChatColor.GRAY + "╔══════════════════════════════╗");
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "  Rank: " + rank.getDisplayName()));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "  Prefix: " + rank.getPrefix()));
        sender.sendMessage(ChatColor.GRAY + "  Weight: " + ChatColor.WHITE + rank.getWeight());
        sender.sendMessage(ChatColor.GRAY + "  XP Required: " + ChatColor.YELLOW + (rank.isUnlockable() ? String.valueOf(rank.getXpRequired()) : "N/A (Admin)"));
        sender.sendMessage(ChatColor.GRAY + "  Permissions: " + ChatColor.WHITE + String.join(", ", rank.getPermissions()));
        sender.sendMessage(ChatColor.GRAY + "╚══════════════════════════════╝");
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("matrx.rank.set")) {
            sender.sendMessage(ChatColor.RED + "No permission!");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /rank set <player> <rank>");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
            return;
        }
        Rank rank = rankManager.getRank(args[2]);
        if (rank == null) {
            sender.sendMessage(ChatColor.RED + "Rank not found: " + args[2]);
            return;
        }
        ParkourPlayer pPlayer = playerManager.getPlayer(target);
        pPlayer.setRankName(rank.getName());
        sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s rank to " + rank.getDisplayName());
        target.sendMessage(ChatColor.GREEN + "Your rank has been set to " + rank.getDisplayName());
        rankManager.updateXpBar(target, pPlayer);
        rankManager.saveRanks();
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("matrx.rank.manage")) {
            sender.sendMessage(ChatColor.RED + "No permission!");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /rank create <name> [xp] [prefix]");
            return;
        }
        String name = args[1].toLowerCase();
        if (rankManager.getRank(name) != null) {
            sender.sendMessage(ChatColor.RED + "Rank already exists: " + name);
            return;
        }
        long xp = args.length > 2 ? parseLong(args[2], -1) : 0;
        String prefix = args.length > 3 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : "&7[" + name + "] ";
        Rank rank = new Rank(name, "&7" + name, prefix, xp, rankManager.getRanks().size() + 1, new ArrayList<>());
        rankManager.addRank(rank);
        sender.sendMessage(ChatColor.GREEN + "Created rank: " + name);
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("matrx.rank.manage")) {
            sender.sendMessage(ChatColor.RED + "No permission!");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /rank delete <name>");
            return;
        }
        if (rankManager.getRank(args[1]) == null) {
            sender.sendMessage(ChatColor.RED + "Rank not found: " + args[1]);
            return;
        }
        rankManager.removeRank(args[1]);
        sender.sendMessage(ChatColor.GREEN + "Deleted rank: " + args[1]);
    }

    private void handleSetPrefix(CommandSender sender, String[] args) {
        if (!sender.hasPermission("matrx.rank.manage")) {
            sender.sendMessage(ChatColor.RED + "No permission!");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /rank setprefix <rank> <prefix>");
            return;
        }
        Rank rank = rankManager.getRank(args[1]);
        if (rank == null) {
            sender.sendMessage(ChatColor.RED + "Rank not found: " + args[1]);
            return;
        }
        String prefix = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        rank.setPrefix(prefix);
        rankManager.saveRanks();
        sender.sendMessage(ChatColor.GREEN + "Set prefix for " + args[1]);
    }

    private void handleSetDisplay(CommandSender sender, String[] args) {
        if (!sender.hasPermission("matrx.rank.manage")) {
            sender.sendMessage(ChatColor.RED + "No permission!");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /rank setdisplay <rank> <display name>");
            return;
        }
        Rank rank = rankManager.getRank(args[1]);
        if (rank == null) {
            sender.sendMessage(ChatColor.RED + "Rank not found: " + args[1]);
            return;
        }
        String displayName = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        rank.setDisplayName(displayName);
        rankManager.saveRanks();
        sender.sendMessage(ChatColor.GREEN + "Set display name for " + args[1]);
    }

    private void handleSetXp(CommandSender sender, String[] args) {
        if (!sender.hasPermission("matrx.rank.manage")) {
            sender.sendMessage(ChatColor.RED + "No permission!");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /rank setxp <rank> <xp>");
            return;
        }
        Rank rank = rankManager.getRank(args[1]);
        if (rank == null) {
            sender.sendMessage(ChatColor.RED + "Rank not found: " + args[1]);
            return;
        }
        long xp = parseLong(args[2], 0);
        rank.setXpRequired(xp);
        rankManager.saveRanks();
        sender.sendMessage(ChatColor.GREEN + "Set XP required for " + args[1] + " to " + xp);
    }

    private void handleAddPerm(CommandSender sender, String[] args) {
        if (!sender.hasPermission("matrx.rank.manage")) {
            sender.sendMessage(ChatColor.RED + "No permission!");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /rank addperm <rank> <permission>");
            return;
        }
        Rank rank = rankManager.getRank(args[1]);
        if (rank == null) {
            sender.sendMessage(ChatColor.RED + "Rank not found: " + args[1]);
            return;
        }
        rank.addPermission(args[2]);
        rankManager.saveRanks();
        sender.sendMessage(ChatColor.GREEN + "Added permission to " + args[1]);
    }

    private void handleRemovePerm(CommandSender sender, String[] args) {
        if (!sender.hasPermission("matrx.rank.manage")) {
            sender.sendMessage(ChatColor.RED + "No permission!");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /rank removeperm <rank> <permission>");
            return;
        }
        Rank rank = rankManager.getRank(args[1]);
        if (rank == null) {
            sender.sendMessage(ChatColor.RED + "Rank not found: " + args[1]);
            return;
        }
        rank.removePermission(args[2]);
        rankManager.saveRanks();
        sender.sendMessage(ChatColor.GREEN + "Removed permission from " + args[1]);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("list", "info", "set", "create", "delete", "setprefix", "setdisplay", "setxp", "addperm", "removeperm");
        }
        if (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("info")) {
            if (args.length == 2) return null; // player names
            if (args.length == 3) return rankManager.getRanks().keySet().stream().collect(Collectors.toList());
        }
        if (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("setprefix")
            || args[0].equalsIgnoreCase("setdisplay") || args[0].equalsIgnoreCase("setxp")
            || args[0].equalsIgnoreCase("addperm") || args[0].equalsIgnoreCase("removeperm")) {
            if (args.length == 2) return rankManager.getRanks().keySet().stream().collect(Collectors.toList());
        }
        return List.of();
    }

    private long parseLong(String s, long def) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
