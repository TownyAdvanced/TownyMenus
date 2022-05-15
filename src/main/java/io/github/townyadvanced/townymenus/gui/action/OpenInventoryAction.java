package io.github.townyadvanced.townymenus.gui.action;

import io.github.townyadvanced.townymenus.gui.MenuInventory;
import io.github.townyadvanced.townymenus.utils.MenuScheduler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class OpenInventoryAction implements ClickAction {
    private final Supplier<MenuInventory> supplier;

    public OpenInventoryAction(@NotNull Supplier<MenuInventory> supplier) {
        this.supplier = supplier;
    }

    @Override
    public void onClick(MenuInventory inventory, InventoryClickEvent event) {
        MenuScheduler.scheduleAsync(event.getWhoClicked(), () -> supplier.get().open(event.getWhoClicked()));
    }
}
