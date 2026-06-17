package net.matrx.plugin;

import net.matrx.plugin.commands.*;
import net.matrx.plugin.gui.GUIClickListener;
import net.matrx.plugin.listeners.*;
import net.matrx.plugin.managers.*;
import net.matrx.plugin.storage.DataStorage;
import org.bukkit.plugin.java.JavaPlugin;

public class MatrxPlugin extends JavaPlugin {

    private DataStorage storage;
    private PlayerManager playerManager;
    private RankManager rankManager;
    private CourseManager courseManager;
    private AnimationManager animationManager;
    private ComboManager comboManager;
    private AchievementManager achievementManager;
    private DeathZoneManager deathZoneManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getDataFolder().mkdirs();

        boolean animations = getConfig().getBoolean("animations-enabled", true);

        this.storage = new DataStorage(this);
        this.animationManager = new AnimationManager(animations);
        this.rankManager = new RankManager(storage);
        this.playerManager = new PlayerManager(storage);
        this.playerManager.setRankManager(rankManager);
        this.courseManager = new CourseManager(storage);
        this.comboManager = new ComboManager(animationManager);
        this.achievementManager = new AchievementManager(playerManager, animationManager);
        this.deathZoneManager = new DeathZoneManager(storage);

        registerCommands();
        registerListeners();

        getLogger().info("✦ MatrxPlugin v1.0 enabled!");
        getLogger().info("  Ranks: " + rankManager.getRanks().size()
            + " | Courses: " + courseManager.getCourses().size()
            + " | Players: " + playerManager.getAllPlayers().size());
        getLogger().info("  Death Zones: " + deathZoneManager.getDeathZones().size());
        getLogger().info("  Features: Invincibility, Combos, Achievements, Hidden Stars, Special Blocks, Death Zones");
    }

    @Override
    public void onDisable() {
        if (playerManager != null) playerManager.shutdown();
        getLogger().info("MatrxPlugin disabled!");
    }

    private void registerCommands() {
        ParkourCommand pkCmd = new ParkourCommand(courseManager, playerManager, rankManager, animationManager, achievementManager);
        getCommand("parkour").setExecutor(pkCmd);
        getCommand("parkour").setTabCompleter(pkCmd);
        getCommand("checkpoint").setExecutor(new CheckpointCommand(playerManager));
        getCommand("rank").setExecutor(new RankCommand(rankManager, playerManager, animationManager));
        getCommand("rank").setTabCompleter(new RankCommand(rankManager, playerManager, animationManager));
        getCommand("diewand").setExecutor(new DiewandCommand(deathZoneManager));
        getCommand("diewand").setTabCompleter(new DiewandCommand(deathZoneManager));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(
            new ParkourListener(courseManager, playerManager, rankManager, animationManager, comboManager, achievementManager, deathZoneManager), this);
        getServer().getPluginManager().registerEvents(
            new PlayerListener(playerManager, rankManager, animationManager), this);
        getServer().getPluginManager().registerEvents(
            new GUIClickListener(courseManager, playerManager, rankManager, achievementManager), this);
        getServer().getPluginManager().registerEvents(
            new InvincibilityListener(), this);
        getServer().getPluginManager().registerEvents(
            new DiewandListener(deathZoneManager), this);
        getServer().getPluginManager().registerEvents(
            new BlockProtectionListener(), this);
    }
}
