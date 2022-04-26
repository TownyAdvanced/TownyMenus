package io.github.townyadvanced.townymenus.gui.action;

import io.github.townyadvanced.townymenus.gui.MenuInventory;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

public class RunnableAction implements ClickAction {
    private final Runnable runnable;

    public RunnableAction(@NotNull Runnable runnable) {
        this.runnable = runnable;
    }

    @Override
    public void onClick(MenuInventory inventory, InventoryClickEvent event) {
        runnable.run();
    }
}
