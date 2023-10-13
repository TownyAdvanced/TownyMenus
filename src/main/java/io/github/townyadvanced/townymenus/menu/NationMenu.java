package io.github.townyadvanced.townymenus.menu;

import com.palmergames.bukkit.towny.object.Translatable;
import net.kyori.adventure.text.Component;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.command.BaseCommand;
import com.palmergames.bukkit.towny.command.NationCommand;
import com.palmergames.bukkit.towny.event.nation.NationRankAddEvent;
import com.palmergames.bukkit.towny.event.nation.NationRankRemoveEvent;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
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
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public class NationMenu {
    public static MenuInventory createNationMenu(@NotNull Player player) {
        final Locale locale = Localization.localeOrDefault(player);
        Nation nation = TownyAPI.getInstance().getNation(player);

        return MenuInventory.builder()
                .title(translatable("nation-menu-title"))
                .rows(6)
                .addItem(MenuHelper.backButton().build())
                .addItem(MenuItem.builder(Material.EMERALD_BLOCK)
                        .name(translatable("nation-menu-bank-title"))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromBottom(1), HorizontalAnchor.fromLeft(3)))
                        .lore(() -> {
                            if (nation == null)
                                return translatable("msg-err-not-part-of-nation");
                            else
                                return translatable("msg-click-to").append(translatable("nation-menu-view-bank"));
                        })
                        .action(nation == null ? ClickAction.NONE : ClickAction.openInventory(() -> formatNationBankMenu(player)))
                        .build())
                .addItem(MenuItem.builder(Material.RED_BED)
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(1)))
                        .name(translatable("nation-menu-nation-spawn"))
                        .lore(() -> {
                            if (nation == null)
                                return translatable("msg-err-not-part-of-nation");
                            else if (!player.hasPermission("towny.nation.spawn.nation"))
                                return translatable("msg-no-permission");
                            else
                                return translatable("msg-click-to").append(translatable("nation-menu-teleport-to-spawn")).color(GRAY);
                        })
                        .action(nation == null || !player.hasPermission("towny.nation.spawn.nation") ? ClickAction.NONE : ClickAction.confirmation(() -> translatable("nation-menu-teleport-confirm"), ClickAction.run(() -> {
                            if (!player.hasPermission("towny.nation.spawn.nation"))
                                return;

                            try {
                                NationCommand.nationSpawn(player, new String[]{}, false);
                            } catch (TownyException e) {
                                TownyMessaging.sendErrorMsg(player, e.getMessage(player));
                            }

                            player.closeInventory();
                        })))
                        .build())
                .addItem(MenuItem.builder(Material.LEVER)
                        .name(translatable("nation-menu-toggle-title"))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromRight(1)))
                        .lore(() -> {
                            if (nation == null)
                                return translatable("msg-must-be-in-nation-to").append(translatable("nation-menu-toggle-open")).color(GRAY);
                            else
                                return translatable("msg-click-to").append(translatable("nation-menu-toggle-open")).color(GRAY);
                        })
                        .action(nation == null ? ClickAction.NONE : ClickAction.openInventory(() -> formatNationToggleMenu(player)))
                        .build())
                .addItem(MenuItem.builder(Material.GRASS_BLOCK)
                        .name(translatable("nation-menu-set-title"))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromBottom(1), HorizontalAnchor.fromRight(3)))
                        .lore(translatable("msg-click-to").append(translatable("nation-menu-set-open")).color(GRAY))
                        .action(ClickAction.openInventory(() -> formatNationSetMenu(player)))
                        .build())
                .addItem(MenuItem.builder(Material.ENDER_EYE)
                        .name(translatable("nation-menu-online"))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromBottom(2), HorizontalAnchor.fromLeft(1)))
                        .lore(() -> {
                            if (nation == null)
                                return translatable("msg-err-not-part-of-nation");
                            else if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_ONLINE.getNode()))
                                return translatable("msg-no-permission-to").append(translatable("nation-menu-view-online")).color(GRAY);
                            else
                                return translatable("msg-click-to").append(translatable("nation-menu-view-online")).color(GRAY);
                        })
                        .action(nation == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_ONLINE.getNode()) ? ClickAction.NONE : ClickAction.openInventory(() -> {
                            final Nation playerNation = TownyAPI.getInstance().getNation(player);
                            if (playerNation == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_ONLINE.getNode()))
                                return MenuInventory.paginator().title(translatable("nation-menu-online")).build();

                            List<MenuItem> online = new ArrayList<>();
                            for (Player onlinePlayer : TownyAPI.getInstance().getOnlinePlayers(playerNation)) {
                                if (!player.canSee(onlinePlayer))
                                    continue;

                                online.add(ResidentMenu.formatResidentInfo(onlinePlayer.getUniqueId(), player).build());
                            }

                            return MenuInventory.paginator().title(translatable("nation-menu-online")).addItems(online).build();
                        }))
                        .build())
                .addItem(MenuItem.builder(Material.PLAYER_HEAD)
                        .name(translatable("nation-menu-resident-overview-title"))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromBottom(2), HorizontalAnchor.fromRight(1)))
                        .lore(() -> {
                            if (nation == null)
                                return translatable("msg-err-not-part-of-nation");
                            else
                                return translatable("msg-click-to").append(translatable("nation-menu-view-manage-resident"));
                        })
                        .action(nation == null ? ClickAction.NONE : ClickAction.openInventory(() -> createResidentOverview(player)))
                        .build())
                .build();
    }

    public static MenuInventory formatNationToggleMenu(Player player) {
        final Locale locale = Localization.localeOrDefault(player);
        final Nation nation = TownyAPI.getInstance().getNation(player);

        final boolean isPublic = nation != null && nation.isPublic();
        final boolean isOpen = nation != null && nation.isOpen();

        return MenuInventory.builder()
                .rows(4)
                .title(translatable("nation-menu-toggle-title"))
                .addItem(MenuHelper.backButton().build())
                // Open
                .addItem(GovernmentMenus.createTogglePropertyItem(player, nation, Material.GRASS_BLOCK, isOpen, "open")
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(1)))
                        .build())
                .addItem(MenuItem.builder(isOpen ? Material.GREEN_CONCRETE : Material.RED_CONCRETE)
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(2), HorizontalAnchor.fromLeft(1)))
                        .name(Component.empty())
                        .build())
                // Public
                .addItem(GovernmentMenus.createTogglePropertyItem(player, nation, Material.GRASS_BLOCK, isPublic, "public")
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(2)))
                        .build())
                .addItem(MenuItem.builder(isPublic ? Material.GREEN_CONCRETE : Material.RED_CONCRETE)
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(2), HorizontalAnchor.fromLeft(2)))
                        .name(Component.empty())
                        .build())
                // TODO: Ability to toggle neutral/peaceful
                .build();
    }

    public static MenuInventory formatNationSetMenu(Player player) {
        final Locale locale = Localization.localeOrDefault(player);
        final Nation nation = TownyAPI.getInstance().getNation(player);

        return MenuInventory.builder()
                .title(translatable("nation-menu-set-title"))
                .rows(3)
                .addItem(MenuHelper.backButton().build())
                .addItem(MenuItem.builder(Material.NAME_TAG)
                        .name(translatable("nation-menu-set-change-name-title"))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(2)))
                        .lore(() -> {
                            if (nation == null)
                                return translatable("msg-must-be-in-nation-to").append(translatable("nation-menu-set-change-name")).color(GRAY);
                            else if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_SET_NAME.getNode()))
                                return translatable("msg-bo-permission-to").append(translatable("nation-menu-set-change-name")).color(GRAY);
                            else
                                return translatable("msg-click-to").append(translatable("nation-menu-set-change-name")).color(GRAY);
                        })
                        .action(nation == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_SET_NAME.getNode()) ? ClickAction.NONE : ClickAction.userInput("Enter new nation name", snapshot -> {
                            final Nation playerNation = TownyAPI.getInstance().getNation(player);
                            if (playerNation == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_SET_NAME.getNode()))
                                return AnvilResponse.close();

                            // Await confirmation events for this player
                            AwaitingConfirmation.await(player);
                            try {
                                NationCommand.nationSet(player, ("name " + snapshot.getText()).split(" "), false, playerNation);
                            } catch (TownyException e) {
                                TownyMessaging.sendErrorMsg(player, e.getMessage(player));
                                return AnvilResponse.text(e.getMessage(player));
                            }

                            MenuHistory.reOpen(player, () -> formatNationSetMenu(player));
                            return AnvilResponse.nil();
                        }))
                        .build())
                .addItem(MenuItem.builder(Material.OAK_SIGN)
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(4)))
                        .name(translatable("nation-menu-set-change-board-title"))
                        .lore(() -> {
                            if (nation == null)
                                return translatable("msg-must-be-in-nation-to").append(translatable("nation-menu-set-change-board")).color(GRAY);
                            else if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_SET_BOARD.getNode()))
                                return translatable("msg-no-permission-to").append(translatable("nation-menu-set-change-board")).color(GRAY);
                            else
                                return translatable("msg-click-to").append(translatable("nation-menu-set-change-board")).color(GRAY);
                        })
                        .action(nation == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_SET_BOARD.getNode()) ? ClickAction.NONE : ClickAction.userInput("Enter nation board", board -> {
                            final Nation playerNation = TownyAPI.getInstance().getNation(player);
                            if (playerNation == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_SET_BOARD.getNode()))
                                return AnvilResponse.close();

                            try {
                                NationCommand.nationSet(player, ("board " + board.getText()).split(" "), false, playerNation);
                            } catch (TownyException e) {
                                TownyMessaging.sendErrorMsg(player, e.getMessage(player));
                                return AnvilResponse.text(e.getMessage(player));
                            }

                            MenuHistory.reOpen(player, () -> formatNationSetMenu(player));
                            return AnvilResponse.nil();
                        }))
                        .build())
                .addItem(MenuItem.builder(Material.RED_BED)
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromRight(2)))
                        .name(translatable("nation-menu-set-change-spawn-title"))
                        .lore(() -> {
                            if (nation == null)
                                return translatable("msg-must-be-in-nation-to").append(translatable("nation-menu-set-change-spawn")).color(GRAY);
                            else if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_SET_SPAWN.getNode()))
                                return translatable("msg-no-permission-to").append(translatable("nation-menu-set-change-spawn")).color(GRAY);
                            else
                                return translatable("msg-click-to").append(translatable("nation-menu-set-change-spawn")).color(GRAY);
                        })
                        .action(nation == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_SET_SPAWN.getNode()) ? ClickAction.NONE : ClickAction.run(() -> {
                            final Nation playerNation = TownyAPI.getInstance().getNation(player);
                            if (playerNation == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_SET_SPAWN.getNode()))
                                return;

                            try {
                                // TODO: Create a PR that exposes nation set methods publicly without string[]
                                NationCommand.nationSet(player, new String[]{"spawn"}, false, playerNation);
                            } catch (TownyException e) {
                                TownyMessaging.sendErrorMsg(player, e.getMessage(player));
                            }
                        }))
                        .build())
                .build();
    }

    public static MenuInventory createResidentOverview(Player player) {
        final Locale locale = Localization.localeOrDefault(player);
        final Nation nation = TownyAPI.getInstance().getNation(player);

        if (nation == null)
            return MenuInventory.paginator().title(translatable("nation-menu-resident-overview-title")).build();

        final MenuInventory.PaginatorBuilder builder = MenuInventory.paginator().title(translatable("nation-menu-resident-overview-title").append(text(" - " + nation.getName())));

        for (Resident resident : nation.getResidents()) {
            builder.addItem(ResidentMenu.formatResidentInfo(resident, player)
                    .lore(text(" "))
                    .lore(translatable("msg-right-click-to").append(translatable("nation-menu-view-resident-options")).color(GRAY))
                    .action(ClickAction.rightClick(ClickAction.openInventory(() -> createResidentManagementScreen(player, nation, resident))))
                    .build());
        }

        return builder.build();
    }

    public static MenuInventory createResidentManagementScreen(Player player, Nation nation, Resident resident) {
        final Locale locale = Localization.localeOrDefault(player);

        return MenuInventory.builder()
                .title(translatable("nation-menu-resident-management"))
                .addItem(MenuHelper.backButton().build())
                .addItem(ResidentMenu.formatResidentInfo(resident, player)
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(4)))
                        .build())
                .addItem(MenuItem.builder(Material.NAME_TAG)
                        .name(translatable("nation-menu-change-resident-title-title").color(GRAY))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(2), HorizontalAnchor.fromLeft(2)))
                        .lore(() -> {
                            if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_SET_TITLE.getNode()))
                                return translatable("msg-no-permission-to").append(translatable("nation-menu-change-resident-title")).color(GRAY);
                            else
                                return Arrays.asList(translatable("msg-click-to").append(translatable("nation-menu-change-resident-title")).color(GRAY),
                                        translatable("msg-right-click-to").append(translatable("nation-menu-clear-resident-title")).color(GRAY));
                        })
                        .action(!player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_SET_TITLE.getNode()) ? ClickAction.NONE : ClickAction.leftClick(ClickAction.userInput(Translatable.of("nation-menu-enter-title").translate(locale), completion -> {
                            final Nation playerNation = TownyAPI.getInstance().getNation(player);
                            if (playerNation == null || playerNation != resident.getNationOrNull())
                                return AnvilResponse.close();

                            try {
                                BaseCommand.checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_SET_TITLE.getNode());
                                NationCommand.nationSet(player, new String[]{"title", resident.getName(), completion.getText()}, false, nation);
                            } catch (TownyException e) {
                                TownyMessaging.sendErrorMsg(player, e.getMessage(player));
                                return AnvilResponse.text(e.getMessage(player));
                            }

                            MenuHistory.last(player);
                            return AnvilResponse.nil();
                        })))
                        .action(!player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_SET_TITLE.getNode()) ? ClickAction.NONE : ClickAction.rightClick(ClickAction.run(() -> {
                            try {
                                BaseCommand.checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_SET_TITLE.getNode());
                                NationCommand.nationSet(player, new String[]{"title", resident.getName(), ""}, false, nation);
                            } catch (TownyException e) {
                                TownyMessaging.sendErrorMsg(player, e.getMessage(player));
                            }
                        })))
                        .build())
                .addItem(MenuItem.builder(Material.NAME_TAG)
                        .name(translatable("nation-menu-change-resident-surname-title"))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(2), HorizontalAnchor.fromRight(2)))
                        .lore(() -> {
                            if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_SET_SURNAME.getNode()))
                                return translatable("msg-no-permission-to").append(translatable("nation-menu-change-resident-surname")).color(GRAY);
                            else
                                return Arrays.asList(translatable("msg-click-to").append(translatable("nation-menu-change-resident-surname")).color(GRAY),
                                        translatable("msg-right-click-to").append(translatable("nation-menu-clear-resident-title")).color(GRAY));
                        })
                        .action(!player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_SET_SURNAME.getNode()) ? ClickAction.NONE : ClickAction.leftClick(ClickAction.userInput(Translatable.of("nation-menu-enter-surname").translate(locale), completion -> {
                            final Nation playerNation = TownyAPI.getInstance().getNation(player);
                            if (playerNation == null || playerNation != resident.getNationOrNull())
                                return AnvilResponse.close();

                            try {
                                BaseCommand.checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_SET_SURNAME.getNode());
                                NationCommand.nationSet(player, new String[]{"surname", resident.getName(), completion.getText()}, false, nation);
                            } catch (TownyException e) {
                                TownyMessaging.sendErrorMsg(player, e.getMessage(player));
                                return AnvilResponse.text(e.getMessage(player));
                            }

                            MenuHistory.last(player);
                            return AnvilResponse.nil();
                        })))
                        .action(!player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_SET_SURNAME.getNode()) ? ClickAction.NONE : ClickAction.rightClick(ClickAction.run(() -> {
                            try {
                                BaseCommand.checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_SET_SURNAME.getNode());
                                NationCommand.nationSet(player, new String[]{"surname", resident.getName(), ""}, false, nation);
                            } catch (TownyException e) {
                                TownyMessaging.sendErrorMsg(player, e.getMessage(player));
                            }
                        })))
                        .build())
                .addItem(MenuItem.builder(Material.KNOWLEDGE_BOOK)
                        .name(translatable("nation-menu-manage-ranks-title"))
                        .lore(translatable("msg-click-to").append(translatable("nation-menu-manage-ranks")))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromBottom(2), HorizontalAnchor.fromLeft(4)))
                        .action(ClickAction.openInventory(() -> formatRankManagementMenu(player, nation, resident)))
                        .build())
                .build();
    }

    // This and the town menu rank menu can maybe be abstracted into one method
    public static MenuInventory formatRankManagementMenu(Player player, Nation nation, Resident resident) {
        final Locale locale = Localization.localeOrDefault(player);

        MenuInventory.PaginatorBuilder paginator = MenuInventory.paginator().title(translatable("nation-menu-manage-ranks-title"));

        for (String nationRank : TownyPerms.getNationRanks()) {
            MenuItem.Builder item = MenuItem.builder(Material.KNOWLEDGE_BOOK)
                    .name(text(nationRank.substring(0, 1).toUpperCase(Locale.ROOT) + nationRank.substring(1), GREEN));

            final boolean hasPermission = player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_RANK.getNode(nationRank.toLowerCase(Locale.ROOT)));

            if (resident.hasNationRank(nationRank)) {
                item.withGlint();

                if (hasPermission) {
                    item.lore(translatable("msg-click-to").append(translatable("rank-remove")).append(text(nationRank)).append(translatable("rank-from")).append(text(resident.getName() + ".")).color(GRAY));
                    item.action(ClickAction.confirmation(translatable("rank-remove-confirmation").append(text(nationRank)).append(translatable("from").append(text(resident.getName() + "?"))).color(GRAY), ClickAction.run(() -> {
                        if (!resident.hasNationRank(nationRank) || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_RANK.getNode(nationRank.toLowerCase(Locale.ROOT)))) {
                            MenuHistory.reOpen(player, () -> formatRankManagementMenu(player, nation, resident));
                            return;
                        }

                        Resident playerResident = TownyAPI.getInstance().getResident(player);
                        if (nation == null || resident.getNationOrNull() != nation || playerResident == null || playerResident.getNationOrNull() != nation)
                            return;

                        NationRankRemoveEvent event = new NationRankRemoveEvent(nation, nationRank, resident);
                        Bukkit.getPluginManager().callEvent(event);

                        if (event.isCancelled()) {
                            TownyMessaging.sendErrorMsg(player, event.getCancelMessage());
                            MenuHistory.reOpen(player, () -> formatRankManagementMenu(player, nation, resident));
                            return;
                        }

                        resident.removeNationRank(nationRank);

                        if (resident.isOnline()) {
                            TownyMessaging.sendMsg(resident, Translatable.of("msg_you_have_had_rank_taken", Translatable.of("nation_sing"), nationRank));
                            Towny.getPlugin().deleteCache(resident);
                        }

                        TownyMessaging.sendMsg(player, Translatable.of("msg_you_have_taken_rank_from", Translatable.of("nation_sing"), nationRank, resident.getName()));
                        MenuHistory.reOpen(player, () -> formatRankManagementMenu(player, nation, resident));
                    })));
                }
            } else if (hasPermission) {
                item.lore(translatable("msg-click-to-grant-the").append(text(nationRank)).append(translatable("rank-to")).append(text(resident.getName() + ".")).color(GRAY));
                item.action(ClickAction.confirmation(translatable("rank-give-confirmation").append(text(nationRank)).append(translatable("to").append(text(resident.getName() + "?"))).color(GRAY), ClickAction.run(() -> {
                    if (resident.hasNationRank(nationRank) || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_RANK.getNode(nationRank.toLowerCase(Locale.ROOT)))) {
                        MenuHistory.reOpen(player, () -> formatRankManagementMenu(player, nation, resident));
                        return;
                    }

                    Resident playerResident = TownyAPI.getInstance().getResident(player);
                    if (nation == null || resident.getNationOrNull() != nation || playerResident == null || playerResident.getNationOrNull() != nation)
                        return;

                    NationRankAddEvent event = new NationRankAddEvent(nation, nationRank, resident);
                    Bukkit.getPluginManager().callEvent(event);

                    if (event.isCancelled()) {
                        TownyMessaging.sendErrorMsg(player, event.getCancelMessage());
                        MenuHistory.reOpen(player, () -> formatRankManagementMenu(player, nation, resident));
                        return;
                    }

                    resident.addNationRank(nationRank);
                    if (resident.isOnline()) {
                        TownyMessaging.sendMsg(resident, Translatable.of("msg_you_have_been_given_rank", Translatable.of("nation_sing"), nationRank));
                        Towny.getPlugin().deleteCache(resident);
                    }

                    TownyMessaging.sendMsg(player, Translatable.of("msg_you_have_given_rank", Translatable.of("nation_sing"), nationRank, resident.getName()));
                    MenuHistory.reOpen(player, () -> formatRankManagementMenu(player, nation, resident));
                })));
            }

            paginator.addItem(item.build());
        }

        return paginator.build();
    }

    public static MenuInventory formatNationBankMenu(final Player player) {
        final Nation nation = TownyAPI.getInstance().getNation(player);
        final Locale locale = Localization.localeOrDefault(player);

        final MenuInventory.Builder builder = MenuInventory.builder()
                .title(translatable("nation-menu-bank-title"))
                .rows(3)
                .addItem(MenuHelper.backButton().build())
                .addItem(MenuItem.builder(Material.EMERALD_BLOCK)
                        .name(translatable("nation-menu-bank-deposit-or-withdraw-title"))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(2)))
                        .lore(() -> {
                            if (nation == null)
                                return translatable("msg-err-not-part-of-nation");
                            else if (!TownyEconomyHandler.isActive())
                                return translatable("msg_err_no_economy").color(GRAY);
                            else
                                return translatable("msg-click-to").append(translatable("nation-menu-bank-deposit-or-withdraw")).color(GRAY);
                        })
                        .action(nation == null || !TownyEconomyHandler.isActive() ? ClickAction.NONE : ClickAction.openInventory(() -> GovernmentMenus.createDepositWithdrawMenu(player, nation)))
                        .build())
                .addItem(MenuItem.builder(Material.WRITABLE_BOOK)
                        .name(translatable("nation-menu-bank-transaction-history-title"))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromRight(2)))
                        .lore(() -> {
                            if (nation == null)
                                return translatable("msg-err-not-part-of-nation");
                            else if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_BANKHISTORY.getNode()))
                                return translatable("msg-no-permission-to").append(translatable("nation-menu-bank-transaction-history"));
                            else
                                return translatable("msg-click-to").append(translatable("nation-menu-bank-transaction-history"));
                        })
                        .action(nation == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_BANKHISTORY.getNode()) ? ClickAction.NONE :
                                ClickAction.openInventory(() -> TownMenu.createBankHistoryMenu(player, nation)))
                        .build());

        if (nation != null && TownyEconomyHandler.isActive()) {
            builder.addItem(TownMenu.formatBankStatus(player, nation.getAccount(), false)
                    .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(0), HorizontalAnchor.fromLeft(4)))
                    .build());
        }

        return builder.build();
    }
}
