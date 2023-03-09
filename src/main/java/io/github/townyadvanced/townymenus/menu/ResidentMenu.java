package io.github.townyadvanced.townymenus.menu;

import com.palmergames.adventure.text.Component;
import com.palmergames.adventure.text.format.NamedTextColor;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.command.ResidentCommand;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.SpawnType;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.utils.SpawnUtil;
import io.github.townyadvanced.townymenus.gui.MenuHelper;
import io.github.townyadvanced.townymenus.gui.MenuHistory;
import io.github.townyadvanced.townymenus.gui.MenuInventory;
import io.github.townyadvanced.townymenus.gui.MenuItem;
import io.github.townyadvanced.townymenus.gui.action.ClickAction;
import io.github.townyadvanced.townymenus.gui.action.UserInputAction;
import io.github.townyadvanced.townymenus.gui.slot.anchor.HorizontalAnchor;
import io.github.townyadvanced.townymenus.gui.slot.anchor.SlotAnchor;
import io.github.townyadvanced.townymenus.gui.slot.anchor.VerticalAnchor;
import io.github.townyadvanced.townymenus.utils.Localization;
import io.github.townyadvanced.townymenus.utils.Time;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.palmergames.bukkit.towny.object.Translatable.*;

public class ResidentMenu {
    public static MenuInventory createResidentMenu(@NotNull Player player) {
        final Locale locale = Localization.localeOrDefault(player);

        return MenuInventory.builder()
                .rows(3)
                .title(of("resident-menu-title").locale(player).component())
                .addItem(MenuItem.builder(Material.PLAYER_HEAD)
                        .name(of("resident-menu-view-friends").locale(player).component())
                        .lore(of("resident-menu-view-friends-subtitle").locale(player).component())
                        .slot(11)
                        .action(ClickAction.openInventory(() -> formatResidentFriends(player)))
                        .build())
                .addItem(formatResidentInfo(player.getUniqueId()).slot(13).build())
                .addItem(MenuItem.builder(Material.RED_BED)
                        .name(of("resident-menu-spawn").locale(player).component())
                        .lore(player.hasPermission(PermissionNodes.TOWNY_COMMAND_RESIDENT_SPAWN.getNode())
                                ? of("resident-res-spawn").component(locale)
                                : of("msg-no-permission").component(locale))
                        .action(!player.hasPermission(PermissionNodes.TOWNY_COMMAND_RESIDENT_SPAWN.getNode()) ? ClickAction.NONE : ClickAction.confirmation(() -> of("msg-click-to-confirm", "/resident spawn").component(locale), ClickAction.run(() -> {
                            if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_RESIDENT_SPAWN.getNode()))
                                return;

                            Resident resident = TownyAPI.getInstance().getResident(player);
                            if (resident == null)
                                return;

                            try {
                                SpawnUtil.sendToTownySpawn(player, new String[]{}, resident, of("msg_err_cant_afford_tp").forLocale(player), false, true, SpawnType.RESIDENT);
                            } catch (TownyException e) {
                                TownyMessaging.sendErrorMsg(player, e.getMessage(player));
                            }

                            player.closeInventory();
                        })))
                        .slot(15)
                        .build())
                .addItem(MenuHelper.backButton().build())
                .build();
    }

    public static MenuItem.Builder formatResidentInfo(@NotNull UUID uuid) {
        return formatResidentInfo(uuid, null);
    }

    public static MenuItem.Builder formatResidentInfo(@NotNull UUID uuid, @Nullable Player viewer) {
        return formatResidentInfo(TownyAPI.getInstance().getResident(uuid), viewer);
    }

    public static MenuItem.Builder formatResidentInfo(@Nullable Resident resident) {
        return formatResidentInfo(resident, null);
    }

    /**
     * @param resident The resident to format
     * @return A formatted menu item, or an 'error' item if the resident isn't registered.
     */
    public static MenuItem.Builder formatResidentInfo(@Nullable Resident resident, @Nullable Player viewer) {
        final Locale locale = Localization.localeOrDefault(viewer);

        if (resident == null)
            return MenuItem.builder(Material.PLAYER_HEAD)
                    .name(of("msg-error").component(locale))
                    .lore(of("resident-info-unknown").component(locale));

        List<Component> lore = new ArrayList<>();

        Player player = resident.getPlayer();
        lore.add(of("resident-info-status").component(locale).append(player != null && (viewer == null || viewer.canSee(player)) ? of("status-online").component(locale) : of("status-offline").component(locale)));

        if (resident.hasTown()) {
            lore.add(of(resident.isMayor() ? "mayor_sing" : "res_sing").append(of("resident-info-of")).component(locale).color(NamedTextColor.DARK_GREEN).append(Component.text(resident.getTownOrNull().getName(), NamedTextColor.GREEN)));

            if (resident.hasNation())
                lore.add(of(resident.isKing() ? "king_sing" : "resident-info-member").append(of("resident-info-of")).component(locale).color(NamedTextColor.DARK_GREEN).append(Component.text(resident.getNationOrNull().getName(), NamedTextColor.GREEN)));
        }

        if (TownySettings.isEconomyAsync() && TownyEconomyHandler.isActive())
            lore.add(of("resident-info-balance").component(locale).append(Component.text(TownyEconomyHandler.getFormattedBalance(resident.getAccount().getCachedBalance()), NamedTextColor.GREEN)));

        lore.add(of("resident-info-registered").component(locale).append(Component.text(Time.formatRegistered(resident.getRegistered()), NamedTextColor.GREEN)));

        if (!resident.isOnline())
            lore.add(of("resident-info-last-online").component(locale).append(Time.ago(resident.getLastOnline()).component(locale).color(NamedTextColor.GREEN)));

        return MenuItem.builder(Material.PLAYER_HEAD)
                .skullOwner(resident.getUUID())
                .name(Component.text(resident.getName(), NamedTextColor.GREEN))
                .lore(lore);
    }

    public static List<MenuItem> formatFriendsView(@NotNull Player player) {
        final Locale locale = Localization.localeOrDefault(player);
        final Resident resident = TownyAPI.getInstance().getResident(player);

        if (resident == null || resident.getFriends().size() == 0)
            return Collections.singletonList(MenuItem.builder(Material.BARRIER)
                    .name(of("msg-error").component(locale))
                    .lore(of("resident-menu-no-friends").component(locale))
                    .build());

        List<MenuItem> friends = new ArrayList<>();
        for (Resident friend : resident.getFriends())
            friends.add(formatResidentInfo(friend.getUUID(), player)
                    .lore(of("resident-menu-remove-friend").locale(player).component())
                    .action(ClickAction.rightClick(ClickAction.confirmation(() -> of("resident-menu-remove-friend-confirm", friend.getName()).locale(player).component(), ClickAction.run(() -> {
                        if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_RESIDENT_FRIEND.getNode())) {
                            TownyMessaging.sendErrorMsg(player, of("msg_err_command_disable"));
                            return;
                        }

                        if (!resident.hasFriend(friend))
                            return;

                        ResidentCommand.residentFriendRemove(player, resident, Collections.singletonList(friend));

                        // Re-open resident friends menu
                        MenuHistory.reOpen(player, () -> formatResidentFriends(player));
                    }))))
                    .build());

        return friends;
    }

    private static MenuInventory formatResidentFriends(Player player) {
        final Locale locale = Localization.localeOrDefault(player);

        final MenuInventory.PaginatorBuilder builder = MenuInventory.paginator()
                .title(of("resident-menu-friends-title").component(locale))
                .addItems(formatFriendsView(player));

        if (player.hasPermission(PermissionNodes.TOWNY_COMMAND_RESIDENT_FRIEND.getNode())) {
            builder.addExtraItem(MenuItem.builder(Material.WRITABLE_BOOK)
                    .name(of("resident-menu-add-friend").component(locale))
                    .lore(of("resident-menu-add-friend-subtitle").component(locale))
                    .slot(SlotAnchor.anchor(VerticalAnchor.fromBottom(0), HorizontalAnchor.fromLeft(1)))
                    .action(ClickAction.userInput(of("resident-menu-add-friend-prompt").translate(locale), name -> {
                        if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_RESIDENT_FRIEND.getNode()))
                            return AnvilGUI.Response.close();

                        Resident resident = TownyAPI.getInstance().getResident(player);
                        if (resident == null)
                            return UserInputAction.text(of("msg_err_not_registered").translate(locale));

                        Resident friend = TownyAPI.getInstance().getResident(name);
                        if (friend == null || friend.isNPC() || friend.getUUID().equals(resident.getUUID()))
                            return UserInputAction.text(of("resident-menu-add-friend-invalid", name).translate(locale));

                        if (resident.hasFriend(friend))
                            return UserInputAction.text(of("resident-menu-add-friend-already-friend", friend.getName()).translate(locale));

                        List<Resident> friends = new ArrayList<>();
                        friends.add(friend);

                        ResidentCommand.residentFriendAdd(player, resident, friends);

                        // Re-open resident friends menu
                        return UserInputAction.reOpen(player, () -> formatResidentFriends(player));
                    }))
                    .build());
        }

        return builder.build();
    }
}
