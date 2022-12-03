package io.github.townyadvanced.townymenus.gui.action;

import com.palmergames.adventure.text.Component;
import io.github.townyadvanced.townymenus.TownyMenus;
import io.github.townyadvanced.townymenus.gui.MenuInventory;
import io.github.townyadvanced.townymenus.gui.MenuItem;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.function.Function;

public class UserInputAction implements ClickAction {
    private final String title;
    private final Function<String, AnvilGUI.Response> inputFunction;

    public UserInputAction(String title, Function<String, AnvilGUI.Response> inputFunction) {
        this.title = title;
        this.inputFunction = inputFunction;
    }

    @Override
    public void onClick(MenuInventory inventory, InventoryClickEvent event) {
        new AnvilGUI.Builder()
                .title(title)
                .onClose(player -> Bukkit.getScheduler().runTask(TownyMenus.getPlugin(), () -> inventory.openSilent(player))) // Re-open previous inventory if closed
                .onComplete((player, input) -> inputFunction.apply(input))
                .itemLeft(MenuItem.builder(Material.PAPER).name(Component.empty()).build().itemStack())
                .plugin(TownyMenus.getPlugin())
                .open((Player) event.getWhoClicked());
    }
}
