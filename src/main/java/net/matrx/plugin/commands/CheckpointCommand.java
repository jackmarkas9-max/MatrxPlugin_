package net.matrx.plugin.commands;

import net.matrx.plugin.managers.*;
import net.matrx.plugin.models.*;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.List;

public class CheckpointCommand implements CommandExecutor {
    private final PlayerManager playerManager;

    public CheckpointCommand(PlayerManager playerManager) {
        this.playerManager = playerManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        ParkourPlayer pPlayer = playerManager.getPlayer(player);

        if (pPlayer.getActiveCourse() == null) {
            player.sendMessage(ChatColor.RED + "You are not currently in a parkour course!");
            return true;
        }

        Checkpoint cp = pPlayer.getLastCheckpoint();
        if (cp == null) {
            player.sendMessage(ChatColor.RED + "You haven't reached any checkpoints yet!");
            return true;
        }

        Location loc = cp.toLocation(player.getWorld());
        player.teleport(loc);
        player.sendMessage(ChatColor.GREEN + "Teleported to your last checkpoint!");
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        return true;
    }
}
