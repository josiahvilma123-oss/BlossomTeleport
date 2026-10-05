package com.blossomsmp.teleport.commands;

import com.blossomsmp.teleport.BlossomTeleport;
import com.blossomsmp.teleport.util.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.List;

/** /blossomtp reload */
public class AdminCommand implements TabExecutor {

    private final BlossomTeleport plugin;

    public AdminCommand(BlossomTeleport plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            plugin.msg(sender, "reloaded");
            return true;
        }
        sender.sendMessage(Text.color("&#FF69B4✿ BlossomTeleport &f" + plugin.getPluginMeta().getVersion()));
        sender.sendMessage(Text.color("&7/" + label + " reload &8- &freload the config"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 ? List.of("reload") : List.of();
    }
}
