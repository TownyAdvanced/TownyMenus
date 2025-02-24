package io.github.townyadvanced.townymenus.gui.action;

import io.github.townyadvanced.townymenus.TownyMenus;
import io.github.townyadvanced.townymenus.gui.MenuInventory;
import io.github.townyadvanced.townymenus.gui.input.response.InputResponse;
import io.github.townyadvanced.townymenus.gui.input.PlayerInput;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.function.Function;

public class UserInputAction implements ClickAction {
    private final String title;
    private final Function<PlayerInput, List<InputResponse>> inputFunction;

    public UserInputAction(String title, Function<PlayerInput, List<InputResponse>> inputFunction) {
        this.title = title;
        this.inputFunction = inputFunction;
    }

    @Override
    public void onClick(MenuInventory inventory, InventoryClickEvent event) {
		TownyMenus.getPlugin().getUserInputBackend().startAwaitingInput((Player) event.getWhoClicked(), inventory, Component.text(title), inputFunction);
    }
}
