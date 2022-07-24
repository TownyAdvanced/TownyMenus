package io.github.townyadvanced.townymenus.listeners;

import io.github.townyadvanced.townymenus.TownyMenus;
import io.github.townyadvanced.townymenus.gui.MenuInventory;
import io.github.townyadvanced.townymenus.gui.action.ClickAction;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

public class InventoryListener implements Listener {
    private final TownyMenus plugin;

    public InventoryListener(TownyMenus plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(final InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuInventory menu))
            return;

        event.setCancelled(true);

        List<ClickAction> actions = menu.actions(event.getSlot());
        if (actions != null)
            actions.forEach(action -> action.onClick(menu, event));
    }
}
