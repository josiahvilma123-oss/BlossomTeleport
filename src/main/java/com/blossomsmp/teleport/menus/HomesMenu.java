package com.blossomsmp.teleport.menus;

import com.blossomsmp.teleport.BlossomTeleport;
import com.blossomsmp.teleport.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The /homes chest menu. */
public final class HomesMenu implements InventoryHolder {

    private final Map<Integer, String> slots = new HashMap<>();
    private Inventory inventory;

    private HomesMenu() {
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /** Which home is in this slot (null = not a home). */
    public String homeAt(int slot) {
        return slots.get(slot);
    }

    public static void open(BlossomTeleport plugin, Player player) {
        Map<String, Location> homes = plugin.homes().homes(player.getUniqueId());
        int limit = plugin.homes().limit(player);
        int shown = Math.min(54, Math.max(homes.size(), Math.min(limit, 54)));
        int size = Math.max(9, Math.min(54, ((shown + 8) / 9) * 9));

        HomesMenu menu = new HomesMenu();
        menu.inventory = Bukkit.createInventory(menu, size, Text.color(plugin.getConfig().getString("homes.menu.title", "Your Homes")));

        Material homeType = material(plugin.getConfig().getString("homes.menu.home-item"), Material.RED_BED);
        Material emptyType = material(plugin.getConfig().getString("homes.menu.empty-item"), Material.GRAY_STAINED_GLASS_PANE);

        int slot = 0;
        for (Map.Entry<String, Location> entry : homes.entrySet()) {
            if (slot >= size) {
                break;
            }
            Location loc = entry.getValue();
            String world = loc.getWorld() == null ? "?" : loc.getWorld().getName();
            ItemStack item = item(homeType,
                    fill(plugin.getConfig().getString("homes.menu.home-name", "{home}"), entry.getKey(), world, loc),
                    plugin.getConfig().getStringList("homes.menu.home-lore"), entry.getKey(), world, loc);
            menu.inventory.setItem(slot, item);
            menu.slots.put(slot, entry.getKey());
            slot++;
        }
        while (slot < Math.min(size, limit)) {
            menu.inventory.setItem(slot, item(emptyType,
                    plugin.getConfig().getString("homes.menu.empty-name", "Empty"),
                    plugin.getConfig().getStringList("homes.menu.empty-lore"), "", "", null));
            slot++;
        }
        player.openInventory(menu.inventory);
    }

    private static ItemStack item(Material type, String name, List<String> lore, String home, String world, Location loc) {
        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Text.item(name));
            List<Component> lines = new ArrayList<>();
            for (String line : lore) {
                lines.add(Text.item(fill(line, home, world, loc)));
            }
            meta.lore(lines);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String fill(String text, String home, String world, Location loc) {
        if (text == null) {
            return "";
        }
        text = text.replace("{home}", home).replace("{world}", world);
        if (loc != null) {
            text = text.replace("{x}", String.valueOf(loc.getBlockX()))
                    .replace("{y}", String.valueOf(loc.getBlockY()))
                    .replace("{z}", String.valueOf(loc.getBlockZ()));
        }
        return text;
    }

    private static Material material(String name, Material fallback) {
        Material m = name == null ? null : Material.matchMaterial(name);
        return m == null || !m.isItem() ? fallback : m;
    }
}
