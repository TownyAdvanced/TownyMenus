package io.github.townyadvanced.townymenus.commands.addon;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.object.Translatable;
import io.github.townyadvanced.townymenus.commands.TownyMenuCommand;
import io.github.townyadvanced.townymenus.gui.MenuHistory;
import io.github.townyadvanced.townymenus.menu.TownMenu;
import io.github.townyadvanced.townymenus.utils.MenuScheduler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TownAddonCommand implements CommandExecutor {
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
            TownMenu.createTownMenu(player).open(player);
        });

        return true;
    }
}
