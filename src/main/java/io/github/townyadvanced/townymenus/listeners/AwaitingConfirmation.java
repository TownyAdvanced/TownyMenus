package io.github.townyadvanced.townymenus.listeners;

import io.github.townyadvanced.townymenus.utils.Components;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import io.papermc.paper.util.Tick;
import net.kyori.adventure.text.format.NamedTextColor;
import com.palmergames.bukkit.towny.confirmations.ConfirmationHandler;
import com.palmergames.bukkit.towny.confirmations.event.ConfirmationSendEvent;
import io.github.townyadvanced.townymenus.TownyMenus;
import io.github.townyadvanced.townymenus.gui.MenuHelper;
import io.github.townyadvanced.townymenus.gui.MenuHistory;
import io.github.townyadvanced.townymenus.gui.action.ClickAction;
import io.github.townyadvanced.townymenus.utils.Localization;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class AwaitingConfirmation implements Listener {
    private static final Map<UUID, ScheduledTask> awaitingConfirmation = new ConcurrentHashMap<>();

    @EventHandler(ignoreCancelled = true)
    public void onConfirmationSend(ConfirmationSendEvent event) {
        if (!(event.getSender() instanceof Player player))
            return;

        final ScheduledTask scheduledTask = awaitingConfirmation.remove(player.getUniqueId());
        if (scheduledTask == null)
            return;

        scheduledTask.cancel();

        event.setSendMessage(false);
        player.getScheduler().runDelayed(TownyMenus.getPlugin(), task -> {
            MenuHelper.createConfirmation(Components.toNative(event.getConfirmation().getTitle().component(Localization.localeOrDefault(player))).colorIfAbsent(NamedTextColor.GRAY), ClickAction.run(() -> {
                ConfirmationHandler.acceptConfirmation(player);
                MenuHistory.last(player);
            }), ClickAction.run(() -> MenuHistory.last(player)))
                    .openSilent(player);
        }, () -> {}, 3L);
    }

    public static void await(Player player) {
        final UUID uuid = player.getUniqueId();

        awaitingConfirmation.put(uuid, Bukkit.getServer().getAsyncScheduler().runDelayed(TownyMenus.getPlugin(), task -> awaitingConfirmation.remove(uuid), Tick.of(5L).toMillis(), TimeUnit.MILLISECONDS));
    }
}
