package io.github.townyadvanced.townymenus.menu;

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
import io.github.townyadvanced.townymenus.gui.input.response.InputResponse;
import io.github.townyadvanced.townymenus.utils.Localization;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static com.palmergames.bukkit.towny.object.Translatable.*;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public class NationMenu {
    public static MenuInventory createNationMenu(@NotNull Player player) {
        final Locale locale = Localization.localeOrDefault(player);
        Nation nation = TownyAPI.getInstance().getNation(player);

        return MenuInventory.builder()
                .title(of("nation-menu-title").component(locale))
                .rows(6)
                .addItem(MenuHelper.backButton().build())
                .addItem(MenuItem.builder(Material.EMERALD_BLOCK)
                        .name(of("nation-menu-bank-title").component(locale))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromBottom(1), HorizontalAnchor.fromLeft(3)))
                        .lore(() -> {
                            if (nation == null)
                                return of("msg-err-not-part-of-nation").component(locale);
                            else
                                return of("msg-click-to").append(of("nation-menu-view-bank")).component(locale).color(GRAY);
                        })
                        .action(nation == null ? ClickAction.NONE : ClickAction.openInventory(() -> formatNationBankMenu(player)))
                        .build())
                .addItem(MenuItem.builder(Material.RED_BED)
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(1)))
                        .name(of("nation-menu-nation-spawn").component(locale))
                        .lore(() -> {
                            if (nation == null)
                                return of("msg-err-not-part-of-nation").component(locale);
                            else if (!player.hasPermission("towny.nation.spawn.nation"))
                                return of("msg-no-permission").component(locale);
                            else
                                return of("msg-click-to").append(of("nation-menu-teleport-to-spawn")).component(locale).color(GRAY);
                        })
                        .action(nation == null || !player.hasPermission("towny.nation.spawn.nation") ? ClickAction.NONE : ClickAction.confirmation(() -> of("nation-menu-teleport-confirm").component(locale), ClickAction.run(() -> {
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
                        .name(of("nation-menu-toggle-title").component(locale))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromRight(1)))
                        .lore(() -> {
                            if (nation == null)
                                return of("msg-must-be-in-nation-to").append(of("nation-menu-toggle-open")).component(locale).color(GRAY);
                            else
                                return of("msg-click-to").append(of("nation-menu-toggle-open")).component(locale).color(GRAY);
                        })
                        .action(nation == null ? ClickAction.NONE : ClickAction.openInventory(() -> formatNationToggleMenu(player)))
                        .build())
                .addItem(MenuItem.builder(Material.GRASS_BLOCK)
                        .name(of("nation-menu-set-title").component(locale))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromBottom(1), HorizontalAnchor.fromRight(3)))
                        .lore(of("msg-click-to").append(of("nation-menu-set-open")).component(locale).color(GRAY))
                        .action(ClickAction.openInventory(() -> formatNationSetMenu(player)))
                        .build())
                .addItem(MenuItem.builder(Material.ENDER_EYE)
                        .name(of("nation-menu-online").component(locale))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromBottom(2), HorizontalAnchor.fromLeft(1)))
                        .lore(() -> {
                            if (nation == null)
                                return of("msg-err-not-part-of-nation").component(locale);
                            else if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_ONLINE.getNode()))
                                return of("msg-no-permission-to").append(of("nation-menu-view-online")).component(locale).color(GRAY);
                            else
                                return of("msg-click-to").append(of("nation-menu-view-online")).component(locale).color(GRAY);
                        })
                        .action(nation == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_ONLINE.getNode()) ? ClickAction.NONE : ClickAction.openInventory(() -> {
                            final Nation playerNation = TownyAPI.getInstance().getNation(player);
                            if (playerNation == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_ONLINE.getNode()))
                                return MenuInventory.paginator().title(of("nation-menu-online").component(locale)).build();

                            List<MenuItem> online = new ArrayList<>();
                            for (Player onlinePlayer : TownyAPI.getInstance().getOnlinePlayers(playerNation)) {
                                if (!player.canSee(onlinePlayer))
                                    continue;

                                online.add(ResidentMenu.formatResidentInfo(onlinePlayer.getUniqueId(), player).build());
                            }

                            return MenuInventory.paginator().title(of("nation-menu-online").component(locale)).addItems(online).build();
                        }))
                        .build())
                .addItem(MenuItem.builder(Material.PLAYER_HEAD)
                        .name(of("nation-menu-resident-overview-title").component(locale))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromBottom(2), HorizontalAnchor.fromRight(1)))
                        .lore(() -> {
                            if (nation == null)
                                return of("msg-err-not-part-of-nation").component(locale);
                            else
                                return of("msg-click-to").append(of("nation-menu-view-manage-resident")).component(locale);
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
                .title(of("nation-menu-toggle-title").component(locale))
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
                .title(of("nation-menu-set-title").component(locale))
                .rows(3)
                .addItem(MenuHelper.backButton().build())
                .addItem(MenuItem.builder(Material.NAME_TAG)
                        .name(of("nation-menu-set-change-name-title").component(locale))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(2)))
                        .lore(() -> {
                            if (nation == null)
                                return of("msg-must-be-in-nation-to").append(of("nation-menu-set-change-name")).component(locale).color(GRAY);
                            else if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_SET_NAME.getNode()))
                                return of("msg-no-permission-to").append(of("nation-menu-set-change-name")).component(locale).color(GRAY);
                            else
                                return of("msg-click-to").append(of("nation-menu-set-change-name")).component(locale).color(GRAY);
                        })
                        .action(nation == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_SET_NAME.getNode()) ? ClickAction.NONE : ClickAction.userInput("Enter new nation name", snapshot -> {
                            final Nation playerNation = TownyAPI.getInstance().getNation(player);
                            if (playerNation == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_SET_NAME.getNode()))
                                return InputResponse.finish();

                            // Await confirmation events for this player
                            AwaitingConfirmation.await(player);
                            try {
                                NationCommand.nationSet(player, ("name " + snapshot.getText()).split(" "), false, playerNation);
                            } catch (TownyException e) {
                                return InputResponse.errorMessage(e.getMessage(player));
                            }

                            MenuHistory.reOpen(player, () -> formatNationSetMenu(player));
                            return InputResponse.doNothing();
                        }))
                        .build())
                .addItem(MenuItem.builder(Material.OAK_SIGN)
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(4)))
                        .name(of("nation-menu-set-change-board-title").component(locale))
                        .lore(() -> {
                            if (nation == null)
                                return of("msg-must-be-in-nation-to").append(of("nation-menu-set-change-board")).component(locale).color(GRAY);
                            else if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_SET_BOARD.getNode()))
                                return of("msg-no-permission-to").append(of("nation-menu-set-change-board")).component(locale).color(GRAY);
                            else
                                return of("msg-click-to").append(of("nation-menu-set-change-board")).component(locale).color(GRAY);
                        })
                        .action(nation == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_SET_BOARD.getNode()) ? ClickAction.NONE : ClickAction.userInput("Enter nation board", board -> {
                            final Nation playerNation = TownyAPI.getInstance().getNation(player);
                            if (playerNation == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_SET_BOARD.getNode()))
                                return InputResponse.finish();

                            try {
                                NationCommand.nationSet(player, ("board " + board.getText()).split(" "), false, playerNation);
                            } catch (TownyException e) {
                                return InputResponse.errorMessage(e.getMessage(player));
                            }

                            MenuHistory.reOpen(player, () -> formatNationSetMenu(player));
                            return InputResponse.doNothing();
                        }))
                        .build())
                .addItem(MenuItem.builder(Material.RED_BED)
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromRight(2)))
                        .name(of("nation-menu-set-change-spawn-title").component(locale))
                        .lore(() -> {
                            if (nation == null)
                                return of("msg-must-be-in-nation-to").append(of("nation-menu-set-change-spawn")).component(locale).color(GRAY);
                            else if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_SET_SPAWN.getNode()))
                                return of("msg-no-permission-to").append(of("nation-menu-set-change-spawn")).component(locale).color(GRAY);
                            else
                                return of("msg-click-to").append(of("nation-menu-set-change-spawn")).component(locale).color(GRAY);
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
            return MenuInventory.paginator().title(of("nation-menu-resident-overview-title").component(locale)).build();

        final MenuInventory.PaginatorBuilder builder = MenuInventory.paginator().title(of("nation-menu-resident-overview-title").append( " - " + nation.getName()).component(locale));

        for (Resident resident : nation.getResidents()) {
            builder.addItem(ResidentMenu.formatResidentInfo(resident, player)
                    .lore(text(" "))
                    .lore(of("msg-right-click-to").append(of("nation-menu-view-resident-options")).component(locale).color(GRAY))
                    .action(ClickAction.rightClick(ClickAction.openInventory(() -> createResidentManagementScreen(player, nation, resident))))
                    .build());
        }

        return builder.build();
    }

    public static MenuInventory createResidentManagementScreen(Player player, Nation nation, Resident resident) {
        final Locale locale = Localization.localeOrDefault(player);

        return MenuInventory.builder()
                .title(of("nation-menu-resident-management").component(locale))
                .addItem(MenuHelper.backButton().build())
                .addItem(ResidentMenu.formatResidentInfo(resident, player)
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(4)))
                        .build())
                .addItem(MenuItem.builder(Material.NAME_TAG)
                        .name(of("nation-menu-change-resident-title-title").component(locale).color(GRAY))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(2), HorizontalAnchor.fromLeft(2)))
                        .lore(() -> {
                            if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_SET_TITLE.getNode()))
                                return of("msg-no-permission-to").append(of("nation-menu-change-resident-title")).component(locale).color(GRAY);
                            else
                                return Arrays.asList(of("msg-click-to").append(of("nation-menu-change-resident-title")).component(locale).color(GRAY),
                                        of("msg-right-click-to").append(of("nation-menu-clear-resident-title")).component(locale).color(GRAY));
                        })
                        .action(!player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_SET_TITLE.getNode()) ? ClickAction.NONE : ClickAction.leftClick(ClickAction.userInput(of("nation-menu-enter-title").translate(locale), completion -> {
                            final Nation playerNation = TownyAPI.getInstance().getNation(player);
                            if (playerNation == null || playerNation != resident.getNationOrNull())
                                return InputResponse.finish();

                            try {
                                BaseCommand.checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_SET_TITLE.getNode());
                                NationCommand.nationSet(player, new String[]{"title", resident.getName(), completion.getText()}, false, nation);
                            } catch (TownyException e) {
                                return InputResponse.errorMessage(e.getMessage(player));
                            }

                            MenuHistory.last(player);
                            return InputResponse.doNothing();
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
                        .name(of("nation-menu-change-resident-surname-title").component(locale))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(2), HorizontalAnchor.fromRight(2)))
                        .lore(() -> {
                            if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_SET_SURNAME.getNode()))
                                return of("msg-no-permission-to").append(of("nation-menu-change-resident-surname")).component(locale).color(GRAY);
                            else
                                return Arrays.asList(of("msg-click-to").append(of("nation-menu-change-resident-surname")).component(locale).color(GRAY),
                                        of("msg-right-click-to").append(of("nation-menu-clear-resident-title")).component(locale).color(GRAY));
                        })
                        .action(!player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_SET_SURNAME.getNode()) ? ClickAction.NONE : ClickAction.leftClick(ClickAction.userInput(of("nation-menu-enter-surname").translate(locale), completion -> {
                            final Nation playerNation = TownyAPI.getInstance().getNation(player);
                            if (playerNation == null || playerNation != resident.getNationOrNull())
                                return InputResponse.finish();

                            try {
                                BaseCommand.checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_SET_SURNAME.getNode());
                                NationCommand.nationSet(player, new String[]{"surname", resident.getName(), completion.getText()}, false, nation);
                            } catch (TownyException e) {
                                return InputResponse.errorMessage(e.getMessage(player));
                            }

                            MenuHistory.last(player);
                            return InputResponse.doNothing();
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
                        .name(of("nation-menu-manage-ranks-title").component(locale))
                        .lore(of("msg-click-to").append(of("nation-menu-manage-ranks")).component(locale))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromBottom(2), HorizontalAnchor.fromLeft(4)))
                        .action(ClickAction.openInventory(() -> formatRankManagementMenu(player, nation, resident)))
                        .build())
                .build();
    }

    // This and the town menu rank menu can maybe be abstracted into one method
    public static MenuInventory formatRankManagementMenu(Player player, Nation nation, Resident resident) {
        final Locale locale = Localization.localeOrDefault(player);

        MenuInventory.PaginatorBuilder paginator = MenuInventory.paginator().title(of("nation-menu-manage-ranks-title").component(locale));

        for (String nationRank : TownyPerms.getNationRanks()) {
            MenuItem.Builder item = MenuItem.builder(Material.KNOWLEDGE_BOOK)
                    .name(text(nationRank.substring(0, 1).toUpperCase(Locale.ROOT) + nationRank.substring(1), GREEN));

            final boolean hasPermission = player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_RANK.getNode(nationRank.toLowerCase(Locale.ROOT)));

            if (resident.hasNationRank(nationRank)) {
                item.withGlint();

                if (hasPermission) {
                    item.lore(of("msg-click-to").append(of("rank-remove")).append(nationRank).append(of("rank-from")).append(resident.getName()).append(text(".")).component(locale).color(GRAY));
                    item.action(ClickAction.confirmation(of("rank-remove-confirmation").append(nationRank).append(of("from").append(resident.getName()).append("?")).component(locale).color(GRAY), ClickAction.run(() -> {
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
                            TownyMessaging.sendMsg(resident, of("msg_you_have_had_rank_taken", of("nation_sing"), nationRank));
                            Towny.getPlugin().deleteCache(resident);
                        }

                        TownyMessaging.sendMsg(player, of("msg_you_have_taken_rank_from", of("nation_sing"), nationRank, resident.getName()));
                        MenuHistory.reOpen(player, () -> formatRankManagementMenu(player, nation, resident));
                    })));
                }
            } else if (hasPermission) {
                item.lore(of("msg-click-to-grant-the").append(nationRank).append(of("rank-to")).append(resident.getName()).append(".").component(locale).color(GRAY));
                item.action(ClickAction.confirmation(of("rank-give-confirmation").append(nationRank).append(of("to").append(resident.getName()).append("?")).component(locale).color(GRAY), ClickAction.run(() -> {
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
                        TownyMessaging.sendMsg(resident, of("msg_you_have_been_given_rank", of("nation_sing"), nationRank));
                        Towny.getPlugin().deleteCache(resident);
                    }

                    TownyMessaging.sendMsg(player, of("msg_you_have_given_rank", of("nation_sing"), nationRank, resident.getName()));
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
                .title(of("nation-menu-bank-title").component(locale))
                .rows(3)
                .addItem(MenuHelper.backButton().build())
                .addItem(MenuItem.builder(Material.EMERALD_BLOCK)
                        .name(of("nation-menu-bank-deposit-or-withdraw-title").component(locale))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(2)))
                        .lore(() -> {
                            if (nation == null)
                                return of("msg-err-not-part-of-nation").component(locale);
                            else if (!TownyEconomyHandler.isActive())
                                return of("msg_err_no_economy").component(locale).color(GRAY);
                            else
                                return of("msg-click-to").append(of("nation-menu-bank-deposit-or-withdraw")).component(locale).color(GRAY);
                        })
                        .action(nation == null || !TownyEconomyHandler.isActive() ? ClickAction.NONE : ClickAction.openInventory(() -> GovernmentMenus.createDepositWithdrawMenu(player, nation)))
                        .build())
                .addItem(MenuItem.builder(Material.WRITABLE_BOOK)
                        .name(of("nation-menu-bank-transaction-history-title").component(locale))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromRight(2)))
                        .lore(() -> {
                            if (nation == null)
                                return of("msg-err-not-part-of-nation").component(locale);
                            else if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_BANKHISTORY.getNode()))
                                return of("msg-no-permission-to").append(of("nation-menu-bank-transaction-history")).component(locale);
                            else
                                return of("msg-click-to").append(of("nation-menu-bank-transaction-history")).component(locale);
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
