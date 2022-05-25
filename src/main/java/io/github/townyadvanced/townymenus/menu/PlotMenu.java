package io.github.townyadvanced.townymenus.menu;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.command.PlotCommand;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.util.NameValidation;
import io.github.townyadvanced.townymenus.gui.MenuHelper;
import io.github.townyadvanced.townymenus.gui.MenuHistory;
import io.github.townyadvanced.townymenus.gui.MenuInventory;
import io.github.townyadvanced.townymenus.gui.MenuItem;
import io.github.townyadvanced.townymenus.gui.action.ClickAction;
import io.github.townyadvanced.townymenus.gui.anchor.HorizontalAnchor;
import io.github.townyadvanced.townymenus.gui.anchor.SlotAnchor;
import io.github.townyadvanced.townymenus.gui.anchor.VerticalAnchor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class PlotMenu {
    public static Supplier<MenuInventory> createPlotMenu(@NotNull Player player) {
        return () -> {
            WorldCoord worldCoord = WorldCoord.parseWorldCoord(player);

            boolean isOwner = testPlotOwner(player, worldCoord);

            return MenuInventory.builder()
                    .title(Component.text("Plot Menu"))
                    .size(54)
                    .addItem(MenuHelper.backButton().slot(SlotAnchor.of(VerticalAnchor.fromBottom(0), HorizontalAnchor.fromRight(0))).build())
                    .addItem(MenuItem.builder(Material.NAME_TAG)
                            .name(Component.text("Plot Set", NamedTextColor.GREEN))
                            .slot(SlotAnchor.of(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(1)))
                            .action(isOwner ? ClickAction.openInventory(createPlotSetMenu(player, worldCoord)) : ClickAction.NONE)
                            .build())
                    .build();
        };
    }

    private static Supplier<MenuInventory> createPlotSetMenu(Player player, WorldCoord worldCoord) {
        return () -> MenuInventory.builder()
                .rows(4)
                .title(Component.text("Plot Set Menu"))
                .addItem(MenuHelper.backButton().slot(SlotAnchor.of(VerticalAnchor.fromBottom(0), HorizontalAnchor.fromRight(0))).build())
                .addItem(MenuItem.builder(Material.NAME_TAG)
                        .slot(SlotAnchor.of(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(1)))
                        .name(Component.text("Set plot name", NamedTextColor.GREEN))
                        .lore(Component.text("Changes the name of the current plot.", NamedTextColor.GRAY))
                        .action(ClickAction.userInput("Input new plot name", newName -> {
                            TownBlock townBlock = TownyAPI.getInstance().getTownBlock(worldCoord);

                            if (!testPlotOwner(player, worldCoord) || townBlock == null) {
                                MenuHistory.last(player);
                                return AnvilGUI.Response.close();
                            }

                            if (!NameValidation.isValidName(newName)) {
                                TownyMessaging.sendErrorMsg(player, Translatable.of("msg_invalid_name"));
                                MenuHistory.last(player);
                                return AnvilGUI.Response.close();
                            }

                            townBlock.setName(newName);
                            townBlock.save();

                            TownyMessaging.sendMsg(player, Translatable.of("msg_plot_name_set_to", townBlock.getName()));
                            MenuHistory.last(player);
                            return AnvilGUI.Response.close();
                        }))
                        .build())
                .build();
    }

    private static boolean testPlotOwner(Player player, WorldCoord worldCoord) {
        Resident resident = TownyAPI.getInstance().getResident(player);
        TownBlock townBlock = TownyAPI.getInstance().getTownBlock(worldCoord);

        if (resident == null || townBlock == null)
            return false;

        try {
            PlotCommand.plotTestOwner(resident, townBlock);
            return true;
        } catch (TownyException e) {
            return false;
        }
    }
}
