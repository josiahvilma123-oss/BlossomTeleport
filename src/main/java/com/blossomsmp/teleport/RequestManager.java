package com.blossomsmp.teleport;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Keeps track of /tpa and /tpahere requests. */
public class RequestManager {

    public enum Type { TPA, TPAHERE }

    public static final class Request {
        public final UUID sender;
        public final UUID target;
        public final Type type;
        BukkitTask expiry;

        Request(UUID sender, UUID target, Type type) {
            this.sender = sender;
            this.target = target;
            this.type = type;
        }
    }

    private final BlossomTeleport plugin;
    // target -> (sender -> request), newest last
    private final Map<UUID, LinkedHashMap<UUID, Request>> incoming = new HashMap<>();

    public RequestManager(BlossomTeleport plugin) {
        this.plugin = plugin;
    }

    public boolean hasSent(Player sender, Player target) {
        LinkedHashMap<UUID, Request> map = incoming.get(target.getUniqueId());
        return map != null && map.containsKey(sender.getUniqueId());
    }

    public void send(Player sender, Player target, Type type) {
        Request request = new Request(sender.getUniqueId(), target.getUniqueId(), type);
        LinkedHashMap<UUID, Request> map = incoming.computeIfAbsent(target.getUniqueId(), k -> new LinkedHashMap<>());
        Request old = map.remove(sender.getUniqueId());
        if (old != null && old.expiry != null) {
            old.expiry.cancel();
        }
        map.put(sender.getUniqueId(), request);

        long ticks = Math.max(1, plugin.getConfig().getInt("tpa.timeout", 60)) * 20L;
        request.expiry = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (removeExact(request)) {
                Player s = Bukkit.getPlayer(request.sender);
                Player t = Bukkit.getPlayer(request.target);
                if (s != null) {
                    plugin.msg(s, "tpa-expired-sender", "{player}", t == null ? "player" : t.getName());
                }
                if (t != null) {
                    plugin.msg(t, "tpa-expired-target", "{player}", s == null ? "player" : s.getName());
                }
            }
        }, ticks);
    }

    /** The request from this sender, or the newest one if sender is null. */
    public Request find(Player target, Player sender) {
        LinkedHashMap<UUID, Request> map = incoming.get(target.getUniqueId());
        if (map == null || map.isEmpty()) {
            return null;
        }
        if (sender != null) {
            return map.get(sender.getUniqueId());
        }
        Request newest = null;
        for (Request r : map.values()) {
            newest = r;
        }
        return newest;
    }

    /** All requests this player has sent. */
    public List<Request> sentBy(Player sender) {
        List<Request> list = new ArrayList<>();
        for (LinkedHashMap<UUID, Request> map : incoming.values()) {
            Request r = map.get(sender.getUniqueId());
            if (r != null) {
                list.add(r);
            }
        }
        return list;
    }

    public void remove(Request request) {
        removeExact(request);
    }

    private boolean removeExact(Request request) {
        LinkedHashMap<UUID, Request> map = incoming.get(request.target);
        if (map == null || map.get(request.sender) != request) {
            return false;
        }
        map.remove(request.sender);
        if (map.isEmpty()) {
            incoming.remove(request.target);
        }
        if (request.expiry != null) {
            request.expiry.cancel();
        }
        return true;
    }

    /** Clears everything to and from a player (when they leave). */
    public void clear(Player player) {
        LinkedHashMap<UUID, Request> mine = incoming.remove(player.getUniqueId());
        if (mine != null) {
            mine.values().forEach(r -> {
                if (r.expiry != null) {
                    r.expiry.cancel();
                }
            });
        }
        for (Request r : sentBy(player)) {
            removeExact(r);
        }
    }
}
