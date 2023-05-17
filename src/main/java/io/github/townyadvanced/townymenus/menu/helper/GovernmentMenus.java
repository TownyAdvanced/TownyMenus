package io.github.townyadvanced.townymenus.menu.helper;

import com.palmergames.adventure.text.Component;
import com.palmergames.adventure.text.format.NamedTextColor;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.command.NationCommand;
import com.palmergames.bukkit.towny.command.TownCommand;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Government;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.util.Version;
import com.palmergames.util.MathUtil;
import com.palmergames.util.StringMgmt;
import io.github.townyadvanced.townymenus.gui.MenuHelper;
import io.github.townyadvanced.townymenus.gui.MenuHistory;
import io.github.townyadvanced.townymenus.gui.MenuInventory;
import io.github.townyadvanced.townymenus.gui.MenuItem;
import io.github.townyadvanced.townymenus.gui.action.ClickAction;
import io.github.townyadvanced.townymenus.gui.action.UserInputAction;
import io.github.townyadvanced.townymenus.gui.slot.anchor.HorizontalAnchor;
import io.github.townyadvanced.townymenus.gui.slot.anchor.SlotAnchor;
import io.github.townyadvanced.townymenus.gui.slot.anchor.VerticalAnchor;
import io.github.townyadvanced.townymenus.menu.NationMenu;
import io.github.townyadvanced.townymenus.menu.TownMenu;
import io.github.townyadvanced.townymenus.utils.AnvilResponse;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
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

    public static MenuInventory createDepositWithdrawMenu(final Player player, final Government government) {
        if (!governmentExists(government))
            return MenuInventory.builder().rows(1)
                    .addItem(MenuHelper.backButton().build())
                    .addItem(MenuItem.builder(Material.BARRIER).name(Component.text("Invalid Government", NamedTextColor.GREEN)).build())
                    .build();

        final PermissionNodes root = government instanceof Town ? PermissionNodes.TOWNY_COMMAND_TOWN : PermissionNodes.TOWNY_COMMAND_NATION;

        return MenuInventory.builder()
                .title(Component.text("Deposit or Withdraw"))
                .rows(3)
                .addItem(MenuHelper.backButton().build())
                .addItem(MenuItem.builder(Material.EMERALD)
                        .name(Component.text("Deposit", NamedTextColor.GREEN))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(2)))
                        .lore(() -> {
                            if (!player.hasPermission(root.getNode("deposit")))
                                return Component.text("You do not have permission to deposit into the bank.", NamedTextColor.GRAY);
                            else
                                return Component.text("Click to deposit into the bank.", NamedTextColor.GRAY);
                        })
                        .action(!player.hasPermission(root.getNode("deposit")) ? ClickAction.NONE : depositOrWithdraw(player, government, false))
                        .build())
                .addItem(MenuItem.builder(Material.REDSTONE)
                        .name(Component.text("Withdraw", NamedTextColor.GREEN))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromRight(2)))
                        .lore(() -> {
                            if (!player.hasPermission(root.getNode("withdraw")))
                                return Component.text("You do not have permission to withdraw from the bank.", NamedTextColor.GRAY);
                            else
                                return Component.text("Click to withdraw from the bank.", NamedTextColor.GRAY);
                        })
                        .action(!player.hasPermission(root.getNode("withdraw")) ? ClickAction.NONE : depositOrWithdraw(player, government, true))
                        .build())
                .build();
    }

    private static UserInputAction depositOrWithdraw(final Player player, final Government government, boolean withdraw) {
        return ClickAction.userInput("Enter " + (withdraw ? "withdraw" : "deposit") + " amount", completion -> {
            try {
                MathUtil.getIntOrThrow(completion.getText());
            } catch (TownyException e) {
                return AnvilResponse.text(e.getMessage(player));
            }

            boolean town = government instanceof Town;
            if (!player.hasPermission(town ? PermissionNodes.TOWNY_COMMAND_TOWN_DEPOSIT.getNode() : PermissionNodes.TOWNY_COMMAND_NATION_DEPOSIT.getNode()))
                return AnvilResponse.close();

            Class<?> clazz = town ? TownCommand.class : NationCommand.class;

            try {
                Method method = clazz.getDeclaredMethod(town ? "townTransaction" : "nationTransaction", Player.class, String[].class, boolean.class);
                method.setAccessible(true);

                // This can be removed once the minimum required version is above 0.99.0.8
                String[] args = new String[]{"", completion.getText()};
                if (town || Version.fromString(Towny.getPlugin().getVersion()).compareTo(Version.fromString("0.99.0.8")) >= 0)
                    args = StringMgmt.remFirstArg(args);

                method.invoke(null, player, args, withdraw);

                MenuHistory.last(player);
                return AnvilResponse.nil();
            } catch (ReflectiveOperationException e) {
                return AnvilResponse.close();
            }
        });
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
