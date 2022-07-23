package io.github.townyadvanced.townymenus.gui;

import io.github.townyadvanced.townymenus.gui.action.ClickAction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

public class MenuHelper {
    /**
     * @return A new back button item builder, without a specific slot.
     */
    public static MenuItem.Builder backButton() {
        return MenuItem.builder(Material.BARRIER)
                .name(Component.text("Back", NamedTextColor.GREEN))
                .lore(Component.text("Click to go back to the previous screen.", NamedTextColor.GRAY))
                .action(ClickAction.back());
    }

    public static int normalizeSize(int size) {
        return (int) Math.min(Math.ceil(size / 9d) * 9, 54);
    }

    public static Component errorMessage(String message) {
        return Component.text("✖ " + message, NamedTextColor.RED);
    }
}
