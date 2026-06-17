package net.matrx.plugin.models;

import java.util.*;

public class ParkourPlayer {
    private UUID uuid;
    private String name;
    private String rankName;
    private long xp;
    private Map<String, Long> bestTimes;
    private Map<String, Long> checkpointTimestamps;
    private Checkpoint lastCheckpoint;
    private String activeCourse;
    private long courseStartTime;
    private List<String> extraPermissions;
    private int comboCount;
    private long lastCheckpointTime;
    private Set<String> unlockedAchievements;
    private Set<String> foundStars;
    private int totalCompletions;
    private long totalPlayTime;
    private int totalCheckpointsPassed;
    private int highestCombo;
    private GhostRecording lastGhostRecording;

    public ParkourPlayer(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.rankName = "bronze";
        this.xp = 0;
        this.bestTimes = new HashMap<>();
        this.checkpointTimestamps = new HashMap<>();
        this.extraPermissions = new ArrayList<>();
        this.courseStartTime = -1;
        this.unlockedAchievements = new HashSet<>();
        this.foundStars = new HashSet<>();
        this.lastGhostRecording = null;
    }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRankName() { return rankName; }
    public void setRankName(String rankName) { this.rankName = rankName; }
    public long getXp() { return xp; }
    public void setXp(long xp) { this.xp = Math.max(0, xp); }
    public void addXp(long amount) { this.xp += amount; }
    public Map<String, Long> getBestTimes() { return bestTimes; }
    public Checkpoint getLastCheckpoint() { return lastCheckpoint; }
    public void setLastCheckpoint(Checkpoint checkpoint) { this.lastCheckpoint = checkpoint; }
    public String getActiveCourse() { return activeCourse; }
    public long getCourseStartTime() { return courseStartTime; }
    public List<String> getExtraPermissions() { return extraPermissions; }
    public int getComboCount() { return comboCount; }
    public int getHighestCombo() { return highestCombo; }
    public Set<String> getUnlockedAchievements() { return unlockedAchievements; }
    public Set<String> getFoundStars() { return foundStars; }
    public int getTotalCompletions() { return totalCompletions; }
    public long getTotalPlayTime() { return totalPlayTime; }
    public int getTotalCheckpointsPassed() { return totalCheckpointsPassed; }
    public GhostRecording getLastGhostRecording() { return lastGhostRecording; }
    public void setLastGhostRecording(GhostRecording rec) { this.lastGhostRecording = rec; }

    public void startCourse(String courseName) {
        this.activeCourse = courseName;
        this.courseStartTime = System.currentTimeMillis();
        this.lastCheckpoint = null;
        this.checkpointTimestamps.clear();
        this.comboCount = 0;
        this.lastCheckpointTime = 0;
    }

    public void finishCourse() {
        this.totalCompletions++;
        this.activeCourse = null;
        this.courseStartTime = -1;
        this.lastCheckpoint = null;
        this.checkpointTimestamps.clear();
    }

    public void leaveCourse() {
        this.activeCourse = null;
        this.courseStartTime = -1;
        this.lastCheckpoint = null;
        this.checkpointTimestamps.clear();
        this.comboCount = 0;
        this.lastCheckpointTime = 0;
    }

    public long getElapsedTime() {
        if (courseStartTime < 0) return -1;
        return System.currentTimeMillis() - courseStartTime;
    }

    public long getBestTime(String courseName) {
        return bestTimes.getOrDefault(courseName, -1L);
    }

    public boolean setBestTime(String courseName, long time) {
        long current = bestTimes.getOrDefault(courseName, -1L);
        if (current == -1 || time < current) {
            bestTimes.put(courseName, time);
            return true;
        }
        return false;
    }

    public int addCombo() {
        comboCount++;
        if (comboCount > highestCombo) highestCombo = comboCount;
        totalCheckpointsPassed++;
        lastCheckpointTime = System.currentTimeMillis();
        return comboCount;
    }

    public void resetCombo() {
        comboCount = 0;
    }

    public long getTimeSinceLastCheckpoint() {
        if (lastCheckpointTime == 0) return -1;
        return System.currentTimeMillis() - lastCheckpointTime;
    }

    public boolean hasAchievement(String id) {
        return unlockedAchievements.contains(id);
    }

    public boolean unlockAchievement(String id) {
        if (unlockedAchievements.contains(id)) return false;
        unlockedAchievements.add(id);
        return true;
    }

    public boolean hasFoundStar(String starId) {
        return foundStars.contains(starId);
    }

    public boolean addFoundStar(String starId) {
        if (foundStars.contains(starId)) return false;
        foundStars.add(starId);
        addXp(25);
        return true;
    }

    public int getStarCount() {
        return foundStars.size();
    }

    public double getComboMultiplier() {
        if (comboCount <= 1) return 1.0;
        return 1.0 + (comboCount * 0.1);
    }

    public Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", name);
        map.put("rank", rankName);
        map.put("xp", xp);
        map.put("total-completions", totalCompletions);
        map.put("total-play-time", totalPlayTime);
        map.put("total-checkpoints", totalCheckpointsPassed);
        map.put("highest-combo", highestCombo);

        Map<String, Object> timesMap = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : bestTimes.entrySet()) {
            timesMap.put(entry.getKey(), entry.getValue());
        }
        map.put("best-times", timesMap);
        map.put("extra-permissions", new ArrayList<>(extraPermissions));
        map.put("achievements", new ArrayList<>(unlockedAchievements));
        map.put("found-stars", new ArrayList<>(foundStars));

        if (lastCheckpoint != null) {
            map.put("last-checkpoint", lastCheckpoint.serialize());
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    public static ParkourPlayer deserialize(UUID uuid, Map<String, Object> map) {
        ParkourPlayer player = new ParkourPlayer(
            uuid,
            (String) map.getOrDefault("name", "Unknown")
        );
        player.rankName = (String) map.getOrDefault("rank", "bronze");
        player.xp = ((Number) map.getOrDefault("xp", 0)).longValue();
        player.totalCompletions = ((Number) map.getOrDefault("total-completions", 0)).intValue();
        player.totalPlayTime = ((Number) map.getOrDefault("total-play-time", 0)).longValue();
        player.totalCheckpointsPassed = ((Number) map.getOrDefault("total-checkpoints", 0)).intValue();
        player.highestCombo = ((Number) map.getOrDefault("highest-combo", 0)).intValue();

        Map<String, Object> timesMap = (Map<String, Object>) map.getOrDefault("best-times", new LinkedHashMap<>());
        for (Map.Entry<String, Object> entry : timesMap.entrySet()) {
            player.bestTimes.put(entry.getKey(), ((Number) entry.getValue()).longValue());
        }

        player.extraPermissions = new ArrayList<>((List<String>) map.getOrDefault("extra-permissions", new ArrayList<>()));
        player.unlockedAchievements = new HashSet<>((List<String>) map.getOrDefault("achievements", new ArrayList<>()));
        player.foundStars = new HashSet<>((List<String>) map.getOrDefault("found-stars", new ArrayList<>()));

        if (map.containsKey("last-checkpoint") && map.get("last-checkpoint") != null) {
            player.lastCheckpoint = Checkpoint.deserialize((Map<String, Object>) map.get("last-checkpoint"));
        }

        return player;
    }

    public static class GhostRecording {
        public final String courseName;
        public final long time;
        public final List<double[]> frames;

        public GhostRecording(String courseName, long time, List<double[]> frames) {
            this.courseName = courseName;
            this.time = time;
            this.frames = frames;
        }
    }
}
