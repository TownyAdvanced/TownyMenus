package io.github.townyadvanced.townymenus.listeners;

import com.palmergames.adventure.text.format.NamedTextColor;
import com.palmergames.bukkit.towny.confirmations.ConfirmationHandler;
import com.palmergames.bukkit.towny.confirmations.event.ConfirmationSendEvent;
import io.github.townyadvanced.townymenus.TownyMenus;
import io.github.townyadvanced.townymenus.gui.MenuHelper;
import io.github.townyadvanced.townymenus.gui.MenuHistory;
import io.github.townyadvanced.townymenus.gui.action.ClickAction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AwaitingConfirmation implements Listener {
    private static final Map<UUID, Integer> awaitingConfirmation = new ConcurrentHashMap<>();

    @EventHandler(ignoreCancelled = true)
    public void onConfirmationSend(ConfirmationSendEvent event) {
        if (!(event.getSender() instanceof Player player))
            return;

        Integer taskID = awaitingConfirmation.remove(player.getUniqueId());
        if (taskID == null)
            return;

        Bukkit.getScheduler().cancelTask(taskID);

        event.setSendMessage(false);
        Bukkit.getScheduler().runTaskLater(TownyMenus.getPlugin(), () -> {
            MenuHelper.createConfirmation(event.getConfirmation().getTitle().locale(player).component().colorIfAbsent(NamedTextColor.GRAY), ClickAction.run(() -> {
                ConfirmationHandler.acceptConfirmation(player);
                MenuHistory.last(player);
            }), ClickAction.run(() -> MenuHistory.last(player)))
                    .openSilent(player);
        }, 3L);
    }

    public static void await(Player player) {
        awaitingConfirmation.put(player.getUniqueId(), Bukkit.getScheduler().runTaskLater(TownyMenus.getPlugin(), () -> awaitingConfirmation.remove(player.getUniqueId()), 5L).getTaskId());
    }
}
