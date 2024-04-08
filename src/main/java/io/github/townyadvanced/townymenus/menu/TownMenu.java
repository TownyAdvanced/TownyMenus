package io.github.townyadvanced.townymenus.menu;

import com.palmergames.adventure.text.Component;
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
import com.palmergames.bukkit.towny.object.Translation;
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

import static com.palmergames.bukkit.towny.object.Translatable.of;
import static com.palmergames.adventure.text.Component.text;
import static com.palmergames.adventure.text.format.NamedTextColor.*;

public class TownMenu {
    public static MenuInventory createTownMenu(@NotNull Player player) {
        final Resident resident = TownyAPI.getInstance().getResident(player);
        final Town town = resident != null ? resident.getTownOrNull() : null;
        final Locale locale = Localization.localeOrDefault(player);

        return MenuInventory.builder()
                .rows(6)
                .title(of("town-menu-title", (town != null ? town.getName() : of("town-menu-no-town"))).component(locale))
                .addItem(MenuHelper.backButton().build())
                .addItem(formatTownInfo(player, town)
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(4)))
                        .build())
                .addItem(MenuItem.builder(Material.EMERALD_BLOCK)
                        .name(of("town-menu-bank").component(locale))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromBottom(1), HorizontalAnchor.fromLeft(3)))
                        .lore(() -> {
                            if (town == null)
                                return of("msg-err-not-part-of-town").component(locale);
                            else
                                return of("msg-click-to").append(of("town-menu-bank-subtitle")).component(locale);
                        })
                        .action(town == null ? ClickAction.NONE : ClickAction.openInventory(() -> formatTownBankMenu(player)))
                        .build())
                .addItem(MenuItem.builder(Material.GRASS_BLOCK)
                        .name(of("town-menu-plots").component(locale))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromBottom(1), HorizontalAnchor.fromRight(3)))
                        .lore(() -> {
                            if (town == null)
                                return of("msg-err-not-part-of-town").component(locale);
                            else if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_PLOTS.getNode()))
                                return of("msg-no-permission-to").append(of("town-menu-plots-subtitle")).component(locale);
                            else
                                return of("msg-click-to").append(of("town-menu-plots-subtitle")).component(locale);
                        })
                        .action(town == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_PLOTS.getNode()) ? ClickAction.NONE : ClickAction.openInventory(() -> {
                            if (!TownyUniverse.getInstance().hasTown(town.getUUID()))
                                return MenuInventory.paginator().title(of("town-menu-plots").component(locale)).build();

                            List<MenuItem> plotItems = new ArrayList<>();
                            TownBlockTypeCache cache = town.getTownBlockTypeCache();

                            for (TownBlockType type : TownBlockTypeHandler.getTypes().values()) {
                                int residentOwned = cache.getNumTownBlocks(type, CacheType.RESIDENTOWNED);

                                plotItems.add(MenuItem.builder(Material.GRASS_BLOCK)
                                        .name(text(type.getFormattedName(), GREEN))
                                        .lore(() -> of("town-menu-plots-resident-owned", residentOwned).component(locale))
                                        .lore(() -> of("town-menu-plots-for-sale", cache.getNumTownBlocks(type, CacheType.FORSALE)).component(locale))
                                        .lore(() -> of("town-menu-plots-total", cache.getNumTownBlocks(type, CacheType.ALL)).component(locale))
                                        .lore(!TownyEconomyHandler.isActive() ? Component.empty() : of("town-menu-plots-daily-revenue", cache.getNumTownBlocks(type, CacheType.ALL)).component(locale))
                                        .build());
                            }

                            return MenuInventory.paginator()
                                    .addItems(plotItems)
                                    .addExtraItem(MenuItem.builder(Material.OAK_SIGN)
                                            .name(of("town-menu-plots").component(locale))
                                            .slot(SlotAnchor.anchor(VerticalAnchor.fromBottom(0), HorizontalAnchor.fromLeft(1)))
                                            .lore(of("msg_town_plots_town_size", town.getTownBlocks().size(), town.getMaxTownBlocksAsAString()).component(locale)
                                                    .append(town.hasUnlimitedClaims()
                                                            ? Component.empty()
                                                            : TownySettings.isSellingBonusBlocks(town) ?
                                                            of("msg_town_plots_town_bought", town.getPurchasedBlocks(), TownySettings.getMaxPurchasedBlocks(town)).component(locale) :
                                                            Component.empty()
                                                                    .append(town.getBonusBlocks() > 0 ?
                                                                            of("msg_town_plots_town_bonus", town.getBonusBlocks()).component(locale) :
                                                                            Component.empty())
                                                                    .append(TownySettings.getNationBonusBlocks(town) > 0 ?
                                                                            of("msg_town_plots_town_nationbonus", TownySettings.getNationBonusBlocks(town)).component(locale) :
                                                                            Component.empty())))
                                            .lore(of("msg_town_plots_town_owned_land", town.getTownBlocks().size() - cache.getNumberOfResidentOwnedTownBlocks()).component(locale))
                                            .build())
                                    .title(of("town-menu-plots").component(locale))
                                    .build();
                        }))
                        .build())
                .addItem(MenuItem.builder(Material.RED_BED)
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(1)))
                        .name(of("town-menu-spawn").component(locale))
                        .lore(() -> {
                            if (town == null)
                                return of("msg-err-not-part-of-town").component(locale);
                            else if (!player.hasPermission("towny.town.spawn.town"))
                                return of("msg-no-permission").component(locale);
                            else
                                return of("msg-click-to").append(of("town-menu-spawn-subtitle")).component(locale);
                        })
                        .action(town == null || !player.hasPermission("towny.town.spawn.town") ? ClickAction.NONE : ClickAction.confirmation(() -> of("msg-click-to-confirm", "/town spawn").component(locale), ClickAction.run(() -> {
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
                        .name(of("town-menu-online").component(locale))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromBottom(2), HorizontalAnchor.fromLeft(1)))
                        .lore(() -> {
                            if (town == null)
                                return of("msg-err-not-part-of-town").component(locale);
                            else if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_ONLINE.getNode()))
                                return of("msg-no-permission-to").append(of("town-menu-online-subtitle")).component(locale);
                            else
                                return of("msg-click-to").append(of("town-menu-online-subtitle")).component(locale);
                        })
                        .action(town == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_ONLINE.getNode()) ? ClickAction.NONE : ClickAction.openInventory(() -> {
                            final Town playerTown = TownyAPI.getInstance().getTown(player);
                            if (playerTown == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_ONLINE.getNode()))
                                return MenuInventory.paginator().title(of("town-menu-online").component(locale)).build();

                            List<MenuItem> online = new ArrayList<>();

                            for (Player onlinePlayer : TownyAPI.getInstance().getOnlinePlayers(playerTown)) {
                                if (!player.canSee(onlinePlayer))
                                    continue;

                                online.add(ResidentMenu.formatResidentInfo(onlinePlayer.getUniqueId(), player).build());
                            }

                            return MenuInventory.paginator().addItems(online).title(of("town-menu-online").component(locale)).build();
                        }))
                        .build())
                .addItem(MenuItem.builder(Material.PLAYER_HEAD)
                        .name(of("town-menu-overview").component(locale))
                        .lore(() -> {
                            if (town == null)
                                return of("msg-err-not-part-of-town").component(locale);
                            else
                                return of("msg-click-to").append(of("town-menu-overview-subtitle")).component(locale);
                        })
                        .action(town == null ? ClickAction.NONE : ClickAction.openInventory(() -> createResidentOverview(player)))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromBottom(2), HorizontalAnchor.fromRight(1)))
                        .build())
                .addItem(MenuItem.builder(Material.GOLDEN_AXE)
                        .name(of("town-menu-management").component(locale))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromRight(1)))
                        .lore(() -> {
                            if (town == null)
                                return of("msg-err-not-part-of-town").component(locale);
                            else
                                return of("msg-click-to").append(of("town-menu-management-subtitle")).component(locale);
                        })
                        .action(town == null ? ClickAction.NONE : ClickAction.openInventory(() -> formatTownManagementMenu(player)))
                        .build())
                .build();
    }

    public static MenuInventory createBankHistoryMenu(Player player, Government government) {
        final Locale locale = Localization.localeOrDefault(player);
        if (government == null || !TownyEconomyHandler.isActive())
            return MenuInventory.paginator().title(of("town-menu-transaction-history").component(locale)).build();

        List<MenuItem> transactionItems = new ArrayList<>();
        List<BankTransaction> transactions = new ArrayList<>(government.getAccount().getAuditor().getTransactions());

        for (int i = 0; i < transactions.size(); i++) {
            BankTransaction transaction = transactions.get(i);
            boolean added = transaction.getType() == TransactionType.ADD || transaction.getType() == TransactionType.DEPOSIT;

            transactionItems.add(MenuItem.builder(added ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK)
                    .name(of("town-menu-transaction-number", i + 1, transaction.getTime()).component(locale).color(added ? GREEN : RED))
                    .lore(of("town-menu-transaction-amount", TownyEconomyHandler.getFormattedBalance(transaction.getAmount())).component(locale))
                    .lore(of("town-menu-transaction-new-balance", TownyEconomyHandler.getFormattedBalance(transaction.getBalance())).component(locale))
                    .lore(of("town-menu-transaction-reason", transaction.getReason()).component(locale))
                    .build());
        }

        Collections.reverse(transactionItems);

        return MenuInventory.paginator().addItems(transactionItems).title(of("town-menu-transaction-history").component(locale)).build();
    }

    public static MenuInventory createResidentOverview(Player player) {
        final Locale locale = Localization.localeOrDefault(player);
        Resident res = TownyAPI.getInstance().getResident(player);
        Town town = res != null ? res.getTownOrNull() : null;

        if (town == null)
            return MenuInventory.paginator().title(of("town-menu-overview").component(locale)).build();

        MenuInventory.PaginatorBuilder builder = MenuInventory.paginator()
                .title(of("town-menu-overview").append(" - ").append(town.getName()).component(locale));

        for (Resident resident : town.getResidents()) {
            builder.addItem(ResidentMenu.formatResidentInfo(resident, player)
                    .lore(of("town-menu-overview-joined-town").append(Time.registeredOrAgo(res.getJoinedTownAt(), Translation.getLocale(player))).component(locale))
                    .lore(Component.space())
                    .lore(of("msg-right-click-additional-options").component(locale))
                    .action(ClickAction.rightClick(ClickAction.openInventory(() -> createResidentManagementScreen(player, town, resident))))
                    .build());
        }

        return builder.build();
    }

    public static MenuInventory createResidentManagementScreen(Player player, Town town, Resident resident) {
        final Locale locale = Localization.localeOrDefault(player);

        return MenuInventory.builder()
                .rows(5)
                .title(of("town-menu-management-resident-title").component(locale))
                .addItem(MenuHelper.backButton().build())
                .addItem(MenuItem.builder(Material.WOODEN_AXE)
                        .name(of("town-menu-management-resident-kick-title").component(locale))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(1)))
                        .lore(() -> {
                            if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_KICK.getNode()))
                                return of("msg-no-permission-to" + "town-menu-management-resident-kick").component(locale);
                            else if (player.getUniqueId().equals(resident.getUUID()))
                                return of("town-menu-management-resident-cannot-kick-self").component(locale);
                            else
                                return of("msg-click-to").append(of("town-menu-management-resident-kick-subtitle")).component(locale);
                        })
                        .action(!player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_KICK.getNode()) || player.getUniqueId().equals(resident.getUUID()) ? ClickAction.NONE : ClickAction.confirmation(Component.text("Are you sure you want to kick " + resident.getName() + "?", GRAY), ClickAction.run(() -> {
                            if (town == null || !TownyUniverse.getInstance().hasTown(town.getUUID()) || !town.hasResident(resident))
                                return;

                            try {
                                TownCommand.townKick(player, new String[]{ resident.getName() });
                            } catch (TownyException e) {
                                TownyMessaging.sendErrorMsg(player, e.getMessage(player));
                            }
                        })))
                        .build())
                .addItem(ResidentMenu.formatResidentInfo(resident, player)
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(4)))
                        .build())
                .addItem(MenuItem.builder(Material.KNOWLEDGE_BOOK)
                        .name(of("town-menu-management-resident-ranks").component(locale))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromRight(1)))
                        .lore(of("msg-click-to").append(of("town-menu-management-resident-ranks-subtitle")).component(locale))
                        .action(ClickAction.openInventory(() -> formatRankManagementMenu(player, town, resident)))
                        .build())
                .addItem(MenuItem.builder(Material.NAME_TAG)
                        .name(of("town-menu-management-resident-title-title").component(locale))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(3), HorizontalAnchor.fromLeft(2)))
                        .lore(() -> {
                            if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_SET_TITLE.getNode()))
                                return of("msg-no-permission-to").append(of("town-menu-management-resident-title-title-subtitle")).component(locale).color(GRAY);
                            else
                                return Arrays.asList(of("msg-click-to").append(of("town-menu-management-resident-title-title-subtitle")).component(locale),
                                        of("msg-right-click-to").append(of("town-menu-management-resident-title-clear")).component(locale));
                        })
                        .action(!player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_SET_TITLE.getNode()) ? ClickAction.NONE : ClickAction.leftClick(ClickAction.userInput(of("town-menu-management-resident-enter-new-title").toString(), completion -> {
                            try {
                                BaseCommand.checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_SET_TITLE.getNode());
                                TownCommand.townSetTitle(player, new String[]{ resident.getName(), completion.getText() }, false);
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
                                TownCommand.townSetTitle(player, new String[]{ resident.getName(), "" }, false);
                            } catch (TownyException e) {
                                TownyMessaging.sendErrorMsg(player, e.getMessage(player));
                            }
                        })))
                        .build())
                .addItem(MenuItem.builder(Material.NAME_TAG)
                        .name(of("town-menu-management-resident-surname").component(locale))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(3), HorizontalAnchor.fromRight(2)))
                        .lore(() -> {
                            if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_SET_SURNAME.getNode()))
                                return of("msg-no-permission-to").append(of("town-menu-management-resident-surname-subtitle")).component(locale);
                            else
                                return Arrays.asList(of("msg-click-to").append(of("town-menu-management-resident-surname-subtitle")).component(locale),
                                        of("msg-right-click-to").append(of("town-menu-management-resident-surname-clear")).component(locale));
                        })
                        .action(!player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_SET_SURNAME.getNode()) ? ClickAction.NONE : ClickAction.leftClick(ClickAction.userInput(of("town-menu-management-resident-enter-new-surname").toString(), completion -> {
                            try {
                                BaseCommand.checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_SET_SURNAME.getNode());
                                TownCommand.townSetSurname(player, new String[]{ resident.getName(), completion.getText() }, false);
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
                                TownCommand.townSetSurname(player, new String[]{ resident.getName(), "" }, false);
                            } catch (TownyException e) {
                                TownyMessaging.sendErrorMsg(player, e.getMessage(player));
                            }
                        })))
                        .build())
                .build();
    }

    public static MenuInventory formatRankManagementMenu(Player player, Town town, Resident resident) {
        final Locale locale = Localization.localeOrDefault(player);

        MenuInventory.PaginatorBuilder paginator = MenuInventory.paginator().title(of("town-menu-management-rank").component(locale));

        for (String townRank : TownyPerms.getTownRanks()) {
            MenuItem.Builder item = MenuItem.builder(Material.KNOWLEDGE_BOOK)
                    .name(Component.text(townRank.substring(0, 1).toUpperCase(Locale.ROOT) + townRank.substring(1), GREEN));

            final boolean hasPermission = player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_RANK.getNode(townRank.toLowerCase(Locale.ROOT)));

            if (resident.hasTownRank(townRank)) {
                item.withGlint();

                if (hasPermission) {
                    item.lore(of("msg-click-to").append(of("rank-remove")).append(townRank).append(of("rank-from")).append(resident.getName()).append(text(".")).component(locale).color(GRAY));
                    item.action(ClickAction.confirmation(of("rank-remove-confirmation").append(townRank).append(of("from").append(resident.getName()).append("?")).component(locale).color(GRAY), ClickAction.run(() -> {
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
                            TownyMessaging.sendMsg(resident, of("msg_you_have_had_rank_taken", of("town_sing"), townRank));
                            Towny.getPlugin().deleteCache(resident);
                        }

                        TownyMessaging.sendMsg(player, of("msg_you_have_taken_rank_from", of("town_sing"), townRank, resident.getName()));
                        MenuHistory.reOpen(player, () -> formatRankManagementMenu(player, town, resident));
                    })));
                }
            } else if (hasPermission) {
                item.lore(of("msg-click-to-grant-the").append(townRank).append(of("rank-to")).append(resident.getName()).append(".").component(locale).color(GRAY));
                item.action(ClickAction.confirmation(of("rank-give-confirmation").append(townRank).append(of("to").append(resident.getName()).append("?")).component(locale).color(GRAY), ClickAction.run(() -> {
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
                        TownyMessaging.sendMsg(resident, of("msg_you_have_been_given_rank", of("town_sing"), townRank));
                        Towny.getPlugin().deleteCache(resident);
                    }

                    TownyMessaging.sendMsg(player, of("msg_you_have_given_rank", of("town_sing"), townRank, resident.getName()));
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
                .title(of("town-menu-management").component(locale))
                .addItem(MenuHelper.backButton().build())
                .addItem(MenuItem.builder(Material.GRASS_BLOCK)
                        .name(of("town-menu-management-town-menu-title").component(locale))
                        .lore(of("msg-click-to").append(of("town-menu-management-town-menu-subtitle")).component(locale))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(2)))
                        .action(ClickAction.openInventory(() -> formatTownSetMenu(player)))
                        .build())
                // TODO: Manage town permissions and trusted players
                .addItem(MenuItem.builder(Material.LEVER)
                        .name(of("town-menu-management-town-toggle-title").component(locale))
                        .lore(of("msg-click-to").append(of("town-menu-management-town-toggle-subtitle")).component(locale))
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
                .title(of("town-menu-management").component(locale))
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
                .title(of("town-menu-town-set-title").component(locale))
                .rows(3)
                .addItem(MenuHelper.backButton().build())
                .addItem(MenuItem.builder(Material.NAME_TAG)
                        .name(of("town-menu-town-set-change-name").component(locale))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(2)))
                        .lore(() -> {
                            if (town == null)
                                return of("msg-err-not-part-of-town").component(locale);
                            else if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_SET_NAME.getNode()))
                                return of("msg-no-permission-to").append(of("town-menu-town-set-change-name-subtitle")).component(locale);
                            else
                                return of("msg-click-to").append(of("town-menu-town-set-change-name-subtitle")).component(locale);
                        })
                        .action(town == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_SET_NAME.getNode()) ? ClickAction.NONE : ClickAction.userInput(of("town-menu-town-set-enter-town-name").toString(), completion -> {
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
                        .name(of("town-menu-town-set-change-board").component(locale))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(4)))
                        .lore(() -> {
                            if (town == null)
                                return of("msg-err-not-part-of-town").component(locale);
                            else if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_SET_BOARD.getNode()))
                                return of("msg-no-permission-to").append(of("town-menu-town-set-change-board-subtitle")).component(locale).color(GRAY);
                            else
                                return Arrays.asList(of("msg-click-to").append(of("town-menu-town-set-change-board-subtitle")).component(locale).color(GRAY),
                                        of("msg-right-click-to").append(of("town-menu-town-set-change-board-subtitle")).component(locale).color(GRAY));
                        })
                        .action(town == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_SET_BOARD.getNode()) ? ClickAction.NONE : ClickAction.leftClick(ClickAction.userInput(of("town-menu-town-set-enter-town-board").toString(), completion -> {
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
                        .name(of("town-menu-town-set-change-spawn").component(locale))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromRight(2)))
                        .lore(() -> {
                            if (town == null)
                                return of("msg-err-not-part-of-town").component(locale);
                            else if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_SET_SPAWN.getNode()))
                                return of("msg-no-permission-to").append(of("town-menu-town-set-change-spawn-subtitle")).component(locale).color(GRAY);
                            else
                                return of("msg-click-to").append(of("town-menu-town-set-change-spawn-subtitle")).component(locale).color(GRAY);
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
                .title(of("town-menu-bank").component(locale))
                .rows(3)
                .addItem(MenuHelper.backButton().build())
                .addItem(MenuItem.builder(Material.EMERALD_BLOCK)
                        .name(of("town-menu-bank-deposit-or-withdraw").component(locale))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(2)))
                        .lore(() -> {
                            if (town == null)
                                return of("msg-err-not-part-of-town").component(locale);
                            else if (!TownyEconomyHandler.isActive())
                                return of("msg_err_no_economy").component(locale).color(GRAY);
                            else
                                return of("msg-click-to").append(of("town-menu-bank-deposit-or-withdraw-subtitle")).component(locale);
                        })
                        .action(town == null || !TownyEconomyHandler.isActive() ? ClickAction.NONE : ClickAction.openInventory(() -> GovernmentMenus.createDepositWithdrawMenu(player, town)))
                        .build())
                .addItem(MenuItem.builder(Material.WRITABLE_BOOK)
                        .name(of("town-menu-bank-transaction-history").component(locale))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromRight(2)))
                        .lore(() -> {
                            if (town == null)
                                return of("msg-err-not-part-of-town").component(locale);
                            else if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_BANKHISTORY.getNode()))
                                return of("msg-no-permission-to").append(of("town-menu-bank-transaction-history")).component(locale);
                            else
                                return of("msg-click-to").append(of("town-menu-bank-transaction-history-subtitle")).component(locale);
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
                .name(of("town-menu-bank-status").component(locale))
                .lore(of("town-menu-bank-balance").component(locale).append(text(TownyEconomyHandler.getFormattedBalance(account.getCachedBalance()), GREEN)))
                .lore(() -> {
                    if (!player.hasPermission((town ? PermissionNodes.TOWNY_COMMAND_TOWN_BANKHISTORY : PermissionNodes.TOWNY_COMMAND_NATION_BANKHISTORY).getNode()))
                        return Component.empty();

                    final List<BankTransaction> transactions = account.getAuditor().getTransactions();

                    if (transactions.isEmpty())
                        return of("town-menu-bank-last-transaction").append(of("town-menu-bank-no-transaction")).component(locale);
                    else
                        // TODO: format as time ago when raw time is exposed in BankTransaction
                        return of("town-menu-bank-last-transaction").component(locale).append(text(transactions.get(transactions.size() - 1).getTime(), GREEN));
                });
    }

    public static MenuItem.Builder formatTownInfo(Player player,Town town) {
        final Locale locale = Localization.localeOrDefault(player);

        if (town == null)
            return MenuItem.builder(Material.GRASS_BLOCK)
                    .name(of("town-menu-no-town").component(locale).color(GREEN));

        List<Component> lore = new ArrayList<>();

        lore.add(of("town-menu-town-info-founded").append(Time.ago(town.getRegistered()).translate()).component(locale).color(GREEN));
        lore.add(text(town.getNumResidents(), DARK_GREEN).append(of("town-menu-town-info-resident").append(town.getNumResidents() == 1 ? "" : "s").component(locale).color(GREEN)));

        if (TownySettings.isEconomyAsync() && TownyEconomyHandler.isActive())
            lore.add(of("town-menu-town-info-balance").component(locale).append(text(TownyEconomyHandler.getFormattedBalance(town.getAccount().getCachedBalance()), GREEN)));

        if (town.getMayor() != null)
            lore.add(of("town-menu-town-info-owned-by").component(locale).append(text(town.getMayor().getName(), GREEN)));

        if (town.getNationOrNull() != null)
            lore.add(of("town-menu-town-info-member-of").component(locale).append(text(town.getNationOrNull().getName(), GREEN)));

        return MenuItem.builder(Material.GRASS_BLOCK)
                .name(text(town.getFormattedName(), GREEN))
                .lore(lore);
    }
}

