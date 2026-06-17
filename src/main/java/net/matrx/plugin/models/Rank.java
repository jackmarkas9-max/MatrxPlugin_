package net.matrx.plugin.models;

import org.bukkit.ChatColor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Rank {
    private String name;
    private String displayName;
    private String prefix;
    private long xpRequired;
    private int weight;
    private List<String> permissions;

    public Rank(String name, String displayName, String prefix, long xpRequired, int weight, List<String> permissions) {
        this.name = name.toLowerCase();
        this.displayName = displayName;
        this.prefix = prefix;
        this.xpRequired = xpRequired;
        this.weight = weight;
        this.permissions = permissions != null ? permissions : new ArrayList<>();
    }

    public String getName() { return name; }
    public String getDisplayName() { return ChatColor.translateAlternateColorCodes('&', displayName); }
    public String getRawDisplayName() { return displayName; }
    public String getPrefix() { return ChatColor.translateAlternateColorCodes('&', prefix); }
    public String getRawPrefix() { return prefix; }
    public long getXpRequired() { return xpRequired; }
    public int getWeight() { return weight; }
    public List<String> getPermissions() { return permissions; }
    public boolean isUnlockable() { return xpRequired >= 0; }

    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setPrefix(String prefix) { this.prefix = prefix; }
    public void setXpRequired(long xpRequired) { this.xpRequired = xpRequired; }
    public void setWeight(int weight) { this.weight = weight; }
    public void addPermission(String permission) { this.permissions.add(permission); }
    public void removePermission(String permission) { this.permissions.remove(permission); }

    public Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("display-name", displayName);
        map.put("prefix", prefix);
        map.put("xp-required", xpRequired);
        map.put("weight", weight);
        map.put("permissions", permissions);
        return map;
    }

    @SuppressWarnings("unchecked")
    public static Rank deserialize(String name, Map<String, Object> map) {
        return new Rank(
            name,
            (String) map.getOrDefault("display-name", "&7" + name),
            (String) map.getOrDefault("prefix", "&7[" + name + "] "),
            ((Number) map.getOrDefault("xp-required", 0)).longValue(),
            ((Number) map.getOrDefault("weight", 0)).intValue(),
            (List<String>) map.getOrDefault("permissions", new ArrayList<>())
        );
    }
}
