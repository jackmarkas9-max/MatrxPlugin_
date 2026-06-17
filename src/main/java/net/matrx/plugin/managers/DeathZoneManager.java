package net.matrx.plugin.managers;

import net.matrx.plugin.models.DeathZone;
import net.matrx.plugin.storage.DataStorage;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import java.util.*;

public class DeathZoneManager {
    private final DataStorage storage;
    private final Map<Integer, DeathZone> deathZones;
    private final Map<UUID, Location[]> selections;

    public DeathZoneManager(DataStorage storage) {
        this.storage = storage;
        this.deathZones = new LinkedHashMap<>();
        this.selections = new HashMap<>();
        load();
    }

    public void load() {
        deathZones.clear();
        deathZones.putAll(storage.loadDeathZones());
    }

    public void save() {
        storage.saveDeathZones(deathZones);
    }

    public Map<Integer, DeathZone> getDeathZones() { return deathZones; }
    public DeathZone get(int id) { return deathZones.get(id); }

    public void add(DeathZone zone) {
        deathZones.put(zone.getId(), zone);
        save();
    }

    public void remove(int id) {
        deathZones.remove(id);
        save();
    }

    public boolean isInAnyDeathZone(Location loc) {
        for (DeathZone zone : deathZones.values()) {
            if (zone.contains(loc)) return true;
        }
        return false;
    }

    public DeathZone findDeathZone(Location loc) {
        for (DeathZone zone : deathZones.values()) {
            if (zone.contains(loc)) return zone;
        }
        return null;
    }

    public void setSelection(Player player, int corner, Location loc) {
        selections.computeIfAbsent(player.getUniqueId(), k -> new Location[2]);
        selections.get(player.getUniqueId())[corner] = loc;
    }

    public Location[] getSelection(Player player) {
        return selections.get(player.getUniqueId());
    }

    public void clearSelection(Player player) {
        selections.remove(player.getUniqueId());
    }

    public int nextId() {
        int id = 1;
        while (deathZones.containsKey(id)) id++;
        return id;
    }
}
