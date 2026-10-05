package com.blossomsmp.teleport;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/** Keeps every player's homes in homes.yml. Home names are stored in lowercase. */
public class HomeManager {

    private static final Pattern VALID = Pattern.compile("[a-zA-Z0-9_-]{1,16}");
    private static final String LIMIT_PERM = "blossomteleport.homes.";

    private final BlossomTeleport plugin;
    private final File file;
    private final Map<UUID, LinkedHashMap<String, Location>> homes = new HashMap<>();

    public HomeManager(BlossomTeleport plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "homes.yml");
        load();
    }

    private void load() {
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        for (String id : yml.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(id);
            } catch (IllegalArgumentException e) {
                continue;
            }
            ConfigurationSection player = yml.getConfigurationSection(id);
            if (player == null) {
                continue;
            }
            LinkedHashMap<String, Location> map = new LinkedHashMap<>();
            for (String name : player.getKeys(false)) {
                Location loc = DataStore.readLocation(player.getConfigurationSection(name));
                if (loc != null) {
                    map.put(name.toLowerCase(Locale.ROOT), loc);
                }
            }
            homes.put(uuid, map);
        }
    }

    public void save() {
        YamlConfiguration out = new YamlConfiguration();
        for (Map.Entry<UUID, LinkedHashMap<String, Location>> entry : homes.entrySet()) {
            for (Map.Entry<String, Location> home : entry.getValue().entrySet()) {
                DataStore.writeLocation(out.createSection(entry.getKey() + "." + home.getKey()), home.getValue());
            }
        }
        try {
            out.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save homes.yml: " + e.getMessage());
        }
    }

    public static boolean validName(String name) {
        return VALID.matcher(name).matches();
    }

    public Map<String, Location> homes(UUID player) {
        LinkedHashMap<String, Location> map = homes.get(player);
        return map == null ? Collections.emptyMap() : Collections.unmodifiableMap(map);
    }

    public Location get(UUID player, String name) {
        Location loc = homes(player).get(name.toLowerCase(Locale.ROOT));
        return loc == null ? null : loc.clone();
    }

    /** Returns true if this replaced an existing home. */
    public boolean set(UUID player, String name, Location location) {
        boolean existed = homes.computeIfAbsent(player, k -> new LinkedHashMap<>())
                .put(name.toLowerCase(Locale.ROOT), location.clone()) != null;
        save();
        return existed;
    }

    public boolean delete(UUID player, String name) {
        LinkedHashMap<String, Location> map = homes.get(player);
        if (map == null || map.remove(name.toLowerCase(Locale.ROOT)) == null) {
            return false;
        }
        save();
        return true;
    }

    /** How many homes this player may have, based on config + permissions. */
    public int limit(Player player) {
        if (player.hasPermission(LIMIT_PERM + "unlimited")) {
            return Integer.MAX_VALUE;
        }
        int best = plugin.getConfig().getInt("homes.default-limit", 3);
        ConfigurationSection named = plugin.getConfig().getConfigurationSection("homes.limits");
        if (named != null) {
            for (String key : named.getKeys(false)) {
                if (player.hasPermission(LIMIT_PERM + key)) {
                    best = Math.max(best, named.getInt(key));
                }
            }
        }
        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            String perm = info.getPermission().toLowerCase(Locale.ROOT);
            if (info.getValue() && perm.startsWith(LIMIT_PERM)) {
                try {
                    best = Math.max(best, Integer.parseInt(perm.substring(LIMIT_PERM.length())));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return best;
    }
}
