package io.github.townyadvanced.townymenus.utils;

import io.github.townyadvanced.townymenus.TownyMenus;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MenuScheduler {
    private static final Set<UUID> hasRunningTask = Collections.synchronizedSet(new HashSet<>());

    public static void scheduleAsync(HumanEntity entity, Runnable runnable) {
        scheduleAsync(entity.getUniqueId(), runnable);
    }

    public static void scheduleAsync(UUID uuid, Runnable runnable) {
        // Prevent unwanted behaviour by only allowing 1 async task per player at a time
        if (hasRunningTask.contains(uuid))
            return;

        Bukkit.getScheduler().runTaskAsynchronously(TownyMenus.getPlugin(), () -> {
            hasRunningTask.add(uuid);

            try {
                runnable.run();
            } finally {
                hasRunningTask.remove(uuid);
            }
        });
    }
}
