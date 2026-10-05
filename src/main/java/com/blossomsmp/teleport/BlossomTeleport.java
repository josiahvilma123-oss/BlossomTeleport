package com.blossomsmp.teleport;

import com.blossomsmp.teleport.commands.AdminCommand;
import com.blossomsmp.teleport.commands.BackCommand;
import com.blossomsmp.teleport.commands.HomeCommands;
import com.blossomsmp.teleport.commands.RtpCommand;
import com.blossomsmp.teleport.commands.SpawnCommands;
import com.blossomsmp.teleport.commands.TpaCommands;
import com.blossomsmp.teleport.listeners.MenuListener;
import com.blossomsmp.teleport.listeners.PlayerListener;
import com.blossomsmp.teleport.util.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.java.JavaPlugin;

public final class BlossomTeleport extends JavaPlugin {

    private TeleportManager teleports;
    private RequestManager requests;
    private HomeManager homes;
    private DataStore data;
    private RtpManager rtp;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        // Adds any NEW settings from future updates without wiping your changes
        getConfig().options().copyDefaults(true);
        saveConfig();

        data = new DataStore(this);
        homes = new HomeManager(this);
        teleports = new TeleportManager(this);
        requests = new RequestManager(this);
        rtp = new RtpManager(this);

        TpaCommands tpa = new TpaCommands(this);
        for (String name : new String[]{"tpa", "tpahere", "tpaccept", "tpdeny", "tpacancel", "tptoggle"}) {
            register(name, tpa);
        }
        HomeCommands home = new HomeCommands(this);
        for (String name : new String[]{"home", "homes", "sethome", "delhome"}) {
            register(name, home);
        }
        SpawnCommands spawn = new SpawnCommands(this);
        register("spawn", spawn);
        register("setspawn", spawn);
        register("back", new BackCommand(this));
        register("rtp", new RtpCommand(this));
        register("blossomtp", new AdminCommand(this));

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getLogger().info("BlossomTeleport is ready!");
    }

    @Override
    public void onDisable() {
        if (teleports != null) {
            teleports.cancelAll();
        }
        if (homes != null) {
            homes.save();
        }
        if (data != null) {
            data.save();
        }
    }

    private void register(String name, TabExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("Command missing from plugin.yml: " + name);
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    /** Gets a message from the config with {placeholders} filled in. "" means turned off. */
    public String raw(String key, Object... replacements) {
        String text = getConfig().getString("messages." + key, "");
        if (text == null) {
            return "";
        }
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            text = text.replace(String.valueOf(replacements[i]), String.valueOf(replacements[i + 1]));
        }
        return text;
    }

    /** Sends a message with the prefix. Does nothing if the message is turned off. */
    public void msg(CommandSender to, String key, Object... replacements) {
        String text = raw(key, replacements);
        if (text.isEmpty()) {
            return;
        }
        to.sendMessage(Text.color(raw("prefix") + text));
    }

    public TeleportManager teleports() {
        return teleports;
    }

    public RequestManager requests() {
        return requests;
    }

    public HomeManager homes() {
        return homes;
    }

    public DataStore data() {
        return data;
    }

    public RtpManager rtp() {
        return rtp;
    }
}
