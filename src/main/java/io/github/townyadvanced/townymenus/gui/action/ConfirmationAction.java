package io.github.townyadvanced.townymenus.gui.action;

import net.kyori.adventure.text.Component;
import io.github.townyadvanced.townymenus.gui.MenuHelper;
import io.github.townyadvanced.townymenus.gui.MenuInventory;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class ConfirmationAction implements ClickAction {
    private final ClickAction confirmAction;
    private final Supplier<Component> warning;

    public ConfirmationAction(@NotNull ClickAction confirmAction) {
        this.confirmAction = confirmAction;
        this.warning = Component::empty;
    }

    public ConfirmationAction(@NotNull Supplier<Component> warning, @NotNull ClickAction confirmAction) {
        this.confirmAction = confirmAction;
        this.warning = warning;
    }

    public ConfirmationAction(@NotNull Component warning, @NotNull ClickAction confirmAction) {
        this.confirmAction = confirmAction;
        this.warning = () -> warning;
    }

    @Override
    public void onClick(MenuInventory inventory, InventoryClickEvent event) {
        MenuHelper.createConfirmation(warning.get(), confirmAction, ClickAction.openSilent(inventory))
                .openSilent(event.getWhoClicked());
    }
}
