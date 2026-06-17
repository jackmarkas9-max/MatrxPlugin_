package net.matrx.plugin.managers;

import net.matrx.plugin.models.*;
import org.bukkit.*;
import org.bukkit.entity.Player;

public class ComboManager {
    private final AnimationManager anim;

    public ComboManager(AnimationManager anim) {
        this.anim = anim;
    }

    public int onCheckpoint(Player player, ParkourPlayer pPlayer, Course course, int checkpointIndex) {
        int combo = pPlayer.addCombo();

        double mult = pPlayer.getComboMultiplier();

        anim.playCheckpointSound(player, combo);
        anim.playComboParticles(player, combo);

        anim.sendComboActionBar(player, combo, mult);

        if (combo == 5) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e&l✦ 5x Combo! &7Getting spicy!"));
        } else if (combo == 10) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6&l✦✦ 10x Combo! &7You're on fire!"));
            anim.playComboMilestone(player, combo);
        } else if (combo == 15) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&l✦✦✦ 15x Combo! &7Unstoppable!"));
        } else if (combo == 20) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5&l✦✦✦✦ 20x Combo! &7GODLIKE!"));
            anim.playComboMilestone(player, combo);
        } else if (combo == 25) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d&l✦✦✦✦✦ 25x Combo! &7LEGENDARY!"));
            anim.playComboMilestone(player, combo);
        }

        return combo;
    }

    public long calculateStyleBonus(ParkourPlayer pPlayer, long baseXp) {
        double mult = pPlayer.getComboMultiplier();
        return Math.round(baseXp * mult);
    }

    public String getComboColor(int combo) {
        if (combo >= 20) return "&d";
        if (combo >= 15) return "&5";
        if (combo >= 10) return "&6";
        if (combo >= 5) return "&e";
        return "&a";
    }
}
