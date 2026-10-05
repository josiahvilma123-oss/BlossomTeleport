package com.blossomsmp.teleport.listeners;

import com.blossomsmp.teleport.BlossomTeleport;
import com.blossomsmp.teleport.commands.HomeCommands;
import com.blossomsmp.teleport.menus.HomesMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class MenuListener implements Listener {

    private final BlossomTeleport plugin;

    public MenuListener(BlossomTeleport plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof HomesMenu menu)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        String home = menu.homeAt(event.getRawSlot());
        if (home == null) {
            return;
        }
        if (event.getClick() == ClickType.SHIFT_RIGHT) {
            plugin.homes().delete(player.getUniqueId(), home);
            plugin.msg(player, "home-deleted", "{home}", home);
            HomesMenu.open(plugin, player); // refresh
            return;
        }
        if (event.getClick().isLeftClick()) {
            player.closeInventory();
            HomeCommands.goHome(plugin, player, home);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof HomesMenu) {
            event.setCancelled(true);
        }
    }
}
