package com.blossomsmp.teleport.commands;

import com.blossomsmp.teleport.BlossomTeleport;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;

/** /back - go to your last spot (before a teleport, or where you died) */
public class BackCommand implements TabExecutor {

    private final BlossomTeleport plugin;

    public BackCommand(BlossomTeleport plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.msg(sender, "players-only");
            return true;
        }
        if (!plugin.getConfig().getBoolean("back.enabled", true)) {
            plugin.msg(player, "disabled");
            return true;
        }
        Location back = plugin.teleports().back(player);
        if (back == null) {
            plugin.msg(player, "back-none");
            return true;
        }
        if (!plugin.teleports().checkCooldown(player, "back")) {
            return true;
        }
        plugin.teleports().teleport(player, () -> back, "back", "Back",
                () -> plugin.msg(player, "back-teleported"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
