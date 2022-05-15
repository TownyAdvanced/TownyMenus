package io.github.townyadvanced.townymenus.gui.action;

import io.github.townyadvanced.townymenus.gui.MenuInventory;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

public class RightClickAction implements ClickAction {
    private final ClickAction rightClickAction;

    public RightClickAction(@NotNull ClickAction rightClickAction) {
        this.rightClickAction = rightClickAction;
    }

    @Override
    public void onClick(MenuInventory inventory, InventoryClickEvent event) {
        if (event.isRightClick())
            this.rightClickAction.onClick(inventory, event);
    }
}
