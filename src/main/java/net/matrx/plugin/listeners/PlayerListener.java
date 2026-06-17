package net.matrx.plugin.listeners;

import net.matrx.plugin.managers.*;
import net.matrx.plugin.models.ParkourPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {
    private final PlayerManager playerManager;
    private final RankManager rankManager;
    private final AnimationManager animationManager;

    public PlayerListener(PlayerManager playerManager, RankManager rankManager, AnimationManager animationManager) {
        this.playerManager = playerManager;
        this.rankManager = rankManager;
        this.animationManager = animationManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ParkourPlayer pPlayer = playerManager.getPlayer(player);
        pPlayer.setName(player.getName());
        rankManager.updateXpBar(player, pPlayer);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        ParkourPlayer pPlayer = playerManager.getPlayer(player);
        if (pPlayer != null) {
            pPlayer.leaveCourse();
        }
    }
}
