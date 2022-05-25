package io.github.townyadvanced.townymenus.gui.action;

import io.github.townyadvanced.townymenus.gui.MenuHistory;
import io.github.townyadvanced.townymenus.gui.MenuInventory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class BackAction implements ClickAction {

    @Override
    public void onClick(MenuInventory inventory, InventoryClickEvent event) {
        MenuHistory.back((Player) event.getWhoClicked());
    }
}
