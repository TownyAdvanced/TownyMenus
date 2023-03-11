package io.github.townyadvanced.townymenus.utils;

import io.github.townyadvanced.townymenus.gui.MenuHistory;
import io.github.townyadvanced.townymenus.gui.MenuInventory;
import net.wesjd.anvilgui.AnvilGUI.ResponseAction;
import java.util.function.Supplier;

/**
 * Simple wrapper for AnvilGUI's {@link ResponseAction} class with some added TownyMenus specific things
 */
public class AnvilResponse {
    private static final ResponseAction NIL = (anvilGUI, player) -> {};

    public static ResponseAction reOpen(Supplier<MenuInventory> supplier) {
        return (anvilGUI, player) -> MenuHistory.reOpen(player, supplier.get());
    }

    public static ResponseAction text(String text) {
        return ResponseAction.replaceInputText(text);
    }

    public static ResponseAction nil() {
        return NIL;
    }

    public static ResponseAction close() {
        return ResponseAction.close();
    }
}
