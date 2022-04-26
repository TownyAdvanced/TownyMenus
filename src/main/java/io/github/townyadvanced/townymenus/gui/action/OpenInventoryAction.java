package io.github.townyadvanced.townymenus.gui.action;

import io.github.townyadvanced.townymenus.gui.MenuInventory;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class OpenInventoryAction implements ClickAction {
    private final Supplier<MenuInventory> supplier;
    private boolean running;

    public OpenInventoryAction(@NotNull Supplier<MenuInventory> supplier) {
        this.supplier = supplier;
    }

    @Override
    public void onClick(MenuInventory inventory, InventoryClickEvent event) {
        if (running)
            return;

        running = true;
        CompletableFuture.runAsync(() -> {
            supplier.get().open(event.getWhoClicked());
            running = false;
        });
    }
}
