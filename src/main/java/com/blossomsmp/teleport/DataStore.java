package com.blossomsmp.teleport;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Saves the spawn point and who has /tptoggle turned off (data.yml). */
public class DataStore {

    private final BlossomTeleport plugin;
    private final File file;
    private Location spawn;
    private final Set<UUID> requestsOff = new HashSet<>();

    public DataStore(BlossomTeleport plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
        load();
    }

    private void load() {
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        spawn = readLocation(yml.getConfigurationSection("spawn"));
        for (String id : yml.getStringList("requests-off")) {
            try {
                requestsOff.add(UUID.fromString(id));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();
        if (spawn != null) {
            writeLocation(yml.createSection("spawn"), spawn);
        }
        yml.set("requests-off", requestsOff.stream().map(UUID::toString).toList());
        try {
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save data.yml: " + e.getMessage());
        }
    }

    public Location spawn() {
        return spawn == null ? null : spawn.clone();
    }

    public void setSpawn(Location location) {
        this.spawn = location.clone();
        save();
    }

    public boolean requestsOff(UUID id) {
        return requestsOff.contains(id);
    }

    /** Flips the setting. Returns true if requests are now OFF. */
    public boolean toggleRequests(UUID id) {
        boolean nowOff;
        if (requestsOff.remove(id)) {
            nowOff = false;
        } else {
            requestsOff.add(id);
            nowOff = true;
        }
        save();
        return nowOff;
    }

    // ---- shared helpers so homes.yml and data.yml look the same ----

    public static void writeLocation(ConfigurationSection section, Location loc) {
        section.set("world", loc.getWorld() == null ? "world" : loc.getWorld().getName());
        section.set("x", loc.getX());
        section.set("y", loc.getY());
        section.set("z", loc.getZ());
        section.set("yaw", (double) loc.getYaw());
        section.set("pitch", (double) loc.getPitch());
    }

    public static Location readLocation(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        World world = Bukkit.getWorld(section.getString("world", "world"));
        if (world == null) {
            return null;
        }
        return new Location(world,
                section.getDouble("x"), section.getDouble("y"), section.getDouble("z"),
                (float) section.getDouble("yaw"), (float) section.getDouble("pitch"));
    }
}
