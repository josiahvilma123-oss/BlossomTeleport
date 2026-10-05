package com.blossomsmp.teleport.listeners;

import com.blossomsmp.teleport.BlossomTeleport;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class PlayerListener implements Listener {

    private final BlossomTeleport plugin;

    public PlayerListener(BlossomTeleport plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock() || !plugin.teleports().hasWarmup(event.getPlayer())) {
            return;
        }
        if (plugin.getConfig().getBoolean("teleport.cancel-on-move", true)) {
            plugin.teleports().cancel(event.getPlayer(), true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player
                && plugin.teleports().hasWarmup(player)
                && plugin.getConfig().getBoolean("teleport.cancel-on-damage", true)) {
            plugin.teleports().cancel(player, true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        plugin.teleports().cancel(player, false);
        if (plugin.getConfig().getBoolean("back.enabled", true) && plugin.getConfig().getBoolean("back.on-death", true)) {
            plugin.teleports().setBack(player, player.getLocation());
            plugin.msg(player, "back-death-hint");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRespawn(PlayerRespawnEvent event) {
        if (!plugin.getConfig().getBoolean("spawn.teleport-on-respawn", true)
                || event.isBedSpawn() || event.isAnchorSpawn()) {
            return;
        }
        Location spawn = plugin.data().spawn();
        if (spawn != null) {
            event.setRespawnLocation(spawn);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Location spawn = plugin.data().spawn();
        if (spawn == null) {
            return;
        }
        boolean firstJoin = !player.hasPlayedBefore() && plugin.getConfig().getBoolean("spawn.teleport-on-first-join", true);
        boolean everyJoin = plugin.getConfig().getBoolean("spawn.teleport-on-join", false);
        if (firstJoin || everyJoin) {
            // Wait a moment so the player has fully loaded in
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.teleportAsync(spawn, PlayerTeleportEvent.TeleportCause.PLUGIN);
                }
            }, 5L);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.teleports().cancel(event.getPlayer(), false);
        plugin.requests().clear(event.getPlayer());
    }
}
