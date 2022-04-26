package io.github.townyadvanced.townymenus.gui.action;

import io.github.townyadvanced.townymenus.gui.MenuInventory;
import io.github.townyadvanced.townymenus.gui.MenuItem;
import net.kyori.adventure.text.Component;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

public class PaginatorAction implements ClickAction {
    private final List<MenuItem> items;
    private final Component title;

    private PaginatorAction(List<MenuItem> items, Component title) {
        this.items = items;
        this.title = title;
    }

    @Override
    public void onClick(MenuInventory inventory, InventoryClickEvent event) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }
}
