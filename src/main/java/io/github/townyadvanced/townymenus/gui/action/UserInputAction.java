package io.github.townyadvanced.townymenus.gui.action;

import com.palmergames.adventure.text.Component;
import io.github.townyadvanced.townymenus.TownyMenus;
import io.github.townyadvanced.townymenus.gui.MenuInventory;
import io.github.townyadvanced.townymenus.gui.MenuItem;
import net.wesjd.anvilgui.AnvilGUI;
import net.wesjd.anvilgui.AnvilGUI.ResponseAction;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class UserInputAction implements ClickAction {
    private final String title;
    private final Function<AnvilGUI.StateSnapshot, List<ResponseAction>> inputFunction;

    public UserInputAction(String title, Function<AnvilGUI.StateSnapshot, List<ResponseAction>> inputFunction) {
        this.title = title;
        this.inputFunction = inputFunction;
    }

    @Override
    public void onClick(MenuInventory inventory, InventoryClickEvent event) {
        new AnvilGUI.Builder()
                .title(title)
                .onClose(snapshot -> TownyMenus.getPlugin().getScheduler().run(snapshot.getPlayer(), () -> inventory.openSilent(snapshot.getPlayer()))) // Re-open previous inventory if closed
                .onClick((slot, snapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT)
                        return Collections.emptyList();

                    return inputFunction.apply(snapshot);
                })
                .itemLeft(MenuItem.builder(Material.PAPER).name(Component.empty()).build().itemStack())
                .plugin(TownyMenus.getPlugin())
                .open((Player) event.getWhoClicked());
    }
}
