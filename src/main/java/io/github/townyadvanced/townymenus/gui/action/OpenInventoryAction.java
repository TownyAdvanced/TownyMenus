package io.github.townyadvanced.townymenus.gui.action;

import io.github.townyadvanced.townymenus.gui.MenuInventory;
import io.github.townyadvanced.townymenus.utils.MenuScheduler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class OpenInventoryAction implements ClickAction {
    private final Supplier<MenuInventory> supplier;
    private final boolean silent;

    public OpenInventoryAction(@NotNull Supplier<MenuInventory> supplier, boolean silent) {
        this.supplier = supplier;
        this.silent = silent;
    }

    @Override
    public void onClick(MenuInventory inventory, InventoryClickEvent event) {
        MenuScheduler.scheduleAsync(event.getWhoClicked(), () -> {
            if (silent)
                supplier.get().openSilent(event.getWhoClicked());
            else
                supplier.get().open(event.getWhoClicked());
        });
    }
}
