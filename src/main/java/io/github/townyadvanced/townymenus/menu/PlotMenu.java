package io.github.townyadvanced.townymenus.menu;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.command.PlotCommand;
import com.palmergames.bukkit.towny.event.PlotPreChangeTypeEvent;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownBlockTypeHandler;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
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
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PlotMenu {
    public static MenuInventory createPlotMenu(@NotNull Player player) {
        WorldCoord worldCoord = WorldCoord.parseWorldCoord(player);
        TownBlock townBlock = TownyAPI.getInstance().getTownBlock(worldCoord);

        boolean isOwner = testPlotOwner(player, worldCoord);
        boolean isWilderness = townBlock == null;

        return MenuInventory.builder()
            .title(Component.text("Plot Menu"))
            .size(27)
            .addItem(MenuHelper.backButton().slot(SlotAnchor.of(VerticalAnchor.fromBottom(0), HorizontalAnchor.fromRight(0))).build())
            .addItem(MenuItem.builder(Material.NAME_TAG)
                    .name(Component.text("Plot Set", NamedTextColor.GREEN))
                    .slot(SlotAnchor.of(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(1)))
                    .lore(isWilderness ? MenuHelper.errorMessage("This menu cannot be opened while in the wilderness.") : Component.empty())
                    .action(isWilderness ? ClickAction.NONE : ClickAction.openInventory(() -> createPlotSetMenu(player, worldCoord, isOwner)))
                    .build())
                .build();
    }

    private static MenuInventory createPlotSetMenu(Player player, WorldCoord worldCoord, boolean isOwner) {
        return MenuInventory.builder()
                .rows(4)
                .title(Component.text("Plot Set Menu"))
                .addItem(MenuHelper.backButton().slot(SlotAnchor.of(VerticalAnchor.fromBottom(0), HorizontalAnchor.fromRight(0))).build())
                .addItem(MenuItem.builder(Material.NAME_TAG)
                        .slot(SlotAnchor.of(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(1)))
                        .name(Component.text("Set plot name", NamedTextColor.GREEN))
                        .lore(Component.text("Changes the name of the current plot.", NamedTextColor.GRAY))
                        .lore(() -> {
                            if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_PLOT_SET_NAME.getNode()))
                                return Component.text("You do not have permission to change this plot's name.", NamedTextColor.GRAY);
                            else if (!isOwner)
                                return Component.text("Only the owner of the plot can change it's name.", NamedTextColor.GRAY);
                            else
                                return Component.empty();
                        })
                        .action(!isOwner || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_PLOT_SET_NAME.getNode()) ? ClickAction.NONE : ClickAction.userInput("Input new plot name", newName -> {
                            TownBlock townBlock = TownyAPI.getInstance().getTownBlock(worldCoord);

                            if (townBlock == null || !testPlotOwner(player, worldCoord) || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_PLOT_SET_NAME.getNode())) {
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
                        .lore(!isOwner ? Component.text("Only the owner of the plot can change it's type.", NamedTextColor.GRAY) : Component.empty())
                        .action(!isOwner ? ClickAction.NONE : ClickAction.openInventory(() -> formatPlotSetType(player, worldCoord)))
                        .build())
                .build();
    }

    private static MenuInventory formatPlotSetType(Player player, WorldCoord worldCoord) {
        List<MenuItem> plotTypeItems = new ArrayList<>();

        TownBlock townBlock = TownyAPI.getInstance().getTownBlock(worldCoord);
        TownBlockType currentType = townBlock == null ? null : townBlock.getType();

        PlotGroup group = townBlock == null ? null : townBlock.getPlotObjectGroup();

        for (TownBlockType type : TownBlockTypeHandler.getTypes().values()) {
            if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_PLOT_SET.getNode(type.getName().toLowerCase(Locale.ROOT))))
                continue;

            boolean alreadySelected = type == currentType;
            double changeCost = group == null ? type.getCost() : type.getCost() * group.getTownBlocks().size();

            Runnable onClick = () -> {
                // Check if the player still has permissions to change the plot type.
                if (!testPlotOwner(player, worldCoord) || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_PLOT_SET.getNode(type.getName().toLowerCase(Locale.ROOT)))) {
                    player.sendMessage(Component.text("You do not have enough permissions to change the type for this plot.", NamedTextColor.RED));
                    MenuHistory.back(player);
                    return;
                }

                // Should never be null, testPlotOwner returns false if it is not claimed anymore.
                TownBlock townBlock1 = TownyAPI.getInstance().getTownBlock(worldCoord);
                if (townBlock1 == null)
                    return;

                PlotGroup plotGroup = townBlock1.getPlotObjectGroup();
                double cost = plotGroup == null ? type.getCost() : type.getCost() * plotGroup.getTownBlocks().size();
                Resident resident = TownyAPI.getInstance().getResident(player);

                if (cost > 0 && TownyEconomyHandler.isActive()) {
                    if (!resident.getAccount().canPayFromHoldings(cost)) {
                        TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_cannot_afford_plot_set_type_cost", type.getFormattedName(), TownyEconomyHandler.getFormattedBalance(cost)));
                        MenuHistory.reOpen(player, formatPlotSetType(player, worldCoord));
                        return;
                    }
                }

                if (plotGroup == null) {

                    if (TownBlockType.ARENA.equals(type) && TownySettings.getOutsidersPreventPVPToggle()) {
                        for (Player target : Bukkit.getOnlinePlayers()) {
                            if (!townBlock1.getTownOrNull().hasResident(target) && !player.getName().equals(target.getName()) && worldCoord.equals(WorldCoord.parseWorldCoord(target))) {
                                TownyMessaging.sendErrorMsg(player, Translatable.of("msg_cant_toggle_pvp_outsider_in_plot"));
                                MenuHistory.reOpen(player, formatPlotSetType(player, worldCoord));
                                return;
                            }
                        }
                    }

                    PlotPreChangeTypeEvent preEvent = new PlotPreChangeTypeEvent(type, townBlock1, resident);
                    Bukkit.getPluginManager().callEvent(preEvent);

                    if (preEvent.isCancelled()) {
                        TownyMessaging.sendErrorMsg(player, preEvent.getCancelMessage());
                        return;
                    }

                    try {
                        townBlock1.setType(type, resident);
                    } catch (TownyException e) {
                        TownyMessaging.sendErrorMsg(player, e.getMessage(player));
                        MenuHistory.reOpen(player, formatPlotSetType(player, worldCoord));
                        return;
                    }
                } else {
                    // Plot group logic
                    for (TownBlock groupBlock : plotGroup.getTownBlocks()) {
                        if (TownBlockType.ARENA.equals(type) && TownySettings.getOutsidersPreventPVPToggle()) {
                            for (Player target : Bukkit.getOnlinePlayers()) {
                                if (!townBlock1.getTownOrNull().hasResident(target) && !player.getName().equals(target.getName()) && groupBlock.getWorldCoord().equals(WorldCoord.parseWorldCoord(target))) {
                                    TownyMessaging.sendErrorMsg(player, Translatable.of("msg_cant_toggle_pvp_outsider_in_plot"));
                                    MenuHistory.reOpen(player, formatPlotSetType(player, worldCoord));
                                    return;
                                }
                            }
                        }

                        PlotPreChangeTypeEvent preEvent = new PlotPreChangeTypeEvent(type, groupBlock, resident);
                        Bukkit.getPluginManager().callEvent(preEvent);

                        if (preEvent.isCancelled()) {
                            TownyMessaging.sendErrorMsg(player, preEvent.getCancelMessage());
                            return;
                        }
                    }

                    TownBlockType oldGroupType = plotGroup.getTownBlockType();
                    List<TownBlock> alteredTownBlocks = new ArrayList<>();

                    for (TownBlock groupBlock : plotGroup.getTownBlocks()) {
                        try {
                            alteredTownBlocks.add(groupBlock);
                            groupBlock.setType(type, resident);
                        } catch (TownyException e) {
                            // If a towny exception happens, send the message and set the plots back to their old type.
                            TownyMessaging.sendErrorMsg(player, e.getMessage(player));

                            for (TownBlock alteredBlock : alteredTownBlocks) {
                                alteredBlock.setType(oldGroupType);
                                alteredBlock.save();
                            }

                            MenuHistory.reOpen(player, formatPlotSetType(player, worldCoord));
                            return;
                        }
                    }
                }

                if (cost > 0 && TownyEconomyHandler.isActive()) {
                    // We've already checked whether the player can pay above, so this should never fail.
                    // If we withdrew at the top of the method we'd need to refund the cost back if the type couldn't be set.
                    resident.getAccount().withdraw(cost, String.format("Plot set to %s", type.getFormattedName()));
                }

                TownyMessaging.sendMsg(player, Translatable.of(plotGroup == null ? "msg_plot_set_type" : "msg_set_group_type_to_x", type.getFormattedName()));

                MenuHistory.reOpen(player, formatPlotSetType(player, worldCoord));
            };

            plotTypeItems.add(MenuItem.builder(Material.GRASS_BLOCK)
                    .name(Component.text(type.getFormattedName(), NamedTextColor.GREEN))
                    .lore(alreadySelected ? Component.text("Currently selected!", NamedTextColor.GRAY) : Component.text("Click to change the plot type to " + type.getFormattedName() + ".", NamedTextColor.GRAY))
                    .lore(!alreadySelected && changeCost > 0 && TownyEconomyHandler.isActive() ? Component.text("Setting this type will cost " + TownyEconomyHandler.getFormattedBalance(changeCost) + ".", NamedTextColor.GRAY) : Component.empty())
                    .lore(!alreadySelected && TownyEconomyHandler.isActive() && townBlock.getTownOrNull() != null && type.getTax(townBlock.getTownOrNull()) > 0 ? Component.text("Tax for this type is " + TownyEconomyHandler.getFormattedBalance(type.getTax(townBlock.getTownOrNull())) + ".", NamedTextColor.GRAY) : Component.empty())
                    .withGlint(alreadySelected)
                    .action(alreadySelected ? ClickAction.NONE : changeCost > 0 && TownyEconomyHandler.isActive() // Add confirmation if cost > 0 and economy is active
                            ? ClickAction.confirmation(() -> Component.text("Changing the plot type will cost " + TownyEconomyHandler.getFormattedBalance(changeCost) + ", are you sure you want to continue?", NamedTextColor.RED), ClickAction.run(onClick))
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
