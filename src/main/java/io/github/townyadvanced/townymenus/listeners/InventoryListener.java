package io.github.townyadvanced.townymenus.listeners;

import com.palmergames.paperlib.PaperLib;
import io.github.townyadvanced.townymenus.TownyMenus;
import io.github.townyadvanced.townymenus.gui.MenuInventory;
import io.github.townyadvanced.townymenus.gui.MenuItem;
import io.github.townyadvanced.townymenus.gui.action.ClickAction;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class InventoryListener implements Listener {
    private final TownyMenus plugin;

    public InventoryListener(TownyMenus plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(final InventoryClickEvent event) {
        if (!(PaperLib.getHolder(event.getInventory(), false).getHolder() instanceof MenuInventory menu))
            return;

        event.setCancelled(true);

        List<ClickAction> actions = menu.actions(event.getSlot());
        if (actions != null)
            actions.forEach(action -> action.onClick(menu, event));
    }

    @EventHandler
    public void onInventoryClose(final InventoryCloseEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuInventory))
            return;

        for (ItemStack item : event.getView().getBottomInventory()) {
            if (item == null)
                continue;

            final ItemMeta meta = item.getItemMeta();
            if (meta == null)
                continue;

            if (meta.getPersistentDataContainer().has(MenuItem.PDC_KEY, PersistentDataType.BYTE))
                item.setAmount(0);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onItemDrop(final PlayerDropItemEvent event) {
        final ItemMeta meta = event.getItemDrop().getItemStack().getItemMeta();

        if (meta != null && meta.getPersistentDataContainer().has(MenuItem.PDC_KEY, PersistentDataType.BYTE))
            event.getItemDrop().remove();
    }
}
