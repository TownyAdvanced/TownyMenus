package io.github.townyadvanced.townymenus.gui.action;

import io.github.townyadvanced.townymenus.gui.MenuInventory;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.Supplier;

public interface ClickAction {
    void onClick(MenuInventory inventory, InventoryClickEvent event);

    ClickAction NONE = (inventory, event) -> {};

    ClickAction CLOSE = (inventory, event) -> event.getWhoClicked().closeInventory(InventoryCloseEvent.Reason.PLUGIN);

    BackAction BACK = new BackAction();

    static OpenInventoryAction openInventory(@NotNull Supplier<MenuInventory> supplier) {
        return new OpenInventoryAction(supplier, false);
    }

    static OpenInventoryAction openSilent(@NotNull Supplier<MenuInventory> supplier) {
        return new OpenInventoryAction(supplier, true);
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

    static ConfirmationAction confirmation(@NotNull Component component, @NotNull ClickAction confirmAction) {
        return new ConfirmationAction(component, confirmAction);
    }

    static SoundAction sound(@NotNull Sound sound) {
        return new SoundAction(sound);
    }

    static BackAction back() {
        return BACK;
    }

    static ClickTypeAction rightClick(@NotNull ClickAction rightClickAction) {
        return new ClickTypeAction(ClickType.RIGHT, rightClickAction);
    }

    static ClickTypeAction leftClick(@NotNull ClickAction rightClickAction) {
        return new ClickTypeAction(ClickType.LEFT, rightClickAction);
    }

    static ClickTypeAction clickType(@NotNull ClickType type, @NotNull ClickAction rightClickAction) {
        return new ClickTypeAction(type, rightClickAction);
    }

    static UserInputAction userInput(String title, Function<String, AnvilGUI.Response> inputFunction) {
        return new UserInputAction(title, inputFunction);
    }
}
