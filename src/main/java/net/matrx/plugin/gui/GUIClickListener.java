package net.matrx.plugin.gui;

import net.matrx.plugin.managers.*;
import net.matrx.plugin.models.*;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GUIClickListener implements Listener {
    private final CourseManager courseManager;
    private final PlayerManager playerManager;
    private final RankManager rankManager;
    private final AchievementManager achievementManager;

    public GUIClickListener(CourseManager courseManager, PlayerManager playerManager,
                            RankManager rankManager, AchievementManager achievementManager) {
        this.courseManager = courseManager;
        this.playerManager = playerManager;
        this.rankManager = rankManager;
        this.achievementManager = achievementManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = ChatColor.stripColor(event.getView().getTitle());

        if (title.startsWith("✦ Parkour Hub ✦")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;
            String display = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());

            if (display.contains("Profile") || display.contains("Stats")) {
                showPlayerStats(player);
            } else if (display.contains("Courses")) {
                ParkourGUI.openCoursesMenu(player, courseManager, playerManager, rankManager);
            } else if (display.contains("Achievements")) {
                ParkourGUI.openAchievementsMenu(player, playerManager);
            } else if (display.contains("Leaderboard")) {
                ParkourGUI.openLeaderboardMenu(player, courseManager);
            } else if (display.contains("Ranks")) {
                ParkourGUI.openRanksMenu(player, rankManager, playerManager);
            }
        } else if (title.startsWith("✦ Courses ✦")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;
            String itemName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());

            for (Course course : courseManager.getCourses().values()) {
                String stripped = ChatColor.stripColor(course.getDisplayName());
                if (!stripped.equalsIgnoreCase(itemName)) continue;

                ParkourPlayer pPlayer = playerManager.getPlayer(player);

                if (!course.getRequiredRank().isEmpty()) {
                    Rank reqRank = rankManager.getRank(course.getRequiredRank());
                    Rank playerRank = rankManager.getRank(pPlayer.getRankName());
                    if (reqRank != null && (playerRank == null || playerRank.getWeight() < reqRank.getWeight())) {
                        player.sendMessage(ChatColor.RED + "You need " + reqRank.getDisplayName() + " rank for this course!");
                        return;
                    }
                }

                if (event.isRightClick()) {
                    showCourseInfo(player, course);
                    return;
                }

                if (course.getStart() != null) {
                    player.teleport(course.getStart().toLocation(player.getWorld()));
                    pPlayer.startCourse(course.getName());
                    player.sendMessage(ChatColor.GREEN + "Started: " + course.getDisplayName());
                    player.closeInventory();
                } else {
                    player.sendMessage(ChatColor.RED + "This course has no start set.");
                }
                return;
            }
        } else if (title.startsWith("✦ Achievements ✦")) {
            event.setCancelled(true);
        } else if (title.startsWith("✦ Leaderboards ✦")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;
            String itemName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());

            for (Course course : courseManager.getCourses().values()) {
                String stripped = ChatColor.stripColor(course.getDisplayName());
                if (stripped.equalsIgnoreCase(itemName)) {
                    ParkourGUI.openLeaderboardForCourse(player, courseManager, playerManager, course.getName());
                    return;
                }
            }
        } else if (title.contains("✦") && title.contains("#")) {
            event.setCancelled(true);
        } else if (title.startsWith("✦ Ranks ✦")) {
            event.setCancelled(true);
        } else {
            for (Course course : courseManager.getCourses().values()) {
                String stripped = ChatColor.stripColor(course.getDisplayName());
                if (title.contains(stripped) && title.contains("#")) {
                    event.setCancelled(true);
                    break;
                }
            }
        }
    }

    private void showPlayerStats(Player player) {
        ParkourPlayer pPlayer = playerManager.getPlayer(player);
        Rank rank = rankManager.getRank(pPlayer.getRankName());
        player.closeInventory();

        player.sendMessage(ChatColor.GRAY + "╔══════════════════════════════╗");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "  &6✦ " + player.getName() + "'s Profile"));
        player.sendMessage(ChatColor.GRAY + "  Rank: " + (rank != null ? rank.getDisplayName() : "None"));
        player.sendMessage(ChatColor.GRAY + "  XP: &e" + pPlayer.getXp());
        player.sendMessage(ChatColor.GRAY + "  Courses Done: &f" + pPlayer.getTotalCompletions());
        player.sendMessage(ChatColor.GRAY + "  Best Combo: &f" + pPlayer.getHighestCombo() + "x");
        player.sendMessage(ChatColor.GRAY + "  Checkpoints Passed: &f" + pPlayer.getTotalCheckpointsPassed());
        player.sendMessage(ChatColor.GRAY + "  Hidden Stars: &b" + pPlayer.getStarCount());
        player.sendMessage(ChatColor.GRAY + "  Achievements: &d" + pPlayer.getUnlockedAchievements().size() + "/" + Achievement.values().size());
        player.sendMessage(ChatColor.GRAY + "╚══════════════════════════════╝");
    }

    private void showCourseInfo(Player player, Course course) {
        ParkourPlayer pPlayer = playerManager.getPlayer(player);
        long bestTime = pPlayer.getBestTime(course.getName());

        player.sendMessage(ChatColor.GRAY + "╔══════════════════════════════╗");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "  &6✦ " + course.getDisplayName()));
        player.sendMessage(ChatColor.GRAY + "  Tier: " + course.getTierDisplay());
        player.sendMessage(ChatColor.GRAY + "  Checkpoints: &f" + course.getTotalCheckpoints());
        player.sendMessage(ChatColor.GRAY + "  Base XP: &e" + course.getXpReward());
        player.sendMessage(ChatColor.GRAY + "  Hidden Stars: &b" + course.getHiddenStars().size());
        player.sendMessage(ChatColor.GRAY + "  Par Time: &b" + AnimationManager.formatTime(course.getParTime()));
        if (bestTime > 0) {
            String par = course.isUnderPar(bestTime) ? " &b✓" : "";
            player.sendMessage(ChatColor.GRAY + "  Your Best: &e" + AnimationManager.formatTime(bestTime) + par);
        }
        if (!course.getRequiredRank().isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "  Requires: " + (
                rankManager.getRank(course.getRequiredRank()) != null
                    ? rankManager.getRank(course.getRequiredRank()).getDisplayName()
                    : course.getRequiredRank()));
        }
        player.sendMessage(ChatColor.GRAY + "╚══════════════════════════════╝");
    }
}
