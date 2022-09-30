package io.github.townyadvanced.townymenus.menu;

import com.palmergames.adventure.text.Component;
import com.palmergames.adventure.text.format.NamedTextColor;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.command.BaseCommand;
import com.palmergames.bukkit.towny.command.NationCommand;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import io.github.townyadvanced.townymenus.gui.MenuHelper;
import io.github.townyadvanced.townymenus.gui.MenuHistory;
import io.github.townyadvanced.townymenus.gui.MenuInventory;
import io.github.townyadvanced.townymenus.gui.MenuItem;
import io.github.townyadvanced.townymenus.gui.action.ClickAction;
import io.github.townyadvanced.townymenus.gui.anchor.HorizontalAnchor;
import io.github.townyadvanced.townymenus.gui.anchor.SlotAnchor;
import io.github.townyadvanced.townymenus.gui.anchor.VerticalAnchor;
import io.github.townyadvanced.townymenus.listeners.AwaitingConfirmation;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class NationMenu {
    public static MenuInventory createNationMenu(@NotNull Player player) {
        Nation nation = TownyAPI.getInstance().getNation(player);

        return MenuInventory.builder()
                .title(Component.text("Nation Menu"))
                .rows(6)
                .addItem(MenuHelper.backButton().build())
                .addItem(MenuItem.builder(Material.WRITABLE_BOOK)
                        .name(Component.text("Transaction History", NamedTextColor.GREEN))
                        .slot(SlotAnchor.of(VerticalAnchor.fromBottom(1), HorizontalAnchor.fromLeft(4)))
                        .lore(() -> {
                            if (nation == null)
                                return Component.text("You are not part of a nation.", NamedTextColor.GRAY);
                            else if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_BANKHISTORY.getNode()))
                                return Component.text("You do not have permission to view the nations's transaction history.", NamedTextColor.GRAY);
                            else
                                return Component.text("Click to view the nations's transaction history.", NamedTextColor.GRAY);
                        })
                        .action(nation == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_BANKHISTORY.getNode()) ? ClickAction.NONE :
                                ClickAction.openInventory(() -> TownMenu.createBankHistoryMenu(nation)))
                        .build())
                .addItem(MenuItem.builder(Material.RED_BED)
                        .slot(SlotAnchor.of(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(2)))
                        .name(Component.text("Nation Spawn", NamedTextColor.GREEN))
                        .lore(() -> {
                            if (nation == null)
                                return Component.text("You are not part of a nation.", NamedTextColor.GRAY);
                            else if (!player.hasPermission("towny.nation.spawn.nation"))
                                return Component.text("You do not have permission to use this.", NamedTextColor.GRAY);
                            else
                                return Component.text("Click to teleport to your nation's spawn.", NamedTextColor.GRAY);
                        })
                        .action(nation == null || !player.hasPermission("towny.nation.spawn.nation") ? ClickAction.NONE : ClickAction.confirmation(() -> Component.text("Click to confirm using /nation spawn.", NamedTextColor.GRAY), ClickAction.run(() -> {
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
                        .name(Component.text("Nation Toggle", NamedTextColor.GREEN))
                        .slot(SlotAnchor.of(VerticalAnchor.fromTop(1), HorizontalAnchor.fromRight(2)))
                        .lore(() -> {
                            if (nation == null)
                                return Component.text("You must be in a nation in order to view the toggle menu.", NamedTextColor.GRAY);
                            else
                                return Component.text("Click to open the toggle menu.", NamedTextColor.GRAY);
                        })
                        .action(nation == null ? ClickAction.NONE : ClickAction.openInventory(() -> formatNationToggleMenu(player)))
                        .build())
                .addItem(MenuItem.builder(Material.GRASS_BLOCK)
                        .name(Component.text("Nation Set", NamedTextColor.GREEN))
                        .slot(SlotAnchor.of(VerticalAnchor.fromBottom(2), HorizontalAnchor.fromRight(2)))
                        .lore(Component.text("Click to open the nation set menu.", NamedTextColor.GRAY))
                        .action(ClickAction.openInventory(() -> formatNationSetMenu(player)))
                        .build())
                .addItem(MenuItem.builder(Material.ENDER_EYE)
                        .name(Component.text("Online in Nation", NamedTextColor.GREEN))
                        .slot(SlotAnchor.of(VerticalAnchor.fromBottom(2), HorizontalAnchor.fromLeft(2)))
                        .lore(() -> {
                            if (nation == null)
                                return Component.text("You are not part of a nation.", NamedTextColor.GRAY);
                            else if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_ONLINE.getNode()))
                                return Component.text("You do not have permission to view residents online in your nation.", NamedTextColor.GRAY);
                            else
                                return Component.text("Click to view online residents in your nation.", NamedTextColor.GRAY);
                        })
                        .action(nation == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_ONLINE.getNode()) ? ClickAction.NONE : ClickAction.openInventory(() -> {
                            final Nation playerNation = TownyAPI.getInstance().getNation(player);
                            if (playerNation == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_ONLINE.getNode()))
                                return MenuInventory.paginator().title(Component.text("Online in Nation")).build();

                            List<MenuItem> online = new ArrayList<>();
                            for (Player onlinePlayer : TownyAPI.getInstance().getOnlinePlayers(playerNation)) {
                                if (!player.canSee(onlinePlayer))
                                    continue;

                                online.add(ResidentMenu.formatResidentInfo(onlinePlayer.getUniqueId(), player).build());
                            }

                            return MenuInventory.paginator().title(Component.text("Online in Nation")).addItems(online).build();
                        }))
                        .build())
                .addItem(MenuItem.builder(Material.PLAYER_HEAD)
                        .name(Component.text("Nation Resident Overview", NamedTextColor.GREEN))
                        .lore(() -> {
                            if (nation == null)
                                return Component.text("You are not part of a nation.", NamedTextColor.GRAY);
                            else
                                return Component.text("Click to view and manage residents in your nation.", NamedTextColor.GRAY);
                        })
                        .action(nation == null ? ClickAction.NONE : ClickAction.openInventory(() -> createResidentOverview(player)))
                        .build())
                .build();
    }

    public static MenuInventory formatNationToggleMenu(Player player) {
        final Nation nation = TownyAPI.getInstance().getNation(player);

        final boolean isPublic = nation != null && nation.isPublic();
        final boolean isOpen = nation != null && nation.isOpen();

        return MenuInventory.builder()
                .rows(4)
                .title(Component.text("Nation Toggle"))
                .addItem(MenuHelper.backButton().build())
                // Open
                .addItem(createTogglePropertyItem(player, nation != null, Material.GRASS_BLOCK, isOpen, "open")
                        .slot(SlotAnchor.of(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(1)))
                        .build())
                .addItem(MenuItem.builder(isOpen ? Material.GREEN_CONCRETE : Material.RED_CONCRETE)
                        .slot(SlotAnchor.of(VerticalAnchor.fromTop(2), HorizontalAnchor.fromLeft(1)))
                        .name(Component.empty())
                        .build())
                // Public
                .addItem(createTogglePropertyItem(player, nation != null, Material.GRASS_BLOCK, isPublic, "public")
                        .slot(SlotAnchor.of(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(2)))
                        .build())
                .addItem(MenuItem.builder(isPublic ? Material.GREEN_CONCRETE : Material.RED_CONCRETE)
                        .slot(SlotAnchor.of(VerticalAnchor.fromTop(2), HorizontalAnchor.fromLeft(2)))
                        .name(Component.empty())
                        .build())
                // TODO: Ability to toggle neutral/peaceful
                .build();
    }

    // TODO: Make its so this is not just a straight copy of townmenu's
    private static MenuItem.Builder createTogglePropertyItem(Player player, boolean hasNation, Material material, boolean propertyEnabled, String property) {
        return MenuItem.builder(material)
                .name(Component.text("Toggle " + property.substring(0, 1).toUpperCase(Locale.ROOT) + property.substring(1), propertyEnabled ? NamedTextColor.GREEN : NamedTextColor.RED))
                .lore(() -> {
                    if (!hasNation)
                        return Component.text("You are not in a nation.", NamedTextColor.GRAY);
                    else if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_TOGGLE.getNode(property)))
                        return Component.text("You do not have permission to toggle " + property + ".", NamedTextColor.GRAY);
                    else
                        return Component.text(String.format("Click to %s %s.", propertyEnabled ? "disable" : "enable", property), NamedTextColor.GRAY);
                })
                .action(!player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_TOGGLE.getNode(property)) ? ClickAction.NONE : ClickAction.confirmation(Component.text("Are you sure you want to toggle " + property + " in your nation?", NamedTextColor.GRAY), ClickAction.run(() -> {
                    final Nation nation = TownyAPI.getInstance().getNation(player);
                    if (nation == null)
                        return;

                    try {
                        NationCommand.nationToggle(player, new String[]{property}, false, nation);
                    } catch (TownyException e) {
                        TownyMessaging.sendErrorMsg(player, e.getMessage(player));
                    }

                    MenuHistory.reOpen(player, () -> formatNationToggleMenu(player));
                })));
    }

    public static MenuInventory formatNationSetMenu(Player player) {
        final Nation nation = TownyAPI.getInstance().getNation(player);

        return MenuInventory.builder()
                .title(Component.text("Nation Set"))
                .rows(3)
                .addItem(MenuHelper.backButton().build())
                .addItem(MenuItem.builder(Material.NAME_TAG)
                        .name(Component.text("Change nation name", NamedTextColor.GREEN))
                        .slot(SlotAnchor.of(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(2)))
                        .lore(() -> {
                            if (nation == null)
                                return Component.text("You must be in a nation in order to change the nation name.", NamedTextColor.GRAY);
                            else if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_SET_NAME.getNode()))
                                return Component.text("You do not have permission to change the nation's name.", NamedTextColor.GRAY);
                            else
                                return Component.text("Click to change the nation's name.", NamedTextColor.GRAY);
                        })
                        .action(nation == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_SET_NAME.getNode()) ? ClickAction.NONE : ClickAction.userInput("Enter new nation name", name -> {
                            final Nation playerNation = TownyAPI.getInstance().getNation(player);
                            if (playerNation == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_SET_NAME.getNode()))
                                return AnvilGUI.Response.close();

                            // Await confirmation events for this player
                            AwaitingConfirmation.await(player);
                            
                            try {
                                NationCommand.nationSet(player, ("name " + name).split(" "), false, playerNation);
                            } catch (TownyException e) {
                                TownyMessaging.sendErrorMsg(player, e.getMessage(player));
                                return AnvilGUI.Response.text(e.getMessage(player));
                            }

                            MenuHistory.reOpen(player, () -> formatNationSetMenu(player));
                            return AnvilGUI.Response.text("");
                        }))
                        .build())
                .addItem(MenuItem.builder(Material.OAK_SIGN)
                        .slot(SlotAnchor.of(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(4)))
                        .name(Component.text("Change nation board", NamedTextColor.GREEN))
                        .lore(() -> {
                            if (nation == null)
                                return Component.text("You must be in a nation in order to change the nation's board.", NamedTextColor.GRAY);
                            else if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_SET_BOARD.getNode()))
                                return Component.text("You do not have permission to change the nation's board.", NamedTextColor.GRAY);
                            else
                                return Component.text("Click to change the nation's board.", NamedTextColor.GRAY);
                        })
                        .action(nation == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_SET_BOARD.getNode()) ? ClickAction.NONE : ClickAction.userInput("Enter nation board", board -> {
                            final Nation playerNation = TownyAPI.getInstance().getNation(player);
                            if (playerNation == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_SET_BOARD.getNode()))
                                return AnvilGUI.Response.close();

                            try {
                                NationCommand.nationSet(player, ("board " + board).split(" "), false, playerNation);
                            } catch (TownyException e) {
                                TownyMessaging.sendErrorMsg(player, e.getMessage(player));
                                return AnvilGUI.Response.text(e.getMessage(player));
                            }

                            MenuHistory.reOpen(player, () -> formatNationSetMenu(player));
                            return AnvilGUI.Response.text("");
                        }))
                        .build())
                .addItem(MenuItem.builder(Material.RED_BED)
                        .slot(SlotAnchor.of(VerticalAnchor.fromTop(1), HorizontalAnchor.fromRight(2)))
                        .name(Component.text("Change nation spawn", NamedTextColor.GREEN))
                        .lore(() -> {
                            if (nation == null)
                                return Component.text("You must be in a nation in order to change the nation's spawn.", NamedTextColor.GRAY);
                            else if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_SET_SPAWN.getNode()))
                                return Component.text("You do not have permission to change the nation's spawn.", NamedTextColor.GRAY);
                            else
                                return Component.text("Click to change the nation's spawn.", NamedTextColor.GRAY);
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
        final Nation nation = TownyAPI.getInstance().getNation(player);
        if (nation == null)
            return MenuInventory.paginator().title(Component.text("Resident Overview")).build();

        final MenuInventory.PaginatorBuilder builder = MenuInventory.paginator().title(Component.text("Resident Overview - " + nation.getName()));

        for (Resident resident : nation.getResidents()) {
            builder.addItem(ResidentMenu.formatResidentInfo(resident, player)
                    .lore(Component.text(" "))
                    .lore(Component.text("Right click to view additional options for this resident.", NamedTextColor.GRAY))
                    .action(ClickAction.rightClick(ClickAction.openInventory(() -> createResidentManagementScreen(player, nation, resident))))
                    .build());
        }

        return builder.build();
    }

    public static MenuInventory createResidentManagementScreen(Player player, Nation nation, Resident resident) {
        return MenuInventory.builder()
                .title(Component.text("Resident Management"))
                .addItem(MenuHelper.backButton().build())
                .addItem(ResidentMenu.formatResidentInfo(resident, player)
                        .slot(SlotAnchor.of(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(4)))
                        .build())
                .addItem(MenuItem.builder(Material.NAME_TAG)
                        .name(Component.text("Change Resident Title", NamedTextColor.GREEN))
                        .slot(SlotAnchor.of(VerticalAnchor.fromTop(2), HorizontalAnchor.fromLeft(2)))
                        .lore(() -> {
                            if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_SET_TITLE.getNode()))
                                return Component.text("You do not have permission to change this resident's title.", NamedTextColor.GRAY);
                            else
                                return Arrays.asList(Component.text("Click to change this resident's title.", NamedTextColor.GRAY),
                                        Component.text("Right click to clear this resident's title.", NamedTextColor.GRAY));
                        })
                        .action(!player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_SET_TITLE.getNode()) ? ClickAction.NONE : ClickAction.leftClick(ClickAction.userInput("Enter new title", title -> {
                            final Nation playerNation = TownyAPI.getInstance().getNation(player);
                            if (playerNation == null || playerNation != resident.getNationOrNull())
                                return AnvilGUI.Response.close();

                            try {
                                BaseCommand.checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_SET_TITLE.getNode());
                                NationCommand.nationSet(player, new String[]{"title", resident.getName(), title}, false, nation);
                            } catch (TownyException e) {
                                TownyMessaging.sendErrorMsg(player, e.getMessage(player));
                                return AnvilGUI.Response.text(e.getMessage(player));
                            }

                            MenuHistory.last(player);
                            return AnvilGUI.Response.text("");
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
                        .name(Component.text("Change Resident Surname", NamedTextColor.GREEN))
                        .slot(SlotAnchor.of(VerticalAnchor.fromTop(2), HorizontalAnchor.fromRight(2)))
                        .lore(() -> {
                            if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_SET_SURNAME.getNode()))
                                return Component.text("You do not have permission to change this resident's surname.", NamedTextColor.GRAY);
                            else
                                return Arrays.asList(Component.text("Click to change this resident's surname.", NamedTextColor.GRAY),
                                        Component.text("Right click to clear this resident's surname.", NamedTextColor.GRAY));
                        })
                        .action(!player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_SET_SURNAME.getNode()) ? ClickAction.NONE : ClickAction.leftClick(ClickAction.userInput("Enter new surname", surname -> {
                            final Nation playerNation = TownyAPI.getInstance().getNation(player);
                            if (playerNation == null || playerNation != resident.getNationOrNull())
                                return AnvilGUI.Response.close();

                            try {
                                BaseCommand.checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_SET_SURNAME.getNode());
                                NationCommand.nationSet(player, new String[]{"surname", resident.getName(), surname}, false, nation);
                            } catch (TownyException e) {
                                TownyMessaging.sendErrorMsg(player, e.getMessage(player));
                                return AnvilGUI.Response.text(e.getMessage(player));
                            }

                            MenuHistory.last(player);
                            return AnvilGUI.Response.text("");
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
                .build();
    }
}
