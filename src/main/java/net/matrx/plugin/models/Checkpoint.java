package net.matrx.plugin.models;

import org.bukkit.Location;
import org.bukkit.World;
import java.util.LinkedHashMap;
import java.util.Map;

public class Checkpoint {
    private String world;
    private double x, y, z;
    private float yaw, pitch;

    public Checkpoint(String world, double x, double y, double z, float yaw, float pitch) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public Checkpoint(Location loc) {
        this.world = loc.getWorld().getName();
        this.x = loc.getX();
        this.y = loc.getY();
        this.z = loc.getZ();
        this.yaw = loc.getYaw();
        this.pitch = loc.getPitch();
    }

    public Location toLocation(World defaultWorld) {
        World w = world != null ? org.bukkit.Bukkit.getWorld(world) : defaultWorld;
        if (w == null) w = defaultWorld;
        return new Location(w, x, y, z, yaw, pitch);
    }

    public Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("world", world);
        map.put("x", x);
        map.put("y", y);
        map.put("z", z);
        map.put("yaw", (double) yaw);
        map.put("pitch", (double) pitch);
        return map;
    }

    @SuppressWarnings("unchecked")
    public static Checkpoint deserialize(Map<String, Object> map) {
        return new Checkpoint(
            (String) map.get("world"),
            ((Number) map.get("x")).doubleValue(),
            ((Number) map.get("y")).doubleValue(),
            ((Number) map.get("z")).doubleValue(),
            ((Number) map.getOrDefault("yaw", 0.0)).floatValue(),
            ((Number) map.getOrDefault("pitch", 0.0)).floatValue()
        );
    }

    public boolean matchesBlock(Location loc) {
        return loc.getBlockX() == (int) Math.floor(x)
            && loc.getBlockY() == (int) Math.floor(y)
            && loc.getBlockZ() == (int) Math.floor(z);
    }

    public String getWorld() { return world; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
}
