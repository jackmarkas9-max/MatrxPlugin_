package net.matrx.plugin.listeners;

import net.matrx.plugin.managers.*;
import net.matrx.plugin.models.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;
import java.util.*;

public class ParkourListener implements Listener {
    private final CourseManager courseManager;
    private final PlayerManager playerManager;
    private final RankManager rankManager;
    private final AnimationManager animationManager;
    private final ComboManager comboManager;
    private final AchievementManager achievementManager;

    public ParkourListener(CourseManager courseManager, PlayerManager playerManager,
                           RankManager rankManager, AnimationManager animationManager,
                           ComboManager comboManager, AchievementManager achievementManager) {
        this.courseManager = courseManager;
        this.playerManager = playerManager;
        this.rankManager = rankManager;
        this.animationManager = animationManager;
        this.comboManager = comboManager;
        this.achievementManager = achievementManager;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (event.getTo() == null) return;
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
            && event.getFrom().getBlockY() == event.getTo().getBlockY()
            && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        ParkourPlayer pPlayer = playerManager.getPlayer(player);
        Location to = event.getTo();

        handleSpecialBlocks(player, to);

        if (pPlayer.getActiveCourse() != null) {
            handleCheckpointDetection(player, pPlayer, to);
            handleStarDetection(player, pPlayer, to);
            handleEndDetection(player, pPlayer);
            updateGhostRecording(player, pPlayer, to);
        } else {
            handleStartDetection(player, pPlayer, to);
        }
    }

    private void handleStartDetection(Player player, ParkourPlayer pPlayer, Location to) {
        Course course = courseManager.getCourseAtStart(player);
        if (course == null) return;

        if (course.getRequiredRank() != null && !course.getRequiredRank().isEmpty()) {
            Rank requiredRank = rankManager.getRank(course.getRequiredRank());
            Rank playerRank = rankManager.getRank(pPlayer.getRankName());
            if (requiredRank != null && (playerRank == null || playerRank.getWeight() < requiredRank.getWeight())) {
                player.sendMessage(ChatColor.RED + "You need " + requiredRank.getDisplayName() + " rank to play this course!");
                return;
            }
        }

        pPlayer.startCourse(course.getName());
        startGhostRecording(player, pPlayer, course.getName());
        animationManager.playCourseStartEffects(player, course.getDisplayName());
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
            "&aStarted: &f" + course.getDisplayName()
            + " &7(Tier " + course.getTier() + " &7- &e" + course.getXpReward() + " XP&7)"));
    }

    private void handleCheckpointDetection(Player player, ParkourPlayer pPlayer, Location to) {
        Course course = courseManager.getCourse(pPlayer.getActiveCourse());
        if (course == null) return;

        int cpIndex = courseManager.findCheckpointIndex(player, pPlayer);
        if (cpIndex < 0) return;

        Checkpoint cp = course.getCheckpoints().get(cpIndex);
        Checkpoint last = pPlayer.getLastCheckpoint();
        if (last != null && last.matchesBlock(player.getLocation())) return;

        pPlayer.setLastCheckpoint(cp);
        int combo = comboManager.onCheckpoint(player, pPlayer, course, cpIndex);
        animationManager.playCheckpointTitle(player, cpIndex + 1, course.getTotalCheckpoints(), combo);
    }

    private void handleStarDetection(Player player, ParkourPlayer pPlayer, Location to) {
        Course.HiddenStar star = courseManager.findHiddenStar(player, pPlayer);
        if (star == null) return;
        if (pPlayer.hasFoundStar(star.id)) return;

        boolean added = pPlayer.addFoundStar(star.id);
        if (added) {
            animationManager.playStarFound(player, star.message, pPlayer.getStarCount());
            achievementManager.checkStarAchievements(player);
        }
    }

    private void handleEndDetection(Player player, ParkourPlayer pPlayer) {
        if (!courseManager.isAtEnd(player, pPlayer)) return;

        Course course = courseManager.getCourse(pPlayer.getActiveCourse());
        if (course == null) return;

        long elapsed = courseManager.finishCourse(player, pPlayer);
        if (elapsed < 0) return;

        boolean isPb = pPlayer.getBestTime(course.getName()) == elapsed;
        boolean isPar = course.isUnderPar(elapsed);

        long baseXp = course.getXpReward();
        long styleBonus = comboManager.calculateStyleBonus(pPlayer, baseXp);
        long totalXp = baseXp + styleBonus;

        playerManager.addXp(player, pPlayer, totalXp, animationManager);

        stopGhostRecording(player, pPlayer, course.getName(), elapsed);

        animationManager.playCourseCompleteEffects(player, course.getDisplayName(), elapsed, isPb, isPar);

        String msg = "&a✦ Course complete! &7Time: &e" + AnimationManager.formatTime(elapsed)
            + " &8| &a+" + baseXp + " XP";
        if (styleBonus > 0) msg += " &8(&e+" + styleBonus + " style&8)";
        if (isPb) msg += " &6&l✦ PB!";
        if (isPar) msg += " &b&l✦ PAR!";
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));

        boolean rankedUp = rankManager.checkAndRankup(player, pPlayer, animationManager);
        if (!rankedUp) rankManager.updateXpBar(player, pPlayer);

        achievementManager.checkCourseAchievements(player);
        achievementManager.checkSpeedAchievement(player, pPlayer, elapsed);
        achievementManager.checkParAchievement(player, pPlayer, course, elapsed);
        achievementManager.checkComboAchievements(player, pPlayer);
        achievementManager.checkPerfectionAchievement(player, pPlayer, course);

        broadcastRecord(player, course, elapsed, isPb);
    }

    private void broadcastRecord(Player player, Course course, long time, boolean isPb) {
        if (!isPb) return;
        Bukkit.getOnlinePlayers().forEach(p -> {
            if (!p.equals(player)) {
                p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&6&l✦ &e" + player.getName() + " &7set a new record on &e" + course.getDisplayName()
                    + " &7- &6" + AnimationManager.formatTime(time)));
            }
        });
    }

    private final Map<UUID, GhostRunData> ghostSessions = new java.util.HashMap<>();

    private void startGhostRecording(Player player, ParkourPlayer pPlayer, String courseName) {
        ghostSessions.put(player.getUniqueId(), new GhostRunData(courseName, System.currentTimeMillis(), new ArrayList<>()));
    }

    private void updateGhostRecording(Player player, ParkourPlayer pPlayer, Location to) {
        GhostRunData data = ghostSessions.get(player.getUniqueId());
        if (data == null) return;
        data.frames.add(new double[]{to.getX(), to.getY(), to.getZ(), to.getYaw(), to.getPitch()});
    }

    private void stopGhostRecording(Player player, ParkourPlayer pPlayer, String courseName, long time) {
        GhostRunData data = ghostSessions.remove(player.getUniqueId());
        if (data == null || data.frames.isEmpty()) return;

        ParkourPlayer.GhostRecording recording = new ParkourPlayer.GhostRecording(
            courseName, time, new ArrayList<>(data.frames)
        );
        pPlayer.setLastGhostRecording(recording);
        courseManager.saveGhostRecording(courseName, time, data.frames);
    }

    private static class GhostRunData {
        final String courseName;
        final long startTime;
        final List<double[]> frames;
        GhostRunData(String courseName, long startTime, List<double[]> frames) {
            this.courseName = courseName;
            this.startTime = startTime;
            this.frames = frames;
        }
    }

    // ============ SPECIAL PARKOUR BLOCKS ============

    private final Material[] SPEED_PADS = {Material.LIGHT_BLUE_CONCRETE, Material.LIGHT_BLUE_WOOL};
    private final Material[] BOUNCY_BLOCKS = {Material.SLIME_BLOCK, Material.HONEY_BLOCK};
    private final Material[] GRAVITY_PADS = {Material.PURPLE_CONCRETE, Material.PURPLE_WOOL};
    private final Material[] ICE_PATHS = {Material.PACKED_ICE, Material.BLUE_ICE};
    private final Material[] LAUNCH_PADS = {Material.RED_CONCRETE, Material.RED_WOOL};
    private final Material[] HEAL_PADS = {Material.GREEN_CONCRETE, Material.GREEN_WOOL};

    private void handleSpecialBlocks(Player player, Location to) {
        Material standingOn = to.getBlock().getType();
        Material below = to.clone().add(0, -1, 0).getBlock().getType();

        for (Material m : SPEED_PADS) {
            if (standingOn == m || below == m) {
                Vector dir = player.getLocation().getDirection().setY(0).normalize();
                player.setVelocity(dir.multiply(1.5));
                player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 0.5f, 1.5f);
                player.spawnParticle(Particle.CLOUD, player.getLocation().add(0, 0.5, 0), 10, 0.5, 0.1, 0.5, 0.01);
                return;
            }
        }

        for (Material m : BOUNCY_BLOCKS) {
            if (standingOn == m || below == m) {
                player.setVelocity(player.getVelocity().setY(1.2));
                player.playSound(player.getLocation(), Sound.ENTITY_SLIME_SQUISH, 1.0f, 0.8f);
                player.spawnParticle(Particle.ITEM_SLIME, player.getLocation().add(0, 0.5, 0), 15, 0.3, 0.1, 0.3, 0.01);
                return;
            }
        }

        for (Material m : GRAVITY_PADS) {
            if (standingOn == m || below == m) {
                player.setVelocity(player.getVelocity().setY(-0.5));
                player.setFallDistance(0);
                player.spawnParticle(Particle.MYCELIUM, player.getLocation().add(0, 0.5, 0), 8, 0.5, 0.1, 0.5, 0.01);
                return;
            }
        }

        for (Material m : ICE_PATHS) {
            if (standingOn == m || below == m) {
                Vector v = player.getVelocity();
                player.setVelocity(v.multiply(1.3));
                return;
            }
        }

        for (Material m : LAUNCH_PADS) {
            if (standingOn == m || below == m) {
                Vector dir = player.getLocation().getDirection();
                player.setVelocity(dir.multiply(2.0).setY(0.8));
                player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.8f, 1.2f);
                player.spawnParticle(Particle.FLAME, player.getLocation().add(0, 0.5, 0), 20, 0.5, 0.2, 0.5, 0.01);
                return;
            }
        }

        for (Material m : HEAL_PADS) {
            if (standingOn == m || below == m) {
                if (player.getHealth() < player.getMaxHealth()) {
                    player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 2));
                    player.spawnParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 5, 0.5, 0.3, 0.5, 0.01);
                }
                return;
            }
        }
    }
}
