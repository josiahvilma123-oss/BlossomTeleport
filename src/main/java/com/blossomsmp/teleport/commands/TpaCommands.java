package com.blossomsmp.teleport.commands;

import com.blossomsmp.teleport.BlossomTeleport;
import com.blossomsmp.teleport.RequestManager;
import com.blossomsmp.teleport.RequestManager.Request;
import com.blossomsmp.teleport.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/** /tpa /tpahere /tpaccept /tpdeny /tpacancel /tptoggle */
public class TpaCommands implements TabExecutor {

    private final BlossomTeleport plugin;

    public TpaCommands(BlossomTeleport plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.msg(sender, "players-only");
            return true;
        }
        if (!plugin.getConfig().getBoolean("tpa.enabled", true)) {
            plugin.msg(player, "disabled");
            return true;
        }
        switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "tpa" -> request(player, args, RequestManager.Type.TPA, label);
            case "tpahere" -> request(player, args, RequestManager.Type.TPAHERE, label);
            case "tpaccept" -> accept(player, args);
            case "tpdeny" -> deny(player, args);
            case "tpacancel" -> cancel(player, args);
            case "tptoggle" -> toggle(player);
            default -> {
            }
        }
        return true;
    }

    private void request(Player player, String[] args, RequestManager.Type type, String label) {
        if (args.length < 1) {
            player.sendMessage(Text.color(plugin.raw("prefix") + "&cUse: &f/" + label + " <player>"));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            target = Bukkit.getPlayer(args[0]);
        }
        if (target == null || !player.canSee(target)) {
            plugin.msg(player, "player-not-found");
            return;
        }
        if (target.equals(player)) {
            plugin.msg(player, "tpa-self");
            return;
        }
        if (plugin.data().requestsOff(target.getUniqueId()) && !player.hasPermission("blossomteleport.bypass.toggle")) {
            plugin.msg(player, "tpa-target-toggled", "{player}", target.getName());
            return;
        }
        if (plugin.requests().hasSent(player, target)) {
            plugin.msg(player, "tpa-already-sent", "{player}", target.getName());
            return;
        }
        if (!plugin.teleports().checkCooldown(player, "tpa")) {
            return;
        }

        plugin.requests().send(player, target, type);
        plugin.teleports().startCooldown(player, "tpa");
        plugin.msg(player, "tpa-sent", "{player}", target.getName(),
                "{seconds}", plugin.getConfig().getInt("tpa.timeout", 60));

        String key = type == RequestManager.Type.TPA ? "tpa-received" : "tpahere-received";
        plugin.msg(target, key, "{player}", player.getName());

        // Clickable [ACCEPT] [DENY] buttons
        Component accept = Text.color(plugin.raw("tpa-accept-button"))
                .clickEvent(ClickEvent.runCommand("/tpaccept " + player.getName()))
                .hoverEvent(HoverEvent.showText(Text.color(plugin.raw("tpa-accept-hover"))));
        Component deny = Text.color(plugin.raw("tpa-deny-button"))
                .clickEvent(ClickEvent.runCommand("/tpdeny " + player.getName()))
                .hoverEvent(HoverEvent.showText(Text.color(plugin.raw("tpa-deny-hover"))));
        target.sendMessage(Text.color(plugin.raw("prefix")).append(accept).append(Component.text("  ")).append(deny));

        String sound = plugin.getConfig().getString("tpa.request-sound", "");
        if (sound != null && !sound.isEmpty()) {
            try {
                target.playSound(target.getLocation(), sound, 1f, 1.2f);
            } catch (Exception ignored) {
            }
        }
    }

    private Request findRequest(Player player, String[] args) {
        Player from = null;
        if (args.length >= 1) {
            from = Bukkit.getPlayerExact(args[0]);
            if (from == null) {
                plugin.msg(player, "player-not-found");
                return null;
            }
        }
        Request request = plugin.requests().find(player, from);
        if (request == null) {
            plugin.msg(player, "tpa-no-requests");
        }
        return request;
    }

    private void accept(Player player, String[] args) {
        Request request = findRequest(player, args);
        if (request == null) {
            return;
        }
        plugin.requests().remove(request);
        Player requester = Bukkit.getPlayer(request.sender);
        if (requester == null) {
            plugin.msg(player, "player-not-found");
            return;
        }
        plugin.msg(requester, "tpa-accepted-sender", "{player}", player.getName());
        plugin.msg(player, "tpa-accepted-target", "{player}", requester.getName());

        // /tpa: the requester comes to you.  /tpahere: you go to the requester.
        Player mover = request.type == RequestManager.Type.TPA ? requester : player;
        Player destination = mover == requester ? player : requester;
        plugin.teleports().teleport(mover,
                () -> destination.isOnline() ? destination.getLocation() : (Location) null,
                null,
                destination.getName(),
                () -> plugin.msg(mover, "tpa-teleported", "{player}", destination.getName()));
    }

    private void deny(Player player, String[] args) {
        Request request = findRequest(player, args);
        if (request == null) {
            return;
        }
        plugin.requests().remove(request);
        Player requester = Bukkit.getPlayer(request.sender);
        String name = requester == null ? "player" : requester.getName();
        plugin.msg(player, "tpa-denied-target", "{player}", name);
        if (requester != null) {
            plugin.msg(requester, "tpa-denied-sender", "{player}", player.getName());
        }
    }

    private void cancel(Player player, String[] args) {
        List<Request> sent = plugin.requests().sentBy(player);
        if (args.length >= 1) {
            sent = sent.stream().filter(r -> {
                Player t = Bukkit.getPlayer(r.target);
                return t != null && t.getName().equalsIgnoreCase(args[0]);
            }).toList();
        }
        if (sent.isEmpty()) {
            plugin.msg(player, "tpa-nothing-to-cancel");
            return;
        }
        for (Request r : sent) {
            plugin.requests().remove(r);
            Player t = Bukkit.getPlayer(r.target);
            if (t != null) {
                plugin.msg(t, "tpa-cancelled-target", "{player}", player.getName());
            }
        }
        plugin.msg(player, "tpa-cancelled");
    }

    private void toggle(Player player) {
        if (!player.hasPermission("blossomteleport.tptoggle")) {
            plugin.msg(player, "no-permission");
            return;
        }
        boolean off = plugin.data().toggleRequests(player.getUniqueId());
        plugin.msg(player, off ? "tpa-toggled-off" : "tpa-toggled-on");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && !command.getName().equalsIgnoreCase("tptoggle")) {
            String start = args[0].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream()
                    .filter(p -> !(sender instanceof Player s) || s.canSee(p))
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(start))
                    .toList();
        }
        return List.of();
    }
}
