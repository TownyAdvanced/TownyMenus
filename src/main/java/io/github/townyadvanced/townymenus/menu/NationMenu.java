package io.github.townyadvanced.townymenus.menu;

import com.palmergames.adventure.text.Component;
import com.palmergames.adventure.text.format.NamedTextColor;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.command.NationCommand;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import io.github.townyadvanced.townymenus.gui.MenuInventory;
import io.github.townyadvanced.townymenus.gui.MenuItem;
import io.github.townyadvanced.townymenus.gui.action.ClickAction;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class NationMenu {
    public static MenuInventory createNationMenu(@NotNull Player player) {
        Nation nation = Optional.ofNullable(TownyAPI.getInstance().getTown(player)).map(Town::getNationOrNull).orElse(null);

        return MenuInventory.builder()
                .title(Component.text("Nation Menu"))
                .rows(6)
                .addItem(MenuItem.builder(Material.WRITABLE_BOOK)
                        .name(Component.text("Transaction History", NamedTextColor.GREEN))
                        .slot(0)
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
                        .slot(1)
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
                .build();
    }
}
