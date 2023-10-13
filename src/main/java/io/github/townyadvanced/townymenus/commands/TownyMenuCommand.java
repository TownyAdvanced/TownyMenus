package io.github.townyadvanced.townymenus.commands;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.object.Translatable;
import io.github.townyadvanced.townymenus.TownyMenus;
import io.github.townyadvanced.townymenus.gui.MenuHistory;
import io.github.townyadvanced.townymenus.gui.MenuInventory;
import io.github.townyadvanced.townymenus.gui.MenuItem;
import io.github.townyadvanced.townymenus.gui.action.ClickAction;
import io.github.townyadvanced.townymenus.menu.NationMenu;
import io.github.townyadvanced.townymenus.menu.PlotMenu;
import io.github.townyadvanced.townymenus.menu.ResidentMenu;
import io.github.townyadvanced.townymenus.menu.TownMenu;
import io.github.townyadvanced.townymenus.utils.Localization;
import io.github.townyadvanced.townymenus.utils.MenuScheduler;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class TownyMenuCommand implements CommandExecutor, TabCompleter {
    private final TownyMenus plugin;

    public TownyMenuCommand(TownyMenus plugin) {
        this.plugin = plugin;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return Collections.emptyList();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            TownyMessaging.sendErrorMsg(sender, "This command cannot be used by console!");
            return true;
        }

        if (Towny.getPlugin().isError()) {
            TownyMessaging.sendErrorMsg(player, "You cannot use this command while Towny is locked in safe mode.");
            return true;
        }

        if (!player.hasPermission("townymenus.command.townymenu")) {
            TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_command_disable"));
            return true;
        }

        MenuHistory.clearHistory(player.getUniqueId());
        MenuScheduler.scheduleAsync(player.getUniqueId(), () -> createRootMenu(player).open(player));

        return true;
    }

    public static MenuInventory createRootMenu(Player player) {
        final Locale locale = Localization.localeOrDefault(player);

        return MenuInventory.builder()
                .rows(3)
                .title(Component.translatable("main-menu-title"))
                .addItem(MenuItem.builder(Material.EMERALD)
                        .name(Component.translatable("main-menu-town"))
                        .lore(Component.translatable("main-menu-town-subtitle"))
                        .slot(10)
                        .action(ClickAction.openInventory(() -> TownMenu.createTownMenu(player)))
                        .build())
                .addItem(MenuItem.builder(Material.DIAMOND)
                        .name(Component.translatable("main-menu-nation"))
                        .lore(Component.translatable("main-menu-nation-subtitle"))
                        .slot(12)
                        .action(ClickAction.openInventory(() -> NationMenu.createNationMenu(player)))
                        .build())
                .addItem(MenuItem.builder(Material.GRASS_BLOCK)
                        .name(Component.translatable("main-menu-plot"))
                        .lore(Component.translatable("main-menu-plot-subtitle"))
                        .slot(14)
                        .action(ClickAction.openInventory(() -> PlotMenu.createPlotMenu(player)))
                        .build())
                .addItem(MenuItem.builder(Material.PLAYER_HEAD)
                        .skullOwner(player.getUniqueId())
                        .name(Component.translatable("main-menu-resident"))
                        .lore(Component.translatable("main-menu-resident-subtitle"))
                        .slot(16)
                        .action(ClickAction.openInventory(() -> ResidentMenu.createResidentMenu(player)))
                        .build())
                .build();
    }
}
