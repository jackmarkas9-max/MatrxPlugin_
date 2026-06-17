package net.matrx.plugin.models;

import java.util.*;
import java.util.stream.Collectors;

public class Course {
    private static int nextId = 1;
    private int id;
    private String name;
    private String displayName;
    private Checkpoint start;
    private Checkpoint end;
    private List<Checkpoint> checkpoints;
    private String requiredRank;
    private long xpReward;
    private int tier;
    private List<HiddenStar> hiddenStars;
    private long parTime;

    public Course(String name, String displayName, Checkpoint start, Checkpoint end,
                  List<Checkpoint> checkpoints, String requiredRank, long xpReward) {
        this.id = nextId++;
        this.name = name.toLowerCase();
        this.displayName = displayName;
        this.start = start;
        this.end = end;
        this.checkpoints = checkpoints != null ? checkpoints : new ArrayList<>();
        this.requiredRank = requiredRank != null ? requiredRank : "";
        this.xpReward = xpReward;
        this.tier = 1;
        this.hiddenStars = new ArrayList<>();
        this.parTime = 30000;
    }

    public static void setNextId(int id) { nextId = Math.max(nextId, id + 1); }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public String getDisplayName() { return org.bukkit.ChatColor.translateAlternateColorCodes('&', displayName); }
    public String getRawDisplayName() { return displayName; }
    public Checkpoint getStart() { return start; }
    public Checkpoint getEnd() { return end; }
    public List<Checkpoint> getCheckpoints() { return checkpoints; }
    public String getRequiredRank() { return requiredRank; }
    public long getXpReward() { return xpReward; }
    public int getTier() { return tier; }
    public List<HiddenStar> getHiddenStars() { return hiddenStars; }
    public long getParTime() { return parTime; }

    public void setStart(Checkpoint s) { this.start = s; }
    public void setEnd(Checkpoint e) { this.end = e; }
    public void setRequiredRank(String r) { this.requiredRank = r != null ? r : ""; }
    public void setXpReward(long xp) { this.xpReward = xp; }
    public void setTier(int t) { this.tier = Math.max(1, t); }
    public void setParTime(long t) { this.parTime = Math.max(1000, t); }
    public void addCheckpoint(Checkpoint cp) { this.checkpoints.add(cp); }
    public void removeCheckpoint(int index) { if (index >= 0 && index < checkpoints.size()) this.checkpoints.remove(index); }

    public int getTotalCheckpoints() { return checkpoints.size(); }

    public void addHiddenStar(HiddenStar star) {
        hiddenStars.add(star);
    }

    public void removeHiddenStar(int index) {
        if (index >= 0 && index < hiddenStars.size()) hiddenStars.remove(index);
    }

    public HiddenStar findStarAt(org.bukkit.Location loc) {
        for (HiddenStar star : hiddenStars) {
            if (star.matches(loc)) return star;
        }
        return null;
    }

    public String getTierDisplay() {
        return switch (tier) {
            case 1 -> "&aEasy";
            case 2 -> "&eMedium";
            case 3 -> "&6Hard";
            case 4 -> "&cInsane";
            case 5 -> "&4&lImpossible";
            default -> "&7Unknown";
        };
    }

    public boolean isUnderPar(long time) {
        return time <= parTime;
    }

    public Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("display-name", displayName);
        map.put("xp-reward", xpReward);
        map.put("required-rank", requiredRank);
        map.put("tier", tier);
        map.put("par-time", parTime);

        if (start != null) map.put("start", start.serialize());
        if (end != null) map.put("end", end.serialize());

        map.put("checkpoints", checkpoints.stream().map(Checkpoint::serialize).collect(Collectors.toList()));
        map.put("hidden-stars", hiddenStars.stream().map(HiddenStar::serialize).collect(Collectors.toList()));
        return map;
    }

    @SuppressWarnings("unchecked")
    public static Course deserialize(String name, Map<String, Object> map) {
        Checkpoint start = map.containsKey("start") ? Checkpoint.deserialize((Map<String, Object>) map.get("start")) : null;
        Checkpoint end = map.containsKey("end") ? Checkpoint.deserialize((Map<String, Object>) map.get("end")) : null;

        List<Checkpoint> checkpoints = new ArrayList<>();
        if (map.containsKey("checkpoints")) {
            for (Map<String, Object> cpMap : (List<Map<String, Object>>) map.get("checkpoints")) {
                checkpoints.add(Checkpoint.deserialize(cpMap));
            }
        }

        List<HiddenStar> stars = new ArrayList<>();
        if (map.containsKey("hidden-stars")) {
            for (Map<String, Object> sMap : (List<Map<String, Object>>) map.get("hidden-stars")) {
                stars.add(HiddenStar.deserialize(sMap));
            }
        }

        Course course = new Course(
            name,
            (String) map.getOrDefault("display-name", name),
            start, end, checkpoints,
            (String) map.getOrDefault("required-rank", ""),
            ((Number) map.getOrDefault("xp-reward", 50)).longValue()
        );
        if (map.containsKey("id")) {
            course.id = ((Number) map.get("id")).intValue();
        }
        course.tier = ((Number) map.getOrDefault("tier", 1)).intValue();
        course.parTime = ((Number) map.getOrDefault("par-time", 30000)).longValue();
        course.hiddenStars = stars;
        return course;
    }

    public static class HiddenStar {
        public final String id;
        public final String world;
        public final double x, y, z;
        public final String message;

        public HiddenStar(String id, String world, double x, double y, double z, String message) {
            this.id = id;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.message = message;
        }

        public boolean matches(org.bukkit.Location loc) {
            return loc.getWorld().getName().equals(world)
                && loc.getBlockX() == (int) Math.floor(x)
                && loc.getBlockY() == (int) Math.floor(y)
                && loc.getBlockZ() == (int) Math.floor(z);
        }

        public Map<String, Object> serialize() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", id);
            map.put("world", world);
            map.put("x", x);
            map.put("y", y);
            map.put("z", z);
            map.put("message", message);
            return map;
        }

        @SuppressWarnings("unchecked")
        public static HiddenStar deserialize(Map<String, Object> map) {
            return new HiddenStar(
                (String) map.get("id"),
                (String) map.get("world"),
                ((Number) map.get("x")).doubleValue(),
                ((Number) map.get("y")).doubleValue(),
                ((Number) map.get("z")).doubleValue(),
                (String) map.getOrDefault("message", "Found a hidden star!")
            );
        }
    }
}
