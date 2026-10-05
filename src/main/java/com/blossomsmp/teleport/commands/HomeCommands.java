package com.blossomsmp.teleport.commands;

import com.blossomsmp.teleport.BlossomTeleport;
import com.blossomsmp.teleport.HomeManager;
import com.blossomsmp.teleport.menus.HomesMenu;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** /home /homes /sethome /delhome */
public class HomeCommands implements TabExecutor {

    private final BlossomTeleport plugin;

    public HomeCommands(BlossomTeleport plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.msg(sender, "players-only");
            return true;
        }
        if (!plugin.getConfig().getBoolean("homes.enabled", true)) {
            plugin.msg(player, "disabled");
            return true;
        }
        switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "home" -> home(player, args);
            case "homes" -> HomesMenu.open(plugin, player);
            case "sethome" -> setHome(player, args);
            case "delhome" -> delHome(player, args);
            default -> {
            }
        }
        return true;
    }

    private void home(Player player, String[] args) {
        Map<String, Location> homes = plugin.homes().homes(player.getUniqueId());
        if (homes.isEmpty()) {
            plugin.msg(player, "home-no-homes");
            return;
        }
        String name;
        if (args.length >= 1) {
            name = args[0];
        } else if (homes.size() == 1) {
            name = homes.keySet().iterator().next();
        } else if (homes.containsKey("home")) {
            name = "home";
        } else {
            HomesMenu.open(plugin, player); // several homes and none called "home": let them pick
            return;
        }
        goHome(plugin, player, name);
    }

    /** Shared with the homes menu. */
    public static void goHome(BlossomTeleport plugin, Player player, String name) {
        Location loc = plugin.homes().get(player.getUniqueId(), name);
        if (loc == null) {
            plugin.msg(player, "home-not-found", "{home}", name);
            return;
        }
        if (!plugin.teleports().checkCooldown(player, "homes")) {
            return;
        }
        String shown = name.toLowerCase(Locale.ROOT);
        plugin.teleports().teleport(player, () -> plugin.homes().get(player.getUniqueId(), shown),
                "homes", "Home: " + shown,
                () -> plugin.msg(player, "home-teleported", "{home}", shown));
    }

    private void setHome(Player player, String[] args) {
        String name = args.length >= 1 ? args[0] : "home";
        if (!HomeManager.validName(name)) {
            plugin.msg(player, "home-bad-name");
            return;
        }
        if (plugin.getConfig().getStringList("homes.blocked-worlds").contains(player.getWorld().getName())) {
            plugin.msg(player, "home-blocked-world");
            return;
        }
        Map<String, Location> homes = plugin.homes().homes(player.getUniqueId());
        boolean exists = homes.containsKey(name.toLowerCase(Locale.ROOT));
        int limit = plugin.homes().limit(player);
        if (!exists && homes.size() >= limit) {
            plugin.msg(player, "home-limit", "{limit}", limit);
            return;
        }
        plugin.homes().set(player.getUniqueId(), name, player.getLocation());
        plugin.msg(player, exists ? "home-updated" : "home-set", "{home}", name.toLowerCase(Locale.ROOT));
    }

    private void delHome(Player player, String[] args) {
        if (args.length < 1) {
            plugin.msg(player, "delhome-usage");
            return;
        }
        if (plugin.homes().delete(player.getUniqueId(), args[0])) {
            plugin.msg(player, "home-deleted", "{home}", args[0].toLowerCase(Locale.ROOT));
        } else {
            plugin.msg(player, "home-not-found", "{home}", args[0]);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (args.length == 1 && sender instanceof Player player && (name.equals("home") || name.equals("delhome"))) {
            String start = args[0].toLowerCase(Locale.ROOT);
            return plugin.homes().homes(player.getUniqueId()).keySet().stream()
                    .filter(h -> h.startsWith(start))
                    .toList();
        }
        return List.of();
    }
}
