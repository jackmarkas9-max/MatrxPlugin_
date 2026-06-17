package net.matrx.plugin.managers;

import net.matrx.plugin.models.ParkourPlayer;
import net.matrx.plugin.models.Rank;
import net.matrx.plugin.storage.DataStorage;
import org.bukkit.entity.Player;
import java.util.*;

public class RankManager {
    private final DataStorage storage;
    private Map<String, Rank> ranks;
    private final List<String> rankOrder;

    public RankManager(DataStorage storage) {
        this.storage = storage;
        this.ranks = new LinkedHashMap<>();
        this.rankOrder = new ArrayList<>();
        loadRanks();
    }

    public void loadRanks() {
        ranks = storage.loadRanks();
        if (ranks.isEmpty()) {
            storage.saveDefaultRanks();
            ranks = storage.loadRanks();
        }
        rankOrder.clear();
        ranks.values().stream()
            .sorted(Comparator.comparingInt(Rank::getWeight))
            .forEach(r -> rankOrder.add(r.getName()));
    }

    public void saveRanks() {
        storage.saveRanks(ranks);
    }

    public Map<String, Rank> getRanks() {
        return ranks;
    }

    public Rank getRank(String name) {
        return ranks.get(name.toLowerCase());
    }

    public Rank getPlayerRank(ParkourPlayer pPlayer) {
        return ranks.getOrDefault(pPlayer.getRankName(), ranks.values().stream()
            .filter(r -> !r.isUnlockable())
            .findFirst()
            .orElse(null));
    }

    public Rank getNextRank(Rank current) {
        Rank next = null;
        for (String rn : rankOrder) {
            Rank r = ranks.get(rn);
            if (!r.isUnlockable()) continue;
            if (r.getWeight() > current.getWeight()) {
                if (next == null || r.getWeight() < next.getWeight()) {
                    next = r;
                }
            }
        }
        return next;
    }

    public Rank getRankForXp(long xp) {
        Rank best = null;
        for (Rank r : ranks.values()) {
            if (!r.isUnlockable()) continue;
            if (xp >= r.getXpRequired()) {
                if (best == null || r.getWeight() > best.getWeight()) {
                    best = r;
                }
            }
        }
        return best;
    }

    public void addRank(Rank rank) {
        ranks.put(rank.getName(), rank);
        rankOrder.clear();
        ranks.values().stream()
            .sorted(Comparator.comparingInt(Rank::getWeight))
            .forEach(r -> rankOrder.add(r.getName()));
        saveRanks();
    }

    public void removeRank(String name) {
        ranks.remove(name.toLowerCase());
        rankOrder.clear();
        ranks.values().stream()
            .sorted(Comparator.comparingInt(Rank::getWeight))
            .forEach(r -> rankOrder.add(r.getName()));
        saveRanks();
    }

    public boolean hasPermission(Player player, String permission) {
        if (player.hasPermission("matrx.admin") || player.hasPermission("matrx.*")) return true;
        if (player.hasPermission(permission)) return true;
        return false;
    }

    public boolean checkAndRankup(Player player, ParkourPlayer pPlayer, AnimationManager anim) {
        Rank currentRank = ranks.get(pPlayer.getRankName());
        Rank newRank = getRankForXp(pPlayer.getXp());
        if (newRank == null) return false;
        if (currentRank == null || newRank.getWeight() > currentRank.getWeight()) {
            pPlayer.setRankName(newRank.getName());
            saveRanks();
            anim.playRankupEffects(player, newRank.getDisplayName());
            updateXpBar(player, pPlayer);
            return true;
        }
        return false;
    }

    public void updateXpBar(Player player, ParkourPlayer pPlayer) {
        Rank current = ranks.get(pPlayer.getRankName());
        if (current == null) return;

        Rank next = getNextRank(current);
        if (next == null) {
            player.setLevel(0);
            player.setExp(0f);
            return;
        }

        long currentXp = pPlayer.getXp();
        long needed = next.getXpRequired() - current.getXpRequired();
        long progress = currentXp - current.getXpRequired();

        if (needed <= 0) {
            player.setExp(1f);
        } else {
            float exp = Math.min(1f, (float) progress / (float) needed);
            player.setExp(exp);
        }
        player.setLevel((int) (currentXp / 10));
    }

    public boolean addPlayerPermission(ParkourPlayer pPlayer, String permission) {
        if (pPlayer.getExtraPermissions().contains(permission)) return false;
        pPlayer.getExtraPermissions().add(permission);
        return true;
    }

    public boolean removePlayerPermission(ParkourPlayer pPlayer, String permission) {
        return pPlayer.getExtraPermissions().remove(permission);
    }

    public List<String> getAllPermissions(ParkourPlayer pPlayer) {
        Set<String> perms = new HashSet<>();
        Rank rank = ranks.get(pPlayer.getRankName());
        if (rank != null) {
            perms.addAll(rank.getPermissions());
        }
        perms.addAll(pPlayer.getExtraPermissions());
        return new ArrayList<>(perms);
    }

    public List<String> getRankOrder() {
        return rankOrder;
    }
}
