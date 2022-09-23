package io.github.townyadvanced.townymenus.listeners;

import io.github.townyadvanced.townymenus.gui.MenuHistory;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        MenuHistory.clearHistory(event.getPlayer().getUniqueId());
    }
}
