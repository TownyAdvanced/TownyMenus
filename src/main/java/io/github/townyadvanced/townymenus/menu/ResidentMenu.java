package io.github.townyadvanced.townymenus.menu;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.command.ResidentCommand;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.SpawnType;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.utils.SpawnUtil;
import io.github.townyadvanced.townymenus.TownyMenus;
import io.github.townyadvanced.townymenus.gui.MenuHelper;
import io.github.townyadvanced.townymenus.gui.MenuHistory;
import io.github.townyadvanced.townymenus.gui.MenuInventory;
import io.github.townyadvanced.townymenus.gui.MenuItem;
import io.github.townyadvanced.townymenus.gui.action.ClickAction;
import io.github.townyadvanced.townymenus.gui.anchor.HorizontalAnchor;
import io.github.townyadvanced.townymenus.gui.anchor.SlotAnchor;
import io.github.townyadvanced.townymenus.gui.anchor.VerticalAnchor;
import io.github.townyadvanced.townymenus.utils.MenuScheduler;
import io.github.townyadvanced.townymenus.utils.Time;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class ResidentMenu {
    public static Supplier<MenuInventory> createResidentMenu(@NotNull Player player) {
        return () -> MenuInventory.builder()
                .rows(3)
                .title(Component.text("Resident Menu"))
                .addItem(MenuItem.builder(Material.PLAYER_HEAD)
                        .name(Component.text("View Friends", NamedTextColor.GREEN))
                        .lore(Component.text("Click to see your friends list.", NamedTextColor.GRAY))
                        .slot(11)
                        .action(ClickAction.openInventory(formatResidentFriends(player)))
                        .build())
                .addItem(formatResidentInfo(player.getUniqueId()).slot(13).build())
                .addItem(MenuItem.builder(Material.RED_BED)
                        .name(Component.text("Spawn", NamedTextColor.GREEN))
                        .lore(player.hasPermission(PermissionNodes.TOWNY_COMMAND_RESIDENT_SPAWN.getNode())
                                ? Component.text("Click to teleport to your spawn!", NamedTextColor.GRAY)
                                : Component.text("✖ You do not have enough permissions to use this!", NamedTextColor.RED))
                        .action(ClickAction.confirmation(() -> Component.text("Click to confirm using /resident spawn.", NamedTextColor.GRAY), ClickAction.run(() -> {
                            if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_RESIDENT_SPAWN.getNode()))
                                return;

                            Resident resident = TownyAPI.getInstance().getResident(player);
                            if (resident == null)
                                return;

                            try {
                                SpawnUtil.sendToTownySpawn(player, new String[]{}, resident, Translatable.of("msg_err_cant_afford_tp").forLocale(player), false, true, SpawnType.RESIDENT);
                            } catch (TownyException e) {
                                TownyMessaging.sendErrorMsg(player, e.getMessage(player));
                            }
                        })))
                        .action(ClickAction.close())
                        .slot(15)
                        .build())
                .addItem(MenuHelper.backButton().slot(SlotAnchor.of(VerticalAnchor.fromBottom(0), HorizontalAnchor.fromRight(0))).build())
                .build();
    }

    public static MenuItem.Builder formatResidentInfo(@NotNull UUID uuid) {
        return formatResidentInfo(TownyAPI.getInstance().getResident(uuid));
    }

    /**
     * @param resident The resident to format
     * @return A formatted menu item, or an 'error' item if the resident isn't registered.
     */
    public static MenuItem.Builder formatResidentInfo(@Nullable Resident resident) {
        if (resident == null)
            return MenuItem.builder(Material.PLAYER_HEAD)
                    .skullOwner(resident.getUUID())
                    .name(Component.text("Error", NamedTextColor.DARK_RED))
                    .lore(Component.text("Unknown or invalid resident.", NamedTextColor.RED));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Status: ", NamedTextColor.DARK_GREEN).append(resident.isOnline() ? Component.text("● Online", NamedTextColor.GREEN) : Component.text("● Offline", NamedTextColor.RED)));

        if (resident.hasTown()) {
            lore.add(Component.text((resident.isMayor() ? "Mayor" : "Resident") + " of ", NamedTextColor.DARK_GREEN).append(Component.text(resident.getTownOrNull().getName(), NamedTextColor.GREEN)));

            if (resident.hasNation())
                lore.add(Component.text((resident.isKing() ? "Leader" : "Member") + " of ", NamedTextColor.DARK_GREEN).append(Component.text(resident.getNationOrNull().getName(), NamedTextColor.GREEN)));
        }

        lore.add(Component.text("Registered ", NamedTextColor.DARK_GREEN).append(Component.text(Time.formatRegistered(resident.getRegistered()), NamedTextColor.GREEN)));

        if (!resident.isOnline())
            lore.add(Component.text("Last online ", NamedTextColor.DARK_GREEN).append(Component.text(Time.formatLastOnline(resident.getLastOnline()), NamedTextColor.GREEN)));

        return MenuItem.builder(Material.PLAYER_HEAD)
                .skullOwner(resident.getUUID())
                .name(Component.text(resident.getName(), NamedTextColor.GREEN))
                .lore(lore);
    }

    public static List<MenuItem> formatFriendsView(@NotNull Player player) {
        Resident resident = TownyAPI.getInstance().getResident(player);

        if (resident == null || resident.getFriends().size() == 0)
            return Collections.singletonList(MenuItem.builder(Material.BARRIER)
                    .name(Component.text("Error", NamedTextColor.RED))
                    .lore(Component.text("You do not have any friends to list.", NamedTextColor.GRAY))
                    .build());

        List<MenuItem> friends = new ArrayList<>();
        for (Resident friend : resident.getFriends())
            friends.add(formatResidentInfo(friend.getUUID())
                    .lore(Component.text("Right click to remove this player as a friend.", NamedTextColor.GRAY))
                    .action(ClickAction.rightClick(ClickAction.confirmation(() -> Component.text("Are you sure you want to remove " + friend.getName() + " as a friend?", NamedTextColor.GRAY), ClickAction.run(() -> {
                        if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_RESIDENT_FRIEND.getNode())) {
                            TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_command_disable"));
                            return;
                        }

                        if (!resident.hasFriend(friend))
                            return;

                        ResidentCommand.residentFriendRemove(player, resident, Collections.singletonList(friend));

                        // Re-open resident friends menu
                        MenuHistory.reOpen(player, () -> formatResidentFriends(player));

                        TownyMenus.logger().info(player.getName() + " has removed " + friend.getName() + " as a friend.");
                    }))))
                    .build());

        return friends;
    }

    private static MenuInventory formatResidentFriends(Player player) {
        return MenuInventory.paginator()
                .title(Component.text("Resident Friends"))
                .addItems(formatFriendsView(player))
                .addExtraItem(MenuItem.builder(Material.WRITABLE_BOOK)
                        .name(Component.text("Add Friend", NamedTextColor.GREEN))
                        .lore(Component.text("Click here to add a player as a friend.", NamedTextColor.GRAY))
                        .slot(SlotAnchor.of(VerticalAnchor.fromBottom(0), HorizontalAnchor.fromLeft(1)))
                        .action(ClickAction.userInput("Enter player name.", name -> {
                            Resident resident = TownyAPI.getInstance().getResident(player);
                            if (resident == null)
                                return AnvilGUI.Response.text("You are not registered.");

                            Resident friend = TownyAPI.getInstance().getResident(name);
                            if (friend == null || friend.getUUID().equals(resident.getUUID()))
                                return AnvilGUI.Response.text("Not a valid resident.");

                            if (resident.hasFriend(friend))
                                return AnvilGUI.Response.text(friend.getName() + " is already your friend!");

                            List<Resident> friends = new ArrayList<>();
                            friends.add(friend);

                            ResidentCommand.residentFriendAdd(player, resident, friends);
                            TownyMenus.logger().info(player.getName() + " has added " + friend.getName() + " as a friend.");

                            // Re-open resident friends menu
                            MenuScheduler.scheduleAsync(player, () -> {
                                MenuHistory.pop(player.getUniqueId());
                                formatResidentFriends(player).open(player);
                            });

                            return AnvilGUI.Response.close();
                        }))
                        .build())
                .build();
    }
}
