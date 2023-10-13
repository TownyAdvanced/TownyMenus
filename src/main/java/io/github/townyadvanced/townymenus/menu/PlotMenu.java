package io.github.townyadvanced.townymenus.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.command.PlotCommand;
import com.palmergames.bukkit.towny.event.PlotPreChangeTypeEvent;
import com.palmergames.bukkit.towny.event.plot.PlotTrustAddEvent;
import com.palmergames.bukkit.towny.event.plot.PlotTrustRemoveEvent;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.PermissionData;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownBlockTypeHandler;
import com.palmergames.bukkit.towny.object.TownyPermission.ActionType;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.utils.PermissionGUIUtil;
import com.palmergames.bukkit.towny.utils.PermissionGUIUtil.SetPermissionType;
import com.palmergames.bukkit.util.NameValidation;
import io.github.townyadvanced.townymenus.gui.MenuHelper;
import io.github.townyadvanced.townymenus.gui.MenuHistory;
import io.github.townyadvanced.townymenus.gui.MenuInventory;
import io.github.townyadvanced.townymenus.gui.MenuItem;
import io.github.townyadvanced.townymenus.gui.action.ClickAction;
import io.github.townyadvanced.townymenus.gui.slot.anchor.HorizontalAnchor;
import io.github.townyadvanced.townymenus.gui.slot.anchor.SlotAnchor;
import io.github.townyadvanced.townymenus.gui.slot.anchor.VerticalAnchor;
import io.github.townyadvanced.townymenus.listeners.AwaitingConfirmation;
import io.github.townyadvanced.townymenus.utils.AnvilResponse;
import io.github.townyadvanced.townymenus.utils.Localization;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public class PlotMenu {
    public static MenuInventory createPlotMenu(@NotNull Player player) {
        return createPlotMenu(player, WorldCoord.parseWorldCoord(player));
    }

    public static MenuInventory createPlotMenu(@NotNull Player player, WorldCoord worldCoord) {
        TownBlock townBlock = TownyAPI.getInstance().getTownBlock(worldCoord);
        final Locale locale = Localization.localeOrDefault(player);

        boolean isOwner = testPlotOwner(player, worldCoord);
        boolean isWilderness = townBlock == null;

        return MenuInventory.builder()
                .title(translatable("plot-menu-title"))
                .rows(4)
                .addItem(MenuHelper.backButton().build())
                .addItem(MenuItem.builder(Material.NAME_TAG)
                        .name(translatable("plot-menu-plot-set"))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(1)))
                        .lore(isWilderness ? translatable("plot-menu-cannot-open-in-wilderness") : Component.empty())
                        .action(isWilderness ? ClickAction.NONE : ClickAction.openInventory(() -> createPlotSetMenu(player, worldCoord, isOwner)))
                        .build())
                .addItem(MenuItem.builder(Material.EMERALD_BLOCK)
                        .name(translatable("plot-menu-sell-plot"))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(3)))
                        .lore(() -> {
                            if (isWilderness)
                                return translatable("plot-menu-cannot-sell-wild-plot");
                            else if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_PLOT_FORSALE.getNode()))
                                return translatable("msg-no-permission-to").append(translatable("plot-menu-put-for-sale")).color(GRAY);
                            else if (!isOwner)
                                return translatable("plot-menu-sell-not-owner");
                            else return Component.empty();
                        })
                        .action(isOwner && player.hasPermission(townBlock != null && townBlock.hasPlotObjectGroup() ? PermissionNodes.TOWNY_COMMAND_PLOT_GROUP_FORSALE.getNode() : PermissionNodes.TOWNY_COMMAND_PLOT_FORSALE.getNode())
                                ? putForSaleOrOpenForSaleMenu(player, worldCoord)
                                : ClickAction.NONE)
                        .build())
                .addItem(MenuItem.builder(Material.REDSTONE_BLOCK)
                        .name(translatable("plot-menu-sell-plot"))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(2), HorizontalAnchor.fromLeft(3)))
                        .lore(() -> {
                            if (isWilderness)
                                return translatable("plot-menu-cannot-sell-wild-plot");
                            else if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_PLOT_FORSALE.getNode()))
                                return translatable("msg-no-permission-to").append(translatable("plot-menu-put-for-sale")).color(GRAY);
                            else if (!isOwner)
                                return translatable("plot-menu-sell-not-owner");
                            else return Component.empty();
                        })
                        .action(isOwner && player.hasPermission(townBlock != null && townBlock.hasPlotObjectGroup() ? PermissionNodes.TOWNY_COMMAND_PLOT_GROUP_NOTFORSALE.getNode() : PermissionNodes.TOWNY_COMMAND_PLOT_NOTFORSALE.getNode())
                                ? ClickAction.run(() -> putNotForSale(player, worldCoord))
                                : ClickAction.NONE)
                        .build())
                .addItem(MenuItem.builder(Material.OAK_SIGN)
                        .name(translatable("plot-menu-trusted-players"))
                        .lore(translatable("msg-click-to").append(translatable("plot-menu-trusted-players-subtitle")).color(GRAY))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(2), HorizontalAnchor.fromLeft(1)))
                        .action(ClickAction.openInventory(() -> formatPlotTrustMenu(player, worldCoord)))
                        .build())
                .addItem(MenuItem.builder(Material.GRASS_BLOCK)
                        .name(translatable("plot-menu-claim-plot-title"))
                        .lore(() -> {
                            if (townBlock == null)
                                return translatable("plot-menu-only-claim-town-plots");
                            else if (!townBlock.isForSale())
                                return translatable("plot-menu-only-claim-for-sale-plots");
                            else
                                return Arrays.asList(translatable("msg-click-to").append(translatable("plot-menu-claim-plot")), !TownyEconomyHandler.isActive() ? Component.empty() :
                                        translatable("plot-menu-claim-plot-will-cost").append(text(TownyEconomyHandler.getFormattedBalance(townBlock.getPlotPrice()))).append(text(".")).color(GRAY));
                        })
                        .action(ClickAction.run(() -> {
                            plotCommand().ifPresent(plotCommand -> {
                                AwaitingConfirmation.await(player);

                                try {
                                    plotCommand.parsePlotCommand(player, new String[]{"claim"});
                                } catch (Exception e) {
                                    if (e instanceof TownyException tex)
                                        TownyMessaging.sendErrorMsg(player, tex.getMessage(player));
                                }
                            });
                        }))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(5)))
                        .build())
                .addItem(MenuItem.builder(Material.LEVER)
                        .name(translatable("plot-menu-plot-toggle-title"))
                        .lore(translatable("msg-click-to").append(translatable("plot-menu-plot-toggle-subtitle")).color(GRAY))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(2), HorizontalAnchor.fromLeft(5)))
                        .action(ClickAction.openInventory(() -> formatPlotToggle(player, worldCoord)))
                        .build())
                .addItem(MenuItem.builder(Material.GRAY_WOOL)
                        .name(translatable("plot-menu-plot-permission-overrides-title"))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromRight(1)))
                        .lore(translatable("msg-click-to").append(translatable("plot-menu-plot-permission-overrides-subtitle")).color(GRAY))
                        .action(ClickAction.openInventory(() -> formatPlotPermissionOverrideMenu(player, worldCoord)))
                        .build())
                .build();
    }

    private static MenuInventory createPlotSetMenu(Player player, WorldCoord worldCoord, boolean isOwner) {
        final Locale locale = Localization.localeOrDefault(player);

        return MenuInventory.builder()
                .rows(4)
                .title(translatable("plot-menu-plot-set-title"))
                .addItem(MenuHelper.backButton().build())
                .addItem(MenuItem.builder(Material.NAME_TAG)
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(1)))
                        .name(translatable("plot-menu-plot-set-name-title"))
                        .lore(translatable("plot-menu-plot-set-name-subtitle"))
                        .lore(() -> {
                            if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_PLOT_SET_NAME.getNode()))
                                return translatable("msg-no-permission-to").append(translatable("plot-menu-plot-set-name")).color(GRAY);
                            else if (!isOwner)
                                return translatable("plot-menu-only-owner-can-set-name");
                            else return Component.empty();
                        })
                        .action(!isOwner || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_PLOT_SET_NAME.getNode()) ? ClickAction.NONE : ClickAction.userInput("Input new plot name", completion -> {
                            TownBlock townBlock = TownyAPI.getInstance().getTownBlock(worldCoord);

                            if (townBlock == null || !testPlotOwner(player, worldCoord) || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_PLOT_SET_NAME.getNode())) {
                                MenuHistory.last(player);
                                return AnvilResponse.close();
                            }

                            String newName = completion.getText().replaceAll(" ", "_");

                            if (NameValidation.isBlacklistName(newName)) {
                                TownyMessaging.sendErrorMsg(player, Translatable.of("msg_invalid_name"));
                                MenuHistory.last(player);
                                return AnvilResponse.close();
                            }

                            townBlock.setName(newName);
                            townBlock.save();

                            TownyMessaging.sendMsg(player, Translatable.of("msg_plot_name_set_to", townBlock.getName()));
                            MenuHistory.last(player);
                            return AnvilResponse.close();
                        }))
                        .build())
                .addItem(MenuItem.builder(Material.REDSTONE_BLOCK)
                        .name(translatable("plot-menu-clear-plot-name-title"))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(3)))
                        .lore(translatable("plot-menu-clear-plot-name-subtitle"))
                        .lore(() -> {
                            if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_PLOT_SET_NAME.getNode()))
                                return translatable("msg-no-permission-to").append(translatable("plot-menu-clear-plot-name")).color(GRAY);
                            else if (!isOwner)
                                return translatable("plot-menu-only-owner-clear-plot-name");
                            else return Component.empty();
                        })
                        .action(!isOwner || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_PLOT_SET_NAME.getNode()) ? ClickAction.NONE : ClickAction.confirmation(() -> text("Click to confirm removing the plot's name.", GRAY), ClickAction.run(() -> {
                            TownBlock townBlock = TownyAPI.getInstance().getTownBlock(worldCoord);

                            if (townBlock == null || !testPlotOwner(player, worldCoord) || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_PLOT_SET_NAME.getNode())) {
                                MenuHistory.last(player);
                                return;
                            }

                            townBlock.setName("");
                            townBlock.save();
                            TownyMessaging.sendMsg(player, Translatable.of("msg_plot_name_removed"));
                            MenuHistory.reOpen(player, () -> createPlotSetMenu(player, worldCoord, true));
                        })))
                        .build())
                .addItem(MenuItem.builder(Material.GRASS_BLOCK)
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(2), HorizontalAnchor.fromLeft(1)))
                        .name(translatable("plot-menu-set-plot-type-title"))
                        .lore(translatable("plot-menu-set-plot-type-subtitle"))
                        .lore(!isOwner ? text("plot-menu-only-owner-can-set-plot-type") : Component.empty())
                        .action(!isOwner ? ClickAction.NONE : ClickAction.openInventory(() -> formatPlotSetType(player, worldCoord)))
                        .build())
                .build();
    }

    private static MenuInventory formatPlotSetType(Player player, WorldCoord worldCoord) {
        final Locale locale = Localization.localeOrDefault(player);
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
                    TownyMessaging.sendErrorMsg(player, Translatable.of("msg-no-permission-to").append(Translatable.of("plot-menu-change-plot-type")));
                    MenuHistory.back(player);
                    return;
                }

                // Should never be null, testPlotOwner returns false if it is not claimed anymore.
                TownBlock townBlock1 = TownyAPI.getInstance().getTownBlock(worldCoord);
                if (townBlock1 == null || townBlock1.getTypeName().equalsIgnoreCase(type.getName()))
                    return;

                PlotGroup plotGroup = townBlock1.getPlotObjectGroup();
                double cost = plotGroup == null ? type.getCost() : type.getCost() * plotGroup.getTownBlocks().size();
                Resident resident = TownyAPI.getInstance().getResident(player);

                if (cost > 0 && TownyEconomyHandler.isActive()) {
                    if (!resident.getAccount().canPayFromHoldings(cost)) {
                        TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_cannot_afford_plot_set_type_cost", type.getFormattedName(), TownyEconomyHandler.getFormattedBalance(cost)));
                        MenuHistory.reOpen(player, () -> formatPlotSetType(player, worldCoord));
                        return;
                    }
                }

                if (plotGroup == null) {
                    if (TownBlockType.ARENA.equals(type) && TownySettings.getOutsidersPreventPVPToggle()) {
                        for (Player target : Bukkit.getOnlinePlayers()) {
                            if (!townBlock1.getTownOrNull().hasResident(target) && !player.getName().equals(target.getName()) && worldCoord.equals(WorldCoord.parseWorldCoord(target))) {
                                TownyMessaging.sendErrorMsg(player, Translatable.of("msg_cant_toggle_pvp_outsider_in_plot"));
                                MenuHistory.reOpen(player, () -> formatPlotSetType(player, worldCoord));
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
                        MenuHistory.reOpen(player, () -> formatPlotSetType(player, worldCoord));
                        return;
                    }
                } else {
                    // Plot group logic
                    for (TownBlock groupBlock : plotGroup.getTownBlocks()) {
                        if (TownBlockType.ARENA.equals(type) && TownySettings.getOutsidersPreventPVPToggle()) {
                            for (Player target : Bukkit.getOnlinePlayers()) {
                                if (!townBlock1.getTownOrNull().hasResident(target) && !player.getName().equals(target.getName()) && groupBlock.getWorldCoord().equals(WorldCoord.parseWorldCoord(target))) {
                                    TownyMessaging.sendErrorMsg(player, Translatable.of("msg_cant_toggle_pvp_outsider_in_plot"));
                                    MenuHistory.reOpen(player, () -> formatPlotSetType(player, worldCoord));
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

                            MenuHistory.reOpen(player, () -> formatPlotSetType(player, worldCoord));
                            return;
                        }
                    }
                }

                if (cost > 0 && TownyEconomyHandler.isActive()) {
                    // We've already checked whether the player can pay above, so this should never fail.
                    // If we withdrew at the top of the method we'd need to refund the cost back if the type couldn't be set.
                    resident.getAccount().withdraw(cost, String.format("Plot set to %s", type.getFormattedName()));
                    TownyMessaging.sendMsg(player, Translatable.of("msg_plot_set_cost", cost, type.getFormattedName()));
                }

                TownyMessaging.sendMsg(player, Translatable.of(plotGroup == null ? "msg_plot_set_type" : "msg_set_group_type_to_x", type.getFormattedName()));

                MenuHistory.reOpen(player, () -> formatPlotSetType(player, worldCoord));
            };

            plotTypeItems.add(MenuItem.builder(Material.GRASS_BLOCK)
                    .name(text(type.getFormattedName(), GREEN))
                    .lore(alreadySelected ? translatable("plot-menu-plot-currently-selected") : translatable("msg-click-to").append(translatable("plot-menu-change-plot-type-to")).append(text(type.getFormattedName() + ".", GRAY)))
                    .lore(!alreadySelected && changeCost > 0 && TownyEconomyHandler.isActive() ? translatable("plot-menu-setting-type-cost").append(text(TownyEconomyHandler.getFormattedBalance(changeCost) + ".", GRAY)) : Component.empty())
                    .lore(!alreadySelected && TownyEconomyHandler.isActive() && townBlock.getTownOrNull() != null && type.getTax(townBlock.getTownOrNull()) > 0 ? translatable("plot-menu-plot-type-tax", TownyEconomyHandler.getFormattedBalance(type.getTax(townBlock.getTownOrNull()))) : Component.empty())
                    .withGlint(alreadySelected)
                    .action(alreadySelected ? ClickAction.NONE : changeCost > 0 && TownyEconomyHandler.isActive() // Add confirmation if cost > 0 and economy is active
                            ? ClickAction.confirmation(() -> translatable("plot-menu-change-type-cost", RED).append(text(TownyEconomyHandler.getFormattedBalance(changeCost))).append(translatable("plot-menu-change-type-confirm")), ClickAction.run(onClick))
                            : ClickAction.run(onClick))
                    .build());
        }

        return MenuInventory.paginator().addItems(plotTypeItems).title(translatable("plot-menu-select-plot-type")).build();
    }

    private static ClickAction putForSaleOrOpenForSaleMenu(Player player, WorldCoord worldCoord) {
        final Locale locale = Localization.localeOrDefault(player);

        ClickAction putForSale = ClickAction.run(() -> {
            TownBlock townBlock = TownyAPI.getInstance().getTownBlock(worldCoord);

            PermissionNodes node = townBlock != null && townBlock.hasPlotObjectGroup() ? PermissionNodes.TOWNY_COMMAND_PLOT_GROUP_FORSALE : PermissionNodes.TOWNY_COMMAND_PLOT_FORSALE;

            if (townBlock == null || !player.hasPermission(node.getNode()) || !testPlotOwner(player, worldCoord)) {
                if (TownyEconomyHandler.isActive())
                    MenuHistory.back(player);

                return;
            }

            putTownBlockForSale(player, townBlock, 0);

            if (TownyEconomyHandler.isActive())
                MenuHistory.back(player);
        });

        // If the economy handler isn't active, put the plot for sale straight away.
        // If it's active, allow the player to either pick between their own price or for free.
        if (!TownyEconomyHandler.isActive())
            return putForSale;
        else
            return ClickAction.openInventory(() -> MenuInventory.builder()
                    .rows(3)
                    .title(translatable("plot-menu-plot-sell-price-title"))
                    .addItem(MenuItem.builder(Material.PAPER)
                            .name(translatable("plot-menu-plot-sell-custom-amount"))
                            .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(2)))
                            .lore(translatable("msg-click-to").append(translatable("plot-menu-plot-sell-enter-new-amount")))
                            .action(ClickAction.userInput(Translatable.of("plot-menu-plot-sell-enter-plot-price").translate(locale), completion -> {
                                TownBlock townBlock = TownyAPI.getInstance().getTownBlock(worldCoord);

                                if (townBlock == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_PLOT_FORSALE.getNode()) || !testPlotOwner(player, townBlock)) {
                                    MenuHistory.reOpen(player, () -> createPlotMenu(player, worldCoord));
                                    return AnvilResponse.close();
                                }

                                double plotPrice;
                                try {
                                    plotPrice = Double.parseDouble(completion.getText());
                                } catch (NumberFormatException e) {
                                    return AnvilResponse.text(completion.getText() + Translatable.of("plot-menu-plot-sell-invalid-price").translate(locale));
                                }

                                if (plotPrice < 0)
                                    return AnvilResponse.text(completion.getText() + Translatable.of("plot-menu-plot-sell-invalid-price").translate(locale));

                                putTownBlockForSale(player, townBlock, plotPrice);

                                MenuHistory.reOpen(player, () -> createPlotMenu(player, worldCoord));
                                return AnvilResponse.close();
                            }))
                            .build())
                    .addItem(MenuItem.builder(Material.EMERALD)
                            .name(translatable("plot-menu-plot-sell-free"))
                            .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromRight(2)))
                            .lore(translatable("msg-click-to").append(translatable("plot-menu-plot-sell-for-free")))
                            .action(putForSale)
                            .build())
                    .addItem(MenuHelper.backButton().build())
                    .build());
    }

    private static void putTownBlockForSale(Player player, TownBlock townBlock, double price) {
        PlotGroup group = townBlock.getPlotObjectGroup();
        Resident resident = TownyAPI.getInstance().getResident(player);

        if (group == null) {
            townBlock.setPlotPrice(Math.min(price, TownySettings.getMaxPlotPrice()));
            townBlock.save();

            TownyMessaging.sendPrefixedTownMessage(townBlock.getTownOrNull(), Translatable.of("msg_plot_for_sale", player.getName(), townBlock.getWorldCoord().toString()));

            if (resident == null || !resident.hasTown() || townBlock.getTownOrNull() != resident.getTownOrNull())
                TownyMessaging.sendMsg(player, Translatable.of("msg_plot_for_sale", player.getName(), townBlock.getWorldCoord().toString()));
        } else {
            group.setPrice(Math.min(price, TownySettings.getMaxPlotPrice()));
            group.save();

            Translatable message = Translatable.of("msg_player_put_group_up_for_sale", player.getName(), group.getName(), TownyEconomyHandler.getFormattedBalance(group.getPrice()));
            TownyMessaging.sendPrefixedTownMessage(townBlock.getTownOrNull(), message);

            if (resident == null || !resident.hasTown() || resident.getTownOrNull() != townBlock.getTownOrNull())
                TownyMessaging.sendMsg(player, message);
        }
    }

    private static void putNotForSale(Player player, WorldCoord worldCoord) {
        TownBlock townBlock = TownyAPI.getInstance().getTownBlock(worldCoord);

        PermissionNodes node = townBlock != null && townBlock.hasPlotObjectGroup() ? PermissionNodes.TOWNY_COMMAND_PLOT_GROUP_NOTFORSALE : PermissionNodes.TOWNY_COMMAND_PLOT_GROUP_FORSALE;

        if (townBlock == null || !player.hasPermission(node.getNode()) || !testPlotOwner(player, townBlock))
            return;

        PlotGroup group = townBlock.getPlotObjectGroup();

        if (group == null) {
            if (townBlock.getPlotPrice() == -1)
                return;

            townBlock.setPlotPrice(-1);
            townBlock.save();

            TownyMessaging.sendMsg(player, Translatable.of("msg_plot_set_to_nfs"));
        } else {
            if (group.getPrice() == -1)
                return;

            group.setPrice(-1);
            group.save();

            TownyMessaging.sendPrefixedTownMessage(townBlock.getTownOrNull(), Translatable.of("msg_player_made_group_not_for_sale", player.getName(), group.getName()));

            Resident resident = TownyAPI.getInstance().getResident(player);
            if (resident == null || !resident.hasTown() || resident.getTownOrNull() != townBlock.getTownOrNull())
                TownyMessaging.sendMsg(player, Translatable.of("msg_player_made_group_not_for_sale", player.getName(), group.getName()));
        }
    }

    private static MenuInventory formatPlotTrustMenu(Player player, WorldCoord worldCoord) {
        final Locale locale = Localization.localeOrDefault(player);

        MenuInventory.PaginatorBuilder builder = MenuInventory.paginator().title(translatable("plot-menu-trust-menu-title"));
        final TownBlock townBlock = TownyAPI.getInstance().getTownBlock(worldCoord);

        if (townBlock == null)
            return builder.build();

        PlotGroup plotGroup = townBlock.getPlotObjectGroup();
        boolean canAddRemove = player.hasPermission(plotGroup == null ? PermissionNodes.TOWNY_COMMAND_PLOT_TRUST.getNode() : PermissionNodes.TOWNY_COMMAND_PLOT_GROUP_TRUST.getNode()) && testPlotOwner(player, townBlock);

        for (Resident trustedResident : townBlock.getTrustedResidents()) {
            MenuItem.Builder itemBuilder = ResidentMenu.formatResidentInfo(trustedResident, player);

            if (canAddRemove)
                itemBuilder.lore(translatable("msg-right-click-to").append(translatable("plot-menu-trust-menu-remove")))
                        .action(ClickAction.rightClick(ClickAction.confirmation(() -> translatable("plot-menu-trust-menu-remove-confirm", text(trustedResident.getName())), ClickAction.run(() -> {
                            TownBlock townBlock1 = TownyAPI.getInstance().getTownBlock(worldCoord);
                            if (townBlock1 == null)
                                return;

                            PlotGroup group = townBlock1.getPlotObjectGroup();

                            if (!player.hasPermission(group == null ? PermissionNodes.TOWNY_COMMAND_PLOT_TRUST.getNode() : PermissionNodes.TOWNY_COMMAND_PLOT_GROUP_TRUST.getNode()) || !testPlotOwner(player, townBlock1)) {
                                TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_command_disable"));
                                MenuHistory.back(player);
                                return;
                            }

                            PlotTrustRemoveEvent event;
                            if (group == null) {
                                event = new PlotTrustRemoveEvent(townBlock, trustedResident, player);
                                Bukkit.getPluginManager().callEvent(event);

                                if (event.isCancelled()) {
                                    TownyMessaging.sendErrorMsg(player, event.getCancelMessage());
                                    return;
                                }

                                townBlock.removeTrustedResident(trustedResident);
                                Towny.getPlugin().deleteCache(trustedResident);

                                TownyMessaging.sendMsg(player, Translatable.of("msg_trusted_removed", trustedResident.getName(), Translatable.of("townblock")));
                                if (trustedResident.isOnline() && !trustedResident.getName().equals(player.getName()))
                                    TownyMessaging.sendMsg(trustedResident, Translatable.of("msg_trusted_removed_2", player.getName(), Translatable.of("townblock"), townBlock.getWorldCoord().getCoord().toString()));
                            } else {
                                event = new PlotTrustRemoveEvent(new ArrayList<>(group.getTownBlocks()), trustedResident, player);
                                Bukkit.getPluginManager().callEvent(event);

                                if (event.isCancelled()) {
                                    TownyMessaging.sendErrorMsg(player, event.getCancelMessage());
                                    MenuHistory.back(player);
                                    return;
                                }

                                group.removeTrustedResident(trustedResident);
                                Towny.getPlugin().deleteCache(trustedResident);

                                TownyMessaging.sendMsg(player, Translatable.of("msg_trusted_removed", trustedResident.getName(), Translatable.of("plotgroup_sing")));

                                if (trustedResident.isOnline() && !trustedResident.getName().equals(player.getName()))
                                    TownyMessaging.sendMsg(trustedResident, Translatable.of("msg_trusted_removed_2", player.getName(), Translatable.of("plotgroup_sing"), group.getName()));
                            }

                            MenuHistory.reOpen(player, () -> formatPlotTrustMenu(player, worldCoord));
                        }))));

            builder.addItem(itemBuilder.build());
        }

        if (canAddRemove)
            builder.addExtraItem(MenuItem.builder(Material.WRITABLE_BOOK)
                    .name(translatable("plot-menu-trust-add"))
                    .lore(translatable("msg-click-to").append(translatable("plot-menu-trust-add-subtitle")).color(GRAY))
                    .slot(SlotAnchor.anchor(VerticalAnchor.fromBottom(0), HorizontalAnchor.fromLeft(1)))
                    .action(ClickAction.userInput(Translatable.of("plot-menu-trust-enter-player").translate(locale), completion -> {
                        Resident resident = TownyAPI.getInstance().getResident(completion.getText());
                        if (resident == null)
                            return AnvilResponse.text(Translatable.of("plot-menu-trust-invalid-resident").translate(locale));

                        TownBlock townBlock1 = TownyAPI.getInstance().getTownBlock(worldCoord);
                        if (townBlock1 == null)
                            return AnvilResponse.close();

                        PlotGroup group = townBlock1.getPlotObjectGroup();

                        // Check if the player can still add players as trusted
                        if (!player.hasPermission(group == null ? PermissionNodes.TOWNY_COMMAND_PLOT_TRUST.getNode() : PermissionNodes.TOWNY_COMMAND_PLOT_GROUP_TRUST.getNode()) || !testPlotOwner(player, townBlock1))
                            return AnvilResponse.close();

                        if (group == null) {
                            if (townBlock1.hasTrustedResident(resident))
                                return AnvilResponse.text(resident.getName() + Translatable.of("plot-menu-trust-already").translate(locale));

                            PlotTrustAddEvent event = new PlotTrustAddEvent(townBlock, resident, player);
                            Bukkit.getPluginManager().callEvent(event);

                            if (event.isCancelled()) {
                                TownyMessaging.sendErrorMsg(player, event.getCancelMessage());
                                return AnvilResponse.close();
                            }

                            townBlock.addTrustedResident(resident);
                            Towny.getPlugin().deleteCache(resident);

                            TownyMessaging.sendMsg(player, Translatable.of("msg_trusted_added", resident.getName(), Translatable.of("townblock")));
                            if (resident.isOnline() && !resident.getName().equals(player.getName()))
                                TownyMessaging.sendMsg(resident, Translatable.of("msg_trusted_added_2", player.getName(), Translatable.of("townblock"), townBlock.getWorldCoord().getCoord().toString()));
                        } else {
                            if (group.hasTrustedResident(resident))
                                return AnvilResponse.text(resident.getName() + Translatable.of("plot-menu-trust-already").translate(locale));

                            PlotTrustAddEvent event = new PlotTrustAddEvent(new ArrayList<>(group.getTownBlocks()), resident, player);
                            Bukkit.getPluginManager().callEvent(event);

                            if (event.isCancelled()) {
                                TownyMessaging.sendErrorMsg(player, event.getCancelMessage());
                                return AnvilResponse.close();
                            }

                            group.addTrustedResident(resident);
                            Towny.getPlugin().deleteCache(resident);

                            TownyMessaging.sendMsg(player, Translatable.of("msg_trusted_added", resident.getName(), Translatable.of("plotgroup_sing")));

                            if (resident.isOnline() && !resident.getName().equals(player.getName()))
                                TownyMessaging.sendMsg(resident, Translatable.of("msg_trusted_added_2", player.getName(), Translatable.of("plotgroup_sing"), group.getName()));
                        }

                        MenuHistory.reOpen(player, () -> formatPlotTrustMenu(player, worldCoord));
                        return AnvilResponse.close();
                    }))
                    .build());

        return builder.build();
    }

    private static MenuInventory formatPlotPermissionOverrideMenu(final Player player, final WorldCoord worldCoord) {
        final Locale locale = Localization.localeOrDefault(player);
        final MenuInventory.PaginatorBuilder builder = MenuInventory.paginator().title(translatable("plot-menu-permission-override-title"));
        final TownBlock townBlock = TownyAPI.getInstance().getTownBlock(worldCoord);

        if (townBlock == null)
            return builder.build();

        final boolean canEdit = testPlotOwner(player, worldCoord);
        for (Map.Entry<Resident, PermissionData> entry : townBlock.getPermissionOverrides().entrySet()) {
            MenuItem.Builder itemBuilder = MenuItem.builder(Material.PLAYER_HEAD)
                    .name(text(entry.getKey().getName(), GREEN))
                    .skullOwner(entry.getKey().getUUID())
                    .lore(formatPermissionTypes(player, entry.getValue().getPermissionTypes()));

            if (canEdit) {
                if (entry.getValue().getLastChangedAt() > 0 && !entry.getValue().getLastChangedBy().isEmpty())
                    itemBuilder.lore(translatable("msg_last_edited", text(TownyFormatter.lastOnlineFormat.format(entry.getValue().getLastChangedAt())), text(entry.getValue().getLastChangedBy())));

                itemBuilder
                        .lore(translatable("msg_click_to_edit"))
                        .lore(translatable("msg-right-click-to").append(translatable("plot-menu-remove-override")).color(GOLD))
                        .action(ClickAction.leftClick(ClickAction.openInventory(() -> openPermissionOverrideEditor(player, worldCoord, entry.getKey(), entry.getValue()))))
                        .action(ClickAction.rightClick(ClickAction.confirmation(translatable("plot-menu-remove-override-confirm").color(GRAY), ClickAction.run(() -> {
                            final TownBlock tb = TownyAPI.getInstance().getTownBlock(worldCoord);
                            if (tb == null || !testPlotOwner(player, tb) || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_PLOT_PERM_REMOVE.getNode())) {
                                MenuHistory.reOpen(player, () -> formatPlotPermissionOverrideMenu(player, worldCoord));
                                return;
                            }

                            PlotGroup group = tb.getPlotObjectGroup();
                            if (group == null) {
                                tb.getPermissionOverrides().remove(entry.getKey());
                                tb.save();
                            } else {
                                group.removePermissionOverride(entry.getKey());
                                group.save();
                            }

                            Towny.getPlugin().deleteCache(entry.getKey());
                            MenuHistory.reOpen(player, () -> formatPlotPermissionOverrideMenu(player, worldCoord));
                        }))));
            }

            builder.addItem(itemBuilder.build());
        }

        if (canEdit) {
            builder.addExtraItem(MenuItem.builder(Material.WRITABLE_BOOK)
                    .name(translatable("plot-menu-permission-add-player"))
                    .slot(SlotAnchor.anchor(VerticalAnchor.fromBottom(0), HorizontalAnchor.fromLeft(1)))
                    .lore(translatable("msg-click-to").append(translatable("plot-menu-permission-add-player-subtitle")).color(GRAY))
                    .action(ClickAction.userInput(Translatable.of("plot-menu-permission-enter-player-name").translate(locale), completion -> {
                        final TownBlock tb = TownyAPI.getInstance().getTownBlock(worldCoord);

                        if (tb == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_PLOT_PERM_ADD.getNode()) || !testPlotOwner(player, tb)) {
                            MenuHistory.reOpen(player, () -> formatPlotPermissionOverrideMenu(player, worldCoord));
                            return AnvilResponse.nil();
                        }

                        final Resident toAdd = TownyAPI.getInstance().getResident(completion.getText());
                        if (toAdd == null)
                            return AnvilResponse.text(Translatable.of("msg_err_not_registered_1").stripColors(true).forLocale(player));

                        if (tb.getPermissionOverrides().containsKey(toAdd)) {
                            TownyMessaging.sendErrorMsg(player, Translatable.of("msg_overrides_already_set", toAdd.getName(), Translatable.of("townblock")));
                            return AnvilResponse.nil();
                        }

                        PlotGroup group = tb.getPlotObjectGroup();

                        if (group != null) {
                            group.putPermissionOverride(toAdd, new PermissionData(PermissionGUIUtil.getDefaultTypes(), player.getName()));
                            group.save();
                        } else {
                            tb.getPermissionOverrides().put(toAdd, new PermissionData(PermissionGUIUtil.getDefaultTypes(), player.getName()));
                            tb.save();
                        }

                        TownyMessaging.sendMsg(player, Translatable.of("msg_overrides_added", toAdd.getName()));
                        MenuHistory.reOpen(player, () -> formatPlotPermissionOverrideMenu(player, worldCoord));
                        return AnvilResponse.nil();
                    }))
                    .build());
        }

        return builder.build();
    }

    private static List<Component> formatPermissionTypes(final Player player, final SetPermissionType[] types) {
        final Locale locale = Localization.localeOrDefault(player);

        return Arrays.asList(
                translatable("plot-menu-permission-build").color(colorFromType(types[ActionType.BUILD.getIndex()])).append(text("   | ", DARK_GRAY)).append(translatable("plot-menu-permission-destroy").color(colorFromType(types[ActionType.DESTROY.getIndex()]))),
                translatable("plot-menu-permission-switch").color(colorFromType(types[ActionType.SWITCH.getIndex()])).append(text("   | ", DARK_GRAY)).append(translatable("plot-menu-permission-item").color(colorFromType(types[ActionType.ITEM_USE.getIndex()])))
        );
    }

    private static NamedTextColor colorFromType(final SetPermissionType type) {
        return switch (type) {
            case UNSET -> NamedTextColor.DARK_GRAY;
            case SET -> NamedTextColor.DARK_GREEN;
            case NEGATED -> NamedTextColor.DARK_RED;
        };
    }

    private static MenuInventory openPermissionOverrideEditor(final Player player, final WorldCoord worldCoord, final Resident resident, final PermissionData data) {
        final MenuInventory.Builder builder = MenuInventory.builder()
                .title(translatable("permission_gui_header"))
                .rows(5)
                .addItem(MenuHelper.backButton().build())
                .addItem(MenuItem.builder(Material.PLAYER_HEAD)
                        .name(text(resident.getName(), GREEN))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(0), HorizontalAnchor.fromLeft(4)))
                        .skullOwner(resident.getUUID())
                        .lore(formatPermissionTypes(player, data.getPermissionTypes()))
                        .build());

        final Consumer<ActionType> onClick = actionType -> {
            final TownBlock townBlock = TownyAPI.getInstance().getTownBlock(worldCoord);
            if (townBlock == null || !testPlotOwner(player, townBlock)) {
                MenuHistory.reOpen(player, () -> formatPlotPermissionOverrideMenu(player, worldCoord));
                return;
            }

            SetPermissionType existing = data.getPermissionTypes()[actionType.getIndex()];

            data.getPermissionTypes()[actionType.getIndex()] = switch (existing) {
                case SET -> SetPermissionType.NEGATED;
                case NEGATED -> SetPermissionType.UNSET;
                case UNSET -> SetPermissionType.SET;
            };

            final PlotGroup group = townBlock.getPlotObjectGroup();
            if (group != null) {
                group.putPermissionOverride(resident, data);
                group.save();
            } else
                townBlock.save();

            Towny.getPlugin().deleteCache(resident);
            MenuHistory.reOpen(player, () -> openPermissionOverrideEditor(player, worldCoord, resident, data));
        };

        for (ActionType type : ActionType.values()) {
            builder.addItem(MenuItem.builder(data.getPermissionTypes()[type.getIndex()].getWoolColour())
                    .name(text(type.getCommonName(), colorFromType(data.getPermissionTypes()[type.getIndex()])).decorate(TextDecoration.BOLD))
                    .slot(switch (type) {
                        case BUILD -> SlotAnchor.anchor(VerticalAnchor.fromTop(2), HorizontalAnchor.fromLeft(3));
                        case DESTROY -> SlotAnchor.anchor(VerticalAnchor.fromTop(2), HorizontalAnchor.fromRight(3));
                        case SWITCH -> SlotAnchor.anchor(VerticalAnchor.fromTop(3), HorizontalAnchor.fromLeft(3));
                        case ITEM_USE -> SlotAnchor.anchor(VerticalAnchor.fromTop(3), HorizontalAnchor.fromRight(3));
                    })
                    .action(ClickAction.run(() -> onClick.accept(type)))
                    .build());
        }

        return builder.build();
    }

    private static MenuInventory formatPlotToggle(final Player player, final WorldCoord worldCoord) {
        final TownBlock townBlock = TownyAPI.getInstance().getTownBlock(worldCoord);

        final boolean fireEnabled = townBlock != null && townBlock.getPermissions().fire;
        final boolean explosionEnabled = townBlock != null && townBlock.getPermissions().explosion;
        final boolean mobsEnabled = townBlock != null && townBlock.getPermissions().mobs;
        final boolean pvpEnabled = townBlock != null && townBlock.getPermissions().pvp;

        return MenuInventory.builder()
                .title(translatable("plot-menu-plot-toggle-title"))
                .rows(4)
                .addItem(MenuHelper.backButton().build())
                // Explosion
                .addItem(createTogglePropertyItem(player, worldCoord, Material.TNT, explosionEnabled, "explosion")
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(1)))
                        .build())
                .addItem(MenuItem.builder(explosionEnabled ? Material.GREEN_CONCRETE : Material.RED_CONCRETE)
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(2), HorizontalAnchor.fromLeft(1)))
                        .name(Component.empty())
                        .build())
                // Fire
                .addItem(createTogglePropertyItem(player, worldCoord, Material.FLINT_AND_STEEL, fireEnabled, "fire")
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(2)))
                        .build())
                .addItem(MenuItem.builder(fireEnabled ? Material.GREEN_CONCRETE : Material.RED_CONCRETE)
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(2), HorizontalAnchor.fromLeft(2)))
                        .name(Component.empty())
                        .build())
                // Mobs
                .addItem(createTogglePropertyItem(player, worldCoord, Material.BAT_SPAWN_EGG, mobsEnabled, "mobs")
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(3)))
                        .build())
                .addItem(MenuItem.builder(mobsEnabled ? Material.GREEN_CONCRETE : Material.RED_CONCRETE)
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(2), HorizontalAnchor.fromLeft(3)))
                        .name(Component.empty())
                        .build())
                // PVP
                .addItem(createTogglePropertyItem(player, worldCoord, Material.WOODEN_AXE, pvpEnabled, "pvp")
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(4)))
                        .build())
                .addItem(MenuItem.builder(pvpEnabled ? Material.GREEN_CONCRETE : Material.RED_CONCRETE)
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(2), HorizontalAnchor.fromLeft(4)))
                        .name(Component.empty())
                        .build())
                .build();
    }

    private static MenuItem.Builder createTogglePropertyItem(Player player, WorldCoord worldCoord, Material material, boolean propertyEnabled, String property) {
        final Locale locale = Localization.localeOrDefault(player);

        return MenuItem.builder(material)
                .name(translatable("plot-menu-toggle-title").append(text(property.substring(0, 1).toUpperCase(Locale.ROOT) + property.substring(1))).color(propertyEnabled ? GREEN : RED))
                .lore(() -> {
                    if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_PLOT_TOGGLE.getNode(property)) || !testPlotOwner(player, worldCoord))
                        return translatable("msg-no-permission-to").append(translatable("plot-menu-toggle")).append(text(property + ".")).color(GRAY);
                    else
                        return translatable("msg-click-to").append(propertyEnabled ? translatable("plot-menu-toggle-disable") : translatable("plot-menu-toggle-enable")).append(text(" " + property + "."));
                })
                .action(!player.hasPermission(PermissionNodes.TOWNY_COMMAND_PLOT_TOGGLE.getNode(property)) ? ClickAction.NONE : ClickAction.confirmation(text("Are you sure you want to toggle " + property + " in this plot?", GRAY), ClickAction.run(() -> {
                    TownBlock townBlock = TownyAPI.getInstance().getTownBlock(worldCoord);
                    if (townBlock == null)
                        return;

                    plotCommand().ifPresent(command -> {
                        final Resident resident = TownyAPI.getInstance().getResident(player);
                        if (resident == null)
                            return;

                        try {
                            command.plotToggle(player, resident, townBlock, new String[]{property});
                        } catch (TownyException e) {
                            TownyMessaging.sendErrorMsg(player, e.getMessage(player));
                        }
                    });

                    MenuHistory.reOpen(player, () -> formatPlotToggle(player, worldCoord));
                })));
    }

    private static Optional<PlotCommand> plotCommand() {
        PluginCommand command = Towny.getPlugin().getCommand("plot");
        if (command == null || !(command.getExecutor() instanceof PlotCommand plotCommand))
            return Optional.empty();

        return Optional.of(plotCommand);
    }

    private static boolean testPlotOwner(Player player, WorldCoord worldCoord) {
        return testPlotOwner(player, TownyAPI.getInstance().getTownBlock(worldCoord));
    }

    private static boolean testPlotOwner(Player player, TownBlock townBlock) {
        Resident resident = TownyAPI.getInstance().getResident(player);

        if (resident == null || townBlock == null)
            return false;

        try {
            TownyAPI.getInstance().testPlotOwnerOrThrow(resident, townBlock);
            return true;
        } catch (TownyException e) {
            return false;
        }
    }
}
