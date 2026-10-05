package com.blossomsmp.teleport.commands;

import com.blossomsmp.teleport.BlossomTeleport;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;

/** /spawn and /setspawn */
public class SpawnCommands implements TabExecutor {

    private final BlossomTeleport plugin;

    public SpawnCommands(BlossomTeleport plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.msg(sender, "players-only");
            return true;
        }
        if (command.getName().equalsIgnoreCase("setspawn")) {
            Location here = player.getLocation();
            plugin.data().setSpawn(here);
            player.getWorld().setSpawnLocation(here);
            plugin.msg(player, "spawn-set");
            return true;
        }
        if (!plugin.getConfig().getBoolean("spawn.enabled", true)) {
            plugin.msg(player, "disabled");
            return true;
        }
        if (plugin.data().spawn() == null) {
            plugin.msg(player, "spawn-not-set");
            return true;
        }
        if (!plugin.teleports().checkCooldown(player, "spawn")) {
            return true;
        }
        plugin.teleports().teleport(player, () -> plugin.data().spawn(), "spawn", "Spawn",
                () -> plugin.msg(player, "spawn-teleported"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
