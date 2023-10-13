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
import io.github.townyadvanced.townymenus.utils.AnvilResponse;
import io.github.townyadvanced.townymenus.utils.Localization;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Locale;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

/**
 * Shared code for towns and nations.
 */
public class GovernmentMenus {
    public static MenuItem.Builder createTogglePropertyItem(Player player, Government government, Material material, boolean propertyEnabled, String property) {
        final Locale locale = Localization.localeOrDefault(player);

        if (!governmentExists(government))
            return MenuItem.builder(Material.BARRIER).name(Component.translatable("government-menus-invalid"));

        final boolean isTown = government instanceof Town;
        final String townOrNation = isTown ? "town" : "nation";
        final String permNode = String.format("towny.command.%s.toggle.%s", townOrNation, property);

        return MenuItem.builder(material)
                .name(Component.translatable("government-menus-toggle").appendSpace().append(text(property.substring(0, 1).toUpperCase(Locale.ROOT) + property.substring(1))).color(propertyEnabled ? GREEN : RED))
                .lore(() -> {
                    if (getGovernment(player, isTown) == null)
                        return Component.translatable("government-menus-not-in").append(text(townOrNation + ".")).color(GRAY);
                    else if (!player.hasPermission(permNode))
                        return Component.translatable("government-menus-no-permission").append(text(property + ".")).color(GRAY);
                    else
                        return Component.translatable("msg-click-to").append(propertyEnabled ? Component.translatable("government-menus-disable") : Component.translatable("government-menus-enable")).append(text(" " + property + ".")).color(GRAY);
                })
                .action(!player.hasPermission(permNode) ? ClickAction.NONE : ClickAction.confirmation(Component.translatable("government-menus-toggle-confirm").append(text(property)).append(Component.translatable("government-menus-in-your")).append(text(townOrNation + "?")), ClickAction.run(() -> {
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
            return MenuInventory.builder()
                    .rows(1)
                    .addItem(MenuHelper.backButton().build())
                    .addItem(MenuItem.builder(Material.BARRIER).name(Component.translatable("government-menus-invalid")).build())
                    .build();

        final PermissionNodes root = government instanceof Town ? PermissionNodes.TOWNY_COMMAND_TOWN : PermissionNodes.TOWNY_COMMAND_NATION;

        return MenuInventory.builder()
                .title(Component.translatable("government-menus-deposit-or-withdraw"))
                .rows(3)
                .addItem(MenuHelper.backButton().build())
                .addItem(MenuItem.builder(Material.EMERALD)
                        .name(Component.translatable("government-menus-deposit"))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromLeft(2)))
                        .lore(() -> {
                            if (!player.hasPermission(root.getNode("deposit")))
                                return Component.translatable("msg-no-permission-to").append(Component.translatable("government-menus-deposit-into-bank")).color(GRAY);
                            else
                                return Component.translatable("msg-click-to").append(Component.translatable("government-menus-deposit-into-bank")).color(GRAY);
                        })
                        .action(!player.hasPermission(root.getNode("deposit")) ? ClickAction.NONE : depositOrWithdraw(player, government, false))
                        .build())
                .addItem(MenuItem.builder(Material.REDSTONE)
                        .name(Component.translatable("government-menus-withdraw"))
                        .slot(SlotAnchor.anchor(VerticalAnchor.fromTop(1), HorizontalAnchor.fromRight(2)))
                        .lore(() -> {
                            if (!player.hasPermission(root.getNode("withdraw")))
                                return Component.translatable("msg-no-permission-to").append(Component.translatable("government-menus-withdraw-from-bank")).color(GRAY);
                            else
                                return Component.translatable("msg-click-to").append(Component.translatable("government-menus-withdraw-from-bank")).color(GRAY);
                        })
                        .action(!player.hasPermission(root.getNode("withdraw")) ? ClickAction.NONE : depositOrWithdraw(player, government, true))
                        .build())
                .build();
    }

    private static UserInputAction depositOrWithdraw(final Player player, final Government government, boolean withdraw) {
        final Locale locale = Localization.localeOrDefault(player);
        final String title = PlainTextComponentSerializer.plainText().serialize(GlobalTranslator.render(
                Component.translatable("government-menus-input").append(withdraw ? Component.translatable("government-menus-input-withdraw") : Component.translatable("government-menus-input-deposit")).append(Component.translatable("government-menus-input-amount")), locale));

        return ClickAction.userInput(title, completion -> {
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
                method.invoke(null, player, new String[]{completion.getText()}, withdraw);

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
