package net.matrx.plugin.managers;

import net.matrx.plugin.models.Achievement;
import net.matrx.plugin.models.ParkourPlayer;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.meta.FireworkMeta;
import java.util.*;

public class AnimationManager {
    private final boolean enabled;
    private final Random random = new Random();

    public AnimationManager(boolean enabled) {
        this.enabled = enabled;
    }

    public void playCheckpointSound(Player player, int combo) {
        if (!enabled) return;
        float pitch = Math.min(2.0f, 0.8f + (combo * 0.05f));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, pitch);

        if (combo > 0 && combo % 5 == 0) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7f, 1.5f);
        }
    }

    public void playComboParticles(Player player, int combo) {
        if (!enabled) return;
        Location loc = player.getLocation().add(0, 1, 0);
        int count = Math.min(50, 10 + combo * 2);

        if (combo >= 20) {
            player.spawnParticle(Particle.FLASH, loc, 1, 0, 0, 0, 0);
            player.spawnParticle(Particle.END_ROD, loc, count, 1.0, 0.5, 1.0, 0.02);
            player.spawnParticle(Particle.TOTEM_OF_UNDYING, loc, count / 2, 0.5, 0.5, 0.5, 0.3);
        } else if (combo >= 10) {
            player.spawnParticle(Particle.ENCHANT, loc, count, 0.8, 0.5, 0.8, 0.5);
            player.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, count / 2, 0.5, 0.3, 0.5, 0.01);
        } else if (combo >= 5) {
            player.spawnParticle(Particle.END_ROD, loc, count, 0.5, 0.3, 0.5, 0.01);
            player.spawnParticle(Particle.ENCHANT, loc, count / 2, 0.5, 0.3, 0.5, 0.3);
        } else {
            player.spawnParticle(Particle.END_ROD, loc, count, 0.3, 0.2, 0.3, 0.01);
        }
    }

    public void playComboMilestone(Player player, int combo) {
        if (!enabled) return;
        Location loc = player.getLocation();
        player.sendTitle(
            ChatColor.translateAlternateColorCodes('&', "&" + (combo >= 20 ? "d" : "6") + combo + "x COMBO!"),
            ChatColor.translateAlternateColorCodes('&', getComboMessage(combo)),
            5, 30, 10
        );
        player.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.0f);
        player.spawnParticle(Particle.FIREWORK, loc.add(0, 1, 0), 30, 1, 1, 1, 0.05);
    }

    private String getComboMessage(int combo) {
        if (combo >= 25) return "&d&lLEGENDARY!";
        if (combo >= 20) return "&5&lGODLIKE!";
        if (combo >= 15) return "&c&lUNSTOPPABLE!";
        if (combo >= 10) return "&6&lON FIRE!";
        if (combo >= 5) return "&e&lGETTING SPICY!";
        return "";
    }

    public void sendComboActionBar(Player player, int combo, double mult) {
        String color = combo >= 20 ? "&d" : combo >= 10 ? "&6" : combo >= 5 ? "&e" : "&a";
        String bar = ChatColor.translateAlternateColorCodes('&',
            color + "✦ COMBO x" + combo + " &8[");
        int filled = Math.min(20, combo);
        for (int i = 0; i < 20; i++) {
            bar += i < filled ? (color + "■") : "&7■";
        }
        bar += "&8] &f" + String.format("%.1f", mult) + "x";
        player.sendActionBar(ChatColor.translateAlternateColorCodes('&', bar));
    }

    public void playCheckpointTitle(Player player, int checkpointNum, int total, int combo) {
        if (!enabled) return;
        String color = combo >= 20 ? "&d" : combo >= 10 ? "&6" : combo >= 5 ? "&e" : "&a";
        player.sendTitle(
            ChatColor.translateAlternateColorCodes('&', color + "&lCHECKPOINT &8- &f#" + checkpointNum),
            ChatColor.translateAlternateColorCodes('&', "&7" + checkpointNum + " / " + total + " &8| " + color + combo + "x Combo"),
            5, 25, 5
        );
    }

    public void playCourseCompleteEffects(Player player, String courseName, long time, boolean isPb, boolean isPar) {
        if (!enabled) return;
        Location loc = player.getLocation();

        String title = isPb ? "&6&l✦ NEW RECORD! ✦" : "&a&l✦ COURSE COMPLETE! ✦";
        String subtitle = "&7" + courseName + " &8- &e" + formatTime(time);
        if (isPar) subtitle += " &8[&bPAR&8]";
        if (isPb) subtitle += " &8[&6PB&8]";

        player.sendTitle(
            ChatColor.translateAlternateColorCodes('&', title),
            ChatColor.translateAlternateColorCodes('&', subtitle),
            10, 60, 10
        );

        player.playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        player.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.2f);

        int fireworkCount = isPb ? 5 : 3;
        for (int i = 0; i < fireworkCount; i++) {
            Bukkit.getScheduler().runTaskLater(player.getServer().getPluginManager().getPlugin("Matrx"),
                () -> spawnFirework(loc.clone().add(random.nextDouble() * 4 - 2, 1, random.nextDouble() * 4 - 2),
                    isPb),
                i * 5L);
        }

        player.spawnParticle(Particle.FIREWORK, loc, 50, 2, 1, 2, 0.05);
        player.spawnParticle(Particle.TOTEM_OF_UNDYING, loc.add(0, 1, 0), 40, 1, 1, 1, 0.3);

        if (isPb) {
            player.spawnParticle(Particle.FLASH, loc, 1, 0, 0, 0, 0);
        }
    }

    public void playRankupEffects(Player player, String rankName) {
        if (!enabled) return;
        Location loc = player.getLocation();
        String cleanName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', rankName));

        player.sendTitle(
            ChatColor.translateAlternateColorCodes('&', "&6&l✦ RANK UP! ✦"),
            ChatColor.translateAlternateColorCodes('&', "&eYou are now &f" + rankName),
            10, 60, 10
        );

        player.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.0f, 1.5f);
        player.playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        spawnRankParticles(player, loc, cleanName);

        for (int i = 0; i < 3; i++) {
            spawnFirework(loc.clone().add(random.nextDouble() * 3 - 1.5, 0, random.nextDouble() * 3 - 1.5), true);
        }
    }

    private void spawnRankParticles(Player player, Location loc, String rankName) {
        loc = loc.add(0, 1, 0);
        switch (rankName.toLowerCase()) {
            case "stone" ->
                player.spawnParticle(Particle.CLOUD, loc, 40, 2, 2, 2, 0.05);
            case "bronze" ->
                player.spawnParticle(Particle.WAX_ON, loc, 30, 2, 2, 2, 0.02);
            case "silver" ->
                player.spawnParticle(Particle.END_ROD, loc, 40, 2, 2, 2, 0.03);
            case "gold" ->
                player.spawnParticle(Particle.ENCHANT, loc, 50, 2, 2, 2, 0.5);
            case "diamond" ->
                player.spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 40, 2, 3, 2, 0.5);
            case "emerald" ->
                player.spawnParticle(Particle.HAPPY_VILLAGER, loc, 40, 2, 2, 2, 0.5);
            case "ruby" ->
                player.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 50, 2, 2, 2, 0.02);
            case "sapphire" ->
                player.spawnParticle(Particle.SNOWFLAKE, loc, 50, 2, 2, 2, 0.05);
            case "legend" ->
                player.spawnParticle(Particle.PORTAL, loc, 60, 2, 3, 2, 0.3);
            case "mythic" -> {
                player.spawnParticle(Particle.FLASH, loc, 1, 0, 0, 0, 0);
                player.spawnParticle(Particle.DRAGON_BREATH, loc, 50, 2, 3, 2, 0.05);
            }
            case "admin" ->
                player.spawnParticle(Particle.FIREWORK, loc, 50, 3, 3, 3, 0.1);
            default ->
                player.spawnParticle(Particle.END_ROD, loc, 40, 3, 3, 3, 0.1);
        }
    }

    public void playCourseStartEffects(Player player, String courseName) {
        if (!enabled) return;
        player.sendTitle(
            ChatColor.translateAlternateColorCodes('&', "&e&l" + courseName),
            ChatColor.translateAlternateColorCodes('&', "&7Go!"),
            10, 20, 10
        );
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
        player.spawnParticle(Particle.CLOUD, player.getLocation().add(0, 0.5, 0), 10, 0.5, 0.2, 0.5, 0.01);
    }

    public void playXpGainEffects(Player player, long xp) {
        if (!enabled) return;
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);
        player.spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.5);
    }

    public void playStarFound(Player player, String message, int totalStars) {
        if (!enabled) return;
        Location loc = player.getLocation();

        player.sendTitle(
            ChatColor.translateAlternateColorCodes('&', "&b&l✦ HIDDEN STAR! ✦"),
            ChatColor.translateAlternateColorCodes('&', "&7" + message),
            10, 40, 10
        );

        player.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.5f);
        player.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.0f, 1.2f);

        player.spawnParticle(Particle.GLOW_SQUID_INK, loc.add(0, 1, 0), 30, 0.5, 1.0, 0.5, 0.1);
        player.spawnParticle(Particle.END_ROD, loc, 40, 1, 1, 1, 0.05);
        player.spawnParticle(Particle.FIREWORK, loc, 20, 0.5, 0.5, 0.5, 0.05);

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
            "&b&l✦ &bHidden Star found! &7(" + totalStars + " total) &e+25 XP"));
    }

    public void playAchievementUnlock(Player player, Achievement achievement) {
        if (!enabled) return;
        Location loc = player.getLocation();

        player.sendTitle(
            ChatColor.translateAlternateColorCodes('&', "&6&l✦ ACHIEVEMENT UNLOCKED! ✦"),
            ChatColor.translateAlternateColorCodes('&', "&e" + achievement.getDisplayName()),
            10, 50, 10
        );

        player.playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        player.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        player.spawnParticle(Particle.FLASH, loc.add(0, 1, 0), 1, 0, 0, 0, 0);
        player.spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 40, 1, 1, 1, 0.3);
        player.spawnParticle(Particle.ENCHANT, loc, 30, 1, 1, 1, 0.5);

        Bukkit.getOnlinePlayers().stream()
            .filter(p -> !p.equals(player))
            .forEach(p -> p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&6&l✦ &e" + player.getName() + " &7unlocked achievement &e" + achievement.getDisplayName() + "&7!")));
    }

    public void sendActionBar(Player player, String message) {
        player.sendActionBar(ChatColor.translateAlternateColorCodes('&', message));
    }

    private void spawnFirework(Location loc, boolean trail) {
        Firework fw = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();
        FireworkEffect effect = FireworkEffect.builder()
            .withColor(
                Color.fromRGB(random.nextInt(256), random.nextInt(256), random.nextInt(256)),
                Color.fromRGB(random.nextInt(256), random.nextInt(256), random.nextInt(256))
            )
            .withFade(Color.WHITE)
            .with(FireworkEffect.Type.values()[random.nextInt(FireworkEffect.Type.values().length)])
            .trail(trail)
            .flicker(random.nextBoolean())
            .build();
        meta.addEffect(effect);
        meta.setPower(random.nextInt(2) + 1);
        fw.setFireworkMeta(meta);
    }

    public static String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        seconds %= 60;
        long millisRemainder = millis % 1000;
        if (minutes > 0) {
            return String.format("%d:%02d.%03d", minutes, seconds, millisRemainder);
        }
        return String.format("%d.%03ds", seconds, millisRemainder);
    }
}
