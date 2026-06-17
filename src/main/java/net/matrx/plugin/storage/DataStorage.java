package net.matrx.plugin.storage;

import net.matrx.plugin.models.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class DataStorage {
    private final JavaPlugin plugin;
    private final File ranksFile;
    private final File coursesFile;
    private final File playersFile;

    public DataStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        ranksFile = new File(plugin.getDataFolder(), "ranks.yml");
        coursesFile = new File(plugin.getDataFolder(), "courses.yml");
        playersFile = new File(plugin.getDataFolder(), "playerdata.yml");
    }

    public Map<String, Rank> loadRanks() {
        if (!ranksFile.exists()) {
            plugin.saveResource("ranks.yml", false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(ranksFile);
        Map<String, Rank> ranks = new LinkedHashMap<>();
        if (config.contains("ranks")) {
            for (String key : config.getConfigurationSection("ranks").getKeys(false)) {
                ranks.put(key.toLowerCase(), Rank.deserialize(key, config.getConfigurationSection("ranks." + key).getValues(true)));
            }
        }
        return ranks;
    }

    public void saveRanks(Map<String, Rank> ranks) {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<String, Rank> entry : ranks.entrySet()) {
            config.set("ranks." + entry.getKey(), entry.getValue().serialize());
        }
        saveConfig(config, ranksFile);
    }

    public Map<String, Course> loadCourses() {
        if (!coursesFile.exists()) {
            plugin.saveResource("courses.yml", false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(coursesFile);
        Map<String, Course> courses = new LinkedHashMap<>();
        if (config.contains("courses")) {
            for (String key : config.getConfigurationSection("courses").getKeys(false)) {
                courses.put(key.toLowerCase(), Course.deserialize(key, config.getConfigurationSection("courses." + key).getValues(true)));
            }
        }
        return courses;
    }

    public void saveCourses(Map<String, Course> courses) {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<String, Course> entry : courses.entrySet()) {
            config.set("courses." + entry.getKey(), entry.getValue().serialize());
        }
        saveConfig(config, coursesFile);
    }

    public Map<UUID, ParkourPlayer> loadPlayers() {
        if (!playersFile.exists()) {
            try {
                playersFile.getParentFile().mkdirs();
                playersFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create playerdata.yml: " + e.getMessage());
            }
            return new HashMap<>();
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(playersFile);
        Map<UUID, ParkourPlayer> players = new HashMap<>();
        if (config.contains("players")) {
            for (String key : config.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    players.put(uuid, ParkourPlayer.deserialize(uuid, config.getConfigurationSection("players." + key).getValues(true)));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in playerdata: " + key);
                }
            }
        }
        return players;
    }

    public void savePlayers(Map<UUID, ParkourPlayer> players) {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, ParkourPlayer> entry : players.entrySet()) {
            config.set("players." + entry.getKey().toString(), entry.getValue().serialize());
        }
        saveConfig(config, playersFile);
    }

    private void saveConfig(YamlConfiguration config, File file) {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save " + file.getName() + ": " + e.getMessage());
        }
    }

    public void saveDefaultRanks() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("ranks.bronze.display-name", "&6Bronze");
        config.set("ranks.bronze.prefix", "&6[Bronze] ");
        config.set("ranks.bronze.xp-required", 0);
        config.set("ranks.bronze.weight", 1);
        config.set("ranks.bronze.permissions", List.of());

        config.set("ranks.silver.display-name", "&7Silver");
        config.set("ranks.silver.prefix", "&7[Silver] ");
        config.set("ranks.silver.xp-required", 100);
        config.set("ranks.silver.weight", 2);
        config.set("ranks.silver.permissions", List.of());

        config.set("ranks.gold.display-name", "&eGold");
        config.set("ranks.gold.prefix", "&e[Gold] ");
        config.set("ranks.gold.xp-required", 300);
        config.set("ranks.gold.weight", 3);
        config.set("ranks.gold.permissions", List.of());

        config.set("ranks.diamond.display-name", "&bDiamond");
        config.set("ranks.diamond.prefix", "&b[Diamond] ");
        config.set("ranks.diamond.xp-required", 600);
        config.set("ranks.diamond.weight", 4);
        config.set("ranks.diamond.permissions", List.of());

        config.set("ranks.emerald.display-name", "&aEmerald");
        config.set("ranks.emerald.prefix", "&a[Emerald] ");
        config.set("ranks.emerald.xp-required", 1000);
        config.set("ranks.emerald.weight", 5);
        config.set("ranks.emerald.permissions", List.of());

        config.set("ranks.legend.display-name", "&5Legend");
        config.set("ranks.legend.prefix", "&5[Legend] ");
        config.set("ranks.legend.xp-required", 2000);
        config.set("ranks.legend.weight", 6);
        config.set("ranks.legend.permissions", List.of());

        config.set("ranks.admin.display-name", "&cAdmin");
        config.set("ranks.admin.prefix", "&c[Admin] ");
        config.set("ranks.admin.xp-required", -1);
        config.set("ranks.admin.weight", 100);
        config.set("ranks.admin.permissions", List.of("matrx.admin", "matrx.*"));

        saveConfig(config, ranksFile);
    }

    public void saveDefaultCourses() {
        YamlConfiguration config = new YamlConfiguration();
        saveConfig(config, coursesFile);
    }
}
