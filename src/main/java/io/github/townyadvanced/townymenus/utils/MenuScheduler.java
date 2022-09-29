package io.github.townyadvanced.townymenus.utils;

import org.bukkit.entity.HumanEntity;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class MenuScheduler {
    private static final Set<UUID> hasRunningTask = ConcurrentHashMap.newKeySet();

    public static void scheduleAsync(HumanEntity entity, Runnable runnable) {
        scheduleAsync(entity.getUniqueId(), runnable);
    }

    public static void scheduleAsync(UUID uuid, Runnable runnable) {
        // Prevent unwanted behaviour by only allowing 1 async task per player at a time
        if (hasRunningTask.contains(uuid))
            return;

        hasRunningTask.add(uuid);

        CompletableFuture.runAsync(runnable).thenRun(() -> hasRunningTask.remove(uuid));
    }
}
