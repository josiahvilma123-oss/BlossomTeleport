package com.blossomsmp.teleport;

import com.blossomsmp.teleport.util.Text;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/** Handles warmups (stand still), cooldowns, /back spots and the actual teleport. */
public class TeleportManager {

    private final BlossomTeleport plugin;
    private final Map<UUID, BukkitTask> warmups = new HashMap<>();
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    private final Map<UUID, Location> backSpots = new HashMap<>();

    public TeleportManager(BlossomTeleport plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------ cooldowns

    /** Returns true if the player may use this feature now. Sends the wait message if not. */
    public boolean checkCooldown(Player player, String feature) {
        if (player.hasPermission("blossomteleport.bypass.cooldown")) {
            return true;
        }
        long until = cooldowns.getOrDefault(player.getUniqueId(), Map.of()).getOrDefault(feature, 0L);
        long left = until - System.currentTimeMillis();
        if (left > 0) {
            plugin.msg(player, "cooldown", "{time}", Text.time(left));
            return false;
        }
        return true;
    }

    /** Starts the cooldown from "<feature>.cooldown" in the config. */
    public void startCooldown(Player player, String feature) {
        int seconds = plugin.getConfig().getInt(feature + ".cooldown", 0);
        if (seconds <= 0 || player.hasPermission("blossomteleport.bypass.cooldown")) {
            return;
        }
        cooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                .put(feature, System.currentTimeMillis() + seconds * 1000L);
    }

    // ------------------------------------------------------------ /back

    public void setBack(Player player, Location location) {
        if (location != null && location.getWorld() != null) {
            backSpots.put(player.getUniqueId(), location.clone());
        }
    }

    public Location back(Player player) {
        Location loc = backSpots.get(player.getUniqueId());
        return loc == null ? null : loc.clone();
    }

    // ------------------------------------------------------------ combat

    /** Asks BlossomCombat (if installed) whether this player is in combat. */
    public boolean inCombat(Player player) {
        if (!plugin.getConfig().getBoolean("teleport.block-in-combat", true)) {
            return false;
        }
        try {
            org.bukkit.plugin.Plugin combat = Bukkit.getPluginManager().getPlugin("BlossomCombat");
            if (combat == null || !combat.isEnabled()) {
                return false;
            }
            Object manager = combat.getClass().getMethod("combat").invoke(combat);
            Object tagged = manager.getClass().getMethod("isTagged", Player.class).invoke(manager, player);
            return Boolean.TRUE.equals(tagged);
        } catch (Throwable ignored) {
            return false;
        }
    }

    // ------------------------------------------------------------ teleporting

    public boolean hasWarmup(Player player) {
        return warmups.containsKey(player.getUniqueId());
    }

    /** Stops a waiting teleport. */
    public void cancel(Player player, boolean tellThem) {
        BukkitTask task = warmups.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
            if (tellThem) {
                plugin.msg(player, "warmup-cancelled");
                player.sendActionBar(Text.color(""));
            }
        }
    }

    public void cancelAll() {
        warmups.values().forEach(BukkitTask::cancel);
        warmups.clear();
    }

    /**
     * Teleports the player after the warmup.
     *
     * @param destination  worked out at the moment of teleporting (so /tpa goes to where the player is NOW)
     * @param feature      config section for the cooldown ("homes", "rtp", ...), or null for none
     * @param place        shown in the subtitle, e.g. "Home: base"
     * @param onSuccess    runs after a successful teleport (to send a message)
     */
    public void teleport(Player player, Supplier<Location> destination, String feature, String place, Runnable onSuccess) {
        if (inCombat(player)) {
            plugin.msg(player, "in-combat");
            return;
        }
        cancel(player, false);

        int warmup = plugin.getConfig().getInt("teleport.warmup", 3);
        if (warmup <= 0 || player.hasPermission("blossomteleport.bypass.warmup")) {
            finish(player, destination, feature, place, onSuccess);
            return;
        }

        plugin.msg(player, "warmup", "{seconds}", warmup);
        UUID id = player.getUniqueId();
        BukkitTask task = new BukkitRunnable() {
            int left = warmup;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    warmups.remove(id);
                    this.cancel();
                    return;
                }
                if (left <= 0) {
                    warmups.remove(id);
                    this.cancel();
                    finish(player, destination, feature, place, onSuccess);
                    return;
                }
                String bar = plugin.raw("warmup-actionbar", "{seconds}", left);
                if (!bar.isEmpty()) {
                    player.sendActionBar(Text.color(bar));
                }
                left--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
        warmups.put(id, task);
    }

    private void finish(Player player, Supplier<Location> destination, String feature, String place, Runnable onSuccess) {
        if (inCombat(player)) {
            plugin.msg(player, "in-combat");
            return;
        }
        Location to = destination.get();
        if (to == null || to.getWorld() == null) {
            plugin.msg(player, "teleport-failed");
            return;
        }
        Location from = player.getLocation();
        player.teleportAsync(to, PlayerTeleportEvent.TeleportCause.PLUGIN).thenAccept(success -> {
            if (!player.isOnline()) {
                return;
            }
            if (!Boolean.TRUE.equals(success)) {
                plugin.msg(player, "teleport-failed");
                return;
            }
            setBack(player, from);
            if (feature != null) {
                startCooldown(player, feature);
            }
            effects(player, place);
            if (onSuccess != null) {
                onSuccess.run();
            }
        });
    }

    private void effects(Player player, String place) {
        String sound = plugin.getConfig().getString("teleport.sound", "");
        if (sound != null && !sound.isEmpty()) {
            try {
                player.playSound(player.getLocation(), sound, 1f, 1f);
            } catch (Exception ignored) {
            }
        }
        if (plugin.getConfig().getBoolean("teleport.particles", true)) {
            player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 60, 0.5, 1, 0.5, 0.2);
        }
        if (plugin.getConfig().getBoolean("teleport.title", true)) {
            player.showTitle(Title.title(
                    Text.color(plugin.raw("title")),
                    Text.color(plugin.raw("subtitle", "{place}", place == null ? "" : place)),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(1500), Duration.ofMillis(400))));
        }
    }
}
