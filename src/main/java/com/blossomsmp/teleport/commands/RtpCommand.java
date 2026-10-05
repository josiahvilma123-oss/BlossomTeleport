package com.blossomsmp.teleport.commands;

import com.blossomsmp.teleport.BlossomTeleport;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** /rtp - random teleport into the wild */
public class RtpCommand implements TabExecutor {

    private final BlossomTeleport plugin;
    private final Set<UUID> searching = new HashSet<>();

    public RtpCommand(BlossomTeleport plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.msg(sender, "players-only");
            return true;
        }
        if (!plugin.getConfig().getBoolean("rtp.enabled", true)) {
            plugin.msg(player, "disabled");
            return true;
        }
        World world = player.getWorld();
        if (plugin.getConfig().getStringList("rtp.blocked-worlds").contains(world.getName())) {
            plugin.msg(player, "rtp-blocked-world");
            return true;
        }
        if (plugin.teleports().inCombat(player)) {
            plugin.msg(player, "in-combat");
            return true;
        }
        if (searching.contains(player.getUniqueId()) || !plugin.teleports().checkCooldown(player, "rtp")) {
            return true;
        }

        searching.add(player.getUniqueId());
        plugin.msg(player, "rtp-searching");
        plugin.rtp().find(world, spot -> {
            searching.remove(player.getUniqueId());
            if (!player.isOnline()) {
                return;
            }
            if (spot == null) {
                plugin.msg(player, "rtp-failed");
                return;
            }
            plugin.teleports().teleport(player, () -> spot, "rtp", "The Wild",
                    () -> plugin.msg(player, "rtp-teleported",
                            "{x}", spot.getBlockX(), "{z}", spot.getBlockZ()));
        });
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
