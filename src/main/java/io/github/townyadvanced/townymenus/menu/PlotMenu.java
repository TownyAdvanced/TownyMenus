package io.github.townyadvanced.townymenus.menu;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.command.PlotCommand;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownBlockTypeHandler;
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
import io.github.townyadvanced.townymenus.utils.MenuScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
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

                            if (!testPlotOwner(player, worldCoord)) {
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
                .addItem(MenuItem.builder(Material.GRASS_BLOCK)
                        .slot(SlotAnchor.of(VerticalAnchor.fromTop(2), HorizontalAnchor.fromLeft(1)))
                        .name(Component.text("Set plot type", NamedTextColor.GREEN))
                        .lore(Component.text("Changes the type of the current plot.", NamedTextColor.GRAY))
                        .action(ClickAction.openInventory(() -> formatPlotSetType(player, worldCoord)))
                        .build())
                .build();
    }

    private static MenuInventory formatPlotSetType(Player player, WorldCoord worldCoord) {
        List<MenuItem> plotTypeItems = new ArrayList<>();

        TownBlock townBlock = TownyAPI.getInstance().getTownBlock(worldCoord);
        TownBlockType currentType = townBlock == null ? null : townBlock.getType();

        for (TownBlockType type : TownBlockTypeHandler.getTypes().values()) {
            boolean alreadySelected = type == currentType;

            Runnable onClick = () -> {
                // Check if the player still has permissions to change the plot type.
                if (!testPlotOwner(player, worldCoord)) {
                    player.sendMessage(Component.text("You do not have enough permissions to change the type for this plot.", NamedTextColor.RED));
                    MenuHistory.back(player);
                    return;
                }

                if (type.getCost() > 0 && TownyEconomyHandler.isActive()) {
                    Resident resident = TownyAPI.getInstance().getResident(player);
                    if (!resident.getAccount().withdraw(type.getCost(), String.format("Plot set to %s", type.getFormattedName()))) {
                        TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_cannot_afford_plot_set_type_cost", type.getFormattedName(), TownyEconomyHandler.getFormattedBalance(type.getCost())));
                        MenuHistory.back(player);
                        return;
                    }
                }

                TownyAPI.getInstance().getTownBlock(worldCoord).setType(type);
                TownyMessaging.sendMsg(player, Translatable.of("msg_plot_set_type", type.getFormattedName()));

                MenuScheduler.scheduleAsync(player.getUniqueId(), () -> {
                    MenuHistory.pop(player.getUniqueId());
                    formatPlotSetType(player, worldCoord).open(player);
                });
            };

            plotTypeItems.add(MenuItem.builder(Material.GRASS_BLOCK)
                    .name(Component.text(type.getFormattedName(), NamedTextColor.GREEN))
                    .lore(alreadySelected ? Component.text("Currently selected!", NamedTextColor.GRAY) : Component.text("Click to change the plot type to " + type.getFormattedName() + ".", NamedTextColor.GRAY))
                    .lore(!alreadySelected && type.getCost() > 0 && TownyEconomyHandler.isActive() ? Component.text("Setting this type will cost " + TownyEconomyHandler.getFormattedBalance(type.getCost()) + ".", NamedTextColor.GRAY) : Component.empty())
                    .lore(!alreadySelected && TownyEconomyHandler.isActive() && townBlock.getTownOrNull() != null && type.getTax(townBlock.getTownOrNull()) > 0 ? Component.text("Tax for this type is " + TownyEconomyHandler.getFormattedBalance(type.getTax(townBlock.getTownOrNull())) + ".", NamedTextColor.GRAY) : Component.empty())
                    .withGlint(alreadySelected)
                    .action(alreadySelected ? ClickAction.NONE : type.getCost() > 0 && TownyEconomyHandler.isActive() // Add confirmation if cost > 0 and economy is active
                            ? ClickAction.confirmation(() -> Component.text("Changing the plot type will cost " + TownyEconomyHandler.getFormattedBalance(type.getCost()) + ", are you sure you want to continue?", NamedTextColor.RED), ClickAction.run(onClick))
                            : ClickAction.run(onClick))
                    .build());
        }

        return MenuInventory.paginator().addItems(plotTypeItems).title(Component.text("Select plot type")).build();
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
