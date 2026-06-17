package net.matrx.plugin.models;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import java.util.LinkedHashMap;
import java.util.Map;

public class DeathZone {
    private int id;
    private String world;
    private double x1, y1, z1;
    private double x2, y2, z2;

    public DeathZone(int id, String world, double x1, double y1, double z1, double x2, double y2, double z2) {
        this.id = id;
        this.world = world;
        this.x1 = Math.min(x1, x2);
        this.y1 = Math.min(y1, y2);
        this.z1 = Math.min(z1, z2);
        this.x2 = Math.max(x1, x2);
        this.y2 = Math.max(y1, y2);
        this.z2 = Math.max(z1, z2);
    }

    public int getId() { return id; }
    public String getWorld() { return world; }

    public boolean contains(Location loc) {
        if (!loc.getWorld().getName().equals(world)) return false;
        double x = loc.getX(), y = loc.getY(), z = loc.getZ();
        return x >= x1 && x <= x2 && y >= y1 && y <= y2 && z >= z1 && z <= z2;
    }

    public Location getCenter(org.bukkit.World w) {
        return new Location(w, (x1 + x2) / 2, (y1 + y2) / 2, (z1 + z2) / 2);
    }

    public Location getCorner1(org.bukkit.World w) {
        return new Location(w, x1, y1, z1);
    }

    public Location getCorner2(org.bukkit.World w) {
        return new Location(w, x2, y2, z2);
    }

    public Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("world", world);
        map.put("x1", x1);
        map.put("y1", y1);
        map.put("z1", z1);
        map.put("x2", x2);
        map.put("y2", y2);
        map.put("z2", z2);
        return map;
    }

    @SuppressWarnings("unchecked")
    public static DeathZone deserialize(int id, Map<String, Object> map) {
        return new DeathZone(
            id,
            (String) map.get("world"),
            ((Number) map.get("x1")).doubleValue(),
            ((Number) map.get("y1")).doubleValue(),
            ((Number) map.get("z1")).doubleValue(),
            ((Number) map.get("x2")).doubleValue(),
            ((Number) map.get("y2")).doubleValue(),
            ((Number) map.get("z2")).doubleValue()
        );
    }

    public String format() {
        return String.format("&e#%d &7[%s] &8(%.0f,%.0f,%.0f)->(%.0f,%.0f,%.0f)",
            id, world, x1, y1, z1, x2, y2, z2);
    }
}
