package net.matrx.plugin.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockProtectionListener implements Listener {

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("matrx.admin")) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Only admins can break blocks!");
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("matrx.admin")) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Only admins can place blocks!");
        }
    }
}
