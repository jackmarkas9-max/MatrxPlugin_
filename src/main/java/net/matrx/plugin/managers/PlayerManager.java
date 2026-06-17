package net.matrx.plugin.managers;

import net.matrx.plugin.models.ParkourPlayer;
import net.matrx.plugin.models.Rank;
import net.matrx.plugin.storage.DataStorage;
import org.bukkit.entity.Player;
import java.util.*;

public class PlayerManager {
    private final DataStorage storage;
    private final Map<UUID, ParkourPlayer> players;
    private final Timer saveTimer;
    private RankManager rankManager;

    public PlayerManager(DataStorage storage) {
        this.storage = storage;
        this.players = new HashMap<>();
        loadPlayers();
        this.saveTimer = new Timer("MatrxPlayerSave", true);
        this.saveTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                saveAll();
            }
        }, 300000, 300000);
    }

    public void setRankManager(RankManager rankManager) { this.rankManager = rankManager; }

    public void loadPlayers() { players.clear(); players.putAll(storage.loadPlayers()); }

    public void saveAll() { storage.savePlayers(players); }

    public ParkourPlayer getPlayer(Player player) {
        return players.computeIfAbsent(player.getUniqueId(), uuid -> {
            ParkourPlayer p = new ParkourPlayer(uuid, player.getName());
            return p;
        });
    }

    public ParkourPlayer getPlayer(UUID uuid) { return players.get(uuid); }
    public void removePlayer(UUID uuid) { players.remove(uuid); }

    public Rank getPlayerRank(ParkourPlayer pPlayer) {
        if (rankManager == null) return null;
        return rankManager.getRank(pPlayer.getRankName());
    }

    public void addXp(Player player, ParkourPlayer pPlayer, long amount, AnimationManager anim) {
        pPlayer.addXp(amount);
        anim.playXpGainEffects(player, amount);
    }

    public void shutdown() {
        saveTimer.cancel();
        saveAll();
    }

    public Collection<ParkourPlayer> getAllPlayers() { return players.values(); }
}
