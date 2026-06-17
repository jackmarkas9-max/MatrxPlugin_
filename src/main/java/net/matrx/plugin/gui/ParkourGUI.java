package net.matrx.plugin.gui;

import net.matrx.plugin.managers.*;
import net.matrx.plugin.models.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import java.util.*;
import java.util.stream.Collectors;

public class ParkourGUI {

    private static final String BG = " ";

    public static void openMainMenu(Player player, CourseManager courseManager,
                                     PlayerManager playerManager, RankManager rankManager,
                                     AchievementManager achievementManager) {
        Inventory inv = createBase(player, 27, "&8✦ Parkour Hub ✦");

        ParkourPlayer pPlayer = playerManager.getPlayer(player);
        Rank rank = rankManager.getRank(pPlayer.getRankName());

        ItemStack profile = createSkull(player,
            "&6✦ " + player.getName(),
            "&7Rank: " + (rank != null ? rank.getDisplayName() : "&7None"),
            "&7XP: &e" + pPlayer.getXp(),
            "&7Courses Done: &f" + pPlayer.getTotalCompletions(),
            "&7Best Combo: &f" + pPlayer.getHighestCombo() + "x",
            "&7Stars Found: &b" + pPlayer.getStarCount(),
            "&7Achievements: &d" + pPlayer.getUnlockedAchievements().size() + "/" + Achievement.values().size(),
            "",
            "&eClick for detailed stats");
        inv.setItem(10, profile);

        ItemStack courses = createItem(Material.GRASS_BLOCK, "&6✦ Courses",
            "&7Browse & play parkour courses",
            "&7Tiers: " + formatTiers(courseManager),
            "",
            "&eClick to browse courses");
        inv.setItem(12, courses);

        ItemStack achievements = createItem(Material.NETHER_STAR, "&6✦ Achievements",
            "&7View your achievements and progress",
            "&dUnlocked: &f" + pPlayer.getUnlockedAchievements().size() + "/" + Achievement.values().size(),
            "",
            "&eClick to view achievements");
        inv.setItem(14, achievements);

        ItemStack leaderboard = createItem(Material.BEACON, "&6✦ Leaderboards",
            "&7View top times for each course",
            "",
            "&eClick to view leaderboards");
        inv.setItem(16, leaderboard);

        ItemStack ranks = createItem(Material.DIAMOND, "&6✦ Ranks",
            "&7View available ranks and progression",
            "",
            "&eClick to view ranks");
        inv.setItem(22, ranks);

        player.openInventory(inv);
    }

    public static void openCoursesMenu(Player player, CourseManager courseManager,
                                        PlayerManager playerManager, RankManager rankManager) {
        Map<String, Course> courses = courseManager.getCourses();
        int size = clampSize(courses.size());
        Inventory inv = createBase(player, size, "&8✦ Courses ✦");

        int slot = 0;
        for (Course course : courses.values()) {
            List<String> lore = new ArrayList<>();
            lore.add("&7Tier: " + course.getTierDisplay());
            lore.add("&7Checkpoints: &f" + course.getTotalCheckpoints());
            lore.add("&7Base XP: &e" + course.getXpReward());
            lore.add("&7Hidden Stars: &b" + course.getHiddenStars().size());

            ParkourPlayer pPlayer = playerManager.getPlayer(player);
            long bestTime = pPlayer.getBestTime(course.getName());
            if (bestTime > 0) {
                String parStr = course.isUnderPar(bestTime) ? " &b✓ PAR" : "";
                lore.add("&7Your Best: &e" + AnimationManager.formatTime(bestTime) + parStr);
            } else {
                lore.add("&7Not completed yet");
            }

            lore.add("&7Par Time: &b" + AnimationManager.formatTime(course.getParTime()));

            if (!course.getRequiredRank().isEmpty()) {
                Rank reqRank = rankManager.getRank(course.getRequiredRank());
                Rank playerRank = rankManager.getRank(pPlayer.getRankName());
                if (reqRank != null && (playerRank == null || playerRank.getWeight() < reqRank.getWeight())) {
                    lore.add("");
                    lore.add("&cRequires: " + reqRank.getDisplayName());
                } else {
                    lore.add("");
                    lore.add("&a✔ Requirements met!");
                }
            }

            if (bestTime > 0) {
                lore.add("");
                lore.add("&eLeft-click to start");
                lore.add("&7Right-click for info");
            } else {
                lore.add("");
                lore.add("&eClick to start this course!");
            }

            Material icon = switch (course.getTier()) {
                case 2 -> Material.LEATHER_BOOTS;
                case 3 -> Material.CHAINMAIL_BOOTS;
                case 4 -> Material.IRON_BOOTS;
                case 5 -> Material.DIAMOND_BOOTS;
                default -> Material.GOLDEN_BOOTS;
            };

            ItemStack item = createItem(icon, "&" + (course.getTier() >= 4 ? "c" : "e") + course.getDisplayName(),
                lore.toArray(new String[0]));
            inv.setItem(slot++, item);
        }

        if (slot == 0) {
            inv.setItem(13, createItem(Material.BARRIER, "&cNo courses available",
                "&7Ask an admin to create some courses."));
        }

        player.openInventory(inv);
    }

    public static void openAchievementsMenu(Player player, PlayerManager playerManager) {
        ParkourPlayer pPlayer = playerManager.getPlayer(player);
        int size = clampSize(Achievement.values().size());
        Inventory inv = createBase(player, size, "&8✦ Achievements ✦");

        int slot = 0;
        for (Achievement achievement : Achievement.values()) {
            boolean unlocked = pPlayer.hasAchievement(achievement.getId());
            int xpReward = unlocked ? 0 : achievement.getXpReward();
            ItemStack item = achievement.createIcon(unlocked);
            inv.setItem(slot++, item);
        }

        player.openInventory(inv);
    }

    public static void openLeaderboardMenu(Player player, CourseManager courseManager) {
        Map<String, Course> courses = courseManager.getCourses();
        if (courses.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No courses available.");
            return;
        }
        int size = clampSize(courses.size());
        Inventory inv = createBase(player, size, "&8✦ Leaderboards ✦");

        int slot = 0;
        for (Course course : courses.values()) {
            ItemStack item = createItem(Material.MAP, "&6" + course.getDisplayName(),
                "&7Tier: " + course.getTierDisplay(),
                "&7Checkpoints: &f" + course.getTotalCheckpoints(),
                "",
                "&eClick to view leaderboard");
            inv.setItem(slot++, item);
        }

        player.openInventory(inv);
    }

    public static void openRanksMenu(Player player, RankManager rankManager, PlayerManager playerManager) {
        Map<String, Rank> ranks = rankManager.getRanks();
        ParkourPlayer pPlayer = playerManager.getPlayer(player);
        int size = clampSize(ranks.size() + 1);
        Inventory inv = createBase(player, size, "&8✦ Ranks ✦");

        int slot = 0;
        for (Rank rank : ranks.values()) {
            boolean isCurrent = pPlayer.getRankName().equals(rank.getName());
            boolean isUnlocked = rankManager.getPlayerRank(pPlayer) != null
                && rankManager.getPlayerRank(pPlayer).getWeight() >= rank.getWeight();

            List<String> lore = new ArrayList<>();
            lore.add("&7Rank: " + rank.getDisplayName());
            lore.add("&7Weight: &f" + rank.getWeight());

            if (rank.isUnlockable()) {
                if (isUnlocked) {
                    lore.add("&a✔ Unlocked!");
                    if (isCurrent) lore.add("&6✦ Your current rank!");
                } else {
                    lore.add("&7Required XP: &e" + rank.getXpRequired());
                    lore.add("&7Your XP: &e" + pPlayer.getXp());
                    long needed = rank.getXpRequired() - pPlayer.getXp();
                    lore.add(needed > 0 ? "&cNeeded: " + needed + " XP" : "&a✔ Available to unlock!");
                }
            } else {
                lore.add("&cAdmin rank");
            }

            if (!rank.getPermissions().isEmpty()) {
                lore.add("&7Permissions:");
                for (String perm : rank.getPermissions()) {
                    lore.add("  &8- &f" + perm);
                }
            }

            Material mat = getRankMaterial(slot);
            ItemStack item = createItem(mat, "&" + (isUnlocked ? "a" : "7") + rank.getDisplayName()
                + (isCurrent ? " &6✦" : ""), lore.toArray(new String[0]));
            inv.setItem(slot++, item);
        }

        player.openInventory(inv);
    }

    public static void openLeaderboardForCourse(Player player, CourseManager courseManager,
                                                  PlayerManager playerManager, String courseName) {
        Course course = courseManager.getCourse(courseName);
        if (course == null) {
            player.sendMessage(ChatColor.RED + "Course not found.");
            return;
        }

        List<Map.Entry<String, Long>> times = playerManager.getAllPlayers().stream()
            .filter(p -> p.getBestTime(courseName) > 0)
            .sorted(Comparator.comparingLong(p -> p.getBestTime(courseName)))
            .limit(10)
            .map(p -> new AbstractMap.SimpleEntry<>(p.getName(), p.getBestTime(courseName)))
            .collect(Collectors.toList());

        Inventory inv = createBase(player, 27, "&8✦ " + course.getDisplayName() + " ✦");

        if (times.isEmpty()) {
            inv.setItem(13, createItem(Material.BARRIER, "&cNo times recorded",
                "&7Be the first to complete this course!"));
        } else {
            for (int i = 0; i < Math.min(10, times.size()); i++) {
                Map.Entry<String, Long> entry = times.get(i);
                String medal = switch (i) {
                    case 0 -> "&6#1";
                    case 1 -> "&7#2";
                    case 2 -> "&e#3";
                    default -> "&f#" + (i + 1);
                };

                Material mat = switch (i) {
                    case 0 -> Material.GOLD_INGOT;
                    case 1 -> Material.IRON_INGOT;
                    case 2 -> Material.COPPER_INGOT;
                    default -> Material.STONE;
                };

                ItemStack item = createPlayerHead(entry.getKey(),
                    ChatColor.translateAlternateColorCodes('&', medal + " &f" + entry.getKey()),
                    "&7Time: &e" + AnimationManager.formatTime(entry.getValue()),
                    "&7Course: &f" + course.getDisplayName());
                inv.setItem(i + 10, item);
            }
        }

        player.openInventory(inv);
    }

    // ============ HELPERS ============

    private static Inventory createBase(Player player, int size, String title) {
        return Bukkit.createInventory(null, Math.max(27, Math.min(54, size)), ChatColor.translateAlternateColorCodes('&', title));
    }

    private static int clampSize(int count) {
        int rows = (count / 9) + 1;
        return Math.max(27, Math.min(54, rows * 9));
    }

    private static ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        meta.setLore(Arrays.stream(lore)
            .map(l -> ChatColor.translateAlternateColorCodes('&', l))
            .toList());
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createSkull(Player player, String name, String... lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(player);
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        meta.setLore(Arrays.stream(lore)
            .map(l -> ChatColor.translateAlternateColorCodes('&', l))
            .toList());
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createPlayerHead(String playerName, String name, String... lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.stream(lore)
            .map(l -> ChatColor.translateAlternateColorCodes('&', l))
            .toList());
        OfflinePlayer offPlayer = Bukkit.getOfflinePlayerIfCached(playerName);
        if (offPlayer != null) meta.setOwningPlayer(offPlayer);
        item.setItemMeta(meta);
        return item;
    }

    private static Material getRankMaterial(int index) {
        return switch (index) {
            case 0 -> Material.COAL;
            case 1 -> Material.IRON_INGOT;
            case 2 -> Material.GOLD_INGOT;
            case 3 -> Material.DIAMOND;
            case 4 -> Material.EMERALD;
            case 5 -> Material.NETHER_STAR;
            default -> Material.AMETHYST_SHARD;
        };
    }

    private static String formatTiers(CourseManager cm) {
        StringBuilder sb = new StringBuilder();
        for (int[] tier : cm.getCourseTiers()) {
            String color = switch (tier[0]) {
                case 1 -> "&a";
                case 2 -> "&e";
                case 3 -> "&6";
                case 4 -> "&c";
                case 5 -> "&4";
                default -> "&7";
            };
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(ChatColor.translateAlternateColorCodes('&', color + "T" + tier[0] + ":" + tier[1]));
        }
        return sb.toString();
    }
}
