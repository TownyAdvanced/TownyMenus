package io.github.townyadvanced.townymenus.gui.action;

import io.github.townyadvanced.townymenus.gui.MenuInventory;
import io.github.townyadvanced.townymenus.gui.MenuItem;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public interface ClickAction {
    void onClick(MenuInventory inventory, InventoryClickEvent event);

    ClickAction EMPTY = (inventory, event) -> {};

    ClickAction CLOSE = (inventory, event) -> event.getWhoClicked().closeInventory(InventoryCloseEvent.Reason.PLAYER);

    BackAction BACK = new BackAction();

    static OpenInventoryAction openInventory(@NotNull Supplier<MenuInventory> supplier) {
        return new OpenInventoryAction(supplier);
    }

    static RunnableAction run(@NotNull Runnable runnable) {
        return new RunnableAction(runnable);
    }

    static ClickAction close() {
        return CLOSE;
    }

    static ConfirmationAction confirmation(@NotNull ClickAction confirmAction) {
        return new ConfirmationAction(confirmAction);
    }

    static ConfirmationAction confirmation(@NotNull Supplier<Component> supplier, @NotNull ClickAction confirmAction) {
        return new ConfirmationAction(supplier, confirmAction);
    }

    static SoundAction sound(@NotNull Sound sound) {
        return new SoundAction(sound);
    }

    static BackAction back() {
        return BACK;
    }

    static PaginatorAction paginate(Component title, Supplier<List<MenuItem>> supplier) {
        return new PaginatorAction(title, supplier);
    }

    static RightClickAction rightClick(@NotNull ClickAction rightClickAction) {
        return new RightClickAction(rightClickAction);
    }

    static UserInputAction userInput(String title, Function<String, AnvilGUI.Response> inputFunction) {
        return new UserInputAction(title, inputFunction);
    }
}
