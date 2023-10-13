package io.github.townyadvanced.townymenus.menu;

import com.palmergames.bukkit.towny.object.Translatable;
import net.kyori.adventure.text.Component;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.command.BaseCommand;
import com.palmergames.bukkit.towny.command.TownCommand;
import com.palmergames.bukkit.towny.event.TownAddResidentRankEvent;
import com.palmergames.bukkit.towny.event.TownRemoveResidentRankEvent;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Government;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownBlockTypeCache;
import com.palmergames.bukkit.towny.object.TownBlockTypeCache.CacheType;
import com.palmergames.bukkit.towny.object.TownBlockTypeHandler;
import com.palmergames.bukkit.towny.object.TransactionType;
import com.palmergames.bukkit.towny.object.economy.Account;
import com.palmergames.bukkit.towny.object.economy.BankTransaction;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import io.github.townyadvanced.townymenus.gui.MenuHelper;
import io.github.townyadvanced.townymenus.gui.MenuHistory;
import io.github.townyadvanced.townymenus.gui.MenuInventory;
import io.github.townyadvanced.townymenus.gui.MenuItem;
import io.github.townyadvanced.townymenus.gui.action.ClickAction;
import io.github.townyadvanced.townymenus.gui.slot.anchor.HorizontalAnchor;
import io.github.townyadvanced.townymenus.gui.slot.anchor.SlotAnchor;
import io.github.townyadvanced.townymenus.gui.slot.anchor.VerticalAnchor;
import io.github.townyadvanced.townymenus.listeners.AwaitingConfirmation;
import io.github.townyadvanced.townymenus.menu.helper.GovernmentMenus;
import io.github.townyadvanced.townymenus.utils.AnvilResponse;
import io.github.townyadvanced.townymenus.utils.Localization;
import io.github.townyadvanced.townymenus.utils.Time;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public class TownMenu {
    public static MenuInventory createTownMenu(@NotNull Player player) {
        final Resident resident = TownyAPI.getInstance().getResident(player);
        final Town town = resident != null ? resident.getTownOrNull() : null;
        final Locale locale = Localization.localeOrDefault(player);

        return MenuInventory.builder()
                .rows(6)
                .title(translatable("town-menu-title", town != null ? text(town.getName()) : translatable("town-menu-no-town")))
                .addItem(MenuHelper.backButton().build())
                .addItem(formatTownInfo(player, town)
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(4)))
                        .build())
                .addItem(MenuItem.builder(Material.EMERALD_BLOCK)
                        .name(translatable("town-menu-bank"))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromBottom(1), HorizontalAnchor.fromLeft(3)))
                        .lore(() -> {
                            if (town == null)
                                return translatable("msg-err-not-part-of-town");
                            else
                                return translatable("msg-click-to").append(translatable("town-menu-bank-subtitle"));
                        })
                        .action(town == null ? ClickAction.NONE : ClickAction.openInventory(() -> formatTownBankMenu(player)))
                        .build())
                .addItem(MenuItem.builder(Material.GRASS_BLOCK)
                        .name(translatable("town-menu-plots"))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromBottom(1), HorizontalAnchor.fromRight(3)))
                        .lore(() -> {
                            if (town == null)
                                return translatable("msg-err-not-part-of-town");
                            else if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_PLOTS.getNode()))
                                return translatable("msg-no-permission-to").append(translatable("town-menu-plots-subtitle"));
                            else
                                return translatable("msg-click-to").append(translatable("town-menu-plots-subtitle"));
                        })
                        .action(town == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_PLOTS.getNode()) ? ClickAction.NONE : ClickAction.openInventory(() -> {
                            if (!TownyUniverse.getInstance().hasTown(town.getUUID()))
                                return MenuInventory.paginator().title(translatable("town-menu-plots")).build();

                            List<MenuItem> plotItems = new ArrayList<>();
                            TownBlockTypeCache cache = town.getTownBlockTypeCache();

                            for (TownBlockType type : TownBlockTypeHandler.getTypes().values()) {
                                int residentOwned = cache.getNumTownBlocks(type, CacheType.RESIDENTOWNED);

                                plotItems.add(MenuItem.builder(Material.GRASS_BLOCK)
                                        .name(text(type.getFormattedName(), GREEN))
                                        .lore(() -> translatable("town-menu-plots-resident-owned", text(residentOwned)))
                                        .lore(() -> translatable("town-menu-plots-for-sale", text(cache.getNumTownBlocks(type, CacheType.FORSALE))))
                                        .lore(() -> translatable("town-menu-plots-total", text(cache.getNumTownBlocks(type, CacheType.ALL))))
                                        .lore(!TownyEconomyHandler.isActive() ? Component.empty() : translatable("town-menu-plots-daily-revenue", text(cache.getNumTownBlocks(type, CacheType.ALL))))
                                        .build());
                            }

                            return MenuInventory.paginator()
                                    .addItems(plotItems)
                                    .addExtraItem(MenuItem.builder(Material.OAK_SIGN)
                                            .name(translatable("town-menu-plots"))
                                            .slot(SlotAnchor.anchor(VerticalAnchor.fromBottom(0), HorizontalAnchor.fromLeft(1)))
                                            .lore(translatable("msg_town_plots_town_size", text(town.getTownBlocks().size()), text(town.getMaxTownBlocksAsAString()))
                                                    .append(town.hasUnlimitedClaims()
                                                            ? Component.empty()
                                                            : TownySettings.isSellingBonusBlocks(town) ?
                                                            translatable("msg_town_plots_town_bought", text(town.getPurchasedBlocks()), text(TownySettings.getMaxPurchasedBlocks(town))) :
                                                            Component.empty()
                                                                    .append(town.getBonusBlocks() > 0
                                                                            ? translatable("msg_town_plots_town_bonus", text(town.getBonusBlocks()))
                                                                            : Component.empty())
                                                                    .append(TownySettings.getNationBonusBlocks(town) > 0
                                                                            ? translatable("msg_town_plots_town_nationbonus", text(TownySettings.getNationBonusBlocks(town)))
                                                                            : Component.empty())))
                                            .lore(translatable("msg_town_plots_town_owned_land", text(town.getTownBlocks().size() - cache.getNumberOfResidentOwnedTownBlocks())))
                                            .build())
                                    .title(translatable("town-menu-plots"))
                                    .build();
                        }))
                        .build())
                .addItem(MenuItem.builder(Material.RED_BED)
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(1)))
                        .name(translatable("town-menu-spawn"))
                        .lore(() -> {
                            if (town == null)
                                return translatable("msg-err-not-part-of-town");
                            else if (!player.hasPermission("towny.town.spawn.town"))
                                return translatable("msg-no-permission");
                            else
                                return translatable("msg-click-to").append(translatable("town-menu-spawn-subtitle"));
                        })
                        .action(town == null || !player.hasPermission("towny.town.spawn.town") ? ClickAction.NONE : ClickAction.confirmation(() -> translatable("msg-click-to-confirm", "/town spawn"), ClickAction.run(() -> {
                            if (!player.hasPermission("towny.town.spawn.town"))
                                return;

                            try {
                                TownCommand.townSpawn(player, new String[]{}, false, true);
                            } catch (TownyException e) {
                                TownyMessaging.sendErrorMsg(player, e.getMessage(player));
                            }

                            player.closeInventory();
                        })))
                        .build())
                .addItem(MenuItem.builder(Material.ENDER_EYE)
                        .name(translatable("town-menu-online"))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromBottom(2), HorizontalAnchor.fromLeft(1)))
                        .lore(() -> {
                            if (town == null)
                                return translatable("msg-err-not-part-of-town");
                            else if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_ONLINE.getNode()))
                                return translatable("msg-no-permission-to").append(translatable("town-menu-online-subtitle"));
                            else
                                return translatable("msg-click-to").append(translatable("town-menu-online-subtitle"));
                        })
                        .action(town == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_ONLINE.getNode()) ? ClickAction.NONE : ClickAction.openInventory(() -> {
                            final Town playerTown = TownyAPI.getInstance().getTown(player);
                            if (playerTown == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_ONLINE.getNode()))
                                return MenuInventory.paginator().title(translatable("town-menu-online")).build();

                            List<MenuItem> online = new ArrayList<>();

                            for (Player onlinePlayer : TownyAPI.getInstance().getOnlinePlayers(playerTown)) {
                                if (!player.canSee(onlinePlayer))
                                    continue;

                                online.add(ResidentMenu.formatResidentInfo(onlinePlayer.getUniqueId(), player).build());
                            }

                            return MenuInventory.paginator().addItems(online).title(translatable("town-menu-online")).build();
                        }))
                        .build())
                .addItem(MenuItem.builder(Material.PLAYER_HEAD)
                        .name(translatable("town-menu-overview"))
                        .lore(() -> {
                            if (town == null)
                                return translatable("msg-err-not-part-of-town");
                            else
                                return translatable("msg-click-to").append(translatable("town-menu-overview-subtitle"));
                        })
                        .action(town == null ? ClickAction.NONE : ClickAction.openInventory(() -> createResidentOverview(player)))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromBottom(2), HorizontalAnchor.fromRight(1)))
                        .build())
                .addItem(MenuItem.builder(Material.GOLDEN_AXE)
                        .name(translatable("town-menu-management"))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromRight(1)))
                        .lore(() -> {
                            if (town == null)
                                return translatable("msg-err-not-part-of-town");
                            else
                                return translatable("msg-click-to").append(translatable("town-menu-management-subtitle"));
                        })
                        .action(town == null ? ClickAction.NONE : ClickAction.openInventory(() -> formatTownManagementMenu(player)))
                        .build())
                .build();
    }

    public static MenuInventory createBankHistoryMenu(Player player, Government government) {
        final Locale locale = Localization.localeOrDefault(player);
        if (government == null || !TownyEconomyHandler.isActive())
            return MenuInventory.paginator().title(translatable("town-menu-transaction-history")).build();

        List<MenuItem> transactionItems = new ArrayList<>();
        List<BankTransaction> transactions = new ArrayList<>(government.getAccount().getAuditor().getTransactions());

        for (int i = 0; i < transactions.size(); i++) {
            BankTransaction transaction = transactions.get(i);
            boolean added = transaction.getType() == TransactionType.ADD || transaction.getType() == TransactionType.DEPOSIT;

            transactionItems.add(MenuItem.builder(added ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK)
                    .name(translatable("town-menu-transaction-number", text(i + 1), text(transaction.getTime())).color(added ? GREEN : RED))
                    .lore(translatable("town-menu-transaction-amount", TownyEconomyHandler.getFormattedBalance(transaction.getAmount())))
                    .lore(translatable("town-menu-transaction-new-balance", TownyEconomyHandler.getFormattedBalance(transaction.getBalance())))
                    .lore(translatable("town-menu-transaction-reason", transaction.getReason()))
                    .build());
        }

        Collections.reverse(transactionItems);

        return MenuInventory.paginator().addItems(transactionItems).title(translatable("town-menu-transaction-history")).build();
    }

    public static MenuInventory createResidentOverview(Player player) {
        final Locale locale = Localization.localeOrDefault(player);
        Resident res = TownyAPI.getInstance().getResident(player);
        Town town = res != null ? res.getTownOrNull() : null;

        if (town == null)
            return MenuInventory.paginator().title(translatable("town-menu-overview")).build();

        MenuInventory.PaginatorBuilder builder = MenuInventory.paginator()
                .title(translatable("town-menu-overview").append(text(" - ")).append(text(town.getName())));

        for (Resident resident : town.getResidents()) {
            builder.addItem(ResidentMenu.formatResidentInfo(resident, player)
                    .lore(translatable("town-menu-overview-joined-town").append(Time.registeredOrAgo(res.getJoinedTownAt())))
                    .lore(Component.space())
                    .lore(translatable("msg-right-click-additional-options"))
                    .action(ClickAction.rightClick(ClickAction.openInventory(() -> createResidentManagementScreen(player, town, resident))))
                    .build());
        }

        return builder.build();
    }

    public static MenuInventory createResidentManagementScreen(Player player, Town town, Resident resident) {
        final Locale locale = Localization.localeOrDefault(player);

        return MenuInventory.builder()
                .rows(5)
                .title(translatable("town-menu-management-resident-title"))
                .addItem(MenuHelper.backButton().build())
                .addItem(MenuItem.builder(Material.WOODEN_AXE)
                        .name(translatable("town-menu-management-resident-kick-title"))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(1)))
                        .lore(() -> {
                            if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_KICK.getNode()))
                                return translatable("msg-no-permission-to" + "town-menu-management-resident-kick");
                            else if (player.getUniqueId().equals(resident.getUUID()))
                                return translatable("town-menu-management-resident-cannot-kick-self");
                            else
                                return translatable("msg-click-to").append(translatable("town-menu-management-resident-kick-subtitle"));
                        })
                        .action(!player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_KICK.getNode()) || player.getUniqueId().equals(resident.getUUID()) ? ClickAction.NONE : ClickAction.confirmation(text("Are you sure you want to kick " + resident.getName() + "?", GRAY), ClickAction.run(() -> {
                            if (town == null || !TownyUniverse.getInstance().hasTown(town.getUUID()) || !town.hasResident(resident))
                                return;

                            TownCommand.townKickResidents(player, TownyAPI.getInstance().getResident(player), town, Collections.singletonList(resident));
                        })))
                        .build())
                .addItem(ResidentMenu.formatResidentInfo(resident, player)
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(4)))
                        .build())
                .addItem(MenuItem.builder(Material.KNOWLEDGE_BOOK)
                        .name(translatable("town-menu-management-resident-ranks"))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromRight(1)))
                        .lore(translatable("msg-click-to").append(translatable("town-menu-management-resident-ranks-subtitle")))
                        .action(ClickAction.openInventory(() -> formatRankManagementMenu(player, town, resident)))
                        .build())
                .addItem(MenuItem.builder(Material.NAME_TAG)
                        .name(translatable("town-menu-management-resident-title-title"))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(3), HorizontalAnchor.fromLeft(2)))
                        .lore(() -> {
                            if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_SET_TITLE.getNode()))
                                return translatable("msg-no-permission-to").append(translatable("town-menu-management-resident-title-title-subtitle")).color(GRAY);
                            else
                                return Arrays.asList(translatable("msg-click-to").append(translatable("town-menu-management-resident-title-title-subtitle")),
                                        translatable("msg-right-click-to").append(translatable("town-menu-management-resident-title-clear")));
                        })
                        .action(!player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_SET_TITLE.getNode()) ? ClickAction.NONE : ClickAction.leftClick(ClickAction.userInput(translatable("town-menu-management-resident-enter-new-title").toString(), completion -> {
                            try {
                                BaseCommand.checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_SET_TITLE.getNode());
                                TownCommand.townSetTitle(player, resident, completion.getText(), false);
                            } catch (TownyException e) {
                                TownyMessaging.sendErrorMsg(player, e.getMessage(player));
                                return AnvilResponse.text(e.getMessage(player));
                            }

                            MenuHistory.last(player);
                            return AnvilResponse.close();
                        })))
                        .action(!player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_SET_TITLE.getNode()) ? ClickAction.NONE : ClickAction.rightClick(ClickAction.run(() -> {
                            try {
                                BaseCommand.checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_SET_TITLE.getNode());
                                TownCommand.townSetTitle(player, resident, "", false);
                            } catch (TownyException e) {
                                TownyMessaging.sendErrorMsg(player, e.getMessage(player));
                            }
                        })))
                        .build())
                .addItem(MenuItem.builder(Material.NAME_TAG)
                        .name(translatable("town-menu-management-resident-surname"))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(3), HorizontalAnchor.fromRight(2)))
                        .lore(() -> {
                            if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_SET_SURNAME.getNode()))
                                return translatable("msg-no-permission-to").append(translatable("town-menu-management-resident-surname-subtitle"));
                            else
                                return Arrays.asList(translatable("msg-click-to").append(translatable("town-menu-management-resident-surname-subtitle")),
                                        translatable("msg-right-click-to").append(translatable("town-menu-management-resident-surname-clear")));
                        })
                        .action(!player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_SET_SURNAME.getNode()) ? ClickAction.NONE : ClickAction.leftClick(ClickAction.userInput(translatable("town-menu-management-resident-enter-new-surname").toString(), completion -> {
                            try {
                                BaseCommand.checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_SET_SURNAME.getNode());
                                TownCommand.townSetSurname(player, resident, completion.getText(), false);
                            } catch (TownyException e) {
                                TownyMessaging.sendErrorMsg(player, e.getMessage(player));
                                return AnvilResponse.text(e.getMessage(player));
                            }

                            MenuHistory.last(player);
                            return AnvilResponse.nil();
                        })))
                        .action(!player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_SET_SURNAME.getNode()) ? ClickAction.NONE : ClickAction.rightClick(ClickAction.run(() -> {
                            try {
                                BaseCommand.checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_SET_SURNAME.getNode());
                                TownCommand.townSetSurname(player, resident, "", false);
                            } catch (TownyException e) {
                                TownyMessaging.sendErrorMsg(player, e.getMessage(player));
                            }
                        })))
                        .build())
                .build();
    }

    public static MenuInventory formatRankManagementMenu(Player player, Town town, Resident resident) {
        final Locale locale = Localization.localeOrDefault(player);

        MenuInventory.PaginatorBuilder paginator = MenuInventory.paginator().title(translatable("town-menu-management-rank"));

        for (String townRank : TownyPerms.getTownRanks()) {
            MenuItem.Builder item = MenuItem.builder(Material.KNOWLEDGE_BOOK)
                    .name(text(townRank.substring(0, 1).toUpperCase(Locale.ROOT) + townRank.substring(1), GREEN));

            final boolean hasPermission = player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_RANK.getNode(townRank.toLowerCase(Locale.ROOT)));

            if (resident.hasTownRank(townRank)) {
                item.withGlint();

                if (hasPermission) {
                    item.lore(translatable("msg-click-to").append(translatable("rank-remove")).append(text(townRank)).append(translatable("rank-from")).append(text(resident.getName() + ".")).color(GRAY));
                    item.action(ClickAction.confirmation(translatable("rank-remove-confirmation").append(text(townRank)).append(translatable("from").append(text(resident.getName() + "?"))).color(GRAY), ClickAction.run(() -> {
                        if (!resident.hasTownRank(townRank) || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_RANK.getNode(townRank.toLowerCase(Locale.ROOT)))) {
                            MenuHistory.reOpen(player, () -> formatRankManagementMenu(player, town, resident));
                            return;
                        }

                        Resident playerResident = TownyAPI.getInstance().getResident(player);
                        if (town == null || resident.getTownOrNull() != town || playerResident == null || playerResident.getTownOrNull() != town)
                            return;

                        TownRemoveResidentRankEvent event = new TownRemoveResidentRankEvent(resident, townRank, town);
                        Bukkit.getPluginManager().callEvent(event);

                        if (event.isCancelled()) {
                            TownyMessaging.sendErrorMsg(player, event.getCancelMessage());
                            MenuHistory.reOpen(player, () -> formatRankManagementMenu(player, town, resident));
                            return;
                        }

                        resident.removeTownRank(townRank);

                        if (resident.isOnline()) {
                            TownyMessaging.sendMsg(resident, Translatable.of("msg_you_have_had_rank_taken", Translatable.of("town_sing"), townRank));
                            Towny.getPlugin().deleteCache(resident);
                        }

                        TownyMessaging.sendMsg(player, Translatable.of("msg_you_have_taken_rank_from", Translatable.of("town_sing"), townRank, resident.getName()));
                        MenuHistory.reOpen(player, () -> formatRankManagementMenu(player, town, resident));
                    })));
                }
            } else if (hasPermission) {
                item.lore(translatable("msg-click-to-grant-the").append(text(townRank)).append(translatable("rank-to")).append(text(resident.getName() + ".")).color(GRAY));
                item.action(ClickAction.confirmation(translatable("rank-give-confirmation").append(text(townRank)).append(translatable("to").append(text(resident.getName() + "?"))).color(GRAY), ClickAction.run(() -> {
                    if (resident.hasTownRank(townRank) || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_RANK.getNode(townRank.toLowerCase(Locale.ROOT)))) {
                        MenuHistory.reOpen(player, () -> formatRankManagementMenu(player, town, resident));
                        return;
                    }

                    Resident playerResident = TownyAPI.getInstance().getResident(player);
                    if (town == null || resident.getTownOrNull() != town || playerResident == null || playerResident.getTownOrNull() != town)
                        return;

                    TownAddResidentRankEvent event = new TownAddResidentRankEvent(resident, townRank, town);
                    Bukkit.getPluginManager().callEvent(event);

                    if (event.isCancelled()) {
                        TownyMessaging.sendErrorMsg(player, event.getCancelMessage());
                        MenuHistory.reOpen(player, () -> formatRankManagementMenu(player, town, resident));
                        return;
                    }

                    resident.addTownRank(townRank);
                    if (resident.isOnline()) {
                        TownyMessaging.sendMsg(resident, Translatable.of("msg_you_have_been_given_rank", Translatable.of("town_sing"), townRank));
                        Towny.getPlugin().deleteCache(resident);
                    }

                    TownyMessaging.sendMsg(player, Translatable.of("msg_you_have_given_rank", Translatable.of("town_sing"), townRank, resident.getName()));
                    MenuHistory.reOpen(player, () -> formatRankManagementMenu(player, town, resident));
                })));
            }

            paginator.addItem(item.build());
        }

        return paginator.build();
    }

    public static MenuInventory formatTownManagementMenu(Player player) {
        final Locale locale = Localization.localeOrDefault(player);

        return MenuInventory.builder()
                .rows(3)
                .title(translatable("town-menu-management"))
                .addItem(MenuHelper.backButton().build())
                .addItem(MenuItem.builder(Material.GRASS_BLOCK)
                        .name(translatable("town-menu-management-town-menu-title"))
                        .lore(translatable("msg-click-to").append(translatable("town-menu-management-town-menu-subtitle")))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(2)))
                        .action(ClickAction.openInventory(() -> formatTownSetMenu(player)))
                        .build())
                // TODO: Manage town permissions and trusted players
                .addItem(MenuItem.builder(Material.LEVER)
                        .name(translatable("town-menu-management-town-toggle-title"))
                        .lore(translatable("msg-click-to").append(translatable("town-menu-management-town-toggle-subtitle")))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromRight(2)))
                        .action(ClickAction.openInventory(() -> formatTownToggleMenu(player)))
                        .build())
                .build();
    }

    public static MenuInventory formatTownToggleMenu(Player player) {
        final Locale locale = Localization.localeOrDefault(player);
        Town town = TownyAPI.getInstance().getTown(player);

        final boolean fireEnabled = town != null && town.isFire();
        final boolean explosionEnabled = town != null && town.isExplosion();
        final boolean mobsEnabled = town != null && town.hasMobs();
        final boolean pvpEnabled = town != null && town.isPVP();

        final boolean isOpen = town != null && town.isOpen();
        final boolean isPublic = town != null && town.isPublic();

        return MenuInventory.builder()
                .title(translatable("town-menu-management"))
                .rows(4)
                .addItem(MenuHelper.backButton().build())
                // Explosion
                .addItem(GovernmentMenus.createTogglePropertyItem(player, town, Material.TNT, explosionEnabled, "explosion")
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(1)))
                        .build())
                .addItem(MenuItem.builder(explosionEnabled ? Material.GREEN_CONCRETE : Material.RED_CONCRETE)
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(2), HorizontalAnchor.fromLeft(1)))
                        .name(Component.empty())
                        .build())
                // Fire
                .addItem(GovernmentMenus.createTogglePropertyItem(player, town, Material.FLINT_AND_STEEL, fireEnabled, "fire")
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(2)))
                        .build())
                .addItem(MenuItem.builder(fireEnabled ? Material.GREEN_CONCRETE : Material.RED_CONCRETE)
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(2), HorizontalAnchor.fromLeft(2)))
                        .name(Component.empty())
                        .build())
                // Mobs
                .addItem(GovernmentMenus.createTogglePropertyItem(player, town, Material.BAT_SPAWN_EGG, mobsEnabled, "mobs")
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(3)))
                        .build())
                .addItem(MenuItem.builder(mobsEnabled ? Material.GREEN_CONCRETE : Material.RED_CONCRETE)
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(2), HorizontalAnchor.fromLeft(3)))
                        .name(Component.empty())
                        .build())
                // PVP
                .addItem(GovernmentMenus.createTogglePropertyItem(player, town, Material.WOODEN_AXE, pvpEnabled, "pvp")
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(4)))
                        .build())
                .addItem(MenuItem.builder(pvpEnabled ? Material.GREEN_CONCRETE : Material.RED_CONCRETE)
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(2), HorizontalAnchor.fromLeft(4)))
                        .name(Component.empty())
                        .build())
                // Open
                .addItem(GovernmentMenus.createTogglePropertyItem(player, town, Material.GRASS_BLOCK, isOpen, "open")
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(6)))
                        .build())
                .addItem(MenuItem.builder(isOpen ? Material.GREEN_CONCRETE : Material.RED_CONCRETE)
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(2), HorizontalAnchor.fromLeft(6)))
                        .name(Component.empty())
                        .build())
                // Public
                .addItem(GovernmentMenus.createTogglePropertyItem(player, town, Material.GRASS_BLOCK, isPublic, "public")
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(7)))
                        .build())
                .addItem(MenuItem.builder(isPublic ? Material.GREEN_CONCRETE : Material.RED_CONCRETE)
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(2), HorizontalAnchor.fromLeft(7)))
                        .name(Component.empty())
                        .build())
                .build();
    }

    public static MenuInventory formatTownSetMenu(Player player) {
        final Locale locale = Localization.localeOrDefault(player);
        final Town town = TownyAPI.getInstance().getTown(player);

        return MenuInventory.builder()
                .title(translatable("town-menu-town-set-title"))
                .rows(3)
                .addItem(MenuHelper.backButton().build())
                .addItem(MenuItem.builder(Material.NAME_TAG)
                        .name(translatable("town-menu-town-set-change-name"))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(2)))
                        .lore(() -> {
                            if (town == null)
                                return translatable("msg-err-not-part-of-town");
                            else if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_SET_NAME.getNode()))
                                return translatable("msg-no-permission-to").append(translatable("town-menu-town-set-change-name-subtitle"));
                            else
                                return translatable("msg-click-to").append(translatable("town-menu-town-set-change-name-subtitle"));
                        })
                        .action(town == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_SET_NAME.getNode()) ? ClickAction.NONE : ClickAction.userInput(translatable("town-menu-town-set-enter-town-name").toString(), completion -> {
                            final Town playerTown = TownyAPI.getInstance().getTown(player);
                            if (playerTown == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_SET_NAME.getNode()))
                                return AnvilResponse.close();

                            AwaitingConfirmation.await(player);

                            try {
                                TownCommand.townSetName(player, new String[]{"", completion.getText().replaceAll(" ", "_")}, town);
                            } catch (TownyException e) {
                                TownyMessaging.sendErrorMsg(player, e.getMessage(player));
                                return AnvilResponse.text(e.getMessage(player));
                            }

                            MenuHistory.reOpen(player, () -> formatTownSetMenu(player));
                            return AnvilResponse.nil();
                        }))
                        .build())
                .addItem(MenuItem.builder(Material.OAK_SIGN)
                        .name(translatable("town-menu-town-set-change-board"))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(4)))
                        .lore(() -> {
                            if (town == null)
                                return translatable("msg-err-not-part-of-town");
                            else if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_SET_BOARD.getNode()))
                                return translatable("msg-no-permission-to").append(translatable("town-menu-town-set-change-board-subtitle")).color(GRAY);
                            else
                                return Arrays.asList(translatable("msg-click-to").append(translatable("town-menu-town-set-change-board-subtitle")).color(GRAY),
                                        translatable("msg-right-click-to").append(translatable("town-menu-town-set-change-board-subtitle")).color(GRAY));
                        })
                        .action(town == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_SET_BOARD.getNode()) ? ClickAction.NONE : ClickAction.leftClick(ClickAction.userInput(translatable("town-menu-town-set-enter-town-board").toString(), completion -> {
                            final Town playerTown = TownyAPI.getInstance().getTown(player);
                            if (playerTown == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_SET_BOARD.getNode()))
                                return AnvilResponse.close();

                            try {
                                TownCommand.townSetBoard(player, completion.getText(), playerTown);
                            } catch (TownyException e) {
                                TownyMessaging.sendErrorMsg(player, e.getMessage(player));
                                return AnvilResponse.text(e.getMessage(player));
                            }

                            MenuHistory.reOpen(player, () -> formatTownSetMenu(player));
                            return AnvilResponse.nil();
                        })))
                        .action(town == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_SET_BOARD.getNode()) ? ClickAction.NONE : ClickAction.rightClick(ClickAction.run(() -> {
                            final Town playerTown = TownyAPI.getInstance().getTown(player);
                            if (playerTown == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_SET_BOARD.getNode()))
                                return;

                            try {
                                TownCommand.townSetBoard(player, "reset", playerTown);
                            } catch (TownyException e) {
                                TownyMessaging.sendErrorMsg(player, e.getMessage(player));
                            }
                        })))
                        .build())
                .addItem(MenuItem.builder(Material.RED_BED)
                        .name(translatable("town-menu-town-set-change-spawn"))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromRight(2)))
                        .lore(() -> {
                            if (town == null)
                                return translatable("msg-err-not-part-of-town");
                            else if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_SET_SPAWN.getNode()))
                                return translatable("msg-no-permission-to").append(translatable("town-menu-town-set-change-spawn-subtitle")).color(GRAY);
                            else
                                return translatable("msg-click-to").append(translatable("town-menu-town-set-change-spawn-subtitle")).color(GRAY);
                        })
                        .action(town == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_SET_SPAWN.getNode()) ? ClickAction.NONE : ClickAction.run(() -> {
                            final Town playerTown = TownyAPI.getInstance().getTown(player);
                            if (playerTown == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_SET_SPAWN.getNode()))
                                return;

                            try {
                                TownCommand.townSetSpawn(player, playerTown, false);
                            } catch (TownyException e) {
                                TownyMessaging.sendErrorMsg(player, e.getMessage(player));
                            }
                        }))
                        .build())
                .build();
    }

    public static MenuInventory formatTownBankMenu(final Player player) {
        final Town town = TownyAPI.getInstance().getTown(player);
        final Locale locale = Localization.localeOrDefault(player);

        final MenuInventory.Builder builder = MenuInventory.builder()
                .title(translatable("town-menu-bank"))
                .rows(3)
                .addItem(MenuHelper.backButton().build())
                .addItem(MenuItem.builder(Material.EMERALD_BLOCK)
                        .name(translatable("town-menu-bank-deposit-or-withdraw"))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(2)))
                        .lore(() -> {
                            if (town == null)
                                return translatable("msg-err-not-part-of-town");
                            else if (!TownyEconomyHandler.isActive())
                                return translatable("msg_err_no_economy").color(GRAY);
                            else
                                return translatable("msg-click-to").append(translatable("town-menu-bank-deposit-or-withdraw-subtitle"));
                        })
                        .action(town == null || !TownyEconomyHandler.isActive() ? ClickAction.NONE : ClickAction.openInventory(() -> GovernmentMenus.createDepositWithdrawMenu(player, town)))
                        .build())
                .addItem(MenuItem.builder(Material.WRITABLE_BOOK)
                        .name(translatable("town-menu-bank-transaction-history"))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromRight(2)))
                        .lore(() -> {
                            if (town == null)
                                return translatable("msg-err-not-part-of-town");
                            else if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_BANKHISTORY.getNode()))
                                return translatable("msg-no-permission-to").append(translatable("town-menu-bank-transaction-history"));
                            else
                                return translatable("msg-click-to").append(translatable("town-menu-bank-transaction-history-subtitle"));
                        })
                        .action(town != null && player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_BANKHISTORY.getNode())
                                ? ClickAction.openInventory(() -> createBankHistoryMenu(player, town)) : ClickAction.NONE)
                        .build());

        if (town != null && TownyEconomyHandler.isActive()) {
            builder.addItem(formatBankStatus(player, town.getAccount(), true)
                    .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(0), HorizontalAnchor.fromLeft(4)))
                    .build());
        }

        return builder.build();
    }

    public static MenuItem.Builder formatBankStatus(final Player player, final Account account, final boolean town) {
        final Locale locale = Localization.localeOrDefault(player);

        return MenuItem.builder(Material.OAK_SIGN)
                .name(translatable("town-menu-bank-status"))
                .lore(translatable("town-menu-bank-balance").append(text(TownyEconomyHandler.getFormattedBalance(account.getCachedBalance()), GREEN)))
                .lore(() -> {
                    if (!player.hasPermission((town ? PermissionNodes.TOWNY_COMMAND_TOWN_BANKHISTORY : PermissionNodes.TOWNY_COMMAND_NATION_BANKHISTORY).getNode()))
                        return Component.empty();

                    final List<BankTransaction> transactions = account.getAuditor().getTransactions();

                    if (transactions.isEmpty())
                        return translatable("town-menu-bank-last-transaction").append(translatable("town-menu-bank-no-transaction"));
                    else
                        // TODO: format as time ago when raw time is exposed in BankTransaction
                        return translatable("town-menu-bank-last-transaction").append(text(transactions.get(transactions.size() - 1).getTime(), GREEN));
                });
    }

    public static MenuItem.Builder formatTownInfo(Player player, Town town) {
        final Locale locale = Localization.localeOrDefault(player);

        if (town == null)
            return MenuItem.builder(Material.GRASS_BLOCK)
                    .name(translatable("town-menu-no-town").color(GREEN));

        List<Component> lore = new ArrayList<>();

        lore.add(translatable("town-menu-town-info-founded").append(Time.ago(town.getRegistered())).color(GREEN));
        lore.add(text(town.getNumResidents(), DARK_GREEN).append(translatable("town-menu-town-info-resident").append(text(town.getNumResidents() == 1 ? "" : "s")).color(GREEN)));

        if (TownySettings.isEconomyAsync() && TownyEconomyHandler.isActive())
            lore.add(translatable("town-menu-town-info-balance").append(text(TownyEconomyHandler.getFormattedBalance(town.getAccount().getCachedBalance()), GREEN)));

        if (town.getMayor() != null)
            lore.add(translatable("town-menu-town-info-owned-by").append(text(town.getMayor().getName(), GREEN)));

        if (town.getNationOrNull() != null)
            lore.add(translatable("town-menu-town-info-member-of").append(text(town.getNationOrNull().getName(), GREEN)));

        return MenuItem.builder(Material.GRASS_BLOCK)
                .name(text(town.getFormattedName(), GREEN))
                .lore(lore);
    }
}

