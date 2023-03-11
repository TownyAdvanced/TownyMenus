package io.github.townyadvanced.townymenus.gui.action;

import com.palmergames.adventure.text.Component;
import io.github.townyadvanced.townymenus.TownyMenus;
import io.github.townyadvanced.townymenus.gui.MenuInventory;
import io.github.townyadvanced.townymenus.gui.MenuItem;
import net.wesjd.anvilgui.AnvilGUI;
import net.wesjd.anvilgui.AnvilGUI.ResponseAction;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.function.Function;

public class UserInputAction implements ClickAction {
    private final String title;
    private final Function<AnvilGUI.Completion, List<ResponseAction>> inputFunction;

    public UserInputAction(String title, Function<AnvilGUI.Completion, List<ResponseAction>> inputFunction) {
        this.title = title;
        this.inputFunction = inputFunction;
    }

    @Override
    public void onClick(MenuInventory inventory, InventoryClickEvent event) {
        new AnvilGUI.Builder()
                .title(title)
                .onClose(player -> Bukkit.getScheduler().runTask(TownyMenus.getPlugin(), () -> inventory.openSilent(player))) // Re-open previous inventory if closed
                .onComplete(inputFunction)
                .itemLeft(MenuItem.builder(Material.PAPER).name(Component.empty()).build().itemStack())
                .plugin(TownyMenus.getPlugin())
                .open((Player) event.getWhoClicked());
    }
}
