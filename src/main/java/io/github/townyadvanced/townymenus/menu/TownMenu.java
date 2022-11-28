package io.github.townyadvanced.townymenus.menu;

import com.palmergames.adventure.text.Component;
import com.palmergames.adventure.text.format.NamedTextColor;
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
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.economy.BankTransaction;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import io.github.townyadvanced.townymenus.gui.MenuHelper;
import io.github.townyadvanced.townymenus.gui.MenuHistory;
import io.github.townyadvanced.townymenus.gui.MenuInventory;
import io.github.townyadvanced.townymenus.gui.MenuItem;
import io.github.townyadvanced.townymenus.gui.action.ClickAction;
import io.github.townyadvanced.townymenus.gui.anchor.HorizontalAnchor;
import io.github.townyadvanced.townymenus.gui.anchor.SlotAnchor;
import io.github.townyadvanced.townymenus.gui.anchor.VerticalAnchor;
import io.github.townyadvanced.townymenus.listeners.AwaitingConfirmation;
import io.github.townyadvanced.townymenus.menu.helper.GovernmentMenus;
import io.github.townyadvanced.townymenus.utils.Time;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class TownMenu {
    public static MenuInventory createTownMenu(@NotNull Player player) {
        Resident resident = TownyAPI.getInstance().getResident(player);
        Town town = resident != null ? resident.getTownOrNull() : null;

        return MenuInventory.builder()
                .rows(6)
                .title(Component.text("Town Menu - " + (town != null ? town.getName() : "No Town")))
                .addItem(MenuHelper.backButton().build())
                .addItem(formatTownInfo(town)
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(4)))
                        .build())
                .addItem(MenuItem.builder(Material.WRITABLE_BOOK)
                        .name(Component.text("Transaction History", NamedTextColor.GREEN))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromBottom(1), HorizontalAnchor.fromLeft(3)))
                        .lore(() -> {
                            if (town == null)
                                return Component.text("You are not part of a town.", NamedTextColor.GRAY);
                            else if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_BANKHISTORY.getNode()))
                                return Component.text("You do not have permission to view the town's transaction history.", NamedTextColor.GRAY);
                            else
                                return Component.text("Click to view the town's transaction history.", NamedTextColor.GRAY);
                        })
                        .action(town != null && player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_BANKHISTORY.getNode())
                            ? ClickAction.openInventory(() -> createBankHistoryMenu(town)) : ClickAction.NONE)
                        .build())
                .addItem(MenuItem.builder(Material.GRASS_BLOCK)
                        .name(Component.text("Town Plots", NamedTextColor.GREEN))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromBottom(1), HorizontalAnchor.fromRight(3)))
                        .lore(() -> {
                            if (town == null)
                                return Component.text("You are not part of a town.", NamedTextColor.GRAY);
                            else if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_PLOTS.getNode()))
                                return Component.text("You do not have permission to view the town's plots.", NamedTextColor.GRAY);
                            else
                                return Component.text("Click to view the town's plots.", NamedTextColor.GRAY);
                        })
                        .action(town == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_PLOTS.getNode()) ? ClickAction.NONE : ClickAction.openInventory(() -> {
                            if (!TownyUniverse.getInstance().hasTown(town.getUUID()))
                                return MenuInventory.paginator().title(Component.text("Town Plots")).build();

                            List<MenuItem> plotItems = new ArrayList<>();
                            TownBlockTypeCache cache = town.getTownBlockTypeCache();

                            for (TownBlockType type : TownBlockTypeHandler.getTypes().values()) {
                                int residentOwned = cache.getNumTownBlocks(type, CacheType.RESIDENTOWNED);

                                plotItems.add(MenuItem.builder(Material.GRASS_BLOCK)
                                        .name(Component.text(type.getFormattedName(), NamedTextColor.GREEN))
                                        .lore(Component.text("Resident Owned: ", NamedTextColor.DARK_GREEN).append(Component.text(residentOwned, NamedTextColor.GREEN)))
                                        .lore(Component.text("For Sale: ", NamedTextColor.DARK_GREEN).append(Component.text(cache.getNumTownBlocks(type, CacheType.FORSALE), NamedTextColor.GREEN)))
                                        .lore(Component.text("Total: ", NamedTextColor.DARK_GREEN).append(Component.text(cache.getNumTownBlocks(type, CacheType.ALL), NamedTextColor.GREEN)))
                                        .lore(!TownyEconomyHandler.isActive() ? Component.empty() : Component.text("Daily Revenue: ", NamedTextColor.DARK_GREEN).append(Component.text(TownyEconomyHandler.getFormattedBalance(residentOwned * type.getTax(town)), NamedTextColor.GREEN)))
                                        .build());
                            }

                            return MenuInventory.paginator()
                                    .addItems(plotItems)
                                    .addExtraItem(MenuItem.builder(Material.OAK_SIGN)
                                            .name(Component.text("Town Plots", NamedTextColor.DARK_GREEN))
                                            .slot(SlotAnchor.anchor(VerticalAnchor.fromBottom(0), HorizontalAnchor.fromLeft(1)))
                                            .lore(Component.text("Town Size: ", NamedTextColor.DARK_GREEN).append(Component.text(town.getTownBlocks().size() + " / " + town.getMaxTownBlocksAsAString(), NamedTextColor.GREEN))
                                                    .append(town.hasUnlimitedClaims()
                                                            ? Component.empty()
                                                            : TownySettings.isSellingBonusBlocks(town) ?
                                                            Component.text(" [Bought: " + town.getPurchasedBlocks() + "/" + TownySettings.getMaxPurchasedBlocks(town) + "]", NamedTextColor.AQUA) :
                                                            Component.empty()
                                                                    .append(town.getBonusBlocks() > 0 ?
                                                                            Component.text(" [Bonus: " + town.getBonusBlocks() + "]", NamedTextColor.AQUA) :
                                                                            Component.empty())
                                                                    .append(TownySettings.getNationBonusBlocks(town) > 0 ?
                                                                            Component.text(" [NationBonus: " + TownySettings.getNationBonusBlocks(town) + "]", NamedTextColor.AQUA) :
                                                                            Component.empty())))
                                            .lore(Component.text("Town Owned Land: ", NamedTextColor.DARK_GREEN).append(Component.text(town.getTownBlocks().size() - cache.getNumberOfResidentOwnedTownBlocks(), NamedTextColor.GREEN)))
                                            .build())
                                    .title(Component.text("Town Plots"))
                                    .build();
                        }))
                        .build())
                .addItem(MenuItem.builder(Material.RED_BED)
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(1)))
                        .name(Component.text("Town Spawn", NamedTextColor.GREEN))
                        .lore(() -> {
                            if (town == null)
                                return Component.text("You are not part of a town.", NamedTextColor.GRAY);
                            else if (!player.hasPermission("towny.town.spawn.town"))
                                return Component.text("You do not have permission to use this.", NamedTextColor.GRAY);
                            else
                                return Component.text("Click to teleport to your town's spawn.", NamedTextColor.GRAY);
                        })
                        .action(town == null || !player.hasPermission("towny.town.spawn.town") ? ClickAction.NONE : ClickAction.confirmation(() -> Component.text("Click to confirm using /town spawn.", NamedTextColor.GRAY), ClickAction.run(() -> {
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
                        .name(Component.text("Online in Town", NamedTextColor.GREEN))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromBottom(2), HorizontalAnchor.fromLeft(1)))
                        .lore(() -> {
                            if (town == null)
                                return Component.text("You are not part of a town.", NamedTextColor.GRAY);
                            else if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_ONLINE.getNode()))
                                return Component.text("You do not have permission to view the town's online player list.", NamedTextColor.GRAY);
                            else
                                return Component.text("Click to view online players in the town.", NamedTextColor.GRAY);
                        })
                        .action(town == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_ONLINE.getNode()) ? ClickAction.NONE : ClickAction.openInventory(() -> {
                            final Town playerTown = TownyAPI.getInstance().getTown(player);
                            if (playerTown == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_ONLINE.getNode()))
                                return MenuInventory.paginator().title(Component.text("Online in Town")).build();

                            List<MenuItem> online = new ArrayList<>();

                            for (Player onlinePlayer : TownyAPI.getInstance().getOnlinePlayers(playerTown)) {
                                if (!player.canSee(onlinePlayer))
                                    continue;

                                online.add(ResidentMenu.formatResidentInfo(onlinePlayer.getUniqueId(), player).build());
                            }

                            return MenuInventory.paginator().addItems(online).title(Component.text("Online in Town")).build();
                        }))
                        .build())
                .addItem(MenuItem.builder(Material.PLAYER_HEAD)
                        .name(Component.text("Resident Overview", NamedTextColor.GREEN))
                        .lore(() -> {
                            if (town == null)
                                return Component.text("You are not part of a town.", NamedTextColor.GRAY);
                            else
                                return Component.text("Click to view and manage residents in your town.", NamedTextColor.GRAY);
                        })
                        .action(town == null ? ClickAction.NONE : ClickAction.openInventory(() -> createResidentOverview(player)))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromBottom(2), HorizontalAnchor.fromRight(1)))
                        .build())
                .addItem(MenuItem.builder(Material.GOLDEN_AXE)
                        .name(Component.text("Town Management", NamedTextColor.GREEN))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromRight(1)))
                        .lore(() -> {
                            if (town == null)
                                return Component.text("You are not part of a town.", NamedTextColor.GRAY);
                            else
                                return Component.text("Click to open the town management menu.", NamedTextColor.GRAY);
                        })
                        .action(town == null ? ClickAction.NONE : ClickAction.openInventory(() -> formatTownManagementMenu(player)))
                        .build())
                .build();
    }

    public static MenuInventory createBankHistoryMenu(Government government) {
        if (government == null || !TownyEconomyHandler.isActive())
            return MenuInventory.paginator().title(Component.text("Transaction History")).build();

        List<MenuItem> transactionItems = new ArrayList<>();
        List<BankTransaction> transactions = new ArrayList<>(government.getAccount().getAuditor().getTransactions());

        for (int i = 0; i < transactions.size(); i++) {
            BankTransaction transaction = transactions.get(i);
            boolean added = transaction.getType() == TransactionType.ADD || transaction.getType() == TransactionType.DEPOSIT;

            transactionItems.add(MenuItem.builder(added ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK)
                    .name(Component.text("Transaction #" + (i + 1) + " - " + transaction.getTime(), added ? NamedTextColor.GREEN : NamedTextColor.RED))
                    .lore(Component.text("Amount: ", NamedTextColor.DARK_GREEN).append(Component.text(TownyEconomyHandler.getFormattedBalance(transaction.getAmount()), NamedTextColor.GREEN)))
                    .lore(Component.text("New balance: ", NamedTextColor.DARK_GREEN).append(Component.text(TownyEconomyHandler.getFormattedBalance(transaction.getBalance()), NamedTextColor.GREEN)))
                    .lore(Component.text("Reason: ", NamedTextColor.DARK_GREEN).append(Component.text(transaction.getReason(), NamedTextColor.GREEN)))
                    .build());
        }

        Collections.reverse(transactionItems);

        return MenuInventory.paginator().addItems(transactionItems).title(Component.text("Transaction History")).build();
    }

    public static MenuInventory createResidentOverview(Player player) {
        Resident res = TownyAPI.getInstance().getResident(player);
        Town town = res != null ? res.getTownOrNull() : null;

        if (town == null)
            return MenuInventory.paginator().title(Component.text("Resident Overview")).build();

        MenuInventory.PaginatorBuilder builder = MenuInventory.paginator()
                .title(Component.text("Resident Overview - " + town.getName()));

        for (Resident resident : town.getResidents()) {
            builder.addItem(ResidentMenu.formatResidentInfo(resident, player)
                    .lore(Component.text("Joined town ", NamedTextColor.DARK_GREEN).append(Component.text(Time.registeredOrAgo(res.getJoinedTownAt()), NamedTextColor.GREEN)))
                    .lore(Component.text(" "))
                    .lore(Component.text("Right click to view additional options.", NamedTextColor.GRAY))
                    .action(ClickAction.rightClick(ClickAction.openInventory(() -> createResidentManagementScreen(player, town, resident))))
                    .build());
        }

        return builder.build();
    }

    public static MenuInventory createResidentManagementScreen(Player player, Town town, Resident resident) {
        return MenuInventory.builder()
                .rows(5)
                .title(Component.text("Resident Management"))
                .addItem(MenuHelper.backButton().build())
                .addItem(MenuItem.builder(Material.WOODEN_AXE)
                        .name(Component.text("Kick Resident", NamedTextColor.GREEN))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(1)))
                        .lore(() -> {
                            if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_KICK.getNode()))
                                return Component.text("You do not have permission to kick this resident.", NamedTextColor.GRAY);
                            else if (player.getUniqueId().equals(resident.getUUID()))
                                return Component.text("You cannot kick yourself!", NamedTextColor.GRAY);
                            else
                                return Component.text("Click to kick this player from the town.", NamedTextColor.GRAY);
                        })
                        .action(!player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_KICK.getNode()) || player.getUniqueId().equals(resident.getUUID()) ? ClickAction.NONE : ClickAction.confirmation(Component.text("Are you sure you want to kick " + resident.getName() + "?", NamedTextColor.GRAY), ClickAction.run(() -> {
                            if (town == null || !TownyUniverse.getInstance().hasTown(town.getUUID()) || !town.hasResident(resident))
                                return;

                            TownCommand.townKickResidents(player, TownyAPI.getInstance().getResident(player), town, Collections.singletonList(resident));
                        })))
                        .build())
                .addItem(ResidentMenu.formatResidentInfo(resident, player)
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(4)))
                        .build())
                .addItem(MenuItem.builder(Material.KNOWLEDGE_BOOK)
                        .name(Component.text("Manage Ranks", NamedTextColor.GREEN))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromRight(1)))
                        .lore(Component.text("Click to manage ranks for this player.", NamedTextColor.GRAY))
                        .action(ClickAction.openInventory(() -> formatRankManagementMenu(player, town, resident)))
                        .build())
                .addItem(MenuItem.builder(Material.NAME_TAG)
                        .name(Component.text("Town Title", NamedTextColor.GREEN))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(3), HorizontalAnchor.fromLeft(2)))
                        .lore(() -> {
                            if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_SET_TITLE.getNode()))
                                return Component.text("You do not have permission to change this resident's title.");
                            else
                                return Arrays.asList(Component.text("Click to change this resident's title.", NamedTextColor.GRAY),
                                        Component.text("Right click to clear this resident's title.", NamedTextColor.GRAY));
                        })
                        .action(!player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_SET_TITLE.getNode()) ? ClickAction.NONE : ClickAction.leftClick(ClickAction.userInput("Enter new title", (title) -> {
                            try {
                                BaseCommand.checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_SET_TITLE.getNode());
                                TownCommand.townSetTitle(player, resident, title, false);
                            } catch (TownyException e) {
                                TownyMessaging.sendErrorMsg(player, e.getMessage(player));
                                return AnvilGUI.Response.text(e.getMessage(player));
                            }

                            MenuHistory.last(player);
                            return AnvilGUI.Response.close();
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
                        .name(Component.text("Town Surname", NamedTextColor.GREEN))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(3), HorizontalAnchor.fromRight(2)))
                        .lore(() -> {
                            if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_SET_SURNAME.getNode()))
                                return Collections.singletonList(Component.text("You do not have permission to change this resident's surname."));
                            else
                                return Arrays.asList(Component.text("Click to change this resident's surname.", NamedTextColor.GRAY),
                                        Component.text("Right click to clear this resident's surname.", NamedTextColor.GRAY));
                        })
                        .action(!player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_SET_SURNAME.getNode()) ? ClickAction.NONE : ClickAction.leftClick(ClickAction.userInput("Enter new surname", (surname) -> {
                            try {
                                BaseCommand.checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_SET_SURNAME.getNode());
                                TownCommand.townSetSurname(player, resident, surname, false);
                            } catch (TownyException e) {
                                TownyMessaging.sendErrorMsg(player, e.getMessage(player));
                                return AnvilGUI.Response.text(e.getMessage(player));
                            }

                            MenuHistory.last(player);
                            return AnvilGUI.Response.close();
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
        MenuInventory.PaginatorBuilder paginator = MenuInventory.paginator().title(Component.text("Rank Management"));

        for (String townRank : TownyPerms.getTownRanks()) {
            MenuItem.Builder item = MenuItem.builder(Material.KNOWLEDGE_BOOK)
                    .name(Component.text(townRank.substring(0, 1).toUpperCase(Locale.ROOT) + townRank.substring(1), NamedTextColor.GREEN));

            final boolean hasPermission = player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_RANK.getNode(townRank.toLowerCase(Locale.ROOT)));

            if (resident.hasTownRank(townRank)) {
                item.withGlint();

                if (hasPermission) {
                    item.lore(Component.text("Click to remove the " + townRank + " rank from " + resident.getName() + ".", NamedTextColor.GRAY));
                    item.action(ClickAction.confirmation(Component.text("Are you sure you want to remove the rank of " + townRank + " from " + resident.getName() + "?", NamedTextColor.GRAY), ClickAction.run(() -> {
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
                item.lore(Component.text("Click to grant the " + townRank + " rank to " + resident.getName() + ".", NamedTextColor.GRAY));
                item.action(ClickAction.confirmation(Component.text("Are you sure you want to give the rank of " + townRank + " to " + resident.getName() + "?", NamedTextColor.GRAY), ClickAction.run(() -> {
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
        return MenuInventory.builder()
                .rows(3)
                .title(Component.text("Town Management"))
                .addItem(MenuHelper.backButton().build())
                .addItem(MenuItem.builder(Material.GRASS_BLOCK)
                        .name(Component.text("Open town set menu", NamedTextColor.GREEN))
                        .lore(Component.text("Click to open the town set menu.", NamedTextColor.GRAY))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(2)))
                        .action(ClickAction.openInventory(() -> formatTownSetMenu(player)))
                        .build())
                // TODO: Manage town permissions and trusted players
                .addItem(MenuItem.builder(Material.LEVER)
                        .name(Component.text("Open town toggle menu", NamedTextColor.GREEN))
                        .lore(Component.text("Click to open the town toggle menu.", NamedTextColor.GRAY))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromRight(2)))
                        .action(ClickAction.openInventory(() -> formatTownToggleMenu(player)))
                        .build())
                .build();
    }

    public static MenuInventory formatTownToggleMenu(Player player) {
        Town town = TownyAPI.getInstance().getTown(player);

        final boolean fireEnabled = town != null && town.isFire();
        final boolean explosionEnabled = town != null && town.isExplosion();
        final boolean mobsEnabled = town != null && town.hasMobs();
        final boolean pvpEnabled = town != null && town.isPVP();

        final boolean isOpen = town != null && town.isOpen();
        final boolean isPublic = town != null && town.isPublic();

        return MenuInventory.builder()
                .title(Component.text("Town Toggle"))
                .rows(4)
                .addItem(MenuHelper.backButton().build())
                // Fire
                .addItem(GovernmentMenus.createTogglePropertyItem(player, town, Material.FLINT_AND_STEEL, fireEnabled, "fire")
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(1)))
                        .build())
                .addItem(MenuItem.builder(fireEnabled ? Material.GREEN_CONCRETE : Material.RED_CONCRETE)
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(2), HorizontalAnchor.fromLeft(1)))
                        .name(Component.empty())
                        .build())
                // Explosion
                .addItem(GovernmentMenus.createTogglePropertyItem(player, town, Material.TNT, explosionEnabled, "explosion")
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(2)))
                        .build())
                .addItem(MenuItem.builder(explosionEnabled ? Material.GREEN_CONCRETE : Material.RED_CONCRETE)
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
        final Town town = TownyAPI.getInstance().getTown(player);

        return MenuInventory.builder()
                .title(Component.text("Town Set Menu"))
                .rows(3)
                .addItem(MenuHelper.backButton().build())
                .addItem(MenuItem.builder(Material.NAME_TAG)
                        .name(Component.text("Change town name", NamedTextColor.GREEN))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(2)))
                        .lore(() -> {
                            if (town == null)
                                return Component.text("You are not part of a town.", NamedTextColor.GRAY);
                            else if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_SET_NAME.getNode()))
                                return Component.text("You do not have permission to change the town's name.", NamedTextColor.GRAY);
                            else
                                return Component.text("Click to change the town's name.", NamedTextColor.GRAY);
                        })
                        .action(town == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_SET_NAME.getNode()) ? ClickAction.NONE : ClickAction.userInput("Enter town name", title -> {
                            final Town playerTown = TownyAPI.getInstance().getTown(player);
                            if (playerTown == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_SET_NAME.getNode()))
                                return AnvilGUI.Response.close();

                            AwaitingConfirmation.await(player);

                            try {
                                TownCommand.townSetName(player, new String[]{"", title}, town);
                            } catch (TownyException e) {
                                TownyMessaging.sendErrorMsg(player, e.getMessage(player));
                                return AnvilGUI.Response.text(e.getMessage(player));
                            }

                            MenuHistory.reOpen(player, () -> formatTownSetMenu(player));
                            return AnvilGUI.Response.text("");
                        }))
                        .build())
                .addItem(MenuItem.builder(Material.OAK_SIGN)
                        .name(Component.text("Change town board", NamedTextColor.GREEN))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(4)))
                        .lore(() -> {
                            if (town == null)
                                return Component.text("You are not part of a town.", NamedTextColor.GRAY);
                            else if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_SET_BOARD.getNode()))
                                return Component.text("You do not have permission to change the town's board.", NamedTextColor.GRAY);
                            else
                                return Arrays.asList(Component.text("Click to change the town's board.", NamedTextColor.GRAY),
                                        Component.text("Right click to clear the town's board.", NamedTextColor.GRAY));
                        })
                        .action(town == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_SET_BOARD.getNode()) ? ClickAction.NONE : ClickAction.leftClick(ClickAction.userInput("Enter town board", board -> {
                            final Town playerTown = TownyAPI.getInstance().getTown(player);
                            if (playerTown == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_SET_BOARD.getNode()))
                                return AnvilGUI.Response.close();

                            try {
                                TownCommand.townSetBoard(player, board, playerTown);
                            } catch (TownyException e) {
                                TownyMessaging.sendErrorMsg(player, e.getMessage(player));
                                return AnvilGUI.Response.text(e.getMessage(player));
                            }

                            MenuHistory.reOpen(player, () -> formatTownSetMenu(player));
                            return AnvilGUI.Response.text("");
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
                        .name(Component.text("Change town spawn", NamedTextColor.GREEN))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromRight(2)))
                        .lore(() -> {
                            if (town == null)
                                return Component.text("You are not part of a town.", NamedTextColor.GRAY);
                            else if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_SET_SPAWN.getNode()))
                                return Component.text("You do not have permission to change the town's spawn.", NamedTextColor.GRAY);
                            else
                                return Component.text("Click to change the town's spawn.", NamedTextColor.GRAY);
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

    public static MenuItem.Builder formatTownInfo(Town town) {
        if (town == null)
            return MenuItem.builder(Material.GRASS_BLOCK)
                    .name(Component.text("No Town", NamedTextColor.GREEN));

        List<Component> lore = new ArrayList<>();

        lore.add(Component.text("Founded ", NamedTextColor.DARK_GREEN).append(Component.text(Time.ago(town.getRegistered()), NamedTextColor.GREEN)));
        lore.add(Component.text(town.getNumResidents(), NamedTextColor.DARK_GREEN).append(Component.text(" Resident" + (town.getNumResidents() == 1 ? "" : "s"), NamedTextColor.GREEN)));

        if (town.getMayor() != null)
            lore.add(Component.text("Owned by ", NamedTextColor.DARK_GREEN).append(Component.text(town.getMayor().getName(), NamedTextColor.GREEN)));

        if (town.getNationOrNull() != null)
            lore.add(Component.text("Member of ", NamedTextColor.DARK_GREEN).append(Component.text(town.getNationOrNull().getName(), NamedTextColor.GREEN)));

        return MenuItem.builder(Material.GRASS_BLOCK) // TODO: placeholder item
                .name(Component.text(town.getFormattedName(), NamedTextColor.GREEN))
                .lore(lore);
    }
}
