package net.matrx.plugin.commands;

import net.matrx.plugin.managers.*;
import net.matrx.plugin.models.*;
import net.matrx.plugin.gui.ParkourGUI;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.*;
import java.util.stream.Collectors;

public class ParkourCommand implements CommandExecutor, TabCompleter {
    private final CourseManager courseManager;
    private final PlayerManager playerManager;
    private final RankManager rankManager;
    private final AnimationManager animationManager;
    private final AchievementManager achievementManager;
    private final Map<UUID, Long> lastCommandTime = new HashMap<>();

    public ParkourCommand(CourseManager courseManager, PlayerManager playerManager,
                          RankManager rankManager, AnimationManager animationManager,
                          AchievementManager achievementManager) {
        this.courseManager = courseManager;
        this.playerManager = playerManager;
        this.rankManager = rankManager;
        this.animationManager = animationManager;
        this.achievementManager = achievementManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length == 0) {
            ParkourGUI.openMainMenu(player, courseManager, playerManager, rankManager, achievementManager);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help" -> sendHelp(player);
            case "list" -> listCourses(player);
            case "gui" -> ParkourGUI.openMainMenu(player, courseManager, playerManager, rankManager, achievementManager);
            case "stats" -> showStats(player, args);
            case "profile" -> showProfile(player);
            case "leaderboard" -> showLeaderboard(player, args);
            case "lb" -> showLeaderboard(player, args);
            case "leave" -> leaveCourse(player);
            case "ghost" -> toggleGhost(player);
            case "top" -> showTopPlayers(player);
            case "achievements" -> showAchievements(player);
            case "record" -> showGhostReplay(player, args);
            case "create" -> createCourse(player, args);
            case "delete" -> deleteCourse(player, args);
            case "setstart" -> setStart(player, args);
            case "setend" -> setEnd(player, args);
            case "addcp" -> addCheckpoint(player, args);
            case "removecp" -> removeCheckpoint(player, args);
            case "setreward" -> setReward(player, args);
            case "setreqrank" -> setRequiredRank(player, args);
            case "settier" -> setTier(player, args);
            case "setpar" -> setParTime(player, args);
            case "info" -> courseInfo(player, args);
            case "tpstart" -> teleportStart(player, args);
            case "tpcheckpoint" -> teleportCheckpoint(player, args);
            case "addstar" -> addHiddenStar(player, args);
            case "removestar" -> removeHiddenStar(player, args);
            case "stars" -> listStars(player, args);
            case "rename" -> renameCourse(player, args);
            case "setdisplay" -> setDisplayName(player, args);
            default -> player.sendMessage(ChatColor.RED + "Unknown subcommand. Use /pk help");
        }
        return true;
    }

    private void sendHelp(Player player) {
        boolean admin = player.hasPermission("matrx.admin") || player.hasPermission("matrx.course.create");
        player.sendMessage(ChatColor.GRAY + "╔══════════════════════════════╗");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "  &6✦ &eMatrx Parkour &7v1.0"));
        player.sendMessage(ChatColor.GRAY + "  /pk" + ChatColor.WHITE + " - Open hub menu");
        player.sendMessage(ChatColor.GRAY + "  /pk list" + ChatColor.WHITE + " - List courses");
        player.sendMessage(ChatColor.GRAY + "  /pk stats [player]" + ChatColor.WHITE + " - View stats");
        player.sendMessage(ChatColor.GRAY + "  /pk profile" + ChatColor.WHITE + " - Your profile");
        player.sendMessage(ChatColor.GRAY + "  /pk leaderboard <course>" + ChatColor.WHITE + " - Leaderboard");
        player.sendMessage(ChatColor.GRAY + "  /pk top" + ChatColor.WHITE + " - Top players");
        player.sendMessage(ChatColor.GRAY + "  /pk achievements" + ChatColor.WHITE + " - View achievements");
        player.sendMessage(ChatColor.GRAY + "  /pk leave" + ChatColor.WHITE + " - Leave course");
        player.sendMessage(ChatColor.GRAY + "  /pk info <course>" + ChatColor.WHITE + " - Course details");
        player.sendMessage(ChatColor.GRAY + "  /cp" + ChatColor.WHITE + " - Return to checkpoint");

        if (admin) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "  &c&l✦ Admin Commands:"));
            player.sendMessage(ChatColor.GRAY + "  /pk create/delete <name>");
            player.sendMessage(ChatColor.GRAY + "  /pk setstart/setend/addcp/removecp <name|id>");
            player.sendMessage(ChatColor.GRAY + "  /pk settier/setpar/setreward/setreqrank <name|id>");
            player.sendMessage(ChatColor.GRAY + "  /pk setdisplay/rename <name|id>");
            player.sendMessage(ChatColor.GRAY + "  /pk addstar/removestar <name|id>");
            player.sendMessage(ChatColor.GRAY + "  /pk tpstart/tpcheckpoint <name|id>");
        }
        player.sendMessage(ChatColor.GRAY + "╚══════════════════════════════╝");
    }

    private void listCourses(Player player) {
        if (courseManager.getCourses().isEmpty()) {
            player.sendMessage(ChatColor.RED + "No courses available.");
            return;
        }
        player.sendMessage(ChatColor.GRAY + "╔══════════════════════════════╗");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "  &6✦ Parkour Courses:"));
        for (Course course : courseManager.getCourses().values()) {
            String req = course.getRequiredRank().isEmpty() ? "" : " &8[&e" + course.getRequiredRank() + "+&8]";
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "  &7#" + course.getId() + " " + course.getTierDisplay() + " &f" + course.getDisplayName()
                + " &7(" + course.getTotalCheckpoints() + " CP, &e" + course.getXpReward() + " XP&7)"
                + req));
        }
        player.sendMessage(ChatColor.GRAY + "╚══════════════════════════════╝");
    }

    private void showStats(Player player, String[] args) {
        Player target = args.length > 1 ? Bukkit.getPlayer(args[1]) : player;
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player not found.");
            return;
        }
        ParkourPlayer pPlayer = playerManager.getPlayer(target);
        Rank rank = rankManager.getRank(pPlayer.getRankName());

        player.sendMessage(ChatColor.GRAY + "╔══════════════════════════════╗");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "  &6✦ " + target.getName() + "'s Stats"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "  Rank: " + (rank != null ? rank.getDisplayName() : "&7None")));
        player.sendMessage(ChatColor.GRAY + "  XP: &e" + pPlayer.getXp());
        player.sendMessage(ChatColor.GRAY + "  Courses Done: &f" + pPlayer.getTotalCompletions());
        player.sendMessage(ChatColor.GRAY + "  Best Combo: &f" + pPlayer.getHighestCombo() + "x");
        player.sendMessage(ChatColor.GRAY + "  Stars Found: &b" + pPlayer.getStarCount());
        player.sendMessage(ChatColor.GRAY + "  Achievements: &d" + pPlayer.getUnlockedAchievements().size() + "/" + Achievement.values().size());

        if (!pPlayer.getBestTimes().isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "  Best Times:");
            pPlayer.getBestTimes().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> {
                    Course c = courseManager.getCourse(e.getKey());
                    String cn = c != null ? c.getDisplayName() : e.getKey();
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "    &7" + cn + ": &e" + AnimationManager.formatTime(e.getValue())));
                });
        }
        player.sendMessage(ChatColor.GRAY + "╚══════════════════════════════╝");
    }

    private void showProfile(Player player) {
        ParkourPlayer pPlayer = playerManager.getPlayer(player);
        Rank rank = rankManager.getRank(pPlayer.getRankName());
        Rank nextRank = rankManager.getNextRank(rank);

        player.sendMessage(ChatColor.GRAY + "╔══════════════════════════════╗");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "  &6✦ &e" + player.getName() + " &8- Profile"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "  Rank: " + (rank != null ? rank.getPrefix() : "")));
        player.sendMessage(ChatColor.GRAY + "  XP: &e" + pPlayer.getXp());

        if (nextRank != null) {
            long needed = nextRank.getXpRequired() - pPlayer.getXp();
            long rankXp = nextRank.getXpRequired()
                - (rank != null ? rank.getXpRequired() : 0);
            long progress = pPlayer.getXp() - (rank != null ? rank.getXpRequired() : 0);
            int pct = rankXp > 0 ? (int) ((progress * 100) / rankXp) : 0;
            player.sendMessage(ChatColor.GRAY + "  Next: " + nextRank.getDisplayName()
                + " &7(" + Math.min(100, pct) + "%)"
                + " &8- &e" + Math.max(0, needed) + " XP left");
        }

        player.sendMessage(ChatColor.GRAY + "  Courses: &f" + pPlayer.getTotalCompletions());
        player.sendMessage(ChatColor.GRAY + "  Best Combo: &f" + pPlayer.getHighestCombo() + "x");
        player.sendMessage(ChatColor.GRAY + "  Stars: &b" + pPlayer.getStarCount());
        player.sendMessage(ChatColor.GRAY + "  Achievements: &d" + pPlayer.getUnlockedAchievements().size() + "/" + Achievement.values().size());
        player.sendMessage(ChatColor.GRAY + "╚══════════════════════════════╝");
    }

    private void showLeaderboard(Player player, String[] args) {
        if (args.length < 2) {
            ParkourGUI.openLeaderboardMenu(player, courseManager);
            return;
        }
        Course course = courseManager.getCourse(args[1]);
        if (course == null) {
            player.sendMessage(ChatColor.RED + "Course not found: " + args[1]);
            return;
        }
        ParkourGUI.openLeaderboardForCourse(player, courseManager, playerManager, course.getName());
    }

    private void showTopPlayers(Player player) {
        List<ParkourPlayer> top = playerManager.getAllPlayers().stream()
            .sorted((a, b) -> Long.compare(b.getXp(), a.getXp()))
            .limit(10)
            .collect(Collectors.toList());

        player.sendMessage(ChatColor.GRAY + "╔══════════════════════════════╗");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "  &6✦ Top Parkour Players"));
        if (top.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "  No players yet.");
        } else {
            for (int i = 0; i < top.size(); i++) {
                ParkourPlayer p = top.get(i);
                Rank r = rankManager.getRank(p.getRankName());
                String medal = switch (i) {
                    case 0 -> "&6#1";
                    case 1 -> "&7#2";
                    case 2 -> "&e#3";
                    default -> "&f#" + (i + 1);
                };
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "  " + medal + " &f" + p.getName()
                    + " &8- " + (r != null ? r.getDisplayName() : "&7?")
                    + " &8- &e" + p.getXp() + " XP"));
            }
        }
        player.sendMessage(ChatColor.GRAY + "╚══════════════════════════════╝");
    }

    private void showAchievements(Player player) {
        ParkourGUI.openAchievementsMenu(player, playerManager);
    }

    private void toggleGhost(Player player) {
        player.sendMessage(ChatColor.YELLOW + "Ghost replay feature coming soon!");
    }

    private void showGhostReplay(Player player, String[] args) {
        player.sendMessage(ChatColor.YELLOW + "Ghost replay coming soon!");
    }

    private void leaveCourse(Player player) {
        ParkourPlayer pPlayer = playerManager.getPlayer(player);
        if (pPlayer.getActiveCourse() == null) {
            player.sendMessage(ChatColor.RED + "You are not in a course.");
            return;
        }
        pPlayer.leaveCourse();
        player.sendMessage(ChatColor.GREEN + "Left the parkour course.");
    }

    // ============ ADMIN COMMANDS ============

    private void createCourse(Player player, String[] args) {
        if (!player.hasPermission("matrx.course.create")) {
            player.sendMessage(ChatColor.RED + "No permission!"); return;
        }
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /pk create <name>"); return;
        }
        String name = args[1].toLowerCase();
        if (courseManager.getCourse(name) != null) {
            player.sendMessage(ChatColor.RED + "Course already exists: " + name); return;
        }
        Course course = new Course(name, "&e" + name, null, null, new ArrayList<>(), "", 50);
        courseManager.addCourse(course);
        player.sendMessage(ChatColor.GREEN + "Created course #" + course.getId() + ": " + name);
        player.sendMessage(ChatColor.GRAY + "Use /pk setstart " + name + " to set start, /pk addcp " + name + " for checkpoints.");
        player.sendMessage(ChatColor.GRAY + "You can also use ID " + course.getId() + " instead of name.");
    }

    private void deleteCourse(Player player, String[] args) {
        if (!player.hasPermission("matrx.course.delete")) {
            player.sendMessage(ChatColor.RED + "No permission!"); return;
        }
        if (args.length < 2) { player.sendMessage(ChatColor.RED + "Usage: /pk delete <name>"); return; }
        if (courseManager.getCourse(args[1]) == null) {
            player.sendMessage(ChatColor.RED + "Course not found: " + args[1]); return;
        }
        courseManager.removeCourse(args[1]);
        player.sendMessage(ChatColor.GREEN + "Deleted course: " + args[1]);
    }

    private void setStart(Player player, String[] args) {
        if (!player.hasPermission("matrx.course.create")) { player.sendMessage(ChatColor.RED + "No permission!"); return; }
        if (args.length < 2) { player.sendMessage(ChatColor.RED + "Usage: /pk setstart <course>"); return; }
        Course course = courseManager.getCourse(args[1]);
        if (course == null) { player.sendMessage(ChatColor.RED + "Course not found."); return; }
        course.setStart(new Checkpoint(player.getLocation()));
        courseManager.saveCourses();
        player.sendMessage(ChatColor.GREEN + "Start set for " + args[1]);
    }

    private void setEnd(Player player, String[] args) {
        if (!player.hasPermission("matrx.course.create")) { player.sendMessage(ChatColor.RED + "No permission!"); return; }
        if (args.length < 2) { player.sendMessage(ChatColor.RED + "Usage: /pk setend <course>"); return; }
        Course course = courseManager.getCourse(args[1]);
        if (course == null) { player.sendMessage(ChatColor.RED + "Course not found."); return; }
        course.setEnd(new Checkpoint(player.getLocation()));
        courseManager.saveCourses();
        player.sendMessage(ChatColor.GREEN + "End set for " + args[1]);
    }

    private void addCheckpoint(Player player, String[] args) {
        if (!player.hasPermission("matrx.course.create")) { player.sendMessage(ChatColor.RED + "No permission!"); return; }
        if (args.length < 2) { player.sendMessage(ChatColor.RED + "Usage: /pk addcp <course>"); return; }
        Course course = courseManager.getCourse(args[1]);
        if (course == null) { player.sendMessage(ChatColor.RED + "Course not found."); return; }
        course.addCheckpoint(new Checkpoint(player.getLocation()));
        courseManager.saveCourses();
        player.sendMessage(ChatColor.GREEN + "Added checkpoint #" + course.getTotalCheckpoints() + " to " + args[1]);
    }

    private void removeCheckpoint(Player player, String[] args) {
        if (!player.hasPermission("matrx.course.create")) { player.sendMessage(ChatColor.RED + "No permission!"); return; }
        if (args.length < 3) { player.sendMessage(ChatColor.RED + "Usage: /pk removecp <course> <num>"); return; }
        Course course = courseManager.getCourse(args[1]);
        if (course == null) { player.sendMessage(ChatColor.RED + "Course not found."); return; }
        int index;
        try { index = Integer.parseInt(args[2]) - 1; } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid number."); return;
        }
        if (index < 0 || index >= course.getTotalCheckpoints()) {
            player.sendMessage(ChatColor.RED + "Checkpoint #" + args[2] + " doesn't exist. (1-" + course.getTotalCheckpoints() + ")"); return;
        }
        course.removeCheckpoint(index);
        courseManager.saveCourses();
        player.sendMessage(ChatColor.GREEN + "Removed checkpoint #" + args[2]);
    }

    private void setReward(Player player, String[] args) {
        if (!player.hasPermission("matrx.course.create")) { player.sendMessage(ChatColor.RED + "No permission!"); return; }
        if (args.length < 3) { player.sendMessage(ChatColor.RED + "Usage: /pk setreward <course> <xp>"); return; }
        Course course = courseManager.getCourse(args[1]);
        if (course == null) { player.sendMessage(ChatColor.RED + "Course not found."); return; }
        try {
            long xp = Long.parseLong(args[2]);
            course.setXpReward(xp);
            courseManager.saveCourses();
            player.sendMessage(ChatColor.GREEN + "Set XP reward to " + xp);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid number.");
        }
    }

    private void setRequiredRank(Player player, String[] args) {
        if (!player.hasPermission("matrx.course.create")) { player.sendMessage(ChatColor.RED + "No permission!"); return; }
        if (args.length < 3) { player.sendMessage(ChatColor.RED + "Usage: /pk setreqrank <course> <rank|none>"); return; }
        Course course = courseManager.getCourse(args[1]);
        if (course == null) { player.sendMessage(ChatColor.RED + "Course not found."); return; }
        String rankName = args[2].equalsIgnoreCase("none") ? "" : args[2].toLowerCase();
        course.setRequiredRank(rankName);
        courseManager.saveCourses();
        player.sendMessage(ChatColor.GREEN + "Required rank set to " + (rankName.isEmpty() ? "none" : rankName));
    }

    private void setTier(Player player, String[] args) {
        if (!player.hasPermission("matrx.course.create")) { player.sendMessage(ChatColor.RED + "No permission!"); return; }
        if (args.length < 3) { player.sendMessage(ChatColor.RED + "Usage: /pk settier <course> <1-5>"); return; }
        Course course = courseManager.getCourse(args[1]);
        if (course == null) { player.sendMessage(ChatColor.RED + "Course not found."); return; }
        try {
            int tier = Integer.parseInt(args[2]);
            if (tier < 1 || tier > 5) { player.sendMessage(ChatColor.RED + "Tier must be 1-5."); return; }
            course.setTier(tier);
            courseManager.saveCourses();
            player.sendMessage(ChatColor.GREEN + "Set tier to " + tier);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid number.");
        }
    }

    private void setParTime(Player player, String[] args) {
        if (!player.hasPermission("matrx.course.create")) { player.sendMessage(ChatColor.RED + "No permission!"); return; }
        if (args.length < 3) { player.sendMessage(ChatColor.RED + "Usage: /pk setpar <course> <seconds>"); return; }
        Course course = courseManager.getCourse(args[1]);
        if (course == null) { player.sendMessage(ChatColor.RED + "Course not found."); return; }
        try {
            double seconds = Double.parseDouble(args[2]);
            course.setParTime((long) (seconds * 1000));
            courseManager.saveCourses();
            player.sendMessage(ChatColor.GREEN + "Set par time to " + seconds + "s");
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid number.");
        }
    }

    private void setDisplayName(Player player, String[] args) {
        if (!player.hasPermission("matrx.course.create")) { player.sendMessage(ChatColor.RED + "No permission!"); return; }
        if (args.length < 3) { player.sendMessage(ChatColor.RED + "Usage: /pk setdisplay <course> <display name>"); return; }
        Course course = courseManager.getCourse(args[1]);
        if (course == null) { player.sendMessage(ChatColor.RED + "Course not found."); return; }
        String name = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        Course updated = new Course(course.getName(), name, course.getStart(), course.getEnd(),
            course.getCheckpoints(), course.getRequiredRank(), course.getXpReward());
        updated.setId(course.getId());
        updated.setTier(course.getTier());
        updated.setParTime(course.getParTime());
        for (Course.HiddenStar star : course.getHiddenStars()) updated.addHiddenStar(star);
        courseManager.getCourses().put(course.getName(), updated);
        courseManager.saveCourses();
        player.sendMessage(ChatColor.GREEN + "Display name updated.");
    }

    private void renameCourse(Player player, String[] args) {
        if (!player.hasPermission("matrx.course.create")) { player.sendMessage(ChatColor.RED + "No permission!"); return; }
        if (args.length < 3) { player.sendMessage(ChatColor.RED + "Usage: /pk rename <old> <new>"); return; }
        Course course = courseManager.getCourse(args[1]);
        if (course == null) { player.sendMessage(ChatColor.RED + "Course not found."); return; }
        String newName = args[2].toLowerCase();
        if (courseManager.getCourse(newName) != null) { player.sendMessage(ChatColor.RED + "Name already exists."); return; }
        courseManager.getCourses().remove(course.getName());
        courseManager.getCourses().put(newName, course);
        courseManager.saveCourses();
        player.sendMessage(ChatColor.GREEN + "Renamed to " + newName);
    }

    private void addHiddenStar(Player player, String[] args) {
        if (!player.hasPermission("matrx.course.create")) { player.sendMessage(ChatColor.RED + "No permission!"); return; }
        if (args.length < 2) { player.sendMessage(ChatColor.RED + "Usage: /pk addstar <course> [message]"); return; }
        Course course = courseManager.getCourse(args[1]);
        if (course == null) { player.sendMessage(ChatColor.RED + "Course not found."); return; }
        String msg = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "You found a hidden star!";
        String id = course.getName() + "_star_" + (course.getHiddenStars().size() + 1);
        Course.HiddenStar star = new Course.HiddenStar(
            id, player.getWorld().getName(),
            player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(),
            msg
        );
        course.addHiddenStar(star);
        courseManager.saveCourses();
        player.sendMessage(ChatColor.GREEN + "Hidden star added to " + args[1] + " at your location.");
        player.spawnParticle(Particle.GLOW_SQUID_INK, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
    }

    private void removeHiddenStar(Player player, String[] args) {
        if (!player.hasPermission("matrx.course.create")) { player.sendMessage(ChatColor.RED + "No permission!"); return; }
        if (args.length < 3) { player.sendMessage(ChatColor.RED + "Usage: /pk removestar <course> <number>"); return; }
        Course course = courseManager.getCourse(args[1]);
        if (course == null) { player.sendMessage(ChatColor.RED + "Course not found."); return; }
        int index;
        try { index = Integer.parseInt(args[2]) - 1; } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid number."); return;
        }
        if (index < 0 || index >= course.getHiddenStars().size()) {
            player.sendMessage(ChatColor.RED + "Star #" + args[2] + " doesn't exist."); return;
        }
        course.removeHiddenStar(index);
        courseManager.saveCourses();
        player.sendMessage(ChatColor.GREEN + "Removed star #" + args[2]);
    }

    private void listStars(Player player, String[] args) {
        if (!player.hasPermission("matrx.course.create")) { player.sendMessage(ChatColor.RED + "No permission!"); return; }
        if (args.length < 2) { player.sendMessage(ChatColor.RED + "Usage: /pk stars <course>"); return; }
        Course course = courseManager.getCourse(args[1]);
        if (course == null) { player.sendMessage(ChatColor.RED + "Course not found."); return; }
        if (course.getHiddenStars().isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No hidden stars in " + args[1]);
            return;
        }
        player.sendMessage(ChatColor.GRAY + "╔══════════════════════════════╗");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "  &b✦ Hidden Stars in " + course.getDisplayName()));
        for (int i = 0; i < course.getHiddenStars().size(); i++) {
            Course.HiddenStar star = course.getHiddenStars().get(i);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "  &b#" + (i + 1) + " &7" + star.id
                + " &8[" + (int)star.x + ", " + (int)star.y + ", " + (int)star.z + "]"));
        }
        player.sendMessage(ChatColor.GRAY + "╚══════════════════════════════╝");
    }

    private void courseInfo(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(ChatColor.RED + "Usage: /pk info <course>"); return; }
        Course course = courseManager.getCourse(args[1]);
        if (course == null) { player.sendMessage(ChatColor.RED + "Course not found."); return; }
        ParkourPlayer pPlayer = playerManager.getPlayer(player);
        long bestTime = pPlayer.getBestTime(course.getName());

        player.sendMessage(ChatColor.GRAY + "╔══════════════════════════════╗");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "  &6✦ " + course.getDisplayName()));
        player.sendMessage(ChatColor.GRAY + "  ID: &f#" + course.getId());
        player.sendMessage(ChatColor.GRAY + "  Internal Name: &f" + course.getName());
        player.sendMessage(ChatColor.GRAY + "  Tier: " + course.getTierDisplay());
        player.sendMessage(ChatColor.GRAY + "  Checkpoints: &f" + course.getTotalCheckpoints());
        player.sendMessage(ChatColor.GRAY + "  Base XP: &e" + course.getXpReward());
        player.sendMessage(ChatColor.GRAY + "  Hidden Stars: &b" + course.getHiddenStars().size());
        player.sendMessage(ChatColor.GRAY + "  Par Time: &b" + AnimationManager.formatTime(course.getParTime()));
        player.sendMessage(ChatColor.GRAY + "  Start Set: " + (course.getStart() != null ? "&aYes" : "&cNo"));
        player.sendMessage(ChatColor.GRAY + "  End Set: " + (course.getEnd() != null ? "&aYes" : "&cNo"));
        if (bestTime > 0) {
            String par = course.isUnderPar(bestTime) ? " &b✓" : "";
            player.sendMessage(ChatColor.GRAY + "  Your Best: &e" + AnimationManager.formatTime(bestTime) + par);
        }
        if (!course.getRequiredRank().isEmpty()) {
            Rank r = rankManager.getRank(course.getRequiredRank());
            player.sendMessage(ChatColor.GRAY + "  Requires: " + (r != null ? r.getDisplayName() : course.getRequiredRank()));
        }
        player.sendMessage(ChatColor.GRAY + "╚══════════════════════════════╝");
    }

    private void teleportStart(Player player, String[] args) {
        if (!player.hasPermission("matrx.course.create")) return;
        if (args.length < 2) { player.sendMessage(ChatColor.RED + "Usage: /pk tpstart <course>"); return; }
        Course course = courseManager.getCourse(args[1]);
        if (course == null || course.getStart() == null) { player.sendMessage(ChatColor.RED + "No start set."); return; }
        player.teleport(course.getStart().toLocation(player.getWorld()));
        player.sendMessage(ChatColor.GREEN + "Teleported to start.");
    }

    private void teleportCheckpoint(Player player, String[] args) {
        if (!player.hasPermission("matrx.course.create")) return;
        if (args.length < 3) { player.sendMessage(ChatColor.RED + "Usage: /pk tpcheckpoint <course> <num>"); return; }
        Course course = courseManager.getCourse(args[1]);
        if (course == null) { player.sendMessage(ChatColor.RED + "Course not found."); return; }
        int index;
        try { index = Integer.parseInt(args[2]) - 1; } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid number."); return;
        }
        if (index < 0 || index >= course.getCheckpoints().size()) {
            player.sendMessage(ChatColor.RED + "Checkpoint not found."); return;
        }
        player.teleport(course.getCheckpoints().get(index).toLocation(player.getWorld()));
        player.sendMessage(ChatColor.GREEN + "Teleported to checkpoint " + args[2]);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("help", "list", "gui", "stats", "profile",
                "leaderboard", "lb", "leave", "top", "achievements", "info"));
            if (sender.hasPermission("matrx.course.create")) {
                subs.addAll(List.of("create", "delete", "setstart", "setend", "addcp", "removecp",
                    "settier", "setpar", "setreward", "setreqrank", "setdisplay", "rename",
                    "addstar", "removestar", "stars", "tpstart", "tpcheckpoint"));
            }
            return subs.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("setstart")
            || args[0].equalsIgnoreCase("setend") || args[0].equalsIgnoreCase("addcp")
            || args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("tpstart")
            || args[0].equalsIgnoreCase("setreward") || args[0].equalsIgnoreCase("setreqrank")
            || args[0].equalsIgnoreCase("settier") || args[0].equalsIgnoreCase("setpar")
            || args[0].equalsIgnoreCase("setdisplay") || args[0].equalsIgnoreCase("rename")
            || args[0].equalsIgnoreCase("addstar") || args[0].equalsIgnoreCase("removestar")
            || args[0].equalsIgnoreCase("stars") || args[0].equalsIgnoreCase("leaderboard")
            || args[0].equalsIgnoreCase("lb") || args[0].equalsIgnoreCase("tpcheckpoint"))) {
            return courseManager.getCourses().keySet().stream()
                .filter(s -> s.startsWith(args[1].toLowerCase()))
                .collect(Collectors.toList());
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("removecp")
            || args[0].equalsIgnoreCase("tpcheckpoint") || args[0].equalsIgnoreCase("removestar"))) {
            Course course = courseManager.getCourse(args[1]);
            if (course != null) {
                int size = args[0].equalsIgnoreCase("removestar")
                    ? course.getHiddenStars().size() : course.getTotalCheckpoints();
                List<String> nums = new ArrayList<>();
                for (int i = 1; i <= size; i++) nums.add(String.valueOf(i));
                return nums;
            }
        }
        return List.of();
    }
}
