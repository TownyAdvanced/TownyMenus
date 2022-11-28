package io.github.townyadvanced.townymenus.gui;

import com.palmergames.adventure.text.Component;
import com.palmergames.adventure.text.format.NamedTextColor;
import com.palmergames.adventure.text.format.TextDecoration;
import io.github.townyadvanced.townymenus.gui.action.ClickAction;
import io.github.townyadvanced.townymenus.gui.anchor.HorizontalAnchor;
import io.github.townyadvanced.townymenus.gui.anchor.SlotAnchor;
import io.github.townyadvanced.townymenus.gui.anchor.VerticalAnchor;
import org.bukkit.Material;

public class MenuHelper {
    /**
     * @return A new back button item builder, without a specific slot.
     */
    public static MenuItem.Builder backButton() {
        return MenuItem.builder(Material.BARRIER)
                .name(Component.text("Back", NamedTextColor.GREEN))
                .lore(Component.text("Click to go back to the previous screen.", NamedTextColor.GRAY))
                .slot(SlotAnchor.bottomRight())
                .action(ClickAction.back());
    }

    public static int normalizeSize(int size) {
        return (int) Math.min(Math.ceil(size / 9d) * 9, 54);
    }

    public static Component errorMessage(String message) {
        return Component.text("✖ " + message, NamedTextColor.RED);
    }

    public static MenuInventory createConfirmation(Component warningMessage, ClickAction confirmAction, ClickAction cancelAction) {
        return MenuInventory.builder()
                .size(27)
                .title(Component.text("Do you you want to continue?"))
                .addItem(MenuItem.builder(Material.GREEN_STAINED_GLASS)
                        .name(Component.text("Confirm", NamedTextColor.DARK_GREEN, TextDecoration.BOLD))
                        .lore(warningMessage)
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(2)))
                        .action(confirmAction)
                        .build())
                .addItem(MenuItem.builder(Material.RED_STAINED_GLASS)
                        .name(Component.text("Cancel", NamedTextColor.DARK_RED, TextDecoration.BOLD))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromRight(2)))
                        .action(cancelAction)
                        .build())
                .build();
    }
}
