package io.github.townyadvanced.townymenus.gui.action;

import io.github.townyadvanced.townymenus.gui.MenuInventory;
import io.github.townyadvanced.townymenus.gui.MenuItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
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
        MenuInventory.builder()
                .size(27)
                .title(Component.text("Do you you want to continue?"))
                .addItem(MenuItem.builder(Material.GREEN_STAINED_GLASS)
                        .name(Component.text("Confirm", NamedTextColor.DARK_GREEN).decorate(TextDecoration.BOLD))
                        .lore(warning.get())
                        .slot(11)
                        .action(confirmAction)
                        .build())
                .addItem(MenuItem.builder(Material.RED_STAINED_GLASS)
                        .name(Component.text("Cancel", NamedTextColor.DARK_RED).decorate(TextDecoration.BOLD))
                        .slot(15)
                        .action(ClickAction.openSilent(inventory))
                        .build())
                .build()
                .openSilent(event.getWhoClicked());
    }
}
