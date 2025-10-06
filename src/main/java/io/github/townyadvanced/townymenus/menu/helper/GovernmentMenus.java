package io.github.townyadvanced.townymenus.menu.helper;

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
import com.palmergames.util.MathUtil;
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
import io.github.townyadvanced.townymenus.gui.input.response.InputResponse;
import io.github.townyadvanced.townymenus.utils.Localization;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Locale;

import static com.palmergames.bukkit.towny.object.Translatable.*;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public class GovernmentMenus {
    public static MenuItem.Builder createTogglePropertyItem(Player player, Government government, Material material, boolean propertyEnabled, String property) {
        final Locale locale = Localization.localeOrDefault(player);

        if (!governmentExists(government))
            return MenuItem.builder(Material.BARRIER).name(of("government-menus-invalid").component(locale));

        final boolean isTown = government instanceof Town;
        final String townOrNation = isTown ? "town" : "nation";
        final String permNode = String.format("towny.command.%s.toggle.%s", townOrNation, property);

        return MenuItem.builder(material)
                .name(of("government-menus-toggle").append(" ").append(property.substring(0, 1).toUpperCase(Locale.ROOT) + property.substring(1)).component(locale).color(propertyEnabled ? GREEN : RED))
                .lore(() -> {
                    if (getGovernment(player, isTown) == null)
                        return of("government-menus-not-in").append(townOrNation + ".").component(locale).color(GRAY);
                    else if (!player.hasPermission(permNode))
                        return of("government-menus-no-permission").append(property + ".").component(locale).color(GRAY);
                    else
                        return of("msg-click-to").append(propertyEnabled ? of("government-menus-disable") : of("government-menus-enable")).append(" " + property + ".").component(locale).color(GRAY);
                })
                .action(!player.hasPermission(permNode) ? ClickAction.NONE : ClickAction.confirmation(of("government-menus-toggle-confirm").append(property).append(of("government-menus-in-your")).append(townOrNation + "?").component(locale), ClickAction.run(() -> {
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
        final Locale locale = Localization.localeOrDefault(player);

        if (!governmentExists(government))
            return MenuInventory.builder().rows(1)
                    .addItem(MenuHelper.backButton().build())
                    .addItem(MenuItem.builder(Material.BARRIER).name(of("government-menus-invalid").component(locale)).build())
                    .build();

        final PermissionNodes root = government instanceof Town ? PermissionNodes.TOWNY_COMMAND_TOWN : PermissionNodes.TOWNY_COMMAND_NATION;

        return MenuInventory.builder()
                .title(of("government-menus-deposit-or-withdraw").component(locale))
                .rows(3)
                .addItem(MenuHelper.backButton().build())
                .addItem(MenuItem.builder(Material.EMERALD)
                        .name(of("government-menus-deposit").component(locale))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(2)))
                        .lore(() -> {
                            if (!player.hasPermission(root.getNode("deposit")))
                                return of("msg-no-permission-to").append(of("government-menus-deposit-into-bank")).component(locale).color(GRAY);
                            else
                                return of("msg-click-to").append(of("government-menus-deposit-into-bank")).component(locale).color(GRAY);
                        })
                        .action(!player.hasPermission(root.getNode("deposit")) ? ClickAction.NONE : depositOrWithdraw(player, government, false))
                        .build())
                .addItem(MenuItem.builder(Material.REDSTONE)
                        .name(of("government-menus-withdraw").component(locale))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromRight(2)))
                        .lore(() -> {
                            if (!player.hasPermission(root.getNode("withdraw")))
                                return of("msg-no-permission-to").append(of("government-menus-withdraw-from-bank")).component(locale).color(GRAY);
                            else
                                return of("msg-click-to").append(of("government-menus-withdraw-from-bank")).component(locale).color(GRAY);
                        })
                        .action(!player.hasPermission(root.getNode("withdraw")) ? ClickAction.NONE : depositOrWithdraw(player, government, true))
                        .build())
                .build();
    }

    private static UserInputAction depositOrWithdraw(final Player player, final Government government, boolean withdraw) {
        final Locale locale = Localization.localeOrDefault(player);

        return ClickAction.userInput(of("government-menus-input").append(withdraw ? of("government-menus-input-withdraw") : of("government-menus-input-deposit")).append(of("government-menus-input-amount")).stripColors(true).translate(locale), completion -> {
            try {
                MathUtil.getIntOrThrow(completion.getText());
            } catch (TownyException e) {
                return InputResponse.errorMessage(e.getMessage(player));
            }

            boolean town = government instanceof Town;
            if (!player.hasPermission(town ? PermissionNodes.TOWNY_COMMAND_TOWN_DEPOSIT.getNode() : PermissionNodes.TOWNY_COMMAND_NATION_DEPOSIT.getNode()))
                return InputResponse.finish();

            Class<?> clazz = town ? TownCommand.class : NationCommand.class;

            try {
                Method method = clazz.getDeclaredMethod(town ? "townTransaction" : "nationTransaction", Player.class, String[].class, boolean.class);
                method.setAccessible(true);
                method.invoke(null, player, new String[]{completion.getText()}, withdraw);

                MenuHistory.last(player);
                return InputResponse.doNothing();
            } catch (ReflectiveOperationException e) {
                return InputResponse.finish();
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
