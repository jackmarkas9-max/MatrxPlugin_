package net.matrx.plugin.listeners;

import net.matrx.plugin.managers.DeathZoneManager;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class DiewandListener implements Listener {
    private final DeathZoneManager dzManager;

    public DiewandListener(DeathZoneManager dzManager) {
        this.dzManager = dzManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("matrx.admin")) return;

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;
        if (!item.getItemMeta().getDisplayName().equals(ChatColor.RED + "✦ Diewand")) return;

        event.setCancelled(true);

        if (event.getClickedBlock() == null) return;
        Location loc = event.getClickedBlock().getLocation();

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            dzManager.setSelection(player, 0, loc);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&aPos 1 set: &f" + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ()));
            player.spawnParticle(Particle.FLAME, loc.add(0.5, 0.5, 0.5), 30, 0.5, 0.5, 0.5, 0.01);
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            dzManager.setSelection(player, 1, loc);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&aPos 2 set: &f" + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ()));
            player.spawnParticle(Particle.FLAME, loc.add(0.5, 0.5, 0.5), 30, 0.5, 0.5, 0.5, 0.01);
        }
    }
}
