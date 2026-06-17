package net.matrx.plugin.managers;

import net.matrx.plugin.models.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import java.util.*;

public class AchievementManager {
    private final PlayerManager playerManager;
    private final AnimationManager anim;

    public AchievementManager(PlayerManager playerManager, AnimationManager anim) {
        this.playerManager = playerManager;
        this.anim = anim;
    }

    public boolean checkAndUnlock(Player player, String achievementId) {
        ParkourPlayer pPlayer = playerManager.getPlayer(player);
        if (pPlayer.hasAchievement(achievementId)) return false;

        Achievement achievement = Achievement.get(achievementId);
        if (achievement == null) return false;

        pPlayer.unlockAchievement(achievementId);
        pPlayer.addXp(achievement.getXpReward());

        anim.playAchievementUnlock(player, achievement);
        return true;
    }

    public void checkAllAchievements(Player player) {
        ParkourPlayer pPlayer = playerManager.getPlayer(player);

        if (pPlayer.getTotalCompletions() >= 1) checkAndUnlock(player, "first_steps");
        if (pPlayer.getTotalCompletions() >= 5) checkAndUnlock(player, "explorer");
        if (pPlayer.getTotalCompletions() >= 50) checkAndUnlock(player, "veteran");
        if (pPlayer.getTotalCompletions() >= 100) checkAndUnlock(player, "marathon");
        if (pPlayer.getHighestCombo() >= 10) checkAndUnlock(player, "combo_king");
        if (pPlayer.getHighestCombo() >= 25) checkAndUnlock(player, "combo_god");
        if (pPlayer.getStarCount() >= 1) checkAndUnlock(player, "star_gazer");
        if (pPlayer.getStarCount() >= 10) checkAndUnlock(player, "star_collector");

        Rank rank = playerManager.getPlayerRank(pPlayer);
        if (rank != null && rank.getWeight() >= 4) checkAndUnlock(player, "diamond_hands");
        if (rank != null && rank.getWeight() >= 6) checkAndUnlock(player, "legendary");
    }

    public void checkCourseAchievements(Player player) {
        ParkourPlayer pPlayer = playerManager.getPlayer(player);
        checkAndUnlock(player, "first_steps");
        if (pPlayer.getTotalCompletions() >= 5) checkAndUnlock(player, "explorer");
        if (pPlayer.getTotalCompletions() >= 50) checkAndUnlock(player, "veteran");
        if (pPlayer.getTotalCompletions() >= 100) checkAndUnlock(player, "marathon");
    }

    public void checkComboAchievements(Player player, ParkourPlayer pPlayer) {
        checkAndUnlock(player, "combo_king");
        checkAndUnlock(player, "combo_god");
    }

    public void checkStarAchievements(Player player) {
        ParkourPlayer pPlayer = playerManager.getPlayer(player);
        checkAndUnlock(player, "star_gazer");
        if (pPlayer.getStarCount() >= 10) checkAndUnlock(player, "star_collector");
    }

    public void checkSpeedAchievement(Player player, ParkourPlayer pPlayer, long time) {
        if (time < 10000) checkAndUnlock(player, "speed_runner");
    }

    public void checkParAchievement(Player player, ParkourPlayer pPlayer, Course course, long time) {
        if (course.isUnderPar(time)) checkAndUnlock(player, "speed_demon");
    }

    public void checkPerfectionAchievement(Player player, ParkourPlayer pPlayer, Course course) {
        if (pPlayer.getComboCount() >= course.getTotalCheckpoints()) {
            checkAndUnlock(player, "perfectionist");
        }
    }

    public void checkDedicationAchievement(Player player) {
        checkAndUnlock(player, "dedication");
    }

    public List<Achievement> getUnlocked(ParkourPlayer pPlayer) {
        List<Achievement> result = new ArrayList<>();
        for (String id : pPlayer.getUnlockedAchievements()) {
            Achievement a = Achievement.get(id);
            if (a != null) result.add(a);
        }
        return result;
    }

    public List<Achievement> getLocked(ParkourPlayer pPlayer) {
        List<Achievement> result = new ArrayList<>();
        for (Achievement a : Achievement.values()) {
            if (!pPlayer.getUnlockedAchievements().contains(a.getId())) {
                result.add(a);
            }
        }
        return result;
    }
}
