package io.github.townyadvanced.townymenus.listeners;

import net.kyori.adventure.text.format.NamedTextColor;
import com.palmergames.bukkit.towny.confirmations.ConfirmationHandler;
import com.palmergames.bukkit.towny.confirmations.event.ConfirmationSendEvent;
import com.palmergames.bukkit.towny.scheduling.ScheduledTask;
import io.github.townyadvanced.townymenus.TownyMenus;
import io.github.townyadvanced.townymenus.gui.MenuHelper;
import io.github.townyadvanced.townymenus.gui.MenuHistory;
import io.github.townyadvanced.townymenus.gui.action.ClickAction;
import io.github.townyadvanced.townymenus.utils.Localization;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AwaitingConfirmation implements Listener {
    private static final Map<UUID, ScheduledTask> awaitingConfirmation = new ConcurrentHashMap<>();

    @EventHandler(ignoreCancelled = true)
    public void onConfirmationSend(ConfirmationSendEvent event) {
        if (!(event.getSender() instanceof Player player))
            return;

        ScheduledTask task = awaitingConfirmation.remove(player.getUniqueId());
        if (task == null)
            return;

        task.cancel();

        event.setSendMessage(false);
        TownyMenus.getPlugin().getScheduler().runLater(() -> {
            MenuHelper.createConfirmation(event.getConfirmation().getTitle().component(Localization.localeOrDefault(player)).colorIfAbsent(NamedTextColor.GRAY), ClickAction.run(() -> {
                ConfirmationHandler.acceptConfirmation(player);
                MenuHistory.last(player);
            }), ClickAction.run(() -> MenuHistory.last(player)))
                    .openSilent(player);
        }, 3L);
    }

    public static void await(Player player) {
        awaitingConfirmation.put(player.getUniqueId(), TownyMenus.getPlugin().getScheduler().runLater(() -> awaitingConfirmation.remove(player.getUniqueId()), 5L));
    }
}
