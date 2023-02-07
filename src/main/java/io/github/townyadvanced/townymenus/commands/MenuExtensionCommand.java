package io.github.townyadvanced.townymenus.commands;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.object.Translatable;
import io.github.townyadvanced.townymenus.gui.MenuHistory;
import io.github.townyadvanced.townymenus.gui.MenuInventory;
import io.github.townyadvanced.townymenus.utils.MenuScheduler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * Used for Towny addon commands.
 */
public class MenuExtensionCommand implements CommandExecutor {
    private final Function<Player, MenuInventory> menuInventoryFunction;

    public MenuExtensionCommand(Function<Player, MenuInventory> menuInventoryFunction) {
        this.menuInventoryFunction = menuInventoryFunction;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            TownyMessaging.sendErrorMsg(sender, "This command cannot be used by console!");
            return true;
        }

        if (!player.hasPermission("townymenus.command.townymenu")) {
            TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_command_disable"));
            return true;
        }

        MenuScheduler.scheduleAsync(player.getUniqueId(), () -> {
            MenuHistory.clearHistory(player.getUniqueId());

            MenuHistory.addHistory(player.getUniqueId(), TownyMenuCommand.createRootMenu(player));
            menuInventoryFunction.apply(player).open(player);
        });

        return true;
    }
}
