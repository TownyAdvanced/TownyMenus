package io.github.townyadvanced.townymenus.commands;

import io.github.townyadvanced.townymenus.TownyMenus;
import io.github.townyadvanced.townymenus.gui.MenuHistory;
import io.github.townyadvanced.townymenus.gui.MenuInventory;
import io.github.townyadvanced.townymenus.gui.MenuItem;
import io.github.townyadvanced.townymenus.gui.action.ClickAction;
import io.github.townyadvanced.townymenus.menu.PlotMenu;
import io.github.townyadvanced.townymenus.menu.ResidentMenu;
import io.github.townyadvanced.townymenus.menu.TownMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
            sender.sendMessage(Component.text("This command cannot be used by console!", NamedTextColor.RED));
            return true;
        }

        MenuHistory.clearHistory(player.getUniqueId());

        MenuInventory.builder()
                .rows(3)
                .title(Component.text("Towny Menu"))
                .addItem(MenuItem.builder(Material.EMERALD)
                        .name(Component.text("Town Settings", NamedTextColor.GREEN))
                        .lore(Component.text("Click to view the town menu!", NamedTextColor.GRAY))
                        .slot(10)
                        .action(ClickAction.openInventory(TownMenu.createTownMenu(player)))
                        .build())
                .addItem(MenuItem.builder(Material.DIAMOND)
                        .name(Component.text("Nation Settings", NamedTextColor.AQUA))
                        .lore(Component.text("Click to view the nation menu!", NamedTextColor.GRAY))
                        .slot(12)
                        .action(ClickAction.close())
                        .build())
                .addItem(MenuItem.builder(Material.GRASS_BLOCK)
                        .name(Component.text("Plot Settings", NamedTextColor.DARK_GREEN))
                        .lore(Component.text("Click to view the plot menu!", NamedTextColor.GRAY))
                        .slot(14)
                        .action(ClickAction.openInventory(() -> PlotMenu.createPlotMenu(player)))
                        .build())
                .addItem(MenuItem.builder(Material.PLAYER_HEAD)
                        .skullOwner(player.getUniqueId())
                        .name(Component.text("Resident Settings", NamedTextColor.YELLOW))
                        .lore(Component.text("Click to view the resident menu!", NamedTextColor.GRAY))
                        .slot(16)
                        .action(ClickAction.openInventory(ResidentMenu.createResidentMenu(player)))
                        .build())
                .build()
                .open(player);

        return true;
    }
}
