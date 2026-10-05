package com.blossomsmp.teleport;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/** Finds a safe random spot without freezing the server (chunks load in the background). */
public class RtpManager {

    private final BlossomTeleport plugin;

    public RtpManager(BlossomTeleport plugin) {
        this.plugin = plugin;
    }

    public void find(World world, Consumer<Location> done) {
        attempt(world, Math.max(1, plugin.getConfig().getInt("rtp.max-tries", 15)), unsafeBlocks(), done);
    }

    private void attempt(World world, int triesLeft, Set<Material> unsafe, Consumer<Location> done) {
        int min = Math.max(0, plugin.getConfig().getInt("rtp.min-distance", 300));
        int max = Math.max(min + 1, plugin.getConfig().getInt("rtp.max-distance", 3000));
        int cx = plugin.getConfig().getInt("rtp.center-x", 0);
        int cz = plugin.getConfig().getInt("rtp.center-z", 0);

        ThreadLocalRandom random = ThreadLocalRandom.current();
        double angle = random.nextDouble(Math.PI * 2);
        double distance = min + random.nextDouble() * (max - min);
        int x = cx + (int) Math.round(Math.cos(angle) * distance);
        int z = cz + (int) Math.round(Math.sin(angle) * distance);

        world.getChunkAtAsync(x >> 4, z >> 4).thenAccept(chunk -> {
            Location found = safeSpot(world, x, z, unsafe);
            if (found != null) {
                done.accept(found);
            } else if (triesLeft > 1) {
                attempt(world, triesLeft - 1, unsafe, done);
            } else {
                done.accept(null);
            }
        }).exceptionally(error -> {
            done.accept(null);
            return null;
        });
    }

    private Location safeSpot(World world, int x, int z, Set<Material> unsafe) {
        if (world.getEnvironment() == World.Environment.NETHER) {
            // Search under the bedrock roof for a floor with 2 air blocks above it
            for (int y = 32; y < 118; y++) {
                Location loc = check(world, x, y, z, unsafe);
                if (loc != null) {
                    return loc;
                }
            }
            return null;
        }
        int y = world.getHighestBlockYAt(x, z);
        return check(world, x, y, z, unsafe);
    }

    private Location check(World world, int x, int y, int z, Set<Material> unsafe) {
        Block ground = world.getBlockAt(x, y, z);
        Block feet = ground.getRelative(0, 1, 0);
        Block head = ground.getRelative(0, 2, 0);
        if (!ground.getType().isSolid() || ground.isLiquid() || unsafe.contains(ground.getType())) {
            return null;
        }
        if (!feet.isPassable() || feet.isLiquid() || unsafe.contains(feet.getType())) {
            return null;
        }
        if (!head.isPassable() || head.isLiquid() || unsafe.contains(head.getType())) {
            return null;
        }
        Location loc = new Location(world, x + 0.5, y + 1, z + 0.5);
        if (!world.getWorldBorder().isInside(loc)) {
            return null;
        }
        return loc;
    }

    private Set<Material> unsafeBlocks() {
        Set<Material> set = EnumSet.noneOf(Material.class);
        for (String name : plugin.getConfig().getStringList("rtp.unsafe-blocks")) {
            Material m = Material.matchMaterial(name);
            if (m != null) {
                set.add(m);
            }
        }
        return set;
    }
}
