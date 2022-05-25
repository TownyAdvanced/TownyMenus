package io.github.townyadvanced.townymenus.gui;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Used for tracking what menus a player has opened in their current 'session'.
 */
public class MenuHistory {
    private static final Map<UUID, List<MenuInventory>> historyMap = new ConcurrentHashMap<>();

    @Nullable
    public static List<MenuInventory> getHistory(UUID uuid) {
        return historyMap.get(uuid);
    }

    public static void clearHistory(UUID uuid) {
        historyMap.remove(uuid);
    }

    public static void addHistory(UUID uuid, MenuInventory menuInventory) {
        historyMap.computeIfAbsent(uuid, k -> new ArrayList<>()).add(menuInventory);
    }

    /**
     * Uses history to open the previous menu (if possible).
     * @param player The player to open the previous menu for.
     */
    public static void back(Player player) {
        List<MenuInventory> history = historyMap.get(player.getUniqueId());

        // Player has no history or not enough history
        if (history == null || history.size() < 2)
            return;

        // Remove the current inventory from history
        history.remove(history.size() - 1);

        // Open the last inventory without adding to history.
        history.get(history.size() - 1).openSilent(player);
    }

    /**
     * Re-opens the last menu for this player.
     * @param player The player to open the last menu for.
     */
    public static void last(Player player) {
        List<MenuInventory> history = historyMap.get(player.getUniqueId());

        if (history == null || history.isEmpty())
            return;

        history.get(history.size() - 1).openSilent(player);
    }

    public static void pop(UUID uuid) {
        List<MenuInventory> history = historyMap.get(uuid);

        if (history == null || history.isEmpty())
            return;

        history.remove(history.size() - 1);
    }
}
