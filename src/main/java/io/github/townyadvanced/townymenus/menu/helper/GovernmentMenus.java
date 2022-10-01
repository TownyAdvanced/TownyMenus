package io.github.townyadvanced.townymenus.menu.helper;

import com.palmergames.adventure.text.Component;
import com.palmergames.adventure.text.format.NamedTextColor;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.command.NationCommand;
import com.palmergames.bukkit.towny.command.TownCommand;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Government;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import io.github.townyadvanced.townymenus.gui.MenuHistory;
import io.github.townyadvanced.townymenus.gui.MenuItem;
import io.github.townyadvanced.townymenus.gui.action.ClickAction;
import io.github.townyadvanced.townymenus.menu.NationMenu;
import io.github.townyadvanced.townymenus.menu.TownMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class GovernmentMenus {
    public static MenuItem.Builder createTogglePropertyItem(Player player, Government government, Material material, boolean propertyEnabled, String property) {
        if (!governmentExists(government))
            return MenuItem.builder(Material.BARRIER).name(Component.text("Invalid Government", NamedTextColor.GREEN));

        final boolean isTown = government instanceof Town;
        final String townOrNation = isTown ? "town" : "nation";
        final String permNode = String.format("towny.command.%s.toggle.%s", townOrNation, property);

        return MenuItem.builder(material)
                .name(Component.text("Toggle " + property.substring(0, 1).toUpperCase(Locale.ROOT) + property.substring(1), propertyEnabled ? NamedTextColor.GREEN : NamedTextColor.RED))
                .lore(() -> {
                    if (getGovernment(player, isTown) == null)
                        return Component.text("You are not in a " + townOrNation + ".", NamedTextColor.GRAY);
                    else if (!player.hasPermission(permNode))
                        return Component.text("You do not have permission to toggle " + property + ".", NamedTextColor.GRAY);
                    else
                        return Component.text(String.format("Click to %s %s.", propertyEnabled ? "disable" : "enable", property), NamedTextColor.GRAY);
                })
                .action(!player.hasPermission(permNode) ? ClickAction.NONE : ClickAction.confirmation(Component.text("Are you sure you want to toggle " + property + " in your " + townOrNation + "?", NamedTextColor.GRAY), ClickAction.run(() -> {
                    Government playerGovernment = getGovernment(player, isTown);
                    if (playerGovernment == null)
                        return;

                    try {
                        if (playerGovernment instanceof Town town)
                            TownCommand.townToggle(player, new String[]{property}, false, town);
                        else if (playerGovernment instanceof Nation nation)
                            NationCommand.nationToggle(player, new String[]{property}, false, nation);
                    } catch (TownyException e) {
                        TownyMessaging.sendErrorMsg(player, e.getMessage(player));
                    }

                    MenuHistory.reOpen(player, () -> isTown ? TownMenu.formatTownToggleMenu(player) : NationMenu.formatNationToggleMenu(player));
                })));
    }

    private static boolean governmentExists(Government government) {
        if (government instanceof Town town)
            return TownyUniverse.getInstance().hasTown(town.getUUID());
        else if (government instanceof Nation nation)
            return TownyUniverse.getInstance().hasNation(nation.getUUID());

        return false;
    }

    private static boolean hasGovernment(Player player, Government government) {
        if (!governmentExists(government))
            return false;

        Government playerGovernment = getGovernment(player, government instanceof Town);
        return playerGovernment != null && playerGovernment.getUUID().equals(government.getUUID());
    }

    @Nullable
    private static Government getGovernment(Player player, boolean town) {
        return town ? TownyAPI.getInstance().getTown(player) : TownyAPI.getInstance().getNation(player);
    }
}
